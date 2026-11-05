package com.domainpvp.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C payload instructing client to display the 6-icon Kit Selection GUI while on the spawn stone.
 */
public record OpenKitSelectionPayload(int votingSeconds) implements CustomPayload {
    public static final CustomPayload.Id<OpenKitSelectionPayload> ID =
            new CustomPayload.Id<>(Identifier.of("domainpvp", "open_kit_selection"));

    public static final PacketCodec<PacketByteBuf, OpenKitSelectionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, OpenKitSelectionPayload::votingSeconds,
            OpenKitSelectionPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

