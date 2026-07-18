package dev.ClasherHD.bodycam.fabric;

import dev.ClasherHD.bodycam.BodycamClient;
import net.fabricmc.api.ClientModInitializer;

public class BodycamClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BodycamClient.init();
    }
}
