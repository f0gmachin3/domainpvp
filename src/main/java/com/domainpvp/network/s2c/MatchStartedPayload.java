package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C notification that countdown concluded and combat is officially active.
 */
public record MatchStartedPayload() implements CustomPayload {
    public static final CustomPayload.Id<MatchStartedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "match_started"));

    public static final PacketCodec<PacketByteBuf, MatchStartedPayload> CODEC =
            PacketCodec.unit(new MatchStartedPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

