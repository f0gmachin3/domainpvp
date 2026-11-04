package com.domainpvp.server;

import com.domainpvp.network.s2c.InvitationReceivedPayload;
import com.domainpvp.network.s2c.PartyUpdatedPayload;
import com.domainpvp.party.Party;
import com.domainpvp.party.PartyInvite;
import com.domainpvp.party.PartyManager;
import com.domainpvp.party.PartyMember;
import com.domainpvp.persistence.PersistenceManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dedicated server entrypoint and command registry for PvP Domain mod.
 */
public class DomainPvPServerMod implements DedicatedServerModInitializer {
    private static boolean initialized = false;

    @Override
    public void onInitializeServer() {
        initializeServerCommon();
    }

    public static synchronized void initializeServerCommon() {
        if (initialized) return;
        initialized = true;

        System.out.println("[PvP Domain] Initializing server systems (networking, commands, match controller)...");

        // Register server packet receivers
        ServerNetworkHandler.registerReceivers();

        // Register authoritative server tick loop
        ServerTickEvents.END_SERVER_TICK.register(ServerMatchController::tick);

        // Server lifecycle hooks
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            File saveDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile();
            PersistenceManager.getInstance().initialize(saveDir);
            System.out.println("[PvP Domain] Server started and persistence storage initialized.");
        });

        // Register in-game commands
        registerCommands();

        System.out.println("[PvP Domain] Server systems successfully initialized.");
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Direct shortcuts: /start, /ready, /unready, /accept
            dispatcher.register(CommandManager.literal("start")
                    .executes(ctx -> executeStart(ctx.getSource().getPlayer(), ctx.getSource().getServer())));

            dispatcher.register(CommandManager.literal("ready")
                    .executes(ctx -> executeReady(ctx.getSource().getPlayer(), ctx.getSource().getServer())));

            dispatcher.register(CommandManager.literal("unready")
                    .executes(ctx -> executeUnready(ctx.getSource().getPlayer(), ctx.getSource().getServer())));

            dispatcher.register(CommandManager.literal("accept")
                    .executes(ctx -> executeAccept(ctx.getSource().getPlayer(), ctx.getSource().getServer())));

            // Register /domain and /party command trees
            for (String baseName : List.of("domain", "party")) {
                dispatcher.register(CommandManager.literal(baseName)
                        .then(CommandManager.literal("start")
                                .executes(ctx -> executeStart(ctx.getSource().getPlayer(), ctx.getSource().getServer())))
                        .then(CommandManager.literal("ready")
                                .executes(ctx -> executeReady(ctx.getSource().getPlayer(), ctx.getSource().getServer())))
                        .then(CommandManager.literal("unready")
                                .executes(ctx -> executeUnready(ctx.getSource().getPlayer(), ctx.getSource().getServer())))
                        .then(CommandManager.literal("create")
                                .executes(ctx -> executeCreate(ctx.getSource().getPlayer(), ctx.getSource().getServer())))
                        .then(CommandManager.literal("accept")
                                .executes(ctx -> executeAccept(ctx.getSource().getPlayer(), ctx.getSource().getServer())))
                        .then(CommandManager.literal("leave")
                                .executes(ctx -> executeLeave(ctx.getSource().getPlayer(), ctx.getSource().getServer())))
                        .then(CommandManager.literal("invite")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .executes(ctx -> executeInvite(
                                                ctx.getSource().getPlayer(),
                                                StringArgumentType.getString(ctx, "player"),
                                                ctx.getSource().getServer()
                                        ))))
                );
            }
        });
    }

    public static int executeStart(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null) return 0;
        Party party = PartyManager.getInstance().getPartyByPlayer(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.literal("§c[PvP Domain] You are not in a party! Create one with /domain create"), false);
            return 0;
        }

        if (!party.getLeaderUuid().equals(player.getUuid())) {
            player.sendMessage(Text.literal("§c[PvP Domain] Only the party leader can start the match!"), false);
            return 0;
        }

        if (party.getMemberCount() < Party.MAX_MEMBERS) {
            player.sendMessage(Text.literal("§c[PvP Domain] Cannot start: Party must have 2 players! Invite an opponent with /domain invite <player>"), false);
            return 0;
        }

        if (!party.areAllMembersReady()) {
            player.sendMessage(Text.literal("§c[PvP Domain] All players are not ready!"), false);
            for (PartyMember member : party.getMembers()) {
                if (!member.isReady()) {
                    player.sendMessage(Text.literal("§e - " + member.getUsername() + " is NOT ready! Type /ready"), false);
                }
            }
            return 0;
        }

        List<PartyMember> members = new ArrayList<>(party.getMembers());
        ServerPlayerEntity p1 = server.getPlayerManager().getPlayer(members.get(0).getUuid());
        ServerPlayerEntity p2 = server.getPlayerManager().getPlayer(members.get(1).getUuid());

        if (p1 == null || p2 == null) {
            player.sendMessage(Text.literal("§c[PvP Domain] One or more party members are offline!"), false);
            return 0;
        }

        p1.sendMessage(Text.literal("§a[PvP Domain] Match starting! Teleporting to domain arena..."), false);
        p2.sendMessage(Text.literal("§a[PvP Domain] Match starting! Teleporting to domain arena..."), false);

        ServerMatchController.startDuelMatch(server, party, p1, p2);
        return 1;
    }

    public static int executeReady(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null) return 0;
        Party party = PartyManager.getInstance().getPartyByPlayer(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.literal("§c[PvP Domain] You are not in a party! Create one with /domain create"), false);
            return 0;
        }

        PartyManager.getInstance().setPlayerReady(player.getUuid(), true);
        ServerNetworkHandler.syncPartyState(server, party);
        player.sendMessage(Text.literal("§a[PvP Domain] You are now marked as READY!"), false);

        if (party.areAllMembersReady()) {
            for (PartyMember member : party.getMembers()) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(member.getUuid());
                if (p != null) {
                    if (member.getUuid().equals(party.getLeaderUuid())) {
                        p.sendMessage(Text.literal("§6[PvP Domain] §aAll players are READY! Type §2§l/start §ato begin duel!"), false);
                    } else {
                        p.sendMessage(Text.literal("§6[PvP Domain] §aAll players are READY! Waiting for party leader to type /start..."), false);
                    }
                }
            }
        }
        return 1;
    }

    public static int executeUnready(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null) return 0;
        Party party = PartyManager.getInstance().getPartyByPlayer(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.literal("§c[PvP Domain] You are not in a party!"), false);
            return 0;
        }

        PartyManager.getInstance().setPlayerReady(player.getUuid(), false);
        ServerNetworkHandler.syncPartyState(server, party);
        player.sendMessage(Text.literal("§e[PvP Domain] You are now marked as UNREADY."), false);
        return 1;
    }

    public static int executeAccept(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null) return 0;
        if (PartyManager.getInstance().getPartyByPlayer(player.getUuid()) != null) {
            player.sendMessage(Text.literal("§c[PvP Domain] You are already in a party!"), false);
            return 0;
        }

        PartyInvite invite = PartyManager.getInstance().getInviteForPlayer(player.getUuid());
        if (invite == null || invite.isExpired()) {
            player.sendMessage(Text.literal("§c[PvP Domain] No pending invitations found."), false);
            return 0;
        }

        boolean success = PartyManager.getInstance().acceptInvite(player.getUuid(), player.getName().getString(), invite.partyId());
        if (success) {
            Party party = PartyManager.getInstance().getPartyById(invite.partyId());
            ServerNetworkHandler.syncPartyState(server, party);
            player.sendMessage(Text.literal("§a[PvP Domain] Joined the party!"), false);
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(invite.leaderUuid());
            if (leader != null) {
                leader.sendMessage(Text.literal("§a[PvP Domain] " + player.getName().getString() + " joined the party!"), false);
            }
            return 1;
        } else {
            player.sendMessage(Text.literal("§c[PvP Domain] Failed to join party. It may be full or expired."), false);
            return 0;
        }
    }

    public static int executeCreate(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null) return 0;
        Party party = PartyManager.getInstance().createParty(player.getUuid(), player.getName().getString());
        if (party != null) {
            ServerNetworkHandler.syncPartyState(server, party);
            player.sendMessage(Text.literal("§a[PvP Domain] Party created! You are the leader. Invite an opponent with /domain invite <player>"), false);
            return 1;
        } else {
            player.sendMessage(Text.literal("§c[PvP Domain] You are already in a party!"), false);
            return 0;
        }
    }

    public static int executeLeave(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null) return 0;
        Party party = PartyManager.getInstance().leaveParty(player.getUuid());
        ServerPlayNetworking.send(
                player,
                new PartyUpdatedPayload(new UUID(0, 0), new UUID(0, 0), "", "", false, "", "", "", false, "")
        );
        player.sendMessage(Text.literal("§e[PvP Domain] Left party."), false);
        if (party != null) {
            ServerNetworkHandler.syncPartyState(server, party);
        }
        return 1;
    }

    public static int executeInvite(ServerPlayerEntity player, String targetName, MinecraftServer server) {
        if (player == null) return 0;
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Text.literal("§c[PvP Domain] Player '" + targetName + "' is not online."), false);
            return 0;
        }
        PartyInvite invite = PartyManager.getInstance().invitePlayer(player.getUuid(), target.getUuid(), targetName);
        if (invite != null) {
            player.sendMessage(Text.literal("§a[PvP Domain] Invited " + targetName + " to your party."), false);
            ServerPlayNetworking.send(
                    target,
                    new InvitationReceivedPayload(invite.partyId(), player.getName().getString())
            );
            target.sendMessage(Text.literal("§b[PvP Domain] " + player.getName().getString() + " invited you to a duel! Type §e/domain accept §bor §e/accept §bto join."), false);
            return 1;
        } else {
            player.sendMessage(Text.literal("§c[PvP Domain] Could not send invite. Are you the party leader? Is party full?"), false);
            return 0;
        }
    }
}
