package dev.ClasherHD.bodycam.network;

import dev.architectury.networking.NetworkManager;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen;
import dev.ClasherHD.bodycam.client.gui.DimensionLocatorScreen;
import dev.ClasherHD.bodycam.client.ClientBodycamCache;
import net.minecraft.client.Minecraft;

public class ClientNetworking {

    public static void handleResetCamera(BodycamResetCameraS2CPacket payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.setCameraEntity(mc.player);
                BodycamViewScreen.isMonitoring = false;
                if (mc.screen instanceof BodycamViewScreen) {
                    mc.setScreen(null);
                }
            }
        });
    }

    public static void handleSyncStates(SyncBodycamStatesS2CPacket payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ClientBodycamCache.update(payload.jammers(), payload.targets(), payload.dimensions(), payload.positions(), payload.anonymizers());
            Minecraft.getInstance().setScreen(new PlayerSelectionScreen(payload.hasReach(), payload.isOnHologram()));
        });
    }

    public static void handleCrossObservationSync(CrossObservationSyncPacket payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (payload.isObserving()) {
                PlayerSelectionScreen.observingMe.add(payload.observerId());
            } else {
                PlayerSelectionScreen.observingMe.remove(payload.observerId());
            }
        });
    }

    public static void handleDimensionLocatorResponse(DimensionLocatorResponsePacket payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            Minecraft.getInstance().setScreen(new DimensionLocatorScreen(payload.dimensions()));
        });
    }

    public static void handleForceClose(BodycamForceClosePacket payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            BodycamViewScreen.isMonitoring = false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.setCameraEntity(mc.player);
            }
            mc.setScreen(null);
        });
    }
}
