package com.domainpvp.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S request explicitly cancelling ready status.
 */
public record PlayerUnreadyRequestPayload() implements CustomPayload {
    public static final CustomPayload.Id<PlayerUnreadyRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "player_unready_request"));

    public static final PacketCodec<PacketByteBuf, PlayerUnreadyRequestPayload> CODEC =
            PacketCodec.unit(new PlayerUnreadyRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

