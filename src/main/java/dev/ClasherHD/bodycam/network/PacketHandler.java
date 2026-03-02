package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.bodycam;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public class PacketHandler {
    public static final ResourceLocation SET_CAMERA_ID = new ResourceLocation(bodycam.MODID, "set_camera");
    public static final ResourceLocation RESET_CAMERA_ID = new ResourceLocation(bodycam.MODID, "reset_camera");

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(SET_CAMERA_ID, BodycamSetCameraPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(RESET_CAMERA_ID, BodycamResetCameraPacket::handle);
    }
}
