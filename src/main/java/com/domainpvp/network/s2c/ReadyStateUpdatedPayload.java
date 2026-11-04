package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * S2C notification of a participant's readiness status change.
 */
public record ReadyStateUpdatedPayload(UUID playerUuid, boolean ready) implements CustomPayload {
    public static final CustomPayload.Id<ReadyStateUpdatedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "ready_state_updated"));

    public static final PacketCodec<PacketByteBuf, ReadyStateUpdatedPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, ReadyStateUpdatedPayload::playerUuid,
            PacketCodecs.BOOL, ReadyStateUpdatedPayload::ready,
            ReadyStateUpdatedPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

