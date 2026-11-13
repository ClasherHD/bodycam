package dev.ClasherHD.bodycam.fabric;

import dev.ClasherHD.bodycam.bodycam;
import net.fabricmc.api.ModInitializer;

public final class BodycamFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        bodycam.init();
    }
}
