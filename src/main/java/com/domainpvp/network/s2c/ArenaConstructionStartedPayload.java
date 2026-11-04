package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C notification initiating progressive domain arena manifestation.
 */
public record ArenaConstructionStartedPayload(int originX, int originY, int originZ, int totalBlocks) implements CustomPayload {
    public static final CustomPayload.Id<ArenaConstructionStartedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "arena_construction_started"));

    public static final PacketCodec<PacketByteBuf, ArenaConstructionStartedPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ArenaConstructionStartedPayload::originX,
            PacketCodecs.VAR_INT, ArenaConstructionStartedPayload::originY,
            PacketCodecs.VAR_INT, ArenaConstructionStartedPayload::originZ,
            PacketCodecs.VAR_INT, ArenaConstructionStartedPayload::totalBlocks,
            ArenaConstructionStartedPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

