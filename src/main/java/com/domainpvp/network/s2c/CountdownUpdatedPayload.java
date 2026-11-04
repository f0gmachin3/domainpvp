package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C synchronized countdown tick (5, 4, 3, 2, 1, 0 = FIGHT).
 */
public record CountdownUpdatedPayload(int remainingSeconds) implements CustomPayload {
    public static final CustomPayload.Id<CountdownUpdatedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "countdown_updated"));

    public static final PacketCodec<PacketByteBuf, CountdownUpdatedPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, CountdownUpdatedPayload::remainingSeconds,
            CountdownUpdatedPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

