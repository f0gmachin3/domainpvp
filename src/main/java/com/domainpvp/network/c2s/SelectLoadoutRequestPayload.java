package com.domainpvp.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S request submitting a chosen PvP preset or custom loadout definition.
 */
public record SelectLoadoutRequestPayload(String presetName, String customLoadoutJson) implements CustomPayload {
    public static final CustomPayload.Id<SelectLoadoutRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "select_loadout_request"));

    public static final PacketCodec<PacketByteBuf, SelectLoadoutRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, SelectLoadoutRequestPayload::presetName,
            PacketCodecs.STRING, SelectLoadoutRequestPayload::customLoadoutJson,
            SelectLoadoutRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

