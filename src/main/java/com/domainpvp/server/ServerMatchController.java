package com.domainpvp.server;

import com.domainpvp.arena.ArenaBlockEntry;
import com.domainpvp.arena.ArenaInstance;
import com.domainpvp.arena.ArenaManager;
import com.domainpvp.arena.ArenaSpawnPoint;
import com.domainpvp.combat.CombatManager;
import com.domainpvp.config.ModConfig;
import com.domainpvp.loadout.Loadout;
import com.domainpvp.loadout.LoadoutItem;
import com.domainpvp.loadout.LoadoutManager;
import com.domainpvp.match.MatchManager;
import com.domainpvp.match.MatchSession;
import com.domainpvp.match.MatchState;
import com.domainpvp.network.s2c.*;
import com.domainpvp.party.Party;
import com.domainpvp.party.PartyManager;
import com.domainpvp.party.PartyMember;
import com.domainpvp.persistence.PersistenceManager;
import com.domainpvp.player.PlayerSnapshot;
import com.domainpvp.player.PlayerStateManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.util.List;
import java.util.UUID;

/**
 * Authoritative controller coordinating match lifecycle ticks, arena manifestation,
 * synchronized countdowns, combat isolation, and reliable rollback.
 */
public class ServerMatchController {
    private static int tickCounter = 0;

    public static void startDuelMatch(MinecraftServer server, Party party, ServerPlayerEntity p1, ServerPlayerEntity p2) {
        // 1. Capture snapshots of both players before entering domain
        capturePlayerSnapshot(p1);
        capturePlayerSnapshot(p2);

        // 2. Allocate arena
        ArenaInstance arena = ArenaManager.getInstance().allocateArena("domainpvp:colosseum");

        // 3. Create match session and ensure state is ARENA_BUILDING
        MatchSession match = MatchManager.getInstance().createMatch(party.getId(), p1.getUuid(), p2.getUuid());
        match.setArenaInstanceId(arena.getInstanceId());
        match.getStateMachine().forceState(MatchState.ARENA_BUILDING);

        // 4. Force load chunks around the arena to ensure block placement succeeds
        ServerWorld world = server.getOverworld();
        int originChunkX = arena.getOriginX() >> 4;
        int originChunkZ = arena.getOriginZ() >> 4;
        for (int cx = originChunkX - 2; cx <= originChunkX + 2; cx++) {
            for (int cz = originChunkZ - 2; cz <= originChunkZ + 2; cz++) {
                world.getChunk(cx, cz);
            }
        }

        // 5. Pre-place solid spawn pedestal blocks and teleport players immediately
        ArenaSpawnPoint sp1 = arena.getSpawn1World();
        ArenaSpawnPoint sp2 = arena.getSpawn2World();
        BlockPos posSp1 = new BlockPos((int) Math.floor(sp1.x()), (int) Math.floor(sp1.y()) - 1, (int) Math.floor(sp1.z()));
        BlockPos posSp2 = new BlockPos((int) Math.floor(sp2.x()), (int) Math.floor(sp2.y()) - 1, (int) Math.floor(sp2.z()));
        world.setBlockState(posSp1, Blocks.CHISELED_STONE_BRICKS.getDefaultState(), 3);
        world.setBlockState(posSp2, Blocks.CHISELED_STONE_BRICKS.getDefaultState(), 3);

        teleportPlayer(p1, world, sp1);
        teleportPlayer(p2, world, sp2);
        lockPlayerToSpawn(p1, sp1);
        lockPlayerToSpawn(p2, sp2);

        // Play domain activation and teleportation sound
        world.playSound(null, posSp1, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.2f, 1.0f);
        world.playSound(null, posSp2, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.2f, 1.0f);

        // 6. Send construction started payload
        ArenaConstructionStartedPayload payload = new ArenaConstructionStartedPayload(
                arena.getOriginX(), arena.getOriginY(), arena.getOriginZ(), arena.getTotalBlocks()
        );
        ServerPlayNetworking.send(p1, payload);
        ServerPlayNetworking.send(p2, payload);

        p1.sendMessage(Text.translatable("domainpvp.match.arena_building"), false);
        p2.sendMessage(Text.translatable("domainpvp.match.arena_building"), false);
    }

    public static void tick(MinecraftServer server) {
        tickCounter++;
        boolean isSecondTick = (tickCounter % 20 == 0);

        for (MatchSession match : MatchManager.getInstance().getActiveMatches()) {
            ServerPlayerEntity p1 = server.getPlayerManager().getPlayer(match.getPlayer1Uuid());
            ServerPlayerEntity p2 = server.getPlayerManager().getPlayer(match.getPlayer2Uuid());
            ArenaInstance arena = ArenaManager.getInstance().getArena(match.getArenaInstanceId());

            if (p1 == null || p2 == null) {
                // If a player disconnected, handle cleanup
                MatchManager.getInstance().handlePlayerDisconnect(p1 == null ? match.getPlayer1Uuid() : match.getPlayer2Uuid());
                continue;
            }

            switch (match.getState()) {
                case ARENA_BUILDING -> {
                    if (arena != null) {
                        ServerWorld world = server.getOverworld();
                        int speed = ModConfig.get().arena.constructionSpeedBlocksPerTick;
                        List<ArenaBlockEntry> placed = ArenaManager.getInstance().tickConstruction(arena, speed);

                        // Strictly lock players in place on their spawn block during building
                        lockPlayerToSpawn(p1, arena.getSpawn1World());
                        lockPlayerToSpawn(p2, arena.getSpawn2World());

                        for (ArenaBlockEntry entry : placed) {
                            BlockPos pos = new BlockPos(arena.getOriginX() + entry.dx(), arena.getOriginY() + entry.dy(), arena.getOriginZ() + entry.dz());
                            BlockState state = parseBlockState(entry.blockState());
                            world.setBlockState(pos, state, 3);

                            if (tickCounter % 2 == 0) {
                                world.spawnParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.02);
                                world.spawnParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.05);
                            }
                        }

                        if (!placed.isEmpty() && tickCounter % 3 == 0) {
                            ArenaBlockEntry sample = placed.get(0);
                            BlockPos sPos = new BlockPos(arena.getOriginX() + sample.dx(), arena.getOriginY() + sample.dy(), arena.getOriginZ() + sample.dz());
                            world.playSound(null, sPos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.6f, 0.9f + (float) (Math.random() * 0.2));
                        }

                        // Send progress action bar and payload
                        int pct = Math.min(100, (arena.getPlacedCount() * 100) / Math.max(1, arena.getTotalBlocks()));
                        p1.sendMessage(Text.literal("§5✦ Manifesting Arena: §d" + pct + "% §5✦"), true);
                        p2.sendMessage(Text.literal("§5✦ Manifesting Arena: §d" + pct + "% §5✦"), true);

                        ArenaConstructionProgressPayload prog = new ArenaConstructionProgressPayload(arena.getPlacedCount(), arena.getTotalBlocks());
                        ServerPlayNetworking.send(p1, prog);
                        ServerPlayNetworking.send(p2, prog);

                        if (arena.getStatus() == ArenaInstance.Status.READY || arena.getConstructionQueue().isEmpty()) {
                            // Arena construction complete!
                            match.getStateMachine().transitionTo(MatchState.COUNTDOWN);
                            match.setCountdownSeconds(ModConfig.get().match.countdownDurationSeconds);

                            ArenaSpawnPoint sp1 = arena.getSpawn1World();
                            ArenaSpawnPoint sp2 = arena.getSpawn2World();

                            // Apply validated loadouts
                            Party party = PartyManager.getInstance().getPartyById(match.getPartyId());
                            String l1 = "CLASSIC";
                            String l2 = "CLASSIC";
                            if (party != null) {
                                PartyMember m1 = party.getMember(p1.getUuid());
                                PartyMember m2 = party.getMember(p2.getUuid());
                                if (m1 != null && m1.getSelectedLoadoutName() != null) l1 = m1.getSelectedLoadoutName();
                                if (m2 != null && m2.getSelectedLoadoutName() != null) l2 = m2.getSelectedLoadoutName();
                            }
                            applyLoadout(p1, l1);
                            applyLoadout(p2, l2);

                            // Sound: Level up chime for arena completion!
                            BlockPos posSp1 = new BlockPos((int) Math.floor(sp1.x()), (int) Math.floor(sp1.y()), (int) Math.floor(sp1.z()));
                            BlockPos posSp2 = new BlockPos((int) Math.floor(sp2.x()), (int) Math.floor(sp2.y()), (int) Math.floor(sp2.z()));
                            world.playSound(null, posSp1, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.2f);
                            world.playSound(null, posSp2, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.2f);

                            ArenaConstructionCompletedPayload completedPayload = new ArenaConstructionCompletedPayload(
                                    sp1.x(), sp1.y(), sp1.z(), sp2.x(), sp2.y(), sp2.z()
                            );
                            ServerPlayNetworking.send(p1, completedPayload);
                            ServerPlayNetworking.send(p2, completedPayload);
                        }
                    }
                }
                case COUNTDOWN -> {
                    // Keep players locked on spawn pedestals during countdown
                    if (arena != null) {
                        lockPlayerToSpawn(p1, arena.getSpawn1World());
                        lockPlayerToSpawn(p2, arena.getSpawn2World());
                    }

                    if (isSecondTick) {
                        int remaining = match.getCountdownSeconds();
                        CountdownUpdatedPayload countPayload = new CountdownUpdatedPayload(remaining);
                        ServerPlayNetworking.send(p1, countPayload);
                        ServerPlayNetworking.send(p2, countPayload);

                        if (remaining > 0) {
                            playCountdownSound(p1, remaining);
                            playCountdownSound(p2, remaining);
                            p1.sendMessage(Text.translatable("domainpvp.match.countdown", remaining), true);
                            p2.sendMessage(Text.translatable("domainpvp.match.countdown", remaining), true);
                            match.decrementCountdown();
                        } else {
                            // 0 = FIGHT!
                            match.getStateMachine().transitionTo(MatchState.FIGHTING);
                            match.setStartTime(java.time.Instant.now());

                            // Release players from spawn lock
                            p1.clearStatusEffects();
                            p2.clearStatusEffects();
                            p1.setVelocity(0, 0, 0);
                            p2.setVelocity(0, 0, 0);

                            MatchStartedPayload startedPayload = new MatchStartedPayload();
                            ServerPlayNetworking.send(p1, startedPayload);
                            ServerPlayNetworking.send(p2, startedPayload);

                            playFightSound(p1);
                            playFightSound(p2);
                            p1.sendMessage(Text.translatable("domainpvp.match.fight"), true);
                            p2.sendMessage(Text.translatable("domainpvp.match.fight"), true);

                            // Spawn fight start burst particles
                            if (arena != null) {
                                ServerWorld world = server.getOverworld();
                                world.spawnParticles(ParticleTypes.EXPLOSION, arena.getOriginX(), arena.getOriginY() + 2, arena.getOriginZ(), 2, 1.0, 0.5, 1.0, 0.1);
                                world.spawnParticles(ParticleTypes.CRIT, arena.getOriginX(), arena.getOriginY() + 2, arena.getOriginZ(), 25, 2.0, 1.0, 2.0, 0.2);
                            }
                        }
                    }
                }
                case FIGHTING -> {
                    // Boundary enforcement
                    if (arena != null) {
                        enforceBoundary(p1, arena);
                        enforceBoundary(p2, arena);
                    }

                    // Fallback death check
                    if (p1.getHealth() <= 0.0f || p1.isDead()) {
                        handlePlayerDeathInDuel(p1, match);
                    } else if (p2.getHealth() <= 0.0f || p2.isDead()) {
                        handlePlayerDeathInDuel(p2, match);
                    }
                }
                case MATCH_FINISHED -> {
                    int remaining = match.decrementFinishCountdownTicks();
                    if (remaining % 20 == 0 && remaining > 0) {
                        int sec = remaining / 20;
                        p1.sendMessage(Text.literal("§7[Domain Collapsing in " + sec + "s...]"), true);
                        p2.sendMessage(Text.literal("§7[Domain Collapsing in " + sec + "s...]"), true);
                    } else if (remaining <= 0) {
                        cleanupMatch(server, match);
                    }
                }
                case CLEANUP -> {
                    cleanupMatch(server, match);
                }
                default -> {}
            }
        }
    }

    public static void handlePlayerDeathInDuel(ServerPlayerEntity victim, MatchSession match) {
        if (match == null || match.getState() != MatchState.FIGHTING) return;

        MinecraftServer server = victim.getServer();
        if (server == null) return;

        UUID winnerUuid = match.getOpponent(victim.getUuid());
        UUID loserUuid = victim.getUuid();
        MatchManager.getInstance().finishMatch(match.getMatchId(), winnerUuid, loserUuid);

        ServerPlayerEntity winner = server.getPlayerManager().getPlayer(winnerUuid);

        // Keep victim from dying into vanilla respawn screen
        victim.setHealth(20.0f);
        victim.clearStatusEffects();
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 255, false, false, false));

        if (winner != null) {
            winner.clearStatusEffects();
            winner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 255, false, false, false));
        }

        String winnerName = winner != null ? winner.getName().getString() : "Opponent";
        String victimName = victim.getName().getString();

        // Broadcast titles & chat messages
        if (winner != null) {
            winner.sendMessage(Text.literal("§6§l[PvP Domain] VICTORY! §aYou defeated " + victimName + "!"), false);
            winner.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
            sendTitle(winner, "§6§lVICTORY!", "§aYou won the duel! Domain collapsing in 5s...");
        }

        victim.sendMessage(Text.literal("§c§l[PvP Domain] DEFEAT! §7Better luck next time against " + winnerName + "."), false);
        victim.playSoundToPlayer(SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.0f);
        sendTitle(victim, "§c§lDEFEAT!", "§7Better luck next time! Domain collapsing in 5s...");

        broadcastMatchFinished(winner != null ? winner : victim, victim, match);

        match.setFinishCountdownTicks(100); // 5 seconds of post-duel celebration before collapse
    }

    private static void sendTitle(ServerPlayerEntity player, String titleText, String subtitleText) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(titleText)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(subtitleText)));
    }

    private static void broadcastMatchFinished(ServerPlayerEntity p1, ServerPlayerEntity p2, MatchSession match) {
        UUID winner = match.getStats().getWinnerUuid();
        UUID loser = match.getStats().getLoserUuid();

        MatchFinishedPayload payload = new MatchFinishedPayload(
                winner,
                loser,
                match.getStats().getPlayer1DamageDealt(),
                match.getStats().getPlayer2DamageDealt(),
                match.getStats().getPlayer1Hits(),
                match.getStats().getPlayer2Hits(),
                match.getStats().getDurationSeconds()
        );

        ServerPlayNetworking.send(p1, payload);
        ServerPlayNetworking.send(p2, payload);
    }

    public static void triggerRematch(MinecraftServer server, MatchSession match) {
        if (match == null) return;
        ServerPlayerEntity p1 = server.getPlayerManager().getPlayer(match.getPlayer1Uuid());
        ServerPlayerEntity p2 = server.getPlayerManager().getPlayer(match.getPlayer2Uuid());
        ArenaInstance arena = ArenaManager.getInstance().getArena(match.getArenaInstanceId());

        if (p1 == null || p2 == null || arena == null) {
            cleanupMatch(server, match);
            return;
        }

        match.clearRematchVotes();
        match.getStateMachine().transitionTo(MatchState.REMATCH);

        // Reset positions to spawn points and lock players
        ArenaSpawnPoint sp1 = arena.getSpawn1World();
        ArenaSpawnPoint sp2 = arena.getSpawn2World();
        teleportPlayer(p1, server.getOverworld(), sp1);
        teleportPlayer(p2, server.getOverworld(), sp2);
        lockPlayerToSpawn(p1, sp1);
        lockPlayerToSpawn(p2, sp2);

        // Reapply loadouts
        Party party = PartyManager.getInstance().getPartyById(match.getPartyId());
        String l1 = "CLASSIC";
        String l2 = "CLASSIC";
        if (party != null) {
            PartyMember m1 = party.getMember(p1.getUuid());
            PartyMember m2 = party.getMember(p2.getUuid());
            if (m1 != null && m1.getSelectedLoadoutName() != null) l1 = m1.getSelectedLoadoutName();
            if (m2 != null && m2.getSelectedLoadoutName() != null) l2 = m2.getSelectedLoadoutName();
        }
        applyLoadout(p1, l1);
        applyLoadout(p2, l2);

        // Transition to COUNTDOWN
        match.getStateMachine().transitionTo(MatchState.COUNTDOWN);
        match.setCountdownSeconds(ModConfig.get().match.countdownDurationSeconds);

        p1.sendMessage(Text.literal("§6§l[PvP Domain] Rematch accepted! Starting countdown..."), false);
        p2.sendMessage(Text.literal("§6§l[PvP Domain] Rematch accepted! Starting countdown..."), false);
    }

    public static void cleanupMatch(MinecraftServer server, MatchSession match) {
        match.getStateMachine().transitionTo(MatchState.CLEANUP);

        ServerPlayerEntity p1 = server.getPlayerManager().getPlayer(match.getPlayer1Uuid());
        ServerPlayerEntity p2 = server.getPlayerManager().getPlayer(match.getPlayer2Uuid());
        ArenaInstance arena = ArenaManager.getInstance().getArena(match.getArenaInstanceId());

        // Notify clients
        CleanupStartedPayload cleanupPayload = new CleanupStartedPayload();
        if (p1 != null) ServerPlayNetworking.send(p1, cleanupPayload);
        if (p2 != null) ServerPlayNetworking.send(p2, cleanupPayload);

        // Teardown placed blocks in arena - wipe everything back to air!
        if (arena != null) {
            ServerWorld world = server.getOverworld();
            while (!arena.getPlacedBlocks().isEmpty()) {
                ArenaBlockEntry entry = arena.getPlacedBlocks().pop();
                BlockPos pos = new BlockPos(arena.getOriginX() + entry.dx(), arena.getOriginY() + entry.dy(), arena.getOriginZ() + entry.dz());
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            }

            // Also remove the spawn pedestal blocks
            ArenaSpawnPoint sp1 = arena.getSpawn1World();
            ArenaSpawnPoint sp2 = arena.getSpawn2World();
            BlockPos posSp1 = new BlockPos((int) Math.floor(sp1.x()), (int) Math.floor(sp1.y()) - 1, (int) Math.floor(sp1.z()));
            BlockPos posSp2 = new BlockPos((int) Math.floor(sp2.x()), (int) Math.floor(sp2.y()) - 1, (int) Math.floor(sp2.z()));
            world.setBlockState(posSp1, Blocks.AIR.getDefaultState(), 3);
            world.setBlockState(posSp2, Blocks.AIR.getDefaultState(), 3);

            // Play collapse sound
            world.playSound(null, posSp1, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.2f, 0.8f);

            arena.setStatus(ArenaInstance.Status.RELEASED);
        }

        // Restore player snapshots and teleport them back home
        if (p1 != null) restorePlayerSnapshot(p1, server);
        if (p2 != null) restorePlayerSnapshot(p2, server);

        DomainDestroyedPayload destroyedPayload = new DomainDestroyedPayload();
        if (p1 != null) {
            ServerPlayNetworking.send(p1, destroyedPayload);
            p1.sendMessage(Text.translatable("domainpvp.match.cleanup"), false);
        }
        if (p2 != null) {
            ServerPlayNetworking.send(p2, destroyedPayload);
            p2.sendMessage(Text.translatable("domainpvp.match.cleanup"), false);
        }

        // Reset party readiness so players can ready up again
        Party party = PartyManager.getInstance().getPartyById(match.getPartyId());
        if (party != null) {
            party.resetReadyStatus();
            ServerNetworkHandler.syncPartyState(server, party);
        }

        MatchManager.getInstance().removeMatch(match.getMatchId());
    }

    private static void capturePlayerSnapshot(ServerPlayerEntity player) {
        PlayerSnapshot snapshot = new PlayerSnapshot(player.getUuid(), player.getName().getString());
        snapshot.setDimensionId(player.getServerWorld().getRegistryKey().getValue().toString());
        snapshot.setX(player.getX());
        snapshot.setY(player.getY());
        snapshot.setZ(player.getZ());
        snapshot.setYaw(player.getYaw());
        snapshot.setPitch(player.getPitch());
        snapshot.setHealth(player.getHealth());
        snapshot.setMaxHealth((float) player.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH));
        snapshot.setFoodLevel(player.getHungerManager().getFoodLevel());
        snapshot.setSaturation(player.getHungerManager().getSaturationLevel());
        snapshot.setExperienceLevel(player.experienceLevel);
        snapshot.setExperienceProgress(player.experienceProgress);
        snapshot.setGameMode(player.interactionManager.getGameMode().getName());

        PlayerStateManager.getInstance().recordSnapshot(snapshot);
        PersistenceManager.getInstance().persistSnapshot(snapshot);
    }

    private static void restorePlayerSnapshot(ServerPlayerEntity player, MinecraftServer server) {
        player.clearStatusEffects();
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0.0f;

        PlayerSnapshot snapshot = PlayerStateManager.getInstance().releaseSnapshot(player.getUuid());
        PersistenceManager.getInstance().removePersistedSnapshot(player.getUuid());

        if (snapshot == null) return;

        ServerWorld targetWorld = server.getOverworld();
        player.teleport(targetWorld, snapshot.getX(), snapshot.getY(), snapshot.getZ(), snapshot.getYaw(), snapshot.getPitch());
        player.setHealth(Math.min(snapshot.getHealth(), (float) player.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH)));
        player.getHungerManager().setFoodLevel(snapshot.getFoodLevel());
        player.getHungerManager().setSaturationLevel(snapshot.getSaturation());
        player.setExperienceLevel(snapshot.getExperienceLevel());
        player.experienceProgress = snapshot.getExperienceProgress();
        player.changeGameMode(GameMode.byName(snapshot.getGameMode(), GameMode.SURVIVAL));
    }

    private static void teleportPlayer(ServerPlayerEntity player, ServerWorld world, ArenaSpawnPoint sp) {
        player.teleport(world, sp.x(), sp.y(), sp.z(), sp.yaw(), sp.pitch());
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0.0f;
    }

    private static void lockPlayerToSpawn(ServerPlayerEntity player, ArenaSpawnPoint sp) {
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0.0f;

        // Apply Slowness 255 (stops WASD walk), Jump Boost 200 (cancels jump), and Resistance 255 (immunity)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 255, false, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 30, 200, false, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, 255, false, false, false));

        // Snap back to pedestal if client attempts any movement
        double dx = player.getX() - sp.x();
        double dz = player.getZ() - sp.z();
        if ((dx * dx + dz * dz) > 0.05 || Math.abs(player.getY() - sp.y()) > 0.3) {
            player.teleport(player.getServerWorld(), sp.x(), sp.y(), sp.z(), sp.yaw(), sp.pitch());
        }
    }

    private static void enforceBoundary(ServerPlayerEntity player, ArenaInstance arena) {
        CombatManager.ClampedPosition clamped = CombatManager.getInstance().clampToBoundary(
                arena, player.getX(), player.getY(), player.getZ()
        );
        if (clamped.outside()) {
            player.teleport(player.getServerWorld(), clamped.x(), clamped.y(), clamped.z(), player.getYaw(), player.getPitch());
            player.setVelocity(0, 0, 0);
        }
    }

    private static void applyLoadout(ServerPlayerEntity player, String presetName) {
        player.getInventory().clear();
        player.clearStatusEffects();
        player.setHealth((float) player.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH));
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);

        Loadout loadout = LoadoutManager.getInstance().getPreset(presetName);
        if (loadout == null) return;

        if (!loadout.getHelmet().isEmpty()) player.getInventory().armor.set(3, createItemStack(loadout.getHelmet()));
        if (!loadout.getChestplate().isEmpty()) player.getInventory().armor.set(2, createItemStack(loadout.getChestplate()));
        if (!loadout.getLeggings().isEmpty()) player.getInventory().armor.set(1, createItemStack(loadout.getLeggings()));
        if (!loadout.getBoots().isEmpty()) player.getInventory().armor.set(0, createItemStack(loadout.getBoots()));
        if (!loadout.getOffhand().isEmpty()) player.getInventory().offHand.set(0, createItemStack(loadout.getOffhand()));

        for (int i = 0; i < 9; i++) {
            LoadoutItem item = loadout.getHotbar().get(i);
            if (!item.isEmpty()) {
                player.getInventory().main.set(i, createItemStack(item));
            }
        }

        for (int i = 0; i < 27; i++) {
            LoadoutItem item = loadout.getInventory().get(i);
            if (!item.isEmpty()) {
                player.getInventory().main.set(i + 9, createItemStack(item));
            }
        }
        player.playerScreenHandler.sendContentUpdates();
    }

    private static ItemStack createItemStack(LoadoutItem item) {
        Identifier id = Identifier.tryParse(item.itemId());
        if (id == null) return ItemStack.EMPTY;
        Item mcItem = Registries.ITEM.get(id);
        if (mcItem == null) return ItemStack.EMPTY;
        return new ItemStack(mcItem, item.count());
    }

    private static BlockState parseBlockState(String blockId) {
        Identifier id = Identifier.tryParse(blockId.split("\\[")[0]);
        if (id == null) return Blocks.STONE_BRICKS.getDefaultState();
        return Registries.BLOCK.get(id).getDefaultState();
    }

    private static void playCountdownSound(ServerPlayerEntity player, int seconds) {
        player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1.0f, 1.0f + (5 - seconds) * 0.15f);
    }

    private static void playFightSound(ServerPlayerEntity player) {
        player.playSoundToPlayer(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.0f, 1.0f);
        player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.5f, 1.5f);
    }
}
