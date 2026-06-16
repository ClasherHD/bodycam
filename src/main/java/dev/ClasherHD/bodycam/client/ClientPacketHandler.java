package dev.ClasherHD.bodycam.client;

import dev.ClasherHD.bodycam.network.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.DimensionLocatorResponsePacket;
import dev.ClasherHD.bodycam.network.SyncBodycamStatesS2CPacket;
import dev.ClasherHD.bodycam.network.CrossObservationSyncPacket;
import net.minecraft.client.Minecraft;

public class ClientPacketHandler {
    public static void handleForceClose(BodycamForceClosePacket msg) {
        dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        mc.setScreen(null);
    }

    public static void handleDimensionResponse(DimensionLocatorResponsePacket msg) {
        Minecraft.getInstance().setScreen(new dev.ClasherHD.bodycam.client.gui.DimensionLocatorScreen(msg.dimensions));
    }

    public static void handleSyncStates(SyncBodycamStatesS2CPacket msg) {
        dev.ClasherHD.bodycam.client.ClientBodycamCache.update(msg.jammers, msg.targets, msg.dimensions, msg.positions, msg.anonymizers);
        Minecraft.getInstance().setScreen(new dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen(msg.hasReach, msg.isOnHologram));
    }

    public static void handleCrossSync(CrossObservationSyncPacket msg) {
        if (msg.isObserving) {
            dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen.observingMe.add(msg.observerId);
        } else {
            dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen.observingMe.remove(msg.observerId);
        }
    }

    public static void handleOpenServerConfig(dev.ClasherHD.bodycam.network.OpenServerConfigS2CPacket msg) {
        Minecraft.getInstance().setScreen(new dev.ClasherHD.bodycam.client.gui.ServerConfigScreen(null, msg));
    }

    public static void handlePlayerLocatorSync(dev.ClasherHD.bodycam.network.PlayerLocatorSyncS2CPacket msg) {
        Minecraft.getInstance().setScreen(new dev.ClasherHD.bodycam.client.gui.PlayerLocatorScreen(msg.jammers, msg.dimensions, msg.positions, msg.currentTarget, msg.hasReach));
    }

    public static void handlePlayerLocatorStructureUpdate(dev.ClasherHD.bodycam.network.PlayerLocatorStructureUpdateS2CPacket msg) {
        dev.ClasherHD.bodycam.client.ClientLocatorCache.updateStructure(msg.structurePos, msg.dimension);
    }

    public static void handlePlayerLocatorTargetUpdate(dev.ClasherHD.bodycam.network.PlayerLocatorTargetUpdateS2CPacket msg) {
        dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.put(msg.targetUUID, msg.pos);
        dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.put(msg.targetUUID, msg.dimension);
    }
}
