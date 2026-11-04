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
import com.domainpvp.party.PartyMember;
import com.domainpvp.persistence.PersistenceManager;
import com.domainpvp.player.PlayerSnapshot;
import com.domainpvp.player.PlayerStateManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import java.util.Map;
import java.util.UUID;

/**
 * Authoritative controller coordinating match lifecycle ticks, arena manifestation,
 * synchronized countdowns, and reliable restorations.
 */
public class ServerMatchController {
    private static int tickCounter = 0;

    public static void startDuelMatch(MinecraftServer server, Party party, ServerPlayerEntity p1, ServerPlayerEntity p2) {
        // 1. Capture snapshots of both players before entering domain
        capturePlayerSnapshot(p1);
        capturePlayerSnapshot(p2);

        // 2. Allocate arena
        ArenaInstance arena = ArenaManager.getInstance().allocateArena("domainpvp:colosseum");

        // 3. Create match session
        MatchSession match = MatchManager.getInstance().createMatch(party.getId(), p1.getUuid(), p2.getUuid());
        match.setArenaInstanceId(arena.getInstanceId());
        match.getStateMachine().transitionTo(MatchState.ARENA_BUILDING);

        // 4. Pre-place solid spawn pedestal blocks and teleport players immediately
        ServerWorld world = server.getOverworld();
        ArenaSpawnPoint sp1 = arena.getSpawn1World();
        ArenaSpawnPoint sp2 = arena.getSpawn2World();
        BlockPos posSp1 = new BlockPos((int) Math.floor(sp1.x()), (int) Math.floor(sp1.y()) - 1, (int) Math.floor(sp1.z()));
        BlockPos posSp2 = new BlockPos((int) Math.floor(sp2.x()), (int) Math.floor(sp2.y()) - 1, (int) Math.floor(sp2.z()));
        world.setBlockState(posSp1, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
        world.setBlockState(posSp2, Blocks.CHISELED_STONE_BRICKS.getDefaultState());

        teleportPlayer(p1, world, sp1);
        teleportPlayer(p2, world, sp2);
        freezePlayer(p1);
        freezePlayer(p2);

        // Play domain activation and teleportation sound
        world.playSound(null, posSp1, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.2f, 1.0f);
        world.playSound(null, posSp2, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.2f, 1.0f);

        // 5. Send construction started payload
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

                        freezePlayer(p1);
                        freezePlayer(p2);

                        for (ArenaBlockEntry entry : placed) {
                            BlockPos pos = new BlockPos(arena.getOriginX() + entry.dx(), arena.getOriginY() + entry.dy(), arena.getOriginZ() + entry.dz());
                            BlockState state = parseBlockState(entry.blockState());
                            world.setBlockState(pos, state);

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

                        if (arena.getStatus() == ArenaInstance.Status.READY) {
                            // Arena construction complete!
                            match.getStateMachine().transitionTo(MatchState.COUNTDOWN);
                            match.setCountdownSeconds(ModConfig.get().match.countdownDurationSeconds);

                            ArenaSpawnPoint sp1 = arena.getSpawn1World();
                            ArenaSpawnPoint sp2 = arena.getSpawn2World();

                            // Apply validated loadouts
                            Party party = PartyManager.getInstance().getPartyByPlayer(p1.getUuid());
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
                    // Freeze movement and attacks during countdown
                    freezePlayer(p1);
                    freezePlayer(p2);

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

                    // Check for player health <= 0 (death detection)
                    if (p1.getHealth() <= 0.0f) {
                        CombatManager.getInstance().onPlayerDeath(p1.getUuid());
                        broadcastMatchFinished(p1, p2, match);
                    } else if (p2.getHealth() <= 0.0f) {
                        CombatManager.getInstance().onPlayerDeath(p2.getUuid());
                        broadcastMatchFinished(p1, p2, match);
                    }
                }
                case CLEANUP -> {
                    cleanupMatch(server, match);
                }
                default -> {}
            }
        }
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
        match.getStateMachine().transitionTo(MatchState.REMATCH);
        match.clearRematchVotes();

        ServerPlayerEntity p1 = server.getPlayerManager().getPlayer(match.getPlayer1Uuid());
        ServerPlayerEntity p2 = server.getPlayerManager().getPlayer(match.getPlayer2Uuid());
        ArenaInstance arena = ArenaManager.getInstance().getArena(match.getArenaInstanceId());

        if (p1 != null && p2 != null && arena != null) {
            ServerWorld world = server.getOverworld();
            teleportPlayer(p1, world, arena.getSpawn1World());
            teleportPlayer(p2, world, arena.getSpawn2World());

            applyLoadout(p1, "CLASSIC");
            applyLoadout(p2, "CLASSIC");

            match.setCountdownSeconds(ModConfig.get().match.countdownDurationSeconds);
            match.getStateMachine().transitionTo(MatchState.COUNTDOWN);
        }
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

        // Teardown placed blocks in arena
        if (arena != null) {
            ServerWorld world = server.getOverworld();
            while (!arena.getPlacedBlocks().isEmpty()) {
                ArenaBlockEntry entry = arena.getPlacedBlocks().pop();
                BlockPos pos = new BlockPos(arena.getOriginX() + entry.dx(), arena.getOriginY() + entry.dy(), arena.getOriginZ() + entry.dz());
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
            arena.setStatus(ArenaInstance.Status.RELEASED);
        }

        // Restore player snapshots
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
    }

    private static void freezePlayer(ServerPlayerEntity player) {
        player.setVelocity(0, player.getVelocity().y < 0 ? player.getVelocity().y : 0, 0);
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
    }
}

