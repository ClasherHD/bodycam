package dev.ClasherHD.bodycam.client.network;

import dev.ClasherHD.bodycam.client.cache.ClientBodycamCache;
import dev.ClasherHD.bodycam.client.cache.ClientLocatorCache;
import dev.ClasherHD.bodycam.client.camera.CameraSnapState;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.client.gui.ClientConfigScreen;
import dev.ClasherHD.bodycam.client.gui.CreepingDeathScreen;
import dev.ClasherHD.bodycam.client.gui.DimensionLocatorScreen;
import dev.ClasherHD.bodycam.client.gui.LogBookPinScreen;
import dev.ClasherHD.bodycam.client.gui.LogBookScreen;
import dev.ClasherHD.bodycam.client.gui.PlayerLocatorScreen;
import dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen;
import dev.ClasherHD.bodycam.client.gui.ServerConfigScreen;
import dev.ClasherHD.bodycam.client.gui.WatchPointRenameScreen;
import dev.ClasherHD.bodycam.network.bodycam.BodycamExitSyncS2CPacket;
import dev.ClasherHD.bodycam.network.bodycam.CrossObservationSyncPacket;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamStatesS2CPacket;
import dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket;
import dev.ClasherHD.bodycam.network.creepingdeath.OpenCreepingDeathScreenS2CPacket;
import dev.ClasherHD.bodycam.network.locator.DimensionLocatorResponsePacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorStructureUpdateS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorSyncS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorTargetUpdateS2CPacket;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookPinScreenS2CPacket;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookScreenS2CPacket;
import dev.ClasherHD.bodycam.network.watchpoint.WatchPointOpenRenameS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleSyncStates(SyncBodycamStatesS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        ClientBodycamCache.update(
                msg.jammers,
                msg.targets,
                msg.dimensions,
                msg.positions,
                msg.anonymizers,
                msg.hidden,
                msg.watchPoints
        );
        ClientBodycamCache.REACH_LEVEL = msg.reachLevel;
        ClientBodycamCache.IS_ON_HOLOGRAM = msg.isOnHologram;
        ClientBodycamCache.IS_ALL_SEEING_EYE = msg.isAllSeeingEye;
        if (mc.player != null && !(mc.screen instanceof BodycamViewScreen)) {
            mc.setScreen(new PlayerSelectionScreen(msg.reachLevel, msg.isOnHologram, msg.isAllSeeingEye));
        }
    }

    public static void handleForceClose() {
        BodycamViewScreen.isMonitoring = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        if (mc.screen instanceof BodycamViewScreen || mc.screen instanceof PlayerSelectionScreen) {
            mc.setScreen(null);
        }
    }

    public static void handleCrossObservationSync(CrossObservationSyncPacket msg) {
        if (msg.observing) {
            PlayerSelectionScreen.observingMe.add(msg.observerId);
        } else {
            PlayerSelectionScreen.observingMe.remove(msg.observerId);
        }
    }

    public static void handleExitSync(BodycamExitSyncS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ClientLevel level = mc.level;
        if (msg.observerEntityId() >= 0 && msg.observerEntityId() != player.getId()) {
            if (level != null) {
                Entity observer = level.getEntity(msg.observerEntityId());
                CameraSnapState.snapRemoteObserverOnExit(
                        observer,
                        msg.observerEntityId(),
                        msg.x(),
                        msg.y(),
                        msg.z(),
                        msg.yRot(),
                        msg.xRot(),
                        msg.vx(),
                        msg.vy(),
                        msg.vz(),
                        msg.fallDistance(),
                        msg.invisible()
                );
                if (msg.removedDummyEntityId() >= 0) {
                    level.removeEntity(msg.removedDummyEntityId(), Entity.RemovalReason.DISCARDED);
                }
            }
            return;
        }
        BodycamViewScreen.isMonitoring = false;
        if (level != null && msg.removedDummyEntityId() >= 0) {
            level.removeEntity(msg.removedDummyEntityId(), Entity.RemovalReason.DISCARDED);
        }
        player.setInvisible(msg.invisible());
        CameraSnapState.recordAtomicExit(
                player,
                msg.x(),
                msg.y(),
                msg.z(),
                msg.yRot(),
                msg.xRot(),
                msg.vx(),
                msg.vy(),
                msg.vz(),
                msg.fallDistance(),
                msg.removedDummyEntityId()
        );
        mc.setCameraEntity(player);
        if (mc.screen instanceof BodycamViewScreen || mc.screen instanceof PlayerSelectionScreen) {
            mc.setScreen(null);
        }
    }

    public static void handleOpenWatchPointRename(WatchPointOpenRenameS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new WatchPointRenameScreen(msg.pos, msg.currentName));
        }
    }

    public static void handlePlayerLocatorSync(PlayerLocatorSyncS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        ClientBodycamCache.positions.clear();
        ClientBodycamCache.dimensions.clear();
        ClientBodycamCache.positions.putAll(msg.positions);
        ClientBodycamCache.dimensions.putAll(msg.dimensions);
        if (mc.player != null) {
            mc.setScreen(new PlayerLocatorScreen(
                    msg.jammers,
                    msg.dimensions,
                    msg.positions,
                    msg.currentTarget,
                    msg.reachLevel
            ));
        }
    }

    public static void handleDimensionLocatorResponse(DimensionLocatorResponsePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new DimensionLocatorScreen(msg.playerDimensions));
        }
    }

    public static void handleOpenLogBookPinScreen(OpenLogBookPinScreenS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new LogBookPinScreen(msg.mode, msg.offHand));
        }
    }

    public static void handleOpenLogBookScreen(OpenLogBookScreenS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new LogBookScreen(msg.data));
        }
    }

    public static void handleOpenCreepingDeathScreen(OpenCreepingDeathScreenS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new CreepingDeathScreen(msg.mode, msg.offHand, msg.entries, msg.selectedSkinUUID));
        }
    }

    public static void handleOpenServerConfig(OpenServerConfigS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new ServerConfigScreen(mc.screen, msg.snapshot));
        }
    }

    public static void handleOpenClientConfig() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new ClientConfigScreen(mc.screen));
        }
    }

    public static void handlePlayerLocatorTargetUpdate(PlayerLocatorTargetUpdateS2CPacket msg) {
        if (msg.targetUUID != null) {
            ClientBodycamCache.positions.put(msg.targetUUID, msg.pos);
            ClientBodycamCache.dimensions.put(msg.targetUUID, msg.dimension);
        }
    }

    public static void handlePlayerLocatorStructureUpdate(PlayerLocatorStructureUpdateS2CPacket msg) {
        ClientLocatorCache.updateStructure(msg.structurePos, msg.dimension);
    }
}
