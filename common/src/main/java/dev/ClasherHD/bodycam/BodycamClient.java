package dev.ClasherHD.bodycam;

import dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;

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
        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT,
                Bodycam.HOLOGRAM_BLOCK.get()
        );

        dev.ClasherHD.bodycam.client.event.BodycamClientEvents.register();

        dev.ClasherHD.bodycam.network.ModNetworking.initClient();
    }
}
