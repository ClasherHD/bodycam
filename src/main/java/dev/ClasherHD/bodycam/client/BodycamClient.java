package dev.ClasherHD.bodycam.client;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;

public class BodycamClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(bodycam.BODYCAM_DUMMY, BodycamDummyRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                if (mc.player.isDeadOrDying()) {
                    BodycamViewScreen.isMonitoring = false;
                    return;
                }
                if (BodycamViewScreen.isMonitoring && mc.screen == null) {
                    mc.setScreen(new BodycamViewScreen(
                            BodycamViewScreen.targetUuid,
                            BodycamViewScreen.targetNameStatic,
                            BodycamViewScreen.hasReachStatic));
                }
            }
        });
    }
}
