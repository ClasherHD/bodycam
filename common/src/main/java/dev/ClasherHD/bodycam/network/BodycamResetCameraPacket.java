package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BodycamResetCameraPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BodycamResetCameraPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bodycam", "reset_camera"));

    public static final StreamCodec<FriendlyByteBuf, BodycamResetCameraPacket> STREAM_CODEC = StreamCodec.unit(new BodycamResetCameraPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
