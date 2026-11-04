package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C notification that domain teardown and player restoration has begun.
 */
public record CleanupStartedPayload() implements CustomPayload {
    public static final CustomPayload.Id<CleanupStartedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "cleanup_started"));

    public static final PacketCodec<PacketByteBuf, CleanupStartedPayload> CODEC =
            PacketCodec.unit(new CleanupStartedPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

