package com.domainpvp.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S request from a participant opting to exit the domain and trigger cleanup.
 */
public record QuitDomainRequestPayload() implements CustomPayload {
    public static final CustomPayload.Id<QuitDomainRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "quit_domain_request"));

    public static final PacketCodec<PacketByteBuf, QuitDomainRequestPayload> CODEC =
            PacketCodec.unit(new QuitDomainRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

