package dev.ClasherHD.bodycam;

import dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer;
import dev.ClasherHD.bodycam.component.ModDataComponents;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.resources.ResourceLocation;

public class BodycamClient {
    public static void init() {
        initEarly();
        initLate();
    }

    public static void initEarly() {
        EntityRendererRegistry.register(Bodycam.COMPASS_DUMMY, BodycamDummyRenderer::new);
        EntityRendererRegistry.register(Bodycam.HOLOGRAM_DUMMY, BodycamDummyRenderer::new);
    }

    public static void initLate() {
        dev.architectury.registry.item.ItemPropertiesRegistry.register(Bodycam.JAMMER.get(), ResourceLocation.fromNamespaceAndPath("bodycam", "mode"),
                (stack, level, entity, seed) -> {
                    int mode = stack.getOrDefault(ModDataComponents.JAMMER_MODE.get(), 0);
                    if (mode == 1) return 0.5F;
                    if (mode == 2) return 1.0F;
                    return 0.0F;
                });

        dev.architectury.registry.item.ItemPropertiesRegistry.register(Bodycam.ANONYMIZER.get(), ResourceLocation.fromNamespaceAndPath("bodycam", "mode"),
                (stack, level, entity, seed) -> stack.getOrDefault(ModDataComponents.ANONYMIZER_ACTIVE.get(), false) ? 1.0F : 0.0F);

        dev.ClasherHD.bodycam.client.event.BodycamClientEvents.register();

        dev.ClasherHD.bodycam.network.ModNetworking.initClient();
    }
}
