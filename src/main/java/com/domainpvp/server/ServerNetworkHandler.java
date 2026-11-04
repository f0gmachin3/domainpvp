package com.domainpvp.server;

import com.domainpvp.arena.ArenaInstance;
import com.domainpvp.arena.ArenaManager;
import com.domainpvp.loadout.Loadout;
import com.domainpvp.loadout.LoadoutManager;
import com.domainpvp.match.MatchManager;
import com.domainpvp.match.MatchSession;
import com.domainpvp.match.MatchState;
import com.domainpvp.network.c2s.*;
import com.domainpvp.network.s2c.*;
import com.domainpvp.party.Party;
import com.domainpvp.party.PartyInvite;
import com.domainpvp.party.PartyManager;
import com.domainpvp.party.PartyMember;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Authoritative processor and router for client requests.
 */
public class ServerNetworkHandler {
    private static final Gson GSON = new Gson();

    public static void registerReceivers() {
        // C2S: CreatePartyRequest
        ServerPlayNetworking.registerGlobalReceiver(CreatePartyRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                Party party = PartyManager.getInstance().createParty(player.getUuid(), player.getName().getString());
                if (party != null) {
                    ServerPlayNetworking.send(player, new PartyCreatedPayload(party.getId(), player.getUuid()));
                    syncPartyState(context.server(), party);
                    player.sendMessage(Text.translatable("domainpvp.party.created"), false);
                } else {
                    player.sendMessage(Text.literal("§cYou are already in a party or have an active session."), false);
                }
            });
        });

        // C2S: InvitePlayerRequest
        ServerPlayNetworking.registerGlobalReceiver(InvitePlayerRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity leader = context.player();
            String targetName = payload.targetUsername();
            context.server().execute(() -> {
                ServerPlayerEntity target = context.server().getPlayerManager().getPlayer(targetName);
                if (target == null) {
                    leader.sendMessage(Text.literal("§cPlayer '" + targetName + "' is not online."), false);
                    return;
                }
                PartyInvite invite = PartyManager.getInstance().invitePlayer(leader.getUuid(), target.getUuid(), targetName);
                if (invite != null) {
                    leader.sendMessage(Text.literal("§aInvitation sent to " + targetName + "."), false);
                    ServerPlayNetworking.send(target, new InvitationReceivedPayload(invite.partyId(), leader.getName().getString()));
                    target.sendMessage(Text.literal("§b" + leader.getName().getString() + " invited you to a PvP Domain duel! Type /domain accept to join."), false);
                } else {
                    leader.sendMessage(Text.literal("§cCould not send invite. Check party status or target availability."), false);
                }
            });
        });

        // C2S: AcceptInviteRequest
        ServerPlayNetworking.registerGlobalReceiver(AcceptInviteRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (payload.accept()) {
                    boolean success = PartyManager.getInstance().acceptInvite(player.getUuid(), player.getName().getString(), payload.partyId());
                    if (success) {
                        Party party = PartyManager.getInstance().getPartyById(payload.partyId());
                        syncPartyState(context.server(), party);
                        player.sendMessage(Text.literal("§aJoined the PvP Domain party!"), false);
                    } else {
                        player.sendMessage(Text.literal("§cFailed to join party. Invitation expired or party full."), false);
                    }
                } else {
                    PartyManager.getInstance().declineInvite(player.getUuid(), payload.partyId());
                    player.sendMessage(Text.literal("§eDeclined invitation."), false);
                }
            });
        });

        // C2S: LeavePartyRequest
        ServerPlayNetworking.registerGlobalReceiver(LeavePartyRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                Party party = PartyManager.getInstance().leaveParty(player.getUuid());
                player.sendMessage(Text.literal("§eYou left the party."), false);
                ServerPlayNetworking.send(player, new PartyUpdatedPayload(new UUID(0, 0), new UUID(0, 0), "", "", false, "", "", "", false, ""));
                if (party != null) {
                    syncPartyState(context.server(), party);
                }
            });
        });

        // C2S: SelectLoadoutRequest
        ServerPlayNetworking.registerGlobalReceiver(SelectLoadoutRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                Loadout loadout;
                if (payload.presetName() != null && !payload.presetName().equalsIgnoreCase("CUSTOM")) {
                    loadout = LoadoutManager.getInstance().getPreset(payload.presetName());
                } else {
                    try {
                        loadout = GSON.fromJson(payload.customLoadoutJson(), Loadout.class);
                    } catch (Exception e) {
                        loadout = null;
                    }
                }

                LoadoutManager.ValidationResult result = LoadoutManager.getInstance().validate(loadout);
                ServerPlayNetworking.send(player, new LoadoutValidatedPayload(result.valid(), result.message()));

                if (result.valid() && loadout != null) {
                    PartyManager.getInstance().setPlayerLoadout(player.getUuid(), loadout.getName());
                    Party party = PartyManager.getInstance().getPartyByPlayer(player.getUuid());
                    if (party != null) {
                        syncPartyState(context.server(), party);
                    }
                }
            });
        });

        // C2S: PlayerReadyRequest
        ServerPlayNetworking.registerGlobalReceiver(PlayerReadyRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                Party party = PartyManager.getInstance().getPartyByPlayer(player.getUuid());
                if (party == null) return;

                PartyManager.getInstance().setPlayerReady(player.getUuid(), payload.ready());
                syncPartyState(context.server(), party);

                if (party.areAllMembersReady()) {
                    for (PartyMember member : party.getMembers()) {
                        ServerPlayerEntity p = context.server().getPlayerManager().getPlayer(member.getUuid());
                        if (p != null) {
                            if (member.getUuid().equals(party.getLeaderUuid())) {
                                p.sendMessage(Text.literal("§6[PvP Domain] §aAll players are READY! Type §2§l/start §ato begin duel!"), false);
                            } else {
                                p.sendMessage(Text.literal("§6[PvP Domain] §aAll players are READY! Waiting for party leader to type /start..."), false);
                            }
                        }
                    }
                }
            });
        });

        // C2S: PlayerUnreadyRequest
        ServerPlayNetworking.registerGlobalReceiver(PlayerUnreadyRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                Party party = PartyManager.getInstance().getPartyByPlayer(player.getUuid());
                if (party != null) {
                    PartyManager.getInstance().setPlayerReady(player.getUuid(), false);
                    syncPartyState(context.server(), party);
                }
            });
        });

        // C2S: RematchRequest
        ServerPlayNetworking.registerGlobalReceiver(RematchRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                MatchSession match = MatchManager.getInstance().getMatchByPlayer(player.getUuid());
                if (match != null && match.getState() == MatchState.MATCH_FINISHED) {
                    match.voteRematch(player.getUuid(), payload.agree());

                    UUID opponentUuid = match.getOpponent(player.getUuid());
                    ServerPlayerEntity opponent = context.server().getPlayerManager().getPlayer(opponentUuid);

                    ServerPlayNetworking.send(player, new RematchVoteUpdatedPayload(player.getUuid(), payload.agree()));
                    if (opponent != null) {
                        ServerPlayNetworking.send(opponent, new RematchVoteUpdatedPayload(player.getUuid(), payload.agree()));
                    }

                    if (match.hasAnyDeclinedRematch()) {
                        ServerMatchController.cleanupMatch(context.server(), match);
                    } else if (match.haveBothAgreedRematch()) {
                        ServerMatchController.triggerRematch(context.server(), match);
                    }
                }
            });
        });

        // C2S: QuitDomainRequest
        ServerPlayNetworking.registerGlobalReceiver(QuitDomainRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                MatchSession match = MatchManager.getInstance().getMatchByPlayer(player.getUuid());
                if (match != null) {
                    ServerMatchController.cleanupMatch(context.server(), match);
                }
            });
        });
    }

    public static void syncPartyState(net.minecraft.server.MinecraftServer server, Party party) {
        if (party == null) return;
        List<PartyMember> list = new ArrayList<>(party.getMembers());
        PartyMember m1 = list.size() > 0 ? list.get(0) : null;
        PartyMember m2 = list.size() > 1 ? list.get(1) : null;

        PartyUpdatedPayload payload = new PartyUpdatedPayload(
                party.getId(),
                party.getLeaderUuid(),
                m1 != null ? m1.getUuid().toString() : "",
                m1 != null ? m1.getUsername() : "",
                m1 != null && m1.isReady(),
                m1 != null ? m1.getSelectedLoadoutName() : "CLASSIC",
                m2 != null ? m2.getUuid().toString() : "",
                m2 != null ? m2.getUsername() : "",
                m2 != null && m2.isReady(),
                m2 != null ? m2.getSelectedLoadoutName() : "CLASSIC"
        );

        for (PartyMember member : party.getMembers()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(member.getUuid());
            if (player != null) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}

