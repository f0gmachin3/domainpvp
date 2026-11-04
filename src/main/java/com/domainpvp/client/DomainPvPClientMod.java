package com.domainpvp.client;

import com.domainpvp.client.effects.DomainEffectManager;
import com.domainpvp.client.ui.ClientUIManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * Client-side entrypoint for PvP Domain mod.
 */
public class DomainPvPClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("[PvP Domain] Initializing client subsystems...");

        // Register client packet receivers
        ClientNetworkHandler.registerReceivers();

        // Register HUD rendering callback
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            ClientUIManager.getInstance().renderCombatHud(drawContext, tickCounter.getTickDelta(true));
        });

        // Register client tick for domain perimeter particles
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            DomainEffectManager.getInstance().tickBoundaryParticles();
        });

        System.out.println("[PvP Domain] Client subsystems initialized.");
    }
}

