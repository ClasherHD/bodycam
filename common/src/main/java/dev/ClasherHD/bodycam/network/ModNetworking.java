package dev.ClasherHD.bodycam.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {
    public static void init() {
        if (dev.ClasherHD.bodycam.util.BodycamHelper.isServer()) {
            NetworkManager.registerS2CPayloadType(SyncBodycamStatesS2CPacket.TYPE, SyncBodycamStatesS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(CrossObservationSyncPacket.TYPE, CrossObservationSyncPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(DimensionLocatorResponsePacket.TYPE, DimensionLocatorResponsePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(BodycamForceClosePacket.TYPE, BodycamForceClosePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(BodycamResetCameraS2CPacket.TYPE, BodycamResetCameraS2CPacket.STREAM_CODEC);
        }

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BodycamSetCameraPacket.TYPE, BodycamSetCameraPacket.STREAM_CODEC, ServerNetworking::handleSetCamera);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BodycamResetCameraPacket.TYPE, BodycamResetCameraPacket.STREAM_CODEC, (payload, context) -> {
            context.queue(() -> {
                ServerNetworking.executeReset((ServerPlayer) context.getPlayer());
            });
        });
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncBodycamRequestC2SPacket.TYPE, SyncBodycamRequestC2SPacket.STREAM_CODEC, ServerNetworking::handleSyncRequest);
    }

    private static boolean clientInitialized = false;

    public static void initClient() {
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, BodycamResetCameraS2CPacket.TYPE, BodycamResetCameraS2CPacket.STREAM_CODEC, ClientNetworking::handleResetCamera);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncBodycamStatesS2CPacket.TYPE, SyncBodycamStatesS2CPacket.STREAM_CODEC, ClientNetworking::handleSyncStates);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CrossObservationSyncPacket.TYPE, CrossObservationSyncPacket.STREAM_CODEC, ClientNetworking::handleCrossObservationSync);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DimensionLocatorResponsePacket.TYPE, DimensionLocatorResponsePacket.STREAM_CODEC, ClientNetworking::handleDimensionLocatorResponse);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, BodycamForceClosePacket.TYPE, BodycamForceClosePacket.STREAM_CODEC, ClientNetworking::handleForceClose);
    }
}
