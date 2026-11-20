package dev.ClasherHD.bodycam.fabric;

import dev.ClasherHD.bodycam.client.event.ClientSetupHandler;
import dev.ClasherHD.bodycam.client.render.GlowingArmorTrimLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

@SuppressWarnings("null")
public final class BodycamFabricClient implements ClientModInitializer {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitializeClient() {
        ClientSetupHandler.registerMenusAndRenderers();
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof AvatarRenderer<?>) {
                registrationHelper.register(new GlowingArmorTrimLayer<>((RenderLayerParent) entityRenderer, context.getModelSet()));
                return;
            }
            if (ClientSetupHandler.HUMANOID_TRIM_ENTITY_TYPES.contains(entityType)
                    && entityRenderer.getModel() instanceof HumanoidModel<?>) {
                registrationHelper.register(new GlowingArmorTrimLayer<>((RenderLayerParent) entityRenderer, context.getModelSet()));
            }
        });
        ClientSetupHandler.initClient();
    }
}
