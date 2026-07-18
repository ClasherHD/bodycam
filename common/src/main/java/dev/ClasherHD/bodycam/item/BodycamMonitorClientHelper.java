package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.network.SyncBodycamRequestC2SPacket;
import dev.architectury.networking.NetworkManager;

public class BodycamMonitorClientHelper {
    public static void sendSyncRequest(boolean hasReach, boolean isOnHologram) {
        NetworkManager.sendToServer(new SyncBodycamRequestC2SPacket(hasReach, isOnHologram));
    }
}
