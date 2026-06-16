package dev.ClasherHD.bodycam.client.network;

import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.locator.DimensionLocatorResponsePacket;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamStatesS2CPacket;
import dev.ClasherHD.bodycam.network.bodycam.CrossObservationSyncPacket;
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
        dev.ClasherHD.bodycam.client.cache.ClientBodycamCache.update(msg.jammers, msg.targets, msg.dimensions, msg.positions, msg.anonymizers);
        Minecraft.getInstance().setScreen(new dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen(msg.hasReach, msg.isOnHologram));
    }

    public static void handleCrossSync(CrossObservationSyncPacket msg) {
        if (msg.isObserving) {
            dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen.observingMe.add(msg.observerId);
        } else {
            dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen.observingMe.remove(msg.observerId);
        }
    }

    public static void handleOpenServerConfig(dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket msg) {
        Minecraft.getInstance().setScreen(new dev.ClasherHD.bodycam.client.gui.ServerConfigScreen(null, msg));
    }

    public static void handlePlayerLocatorSync(dev.ClasherHD.bodycam.network.locator.PlayerLocatorSyncS2CPacket msg) {
        Minecraft.getInstance().setScreen(new dev.ClasherHD.bodycam.client.gui.PlayerLocatorScreen(msg.jammers, msg.dimensions, msg.positions, msg.currentTarget, msg.hasReach));
    }

    public static void handlePlayerLocatorStructureUpdate(dev.ClasherHD.bodycam.network.locator.PlayerLocatorStructureUpdateS2CPacket msg) {
        dev.ClasherHD.bodycam.client.cache.ClientLocatorCache.updateStructure(msg.structurePos, msg.dimension);
    }

    public static void handlePlayerLocatorTargetUpdate(dev.ClasherHD.bodycam.network.locator.PlayerLocatorTargetUpdateS2CPacket msg) {
        dev.ClasherHD.bodycam.client.cache.ClientBodycamCache.positions.put(msg.targetUUID, msg.pos);
        dev.ClasherHD.bodycam.client.cache.ClientBodycamCache.dimensions.put(msg.targetUUID, msg.dimension);
    }
}
