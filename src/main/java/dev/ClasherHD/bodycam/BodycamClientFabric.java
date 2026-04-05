package dev.ClasherHD.bodycam;

import net.fabricmc.api.ClientModInitializer;


public class BodycamClientFabric implements ClientModInitializer {
    private long lastPacketSendTime = 0;

    @Override
    public void onInitializeClient() {
        dev.ClasherHD.bodycam.client.event.BodycamClientEvents.register();

        dev.ClasherHD.bodycam.network.ClientNetworking.register();
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                dev.ClasherHD.bodycam.entity.EntityRegistryFabric.COMPASS_DUMMY,
                dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                dev.ClasherHD.bodycam.entity.EntityRegistryFabric.HOLOGRAM_DUMMY,
                dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);

        net.minecraft.client.renderer.item.ItemProperties.register(dev.ClasherHD.bodycam.BodycamFabric.JAMMER,
                new net.minecraft.resources.ResourceLocation("bodycam", "mode"), (stack, level, entity, seed) -> {
                    int mode = stack.hasTag() ? stack.getTag().getInt("JammerMode") : 0;
                    if (mode == 2)
                        return 1.0F;
                    if (mode == 1)
                        return 0.5F;
                    return 0.0F;
                });

        net.minecraft.client.renderer.item.ItemProperties.register(dev.ClasherHD.bodycam.BodycamFabric.ANONYMIZER,
                new net.minecraft.resources.ResourceLocation("bodycam", "mode"), (stack, level, entity, seed) -> {
                    return (stack.hasTag() && stack.getTag().getBoolean("AnonymizerActive")) ? 1.0F : 0.0F;
                });

        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.isCrouching()) {
                boolean onHologram = world.getBlockState(player.blockPosition().below())
                        .is(dev.ClasherHD.bodycam.BodycamFabric.HOLOGRAM_BLOCK) ||
                        world.getBlockState(player.blockPosition())
                                .is(dev.ClasherHD.bodycam.BodycamFabric.HOLOGRAM_BLOCK);
                if (onHologram) {
                    if (world.isClientSide()) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - this.lastPacketSendTime >= 1000) {
                            net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs
                                    .create();
                            buf.writeInt(0);
                            buf.writeBoolean(true);
                            buf.writeBoolean(true);
                            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
                                    .send(dev.ClasherHD.bodycam.network.BodycamPacketIDs.REQUEST_CAMERA_PACKET_ID, buf);
                            this.lastPacketSendTime = currentTime;
                        }
                    }
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.getCameraEntity() != null && mc.getCameraEntity() != mc.player) {
                int width = mc.getWindow().getGuiScaledWidth();
                int height = mc.getWindow().getGuiScaledHeight();
                String txt = net.minecraft.network.chat.Component
                        .translatable("gui.bodycam.exit", mc.options.keyShift.getTranslatedKeyMessage().getString())
                        .getString();
                int textWidth = mc.font.width(txt);
                drawContext.drawString(mc.font, txt, (width - textWidth) / 2, height - 30, 0xFFFFFF, true);
            }
        });
    }
}
