package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * S2C announcement of duel outcome and statistical summary.
 */
public record MatchFinishedPayload(
        UUID winnerUuid,
        UUID loserUuid,
        float player1Damage,
        float player2Damage,
        int player1Hits,
        int player2Hits,
        long durationSeconds
) implements CustomPayload {
    public static final CustomPayload.Id<MatchFinishedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "match_finished"));

    public static final PacketCodec<PacketByteBuf, MatchFinishedPayload> CODEC = PacketCodec.of(
            (val, buf) -> {
                Uuids.PACKET_CODEC.encode(buf, val.winnerUuid());
                Uuids.PACKET_CODEC.encode(buf, val.loserUuid());
                PacketCodecs.FLOAT.encode(buf, val.player1Damage());
                PacketCodecs.FLOAT.encode(buf, val.player2Damage());
                PacketCodecs.VAR_INT.encode(buf, val.player1Hits());
                PacketCodecs.VAR_INT.encode(buf, val.player2Hits());
                PacketCodecs.VAR_LONG.encode(buf, val.durationSeconds());
            },
            buf -> new MatchFinishedPayload(
                    Uuids.PACKET_CODEC.decode(buf),
                    Uuids.PACKET_CODEC.decode(buf),
                    PacketCodecs.FLOAT.decode(buf),
                    PacketCodecs.FLOAT.decode(buf),
                    PacketCodecs.VAR_INT.decode(buf),
                    PacketCodecs.VAR_INT.decode(buf),
                    PacketCodecs.VAR_LONG.decode(buf)
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

