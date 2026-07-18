package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.UUIDUtil;
import java.util.UUID;

public record BodycamSetCameraPacket(UUID targetId, boolean hasReach, boolean isOnHologram) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BodycamSetCameraPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bodycam", "set_camera"));

    public static final StreamCodec<FriendlyByteBuf, BodycamSetCameraPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, BodycamSetCameraPacket::targetId,
        ByteBufCodecs.BOOL, BodycamSetCameraPacket::hasReach,
        ByteBufCodecs.BOOL, BodycamSetCameraPacket::isOnHologram,
        BodycamSetCameraPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
