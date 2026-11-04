package com.domainpvp.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S request marking a player as ready or not ready.
 */
public record PlayerReadyRequestPayload(boolean ready) implements CustomPayload {
    public static final CustomPayload.Id<PlayerReadyRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "player_ready_request"));

    public static final PacketCodec<PacketByteBuf, PlayerReadyRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, PlayerReadyRequestPayload::ready,
            PlayerReadyRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

