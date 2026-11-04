package com.domainpvp.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S request from party leader to invite a target player by username.
 */
public record InvitePlayerRequestPayload(String targetUsername) implements CustomPayload {
    public static final CustomPayload.Id<InvitePlayerRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "invite_player_request"));

    public static final PacketCodec<PacketByteBuf, InvitePlayerRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, InvitePlayerRequestPayload::targetUsername,
            InvitePlayerRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

