package com.domainpvp.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S request from a member to depart their current party.
 */
public record LeavePartyRequestPayload() implements CustomPayload {
    public static final CustomPayload.Id<LeavePartyRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "leave_party_request"));

    public static final PacketCodec<PacketByteBuf, LeavePartyRequestPayload> CODEC =
            PacketCodec.unit(new LeavePartyRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

