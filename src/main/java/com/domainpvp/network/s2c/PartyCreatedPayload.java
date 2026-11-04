package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * S2C notification that a party was successfully established.
 */
public record PartyCreatedPayload(UUID partyId, UUID leaderUuid) implements CustomPayload {
    public static final CustomPayload.Id<PartyCreatedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "party_created"));

    public static final PacketCodec<PacketByteBuf, PartyCreatedPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, PartyCreatedPayload::partyId,
            Uuids.PACKET_CODEC, PartyCreatedPayload::leaderUuid,
            PartyCreatedPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

