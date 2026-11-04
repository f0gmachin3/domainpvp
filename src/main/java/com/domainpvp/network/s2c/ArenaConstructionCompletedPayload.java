package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C notification that arena block manifestation is complete and players can be positioned.
 */
public record ArenaConstructionCompletedPayload(
        double spawn1X, double spawn1Y, double spawn1Z,
        double spawn2X, double spawn2Y, double spawn2Z
) implements CustomPayload {
    public static final CustomPayload.Id<ArenaConstructionCompletedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "arena_construction_completed"));

    public static final PacketCodec<PacketByteBuf, ArenaConstructionCompletedPayload> CODEC = PacketCodec.of(
            (val, buf) -> {
                PacketCodecs.DOUBLE.encode(buf, val.spawn1X());
                PacketCodecs.DOUBLE.encode(buf, val.spawn1Y());
                PacketCodecs.DOUBLE.encode(buf, val.spawn1Z());
                PacketCodecs.DOUBLE.encode(buf, val.spawn2X());
                PacketCodecs.DOUBLE.encode(buf, val.spawn2Y());
                PacketCodecs.DOUBLE.encode(buf, val.spawn2Z());
            },
            buf -> new ArenaConstructionCompletedPayload(
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf)
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

