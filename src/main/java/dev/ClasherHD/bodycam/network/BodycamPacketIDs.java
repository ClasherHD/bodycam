package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.BodycamFabric;
import net.minecraft.resources.ResourceLocation;

public class BodycamPacketIDs {
    public static final ResourceLocation SET_CAMERA_PACKET_ID = new ResourceLocation(BodycamFabric.MOD_ID, "set_camera");
    public static final ResourceLocation RESET_CAMERA_PACKET_ID = new ResourceLocation(BodycamFabric.MOD_ID, "reset_camera");
    public static final ResourceLocation REQUEST_CAMERA_PACKET_ID = new ResourceLocation(BodycamFabric.MOD_ID, "request_camera");
    public static final ResourceLocation SYNC_STATES_PACKET_ID = new ResourceLocation(BodycamFabric.MOD_ID, "sync_states");
    public static final ResourceLocation CROSS_SYNC_PACKET_ID = new ResourceLocation(BodycamFabric.MOD_ID, "cross_sync");
    public static final ResourceLocation DIMENSION_LOCATOR_PACKET_ID = new ResourceLocation(BodycamFabric.MOD_ID, "dimension_locator");
}
