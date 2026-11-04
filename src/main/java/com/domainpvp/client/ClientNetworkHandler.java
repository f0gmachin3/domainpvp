package com.domainpvp.client;

import com.domainpvp.client.effects.DomainEffectManager;
import com.domainpvp.client.ui.ClientUIManager;
import com.domainpvp.network.s2c.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Client-side packet reception and interface dispatcher.
 */
public class ClientNetworkHandler {

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PartyCreatedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.sendMessage(Text.literal("§a[PvP Domain] Party created successfully!"), false);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(InvitationReceivedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.sendMessage(Text.literal("§b[PvP Domain] Duel invitation from " + payload.leaderName() + "! Type /domain accept"), false);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PartyUpdatedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientUIManager.getInstance().updateParty(payload);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(LoadoutValidatedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player != null) {
                    if (payload.valid()) {
                        context.client().player.sendMessage(Text.literal("§a[PvP Domain] Loadout validated: " + payload.message()), false);
                    } else {
                        context.client().player.sendMessage(Text.literal("§c[PvP Domain] Loadout rejected: " + payload.message()), false);
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ArenaConstructionStartedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                DomainEffectManager.getInstance().onArenaStarted(payload.originX(), payload.originY(), payload.originZ(), 20.0);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ArenaConstructionProgressPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Construction tick
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ArenaConstructionCompletedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Arena ready
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CountdownUpdatedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientUIManager.getInstance().setCountdown(payload.remainingSeconds());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(MatchStartedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientUIManager.getInstance().setCombatActive(true);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(MatchFinishedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientUIManager.getInstance().setMatchResult(payload);
                MinecraftClient mc = context.client();
                if (mc.player != null) {
                    boolean won = mc.player.getUuid().equals(payload.winnerUuid());
                    if (won) {
                        mc.player.sendMessage(Text.translatable("domainpvp.match.victory", mc.player.getName().getString()), false);
                    } else {
                        mc.player.sendMessage(Text.translatable("domainpvp.match.defeat"), false);
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CleanupStartedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientUIManager.getInstance().reset();
                DomainEffectManager.getInstance().onDomainCollapsed();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(DomainDestroyedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientUIManager.getInstance().reset();
            });
        });
    }
}

