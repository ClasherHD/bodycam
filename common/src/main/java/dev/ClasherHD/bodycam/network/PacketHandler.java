package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.network.bodycam.BodycamExitSyncS2CPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.CrossObservationSyncPacket;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamRequestC2SPacket;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamStatesS2CPacket;
import dev.ClasherHD.bodycam.network.config.OpenClientConfigS2CPacket;
import dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket;
import dev.ClasherHD.bodycam.network.config.ReloadClientConfigS2CPacket;
import dev.ClasherHD.bodycam.network.config.SaveServerConfigC2SPacket;
import dev.ClasherHD.bodycam.network.creepingdeath.CreepingDeathActionC2SPacket;
import dev.ClasherHD.bodycam.network.creepingdeath.OpenCreepingDeathScreenS2CPacket;
import dev.ClasherHD.bodycam.network.locator.DimensionLocatorRequestPacket;
import dev.ClasherHD.bodycam.network.locator.DimensionLocatorResponsePacket;
import dev.ClasherHD.bodycam.network.locator.DimensionLocatorSelectC2SPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorSelectC2SPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorStructureUpdateS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorSyncS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorTargetUpdateS2CPacket;
import dev.ClasherHD.bodycam.network.logbook.LogBookActionC2SPacket;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookPinScreenS2CPacket;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookScreenS2CPacket;
import dev.ClasherHD.bodycam.network.portal.PortablePortalActionC2SPacket;
import dev.ClasherHD.bodycam.network.watchpoint.WatchPointLookC2SPacket;
import dev.ClasherHD.bodycam.network.watchpoint.WatchPointOpenRenameS2CPacket;
import dev.ClasherHD.bodycam.network.watchpoint.WatchPointRenameC2SPacket;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PacketHandler {
    private static boolean initialized = false;

    private PacketHandler() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BodycamSetCameraPacket.TYPE, BodycamSetCameraPacket.STREAM_CODEC, BodycamSetCameraPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BodycamResetCameraPacket.TYPE, BodycamResetCameraPacket.STREAM_CODEC, BodycamResetCameraPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncBodycamRequestC2SPacket.TYPE, SyncBodycamRequestC2SPacket.STREAM_CODEC, SyncBodycamRequestC2SPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, WatchPointLookC2SPacket.TYPE, WatchPointLookC2SPacket.STREAM_CODEC, WatchPointLookC2SPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, WatchPointRenameC2SPacket.TYPE, WatchPointRenameC2SPacket.STREAM_CODEC, WatchPointRenameC2SPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PlayerLocatorSelectC2SPacket.TYPE, PlayerLocatorSelectC2SPacket.STREAM_CODEC, PlayerLocatorSelectC2SPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DimensionLocatorRequestPacket.TYPE, DimensionLocatorRequestPacket.STREAM_CODEC, DimensionLocatorRequestPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DimensionLocatorSelectC2SPacket.TYPE, DimensionLocatorSelectC2SPacket.STREAM_CODEC, DimensionLocatorSelectC2SPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PortablePortalActionC2SPacket.TYPE, PortablePortalActionC2SPacket.STREAM_CODEC, PortablePortalActionC2SPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, LogBookActionC2SPacket.TYPE, LogBookActionC2SPacket.STREAM_CODEC, LogBookActionC2SPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CreepingDeathActionC2SPacket.TYPE, CreepingDeathActionC2SPacket.STREAM_CODEC, CreepingDeathActionC2SPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SaveServerConfigC2SPacket.TYPE, SaveServerConfigC2SPacket.STREAM_CODEC, SaveServerConfigC2SPacket::handle);

        if (Platform.getEnvironment() == Env.CLIENT) {
            registerClientReceivers();
        } else {
            NetworkManager.registerS2CPayloadType(BodycamExitSyncS2CPacket.TYPE, BodycamExitSyncS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(BodycamForceClosePacket.TYPE, BodycamForceClosePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(CrossObservationSyncPacket.TYPE, CrossObservationSyncPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(SyncBodycamStatesS2CPacket.TYPE, SyncBodycamStatesS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(WatchPointOpenRenameS2CPacket.TYPE, WatchPointOpenRenameS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PlayerLocatorSyncS2CPacket.TYPE, PlayerLocatorSyncS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PlayerLocatorTargetUpdateS2CPacket.TYPE, PlayerLocatorTargetUpdateS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PlayerLocatorStructureUpdateS2CPacket.TYPE, PlayerLocatorStructureUpdateS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(DimensionLocatorResponsePacket.TYPE, DimensionLocatorResponsePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(ReloadClientConfigS2CPacket.TYPE, ReloadClientConfigS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(OpenServerConfigS2CPacket.TYPE, OpenServerConfigS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(OpenClientConfigS2CPacket.TYPE, OpenClientConfigS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(OpenLogBookPinScreenS2CPacket.TYPE, OpenLogBookPinScreenS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(OpenLogBookScreenS2CPacket.TYPE, OpenLogBookScreenS2CPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(OpenCreepingDeathScreenS2CPacket.TYPE, OpenCreepingDeathScreenS2CPacket.STREAM_CODEC);
        }
    }

    private static void registerClientReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, BodycamExitSyncS2CPacket.TYPE, BodycamExitSyncS2CPacket.STREAM_CODEC, BodycamExitSyncS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, BodycamForceClosePacket.TYPE, BodycamForceClosePacket.STREAM_CODEC, BodycamForceClosePacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CrossObservationSyncPacket.TYPE, CrossObservationSyncPacket.STREAM_CODEC, CrossObservationSyncPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncBodycamStatesS2CPacket.TYPE, SyncBodycamStatesS2CPacket.STREAM_CODEC, SyncBodycamStatesS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, WatchPointOpenRenameS2CPacket.TYPE, WatchPointOpenRenameS2CPacket.STREAM_CODEC, WatchPointOpenRenameS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PlayerLocatorSyncS2CPacket.TYPE, PlayerLocatorSyncS2CPacket.STREAM_CODEC, PlayerLocatorSyncS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PlayerLocatorTargetUpdateS2CPacket.TYPE, PlayerLocatorTargetUpdateS2CPacket.STREAM_CODEC, PlayerLocatorTargetUpdateS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PlayerLocatorStructureUpdateS2CPacket.TYPE, PlayerLocatorStructureUpdateS2CPacket.STREAM_CODEC, PlayerLocatorStructureUpdateS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DimensionLocatorResponsePacket.TYPE, DimensionLocatorResponsePacket.STREAM_CODEC, DimensionLocatorResponsePacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ReloadClientConfigS2CPacket.TYPE, ReloadClientConfigS2CPacket.STREAM_CODEC, ReloadClientConfigS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenServerConfigS2CPacket.TYPE, OpenServerConfigS2CPacket.STREAM_CODEC, OpenServerConfigS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenClientConfigS2CPacket.TYPE, OpenClientConfigS2CPacket.STREAM_CODEC, OpenClientConfigS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenLogBookPinScreenS2CPacket.TYPE, OpenLogBookPinScreenS2CPacket.STREAM_CODEC, OpenLogBookPinScreenS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenLogBookScreenS2CPacket.TYPE, OpenLogBookScreenS2CPacket.STREAM_CODEC, OpenLogBookScreenS2CPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenCreepingDeathScreenS2CPacket.TYPE, OpenCreepingDeathScreenS2CPacket.STREAM_CODEC, OpenCreepingDeathScreenS2CPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        NetworkManager.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player != null && player.connection != null) {
            NetworkManager.sendToPlayer(player, payload);
        }
    }
}
