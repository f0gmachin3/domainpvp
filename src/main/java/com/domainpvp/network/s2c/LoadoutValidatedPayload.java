package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C acknowledgment of loadout validity from server-side verification.
 */
public record LoadoutValidatedPayload(boolean valid, String message) implements CustomPayload {
    public static final CustomPayload.Id<LoadoutValidatedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "loadout_validated"));

    public static final PacketCodec<PacketByteBuf, LoadoutValidatedPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, LoadoutValidatedPayload::valid,
            PacketCodecs.STRING, LoadoutValidatedPayload::message,
            LoadoutValidatedPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

