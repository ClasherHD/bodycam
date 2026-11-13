package dev.ClasherHD.bodycam.fabric;

import dev.ClasherHD.bodycam.client.event.ClientSetupHandler;
import dev.ClasherHD.bodycam.client.render.BodycamItemInHandLayer;
import dev.ClasherHD.bodycam.client.render.GlowingArmorTrimLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class BodycamFabricClient implements ClientModInitializer {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitializeClient() {
        ClientSetupHandler.registerMenusAndRenderers();
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerRenderer playerRenderer) {
                ClientSetupHandler.attachPlayerLayers(playerRenderer, context.getItemInHandRenderer(), false);
                registrationHelper.register(new BodycamItemInHandLayer<>(playerRenderer, context.getItemInHandRenderer()));
                registrationHelper.register(new GlowingArmorTrimLayer<>(playerRenderer));
                return;
            }
            if (ClientSetupHandler.HUMANOID_TRIM_ENTITY_TYPES.contains(entityType)
                    && entityRenderer.getModel() instanceof HumanoidModel<?>) {
                registrationHelper.register(new GlowingArmorTrimLayer<>((RenderLayerParent) entityRenderer));
            }
        });
        ClientSetupHandler.initClient();
    }
}
