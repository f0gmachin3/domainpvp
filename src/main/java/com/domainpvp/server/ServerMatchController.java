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
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
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
 * Authoritative controller coordinating Best of 3 (Bo3) matches, 6-kit voting,
 * arena manifestation, boundary security, pre-match inventory rollback, and fireworks celebrations.
 */
public class ServerMatchController {
    private static int tickCounter = 0;

    /**
     * Initializes vanilla scoreboard objective so hearts are displayed over players' heads.
     */
    public static void initializeHealthScoreboard(MinecraftServer server) {
        try {
            Scoreboard scoreboard = server.getScoreboard();
            ScoreboardObjective obj = scoreboard.getNullableObjective("pvp_hearts");
            if (obj == null) {
                obj = scoreboard.addObjective(
                        "pvp_hearts",
                        ScoreboardCriterion.HEALTH,
                        Text.literal("§c❤"),
                        ScoreboardCriterion.RenderType.INTEGER,
                        true,
                        null
                );
            }
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.BELOW_NAME, obj);
        } catch (Exception e) {
            System.err.println("[PvP Domain] Failed to initialize health display: " + e.getMessage());
        }
    }

    public static void startDuelMatch(MinecraftServer server, Party party, ServerPlayerEntity p1, ServerPlayerEntity p2) {
        // 0. Ensure health scoreboard is active
        initializeHealthScoreboard(server);

        // 1. Capture complete snapshots (including inventories) before entering domain
        capturePlayerSnapshot(p1);
        capturePlayerSnapshot(p2);

        // 2. Allocate arena
        ArenaInstance arena = ArenaManager.getInstance().allocateArena("domainpvp:colosseum");

        // 3. Create match session and ensure state is ARENA_BUILDING
        MatchSession match = MatchManager.getInstance().createMatch(party.getId(), p1.getUuid(), p2.getUuid());
        match.setArenaInstanceId(arena.getInstanceId());
        match.getStateMachine().forceState(MatchState.ARENA_BUILDING);
        match.setKitVotingTicks(140); // 7s for kit voting while arena manifests

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

        // 7. Prompt both players with the 6-icon Kit Selection GUI
        ServerPlayNetworking.send(p1, new OpenKitSelectionPayload(8));
        ServerPlayNetworking.send(p2, new OpenKitSelectionPayload(8));

        p1.sendMessage(Text.literal("§6§l[PvP Domain] §eSelect your PvP Kit! Click an icon on-screen or type §b/kit <1-6>§e."), false);
        p2.sendMessage(Text.literal("§6§l[PvP Domain] §eSelect your PvP Kit! Click an icon on-screen or type §b/kit <1-6>§e."), false);
    }

    public static void resolveAndApplyKit(MinecraftServer server, MatchSession match) {
        if (match == null) return;
        String chosenKit = match.resolveKit();
        match.setActiveKitName(chosenKit);

        ServerPlayerEntity p1 = server.getPlayerManager().getPlayer(match.getPlayer1Uuid());
        ServerPlayerEntity p2 = server.getPlayerManager().getPlayer(match.getPlayer2Uuid());

        if (p1 != null) {
            applyLoadout(p1, chosenKit);
            p1.sendMessage(Text.literal("§6§l[PvP Domain] §aKit Selected: §e§l" + chosenKit + "§a!"), false);
            p1.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1.0f, 1.2f);
        }
        if (p2 != null) {
            applyLoadout(p2, chosenKit);
            p2.sendMessage(Text.literal("§6§l[PvP Domain] §aKit Selected: §e§l" + chosenKit + "§a!"), false);
            p2.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1.0f, 1.2f);
        }
    }

    public static void tick(MinecraftServer server) {
        tickCounter++;
        boolean isSecondTick = (tickCounter % 20 == 0);

        for (MatchSession match : MatchManager.getInstance().getActiveMatches()) {
            ServerPlayerEntity p1 = server.getPlayerManager().getPlayer(match.getPlayer1Uuid());
            ServerPlayerEntity p2 = server.getPlayerManager().getPlayer(match.getPlayer2Uuid());
            ArenaInstance arena = ArenaManager.getInstance().getArena(match.getArenaInstanceId());

            if (p1 == null || p2 == null) {
                MatchManager.getInstance().handlePlayerDisconnect(p1 == null ? match.getPlayer1Uuid() : match.getPlayer2Uuid());
                continue;
            }

            // Handle inter-round transition countdown (e.g. 4 seconds between rounds in Bo3)
            if (match.getRoundTransitionTicks() > 0) {
                int rem = match.decrementRoundTransitionTicks();
                if (rem % 20 == 0 && rem > 0) {
                    int sec = rem / 20;
                    p1.sendMessage(Text.literal("§e✦ Round " + (match.getCurrentRound() + 1) + " starting in " + sec + "s... ✦"), true);
                    p2.sendMessage(Text.literal("§e✦ Round " + (match.getCurrentRound() + 1) + " starting in " + sec + "s... ✦"), true);
                } else if (rem == 0) {
                    match.nextRound();
                    match.getStateMachine().forceState(MatchState.COUNTDOWN);
                    match.setCountdownSeconds(3);
                }
            }

            switch (match.getState()) {
                case ARENA_BUILDING -> {
                    if (arena != null) {
                        ServerWorld world = server.getOverworld();
                        int speed = ModConfig.get().arena.constructionSpeedBlocksPerTick;
                        List<ArenaBlockEntry> placed = ArenaManager.getInstance().tickConstruction(arena, speed);

                        // Keep players locked on spawn pedestals during building
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
                        p1.sendMessage(Text.literal("§5✦ Manifesting Colosseum: §d" + pct + "% §5✦"), true);
                        p2.sendMessage(Text.literal("§5✦ Manifesting Colosseum: §d" + pct + "% §5✦"), true);

                        ArenaConstructionProgressPayload prog = new ArenaConstructionProgressPayload(arena.getPlacedCount(), arena.getTotalBlocks());
                        ServerPlayNetworking.send(p1, prog);
                        ServerPlayNetworking.send(p2, prog);

                        // Check kit voting timer
                        int vTicks = match.decrementKitVotingTicks();
                        if (match.haveBothVotedKit() || vTicks <= 0 || arena.getStatus() == ArenaInstance.Status.READY) {
                            if (match.getKitVotingTicks() > -999) {
                                resolveAndApplyKit(server, match);
                                match.setKitVotingTicks(-1000); // mark kit resolved
                            }
                        }

                        if (arena.getStatus() == ArenaInstance.Status.READY || arena.getConstructionQueue().isEmpty()) {
                            // Arena construction complete!
                            match.getStateMachine().transitionTo(MatchState.COUNTDOWN);
                            match.setCountdownSeconds(3);

                            ArenaSpawnPoint sp1 = arena.getSpawn1World();
                            ArenaSpawnPoint sp2 = arena.getSpawn2World();

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
                    // Lock players to spawn pedestals during countdown
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
                            p1.sendMessage(Text.literal("§e§lRound " + match.getCurrentRound() + " starting in §6§l" + remaining + "..."), true);
                            p2.sendMessage(Text.literal("§e§lRound " + match.getCurrentRound() + " starting in §6§l" + remaining + "..."), true);
                            match.decrementCountdown();
                        } else {
                            // 0 = FIGHT!
                            match.getStateMachine().transitionTo(MatchState.FIGHTING);
                            match.setStartTime(java.time.Instant.now());

                            p1.clearStatusEffects();
                            p2.clearStatusEffects();
                            p1.setVelocity(0, 0, 0);
                            p2.setVelocity(0, 0, 0);

                            MatchStartedPayload startedPayload = new MatchStartedPayload();
                            ServerPlayNetworking.send(p1, startedPayload);
                            ServerPlayNetworking.send(p2, startedPayload);

                            playFightSound(p1);
                            playFightSound(p2);
                            p1.sendMessage(Text.literal("§c§l[Round " + match.getCurrentRound() + "] FIGHT!"), true);
                            p2.sendMessage(Text.literal("§c§l[Round " + match.getCurrentRound() + "] FIGHT!"), true);

                            if (arena != null) {
                                ServerWorld world = server.getOverworld();
                                world.spawnParticles(ParticleTypes.EXPLOSION, arena.getOriginX(), arena.getOriginY() + 2, arena.getOriginZ(), 2, 1.0, 0.5, 1.0, 0.1);
                                world.spawnParticles(ParticleTypes.CRIT, arena.getOriginX(), arena.getOriginY() + 2, arena.getOriginZ(), 25, 2.0, 1.0, 2.0, 0.2);
                            }
                        }
                    }
                }
                case FIGHTING -> {
                    // Security boundary enforcement
                    if (arena != null) {
                        enforceBoundary(p1, arena);
                        enforceBoundary(p2, arena);
                    }

                    // Fallback health check
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
        ServerPlayerEntity winner = server.getPlayerManager().getPlayer(winnerUuid);
        ArenaInstance arena = ArenaManager.getInstance().getArena(match.getArenaInstanceId());

        // 1. Record Best of 3 round win
        match.recordRoundWin(winnerUuid);

        // 2. Launch celebratory fireworks from all sides of the arena
        if (arena != null) {
            spawnRoundWinFireworks(server, arena);
        }

        // 3. Keep victim from dying into vanilla respawn screen
        victim.setHealth(20.0f);
        victim.clearStatusEffects();
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 255, false, false, false));

        if (winner != null) {
            winner.clearStatusEffects();
            winner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 255, false, false, false));
        }

        String winnerName = winner != null ? winner.getName().getString() : "Opponent";
        String victimName = victim.getName().getString();
        int p1Score = match.getPlayer1Score();
        int p2Score = match.getPlayer2Score();
        String scoreText = "§eScore: " + p1Score + " - " + p2Score;

        // 4. Check if Match is decided (Best of 3: first to 2 round wins)
        if (match.isMatchDecided()) {
            MatchManager.getInstance().finishMatch(match.getMatchId(), winnerUuid, victim.getUuid());

            if (winner != null) {
                winner.sendMessage(Text.literal("§6§l[PvP Domain] MATCH VICTORY! §aYou won the Best of 3 (" + scoreText + ")!"), false);
                winner.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.2f, 1.0f);
                sendTitle(winner, "§6§lMATCH VICTORY!", scoreText + " §7(Domain collapsing in 5s...)");
            }

            victim.sendMessage(Text.literal("§c§l[PvP Domain] MATCH DEFEAT! §7Better luck next time (" + scoreText + ")."), false);
            victim.playSoundToPlayer(SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.0f);
            sendTitle(victim, "§c§lMATCH DEFEAT!", scoreText + " §7(Domain collapsing in 5s...)");

            broadcastMatchFinished(winner != null ? winner : victim, victim, match);
            match.setFinishCountdownTicks(100); // 5s celebration before domain collapse
        } else {
            // Next Round in Best of 3!
            int roundWon = match.getCurrentRound();
            if (winner != null) {
                winner.sendMessage(Text.literal("§6§l[PvP Domain] Round " + roundWon + " Won! " + scoreText), false);
                winner.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.2f);
                sendTitle(winner, "§6§lROUND " + roundWon + " WON!", scoreText + " §7(Next round in 4s)");
            }

            victim.sendMessage(Text.literal("§c§l[PvP Domain] Round " + roundWon + " Lost! " + scoreText), false);
            victim.playSoundToPlayer(SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.0f);
            sendTitle(victim, "§c§lROUND " + roundWon + " LOST!", scoreText + " §7(Next round in 4s)");

            // Wipe temporary player placed blocks (water, lava, cobwebs, planks)
            if (arena != null) {
                ServerWorld world = server.getOverworld();
                for (BlockPos bpos : match.getPlacedBlocks()) {
                    world.setBlockState(bpos, Blocks.AIR.getDefaultState(), 3);
                }
                match.clearPlacedBlocks();
            }

            // Reset positions to spawn pedestals and re-lock
            if (arena != null) {
                teleportPlayer(victim, server.getOverworld(), arena.getSpawn1World());
                lockPlayerToSpawn(victim, arena.getSpawn1World());
                if (winner != null) {
                    teleportPlayer(winner, server.getOverworld(), arena.getSpawn2World());
                    lockPlayerToSpawn(winner, arena.getSpawn2World());
                }
            }

            // Re-apply the selected kit for the next round
            applyLoadout(victim, match.getActiveKitName());
            if (winner != null) {
                applyLoadout(winner, match.getActiveKitName());
            }

            // 4 seconds transition delay before countdown of the next round
            match.setRoundTransitionTicks(80);
        }
    }

    /**
     * Spawns festive fireworks from 8 perimeter directions around the Colosseum.
     */
    public static void spawnRoundWinFireworks(MinecraftServer server, ArenaInstance arena) {
        ServerWorld world = server.getOverworld();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            double fx = arena.getOriginX() + Math.cos(angle) * 20.0;
            double fz = arena.getOriginZ() + Math.sin(angle) * 20.0;
            double fy = arena.getOriginY() + 4.0;

            world.spawnParticles(ParticleTypes.FIREWORK, fx, fy + 8, fz, 30, 0.8, 0.8, 0.8, 0.15);
            world.playSound(null, BlockPos.ofFloored(fx, fy + 8, fz), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.2f, 1.0f);
            world.playSound(null, BlockPos.ofFloored(fx, fy + 8, fz), SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 1.2f, 1.0f);
        }
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
        applyLoadout(p1, match.getActiveKitName());
        applyLoadout(p2, match.getActiveKitName());

        // Transition to COUNTDOWN
        match.getStateMachine().transitionTo(MatchState.COUNTDOWN);
        match.setCountdownSeconds(3);

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

        // Wipe temporary player-placed blocks (water, lava, cobwebs, planks)
        if (arena != null) {
            ServerWorld world = server.getOverworld();
            for (BlockPos pos : match.getPlacedBlocks()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            }
            match.clearPlacedBlocks();
        }

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

        // Clear all given kit items and restore pre-match inventories & locations
        if (p1 != null) {
            p1.getInventory().clear();
            restorePlayerSnapshot(p1, server);
        }
        if (p2 != null) {
            p2.getInventory().clear();
            restorePlayerSnapshot(p2, server);
        }

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

        // Snapshot full inventory loss-lessly
        snapshot.getSavedMain().clear();
        for (ItemStack stack : player.getInventory().main) {
            snapshot.getSavedMain().add(stack.copy());
        }
        snapshot.getSavedArmor().clear();
        for (ItemStack stack : player.getInventory().armor) {
            snapshot.getSavedArmor().add(stack.copy());
        }
        snapshot.setSavedOffhand(player.getInventory().offHand.get(0).copy());

        PlayerStateManager.getInstance().recordSnapshot(snapshot);
        PersistenceManager.getInstance().persistSnapshot(snapshot);
    }

    private static void restorePlayerSnapshot(ServerPlayerEntity player, MinecraftServer server) {
        player.clearStatusEffects();
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0.0f;

        // Wipe any kit items currently on the player!
        player.getInventory().clear();

        PlayerSnapshot snapshot = PlayerStateManager.getInstance().releaseSnapshot(player.getUuid());
        PersistenceManager.getInstance().removePersistedSnapshot(player.getUuid());

        if (snapshot == null) return;

        ServerWorld targetWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(snapshot.getDimensionId())));
        if (targetWorld == null) targetWorld = server.getOverworld();
        player.teleport(targetWorld, snapshot.getX(), snapshot.getY(), snapshot.getZ(), snapshot.getYaw(), snapshot.getPitch());
        player.setHealth(Math.min(snapshot.getHealth(), (float) player.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH)));
        player.getHungerManager().setFoodLevel(snapshot.getFoodLevel());
        player.getHungerManager().setSaturationLevel(snapshot.getSaturation());
        player.setExperienceLevel(snapshot.getExperienceLevel());
        player.experienceProgress = snapshot.getExperienceProgress();
        player.changeGameMode(GameMode.byName(snapshot.getGameMode(), GameMode.SURVIVAL));

        // Restore pre-match inventory items
        for (int i = 0; i < snapshot.getSavedMain().size() && i < player.getInventory().main.size(); i++) {
            player.getInventory().main.set(i, snapshot.getSavedMain().get(i).copy());
        }
        for (int i = 0; i < snapshot.getSavedArmor().size() && i < player.getInventory().armor.size(); i++) {
            player.getInventory().armor.set(i, snapshot.getSavedArmor().get(i).copy());
        }
        if (snapshot.getSavedOffhand() != null && !snapshot.getSavedOffhand().isEmpty()) {
            player.getInventory().offHand.set(0, snapshot.getSavedOffhand().copy());
        }
        player.playerScreenHandler.sendContentUpdates();
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
            // Downward velocity to stop any wall climbing or jumping over the rim
            player.setVelocity(0, -0.6, 0);
            player.sendMessage(Text.literal("§c[!] Arena Boundary: You cannot escape the Colosseum!"), true);
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

        // Custom Splash Potion of Healing II creation
        if (id.getPath().equals("splash_potion")) {
            return PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.STRONG_HEALING);
        }

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
