package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BodycamResetCameraS2CPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BodycamResetCameraS2CPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("bodycam", "reset_camera_s2c"));

    public static final StreamCodec<FriendlyByteBuf, BodycamResetCameraS2CPacket> STREAM_CODEC = StreamCodec.unit(new BodycamResetCameraS2CPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
