package dev.ClasherHD.bodycam.client;

import dev.ClasherHD.bodycam.bodycam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = bodycam.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@SuppressWarnings("null")
public class ClientSetupHandler {

    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.item.ItemProperties.register(bodycam.JAMMER.get(),
                    new net.minecraft.resources.ResourceLocation("bodycam", "mode"),
                    (stack, level, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("JammerMode")) {
                            return stack.getTag().getInt("JammerMode");
                        }
                        return 0.0F;
                    });
            net.minecraft.client.renderer.item.ItemProperties.register(bodycam.ANONYMIZER.get(),
                    new net.minecraft.resources.ResourceLocation("bodycam", "active"),
                    (stack, level, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("AnonymizerActive")
                                && stack.getTag().getBoolean("AnonymizerActive")) {
                            return 1.0F;
                        }
                        return 0.0F;
                    });
            registerConfigScreen();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(bodycam.COMPASS_DUMMY.get(),
                dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);
        event.registerEntityRenderer(bodycam.HOLOGRAM_DUMMY.get(),
                dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);
    }

    public static void registerConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft,
                        screen) -> new dev.ClasherHD.bodycam.client.gui.ClientConfigScreen(
                                screen)));
    }
}
