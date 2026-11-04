package com.domainpvp.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S request casting a vote on whether to rematch.
 */
public record RematchRequestPayload(boolean agree) implements CustomPayload {
    public static final CustomPayload.Id<RematchRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "rematch_request"));

    public static final PacketCodec<PacketByteBuf, RematchRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, RematchRequestPayload::agree,
            RematchRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

