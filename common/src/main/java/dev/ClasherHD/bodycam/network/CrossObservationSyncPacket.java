package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.UUIDUtil;
import java.util.UUID;

public record CrossObservationSyncPacket(UUID observerId, boolean isObserving) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CrossObservationSyncPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bodycam", "cross_sync"));
    public static final StreamCodec<FriendlyByteBuf, CrossObservationSyncPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, CrossObservationSyncPacket::observerId,
        ByteBufCodecs.BOOL, CrossObservationSyncPacket::isObserving,
        CrossObservationSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
