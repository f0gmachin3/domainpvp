package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C notification confirming the temporary domain has completely collapsed and been released.
 */
public record DomainDestroyedPayload() implements CustomPayload {
    public static final CustomPayload.Id<DomainDestroyedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "domain_destroyed"));

    public static final PacketCodec<PacketByteBuf, DomainDestroyedPayload> CODEC =
            PacketCodec.unit(new DomainDestroyedPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

