package com.domainpvp.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S request to form a new 2-player party with the sender as leader.
 */
public record CreatePartyRequestPayload(String partyName) implements CustomPayload {
    public static final CustomPayload.Id<CreatePartyRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "create_party_request"));

    public static final PacketCodec<PacketByteBuf, CreatePartyRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, CreatePartyRequestPayload::partyName,
            CreatePartyRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
