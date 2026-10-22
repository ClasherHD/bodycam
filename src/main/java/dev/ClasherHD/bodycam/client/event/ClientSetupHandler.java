package dev.ClasherHD.bodycam.client.event;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.gui.AncientTableScreen;
import dev.ClasherHD.bodycam.client.render.PodestBlockEntityRenderer;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
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
            MenuScreens.register(ModMenuTypes.ANCIENT_TABLE.get(), AncientTableScreen::new);

            ResourceLocation emptyProperty = new ResourceLocation("bodycam", "empty");
            ClampedItemPropertyFunction emptyFunc = (stack, level, entity, seed) -> ItemDurabilityHelper.isEmpty(stack) ? 1.0F : 0.0F;
            ItemProperties.register(ModItems.BODYCAM_MONITOR.get(), emptyProperty, emptyFunc);
            ItemProperties.register(ModItems.DIMENSION_LOCATOR.get(), emptyProperty, emptyFunc);
            ItemProperties.register(ModItems.JAMMER.get(), emptyProperty, emptyFunc);
            ItemProperties.register(ModItems.ANONYMIZER.get(), emptyProperty, emptyFunc);
            ItemProperties.register(ModItems.PLAYER_LOCATOR_COMPASS.get(), emptyProperty, emptyFunc);

            ItemProperties.register(ModItems.JAMMER.get(),
                    new ResourceLocation("bodycam", "mode"),
                    (stack, level, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("JammerMode")) {
                            return stack.getTag().getInt("JammerMode");
                        }
                        return 0.0F;
                    });
            ItemProperties.register(ModItems.PORTABLE_NETHER_PORTAL.get(),
                    new ResourceLocation("bodycam", "charged"),
                    (stack, level, entity, seed) -> dev.ClasherHD.bodycam.item.PortableNetherPortalItem.getCharges(stack) > 0 ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.ANONYMIZER.get(),
                    new ResourceLocation("bodycam", "active"),
                    (stack, level, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("AnonymizerActive")
                                && stack.getTag().getBoolean("AnonymizerActive")) {
                            return 1.0F;
                        }
                        return 0.0F;
                    });

            ItemProperties.register(ModItems.PLAYER_LOCATOR_COMPASS.get(),
                    new ResourceLocation("bodycam", "angle"),
                    new net.minecraft.client.renderer.item.CompassItemPropertyFunction((level, stack, entity) -> {
                        if (stack.hasTag() && stack.getTag().contains("LocatorTargetUUID")) {
                            int state = stack.getTag().getInt("LocatorState");
                            if (state == 1) {
                                java.util.UUID targetUUID = stack.getTag().getUUID("LocatorTargetUUID");
                                net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.cache.ClientBodycamCache.positions.get(targetUUID);
                                String targetDim = dev.ClasherHD.bodycam.client.cache.ClientBodycamCache.dimensions.get(targetUUID);
                                if (targetPos != null && targetDim != null) {
                                    return net.minecraft.core.GlobalPos.of(
                                            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation(targetDim)),
                                            targetPos
                                    );
                                }
                            } else if (state == 3) {
                                if (dev.ClasherHD.bodycam.client.cache.ClientLocatorCache.structureTarget != null && dev.ClasherHD.bodycam.client.cache.ClientLocatorCache.structureTargetDimension != null) {
                                    return net.minecraft.core.GlobalPos.of(
                                            dev.ClasherHD.bodycam.client.cache.ClientLocatorCache.structureTargetDimension,
                                            dev.ClasherHD.bodycam.client.cache.ClientLocatorCache.structureTarget
                                    );
                                }
                            }
                        }
                        return null;
                    }));

            ItemProperties.register(ModItems.PLAYER_LOCATOR_COMPASS.get(),
                    new ResourceLocation("bodycam", "state"),
                    (stack, level, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("LocatorState")) {
                            return (float) stack.getTag().getInt("LocatorState");
                        }
                        return 0.0F;
                    });

            registerConfigScreen();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(dev.ClasherHD.bodycam.registry.ModEntityTypes.COMPASS_DUMMY.get(),
                dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);
        event.registerEntityRenderer(dev.ClasherHD.bodycam.registry.ModEntityTypes.HOLOGRAM_DUMMY.get(),
                dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);
        event.registerEntityRenderer(dev.ClasherHD.bodycam.registry.ModEntityTypes.WATCH_POINT_DUMMY.get(),
                dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PODEST.get(),
                PodestBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skinType : new String[]{"default", "slim"}) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skinType);
            if (renderer != null) {
                renderer.layers.removeIf(layer -> layer instanceof net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer);
                renderer.addLayer(new dev.ClasherHD.bodycam.client.render.BodycamItemInHandLayer<>(renderer, net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()));
                tryAddGlowingTrimLayer(renderer);
            }
        }

        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.ARMOR_STAND));
        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.ZOMBIE));
        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.SKELETON));
        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.DROWNED));
        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.STRAY));
        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.HUSK));
        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.WITHER_SKELETON));
        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.PIGLIN));
        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.PIGLIN_BRUTE));
        tryAddGlowingTrimLayer(event.getRenderer(net.minecraft.world.entity.EntityType.ZOMBIFIED_PIGLIN));
    }

    private static void tryAddGlowingTrimLayer(net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> renderer) {
        if (renderer != null) {
            net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<?, ?, ?> armorLayer = null;
            for (net.minecraft.client.renderer.entity.layers.RenderLayer<?, ?> layer : renderer.layers) {
                if (layer instanceof net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer) {
                    armorLayer = (net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<?, ?, ?>) layer;
                    break;
                }
            }
            if (armorLayer != null) {
                boolean alreadyHas = false;
                for (net.minecraft.client.renderer.entity.layers.RenderLayer<?, ?> layer : renderer.layers) {
                    if (layer instanceof dev.ClasherHD.bodycam.client.render.GlowingArmorTrimLayer) {
                        alreadyHas = true;
                        break;
                    }
                }
                if (!alreadyHas) {
                    addLayerHelper(renderer, armorLayer);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayerHelper(net.minecraft.client.renderer.entity.LivingEntityRenderer renderer, net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer armorLayer) {
        renderer.addLayer(new dev.ClasherHD.bodycam.client.render.GlowingArmorTrimLayer(renderer, armorLayer));
    }

    @SubscribeEvent
    public static void registerGuiOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("portal_channel_overlay", dev.ClasherHD.bodycam.client.render.PortalChannelOverlay.INSTANCE);
    }

    public static void registerConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft,
                        screen) -> new dev.ClasherHD.bodycam.client.gui.ClientConfigScreen(
                                screen)));
    }
}
