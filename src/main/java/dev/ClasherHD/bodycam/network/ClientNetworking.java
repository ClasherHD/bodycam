package dev.ClasherHD.bodycam.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworking {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BodycamPacketIDs.SET_CAMERA_PACKET_ID, (client, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            client.execute(() -> {
                if (client.level != null) {
                    net.minecraft.world.entity.Entity target = client.level.getEntity(entityId);
                    if (target != null) {
                        client.setCameraEntity(target);
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(BodycamPacketIDs.RESET_CAMERA_PACKET_ID, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                if (client.player != null) {
                    client.setCameraEntity(client.player);
                    dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring = false;
                    if (client.screen instanceof dev.ClasherHD.bodycam.client.gui.BodycamViewScreen) {
                        client.setScreen(null);
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(BodycamPacketIDs.SYNC_STATES_PACKET_ID, (client, handler, buf, responseSender) -> {
            dev.ClasherHD.bodycam.network.SyncBodycamStatesS2CPacket msg = dev.ClasherHD.bodycam.network.SyncBodycamStatesS2CPacket.decode(buf);
            client.execute(() -> {
                dev.ClasherHD.bodycam.client.ClientBodycamCache.update(msg.jammers, msg.targets, msg.dimensions, msg.positions, msg.anonymizers);
                client.setScreen(new dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen(msg.hasReach, msg.isOnHologram));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(BodycamPacketIDs.CROSS_SYNC_PACKET_ID, (client, handler, buf, responseSender) -> {
            dev.ClasherHD.bodycam.network.CrossObservationSyncPacket msg = dev.ClasherHD.bodycam.network.CrossObservationSyncPacket.decode(buf);
            client.execute(() -> {
                if (msg.isObserving) {
                    dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen.observingMe.add(msg.observerId);
                } else {
                    dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen.observingMe.remove(msg.observerId);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(BodycamPacketIDs.DIMENSION_LOCATOR_PACKET_ID, (client, handler, buf, responseSender) -> {
            dev.ClasherHD.bodycam.network.DimensionLocatorResponsePacket msg = dev.ClasherHD.bodycam.network.DimensionLocatorResponsePacket.decode(buf);
            client.execute(() -> {
                client.setScreen(new dev.ClasherHD.bodycam.client.gui.DimensionLocatorScreen(msg.dimensions));
            });
        });
    }
}
