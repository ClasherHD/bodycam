package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BodycamForceClosePacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BodycamForceClosePacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bodycam", "force_close"));
    public static final StreamCodec<FriendlyByteBuf, BodycamForceClosePacket> STREAM_CODEC = StreamCodec.unit(new BodycamForceClosePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
