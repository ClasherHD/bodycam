package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.network.bodycam.*;
import dev.ClasherHD.bodycam.network.locator.*;
import dev.ClasherHD.bodycam.network.config.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(bodycam.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void init() {
        INSTANCE.registerMessage(id(), BodycamSetCameraPacket.class,
                BodycamSetCameraPacket::encode,
                BodycamSetCameraPacket::decode,
                BodycamSetCameraPacket::handle);

        INSTANCE.registerMessage(id(), BodycamResetCameraPacket.class,
                BodycamResetCameraPacket::encode,
                BodycamResetCameraPacket::decode,
                BodycamResetCameraPacket::handle);

        INSTANCE.registerMessage(id(), BodycamForceClosePacket.class,
                BodycamForceClosePacket::encode,
                BodycamForceClosePacket::decode,
                BodycamForceClosePacket::handle);

        INSTANCE.registerMessage(id(), DimensionLocatorResponsePacket.class,
                DimensionLocatorResponsePacket::encode,
                DimensionLocatorResponsePacket::decode,
                DimensionLocatorResponsePacket::handle);

        INSTANCE.registerMessage(id(), CrossObservationSyncPacket.class,
                CrossObservationSyncPacket::encode,
                CrossObservationSyncPacket::decode,
                CrossObservationSyncPacket::handle);

        INSTANCE.registerMessage(id(), SyncBodycamRequestC2SPacket.class,
                SyncBodycamRequestC2SPacket::encode,
                SyncBodycamRequestC2SPacket::decode,
                SyncBodycamRequestC2SPacket::handle);

        INSTANCE.registerMessage(id(), SyncBodycamStatesS2CPacket.class,
                SyncBodycamStatesS2CPacket::encode,
                SyncBodycamStatesS2CPacket::decode,
                SyncBodycamStatesS2CPacket::handle);

        INSTANCE.registerMessage(id(), OpenServerConfigS2CPacket.class,
                OpenServerConfigS2CPacket::encode,
                OpenServerConfigS2CPacket::decode,
                OpenServerConfigS2CPacket::handle);

        INSTANCE.registerMessage(id(), SaveServerConfigC2SPacket.class,
                SaveServerConfigC2SPacket::encode,
                SaveServerConfigC2SPacket::decode,
                SaveServerConfigC2SPacket::handle);

        INSTANCE.registerMessage(id(), PlayerLocatorSelectC2SPacket.class,
                PlayerLocatorSelectC2SPacket::encode,
                PlayerLocatorSelectC2SPacket::decode,
                PlayerLocatorSelectC2SPacket::handle);

        INSTANCE.registerMessage(id(), PlayerLocatorSyncS2CPacket.class,
                PlayerLocatorSyncS2CPacket::encode,
                PlayerLocatorSyncS2CPacket::decode,
                PlayerLocatorSyncS2CPacket::handle);

        INSTANCE.registerMessage(id(), PlayerLocatorStructureUpdateS2CPacket.class,
                PlayerLocatorStructureUpdateS2CPacket::encode,
                PlayerLocatorStructureUpdateS2CPacket::decode,
                PlayerLocatorStructureUpdateS2CPacket::handle);

        INSTANCE.registerMessage(id(), PlayerLocatorTargetUpdateS2CPacket.class,
                PlayerLocatorTargetUpdateS2CPacket::encode,
                PlayerLocatorTargetUpdateS2CPacket::decode,
                PlayerLocatorTargetUpdateS2CPacket::handle);
    }
}
