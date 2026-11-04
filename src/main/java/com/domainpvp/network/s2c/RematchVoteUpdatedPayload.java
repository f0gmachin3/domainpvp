package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * S2C synchronization of a player's rematch decision.
 */
public record RematchVoteUpdatedPayload(UUID playerUuid, boolean agreed) implements CustomPayload {
    public static final CustomPayload.Id<RematchVoteUpdatedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "rematch_vote_updated"));

    public static final PacketCodec<PacketByteBuf, RematchVoteUpdatedPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, RematchVoteUpdatedPayload::playerUuid,
            PacketCodecs.BOOL, RematchVoteUpdatedPayload::agreed,
            RematchVoteUpdatedPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

