package com.domainpvp.network;

import com.domainpvp.network.c2s.*;
import com.domainpvp.network.s2c.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers all C2S and S2C custom payload channels with Fabric API.
 */
public class NetworkingManager {

    public static void registerAllPayloads() {
        // Register Client-to-Server (C2S) Payloads
        PayloadTypeRegistry.playC2S().register(CreatePartyRequestPayload.ID, CreatePartyRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(InvitePlayerRequestPayload.ID, InvitePlayerRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AcceptInviteRequestPayload.ID, AcceptInviteRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LeavePartyRequestPayload.ID, LeavePartyRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectLoadoutRequestPayload.ID, SelectLoadoutRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlayerReadyRequestPayload.ID, PlayerReadyRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlayerUnreadyRequestPayload.ID, PlayerUnreadyRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RematchRequestPayload.ID, RematchRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(QuitDomainRequestPayload.ID, QuitDomainRequestPayload.CODEC);

        // Register Server-to-Client (S2C) Payloads
        PayloadTypeRegistry.playS2C().register(PartyCreatedPayload.ID, PartyCreatedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(InvitationReceivedPayload.ID, InvitationReceivedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyUpdatedPayload.ID, PartyUpdatedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LoadoutValidatedPayload.ID, LoadoutValidatedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ReadyStateUpdatedPayload.ID, ReadyStateUpdatedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ArenaConstructionStartedPayload.ID, ArenaConstructionStartedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ArenaConstructionProgressPayload.ID, ArenaConstructionProgressPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ArenaConstructionCompletedPayload.ID, ArenaConstructionCompletedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CountdownUpdatedPayload.ID, CountdownUpdatedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MatchStartedPayload.ID, MatchStartedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MatchFinishedPayload.ID, MatchFinishedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RematchVoteUpdatedPayload.ID, RematchVoteUpdatedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CleanupStartedPayload.ID, CleanupStartedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DomainDestroyedPayload.ID, DomainDestroyedPayload.CODEC);
    }
}

