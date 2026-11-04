package com.domainpvp.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * C2S request from an invited player accepting or declining a party invitation.
 */
public record AcceptInviteRequestPayload(UUID partyId, boolean accept) implements CustomPayload {
    public static final CustomPayload.Id<AcceptInviteRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "accept_invite_request"));

    public static final PacketCodec<PacketByteBuf, AcceptInviteRequestPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, AcceptInviteRequestPayload::partyId,
            PacketCodecs.BOOL, AcceptInviteRequestPayload::accept,
            AcceptInviteRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

