package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.ByteBufCodecs;

public record SyncBodycamRequestC2SPacket(boolean hasReach, boolean isOnHologram) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncBodycamRequestC2SPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bodycam", "sync_request"));
    public static final StreamCodec<FriendlyByteBuf, SyncBodycamRequestC2SPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, SyncBodycamRequestC2SPacket::hasReach,
        ByteBufCodecs.BOOL, SyncBodycamRequestC2SPacket::isOnHologram,
        SyncBodycamRequestC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
