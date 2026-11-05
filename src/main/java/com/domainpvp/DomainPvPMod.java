package com.domainpvp;

import com.domainpvp.combat.CombatManager;
import com.domainpvp.config.ModConfig;
import com.domainpvp.network.NetworkingManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.util.UUID;

/**
 * Common entrypoint for PvP Domain mod.
 */
public class DomainPvPMod implements ModInitializer {
    public static final String MOD_ID = "domainpvp";

    @Override
    public void onInitialize() {
        System.out.println("[PvP Domain] Initializing common mod systems...");

        // Load configuration
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        ModConfig.load(configDir);

        // Register Fabric CustomPayload networking channels
        NetworkingManager.registerAllPayloads();

        // Enforce combat damage rules via Fabric API events
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity victim) {
                UUID attackerUuid = source.getAttacker() != null ? source.getAttacker().getUuid() : null;
                return CombatManager.getInstance().canInflictDamage(attackerUuid, victim.getUuid(), amount);
            }
            return true;
        });

        // Intercept lethal damage to handle match finish cleanly without vanilla death screen
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity victim) {
                com.domainpvp.match.MatchSession match = com.domainpvp.match.MatchManager.getInstance().getMatchByPlayer(victim.getUuid());
                if (match != null && match.getState() == com.domainpvp.match.MatchState.FIGHTING) {
                    com.domainpvp.server.ServerMatchController.handlePlayerDeathInDuel(victim, match);
                    return false;
                }
            }
            return true;
        });

        // Enforce arena block breaking protection via Fabric API events
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            return CombatManager.getInstance().canBreakBlock(player.getUuid());
        });

        // Initialize server handlers and commands for both dedicated & integrated servers
        com.domainpvp.server.DomainPvPServerMod.initializeServerCommon();

        System.out.println("[PvP Domain] Common initialization complete.");
    }
}

