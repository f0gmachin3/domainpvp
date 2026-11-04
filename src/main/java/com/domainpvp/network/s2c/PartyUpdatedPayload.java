package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * S2C synchronization of the 2-member party roster, readiness, and loadout names.
 */
public record PartyUpdatedPayload(
        UUID partyId,
        UUID leaderUuid,
        String member1Uuid,
        String member1Name,
        boolean member1Ready,
        String member1Loadout,
        String member2Uuid,
        String member2Name,
        boolean member2Ready,
        String member2Loadout
) implements CustomPayload {
    public static final CustomPayload.Id<PartyUpdatedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "party_updated"));

    public static final PacketCodec<PacketByteBuf, PartyUpdatedPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                Uuids.PACKET_CODEC.encode(buf, value.partyId());
                Uuids.PACKET_CODEC.encode(buf, value.leaderUuid());
                PacketCodecs.STRING.encode(buf, value.member1Uuid());
                PacketCodecs.STRING.encode(buf, value.member1Name());
                PacketCodecs.BOOL.encode(buf, value.member1Ready());
                PacketCodecs.STRING.encode(buf, value.member1Loadout());
                PacketCodecs.STRING.encode(buf, value.member2Uuid());
                PacketCodecs.STRING.encode(buf, value.member2Name());
                PacketCodecs.BOOL.encode(buf, value.member2Ready());
                PacketCodecs.STRING.encode(buf, value.member2Loadout());
            },
            buf -> new PartyUpdatedPayload(
                    Uuids.PACKET_CODEC.decode(buf),
                    Uuids.PACKET_CODEC.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.BOOL.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.BOOL.decode(buf),
                    PacketCodecs.STRING.decode(buf)
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

