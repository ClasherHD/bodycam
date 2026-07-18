package dev.ClasherHD.bodycam.fabric;

import dev.ClasherHD.bodycam.Bodycam;
import net.fabricmc.api.ModInitializer;

public class BodycamFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Bodycam.init();
    }
}
