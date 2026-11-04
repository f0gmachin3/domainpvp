package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * S2C notification alerting an invited player of a duel invitation.
 */
public record InvitationReceivedPayload(UUID partyId, String leaderName) implements CustomPayload {
    public static final CustomPayload.Id<InvitationReceivedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "invitation_received"));

    public static final PacketCodec<PacketByteBuf, InvitationReceivedPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, InvitationReceivedPayload::partyId,
            PacketCodecs.STRING, InvitationReceivedPayload::leaderName,
            InvitationReceivedPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

