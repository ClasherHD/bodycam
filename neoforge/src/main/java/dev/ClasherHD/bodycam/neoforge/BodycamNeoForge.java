package dev.ClasherHD.bodycam.neoforge;

import dev.ClasherHD.bodycam.Bodycam;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Bodycam.MOD_ID)
public class BodycamNeoForge {
    public BodycamNeoForge(IEventBus modEventBus, ModContainer container) {
        Bodycam.init();

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            ClientHelper.registerClient(modEventBus, container);
        }
    }

    private static class ClientHelper {
        private static void registerClient(IEventBus modEventBus, ModContainer container) {
            dev.ClasherHD.bodycam.network.ModNetworking.initClient();
            dev.ClasherHD.bodycam.BodycamClient.initEarly();
            modEventBus.addListener(ClientHelper::onClientSetup);
            container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (minecraft, modListScreen) -> new dev.ClasherHD.bodycam.client.gui.ClientConfigScreen(modListScreen)
            );
        }

        private static void onClientSetup(final net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            dev.ClasherHD.bodycam.BodycamClient.initLate();
        }
    }
}
