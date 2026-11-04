package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C progress update on progressive arena block manifestation.
 */
public record ArenaConstructionProgressPayload(int blocksPlaced, int totalBlocks) implements CustomPayload {
    public static final CustomPayload.Id<ArenaConstructionProgressPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "arena_construction_progress"));

    public static final PacketCodec<PacketByteBuf, ArenaConstructionProgressPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ArenaConstructionProgressPayload::blocksPlaced,
            PacketCodecs.VAR_INT, ArenaConstructionProgressPayload::totalBlocks,
            ArenaConstructionProgressPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

