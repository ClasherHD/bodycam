package dev.ClasherHD.bodycam.client.event;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.cache.ClientBodycamCache;
import dev.ClasherHD.bodycam.client.cache.ClientLocatorCache;
import dev.ClasherHD.bodycam.client.gui.AncientTableScreen;
import dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer;
import dev.ClasherHD.bodycam.client.render.BodycamItemInHandLayer;
import dev.ClasherHD.bodycam.client.render.CreepingDeathRenderer;
import dev.ClasherHD.bodycam.client.render.GlowingArmorTrimLayer;
import dev.ClasherHD.bodycam.client.render.PodestBlockEntityRenderer;
import dev.ClasherHD.bodycam.client.render.PortalChannelOverlay;
import dev.ClasherHD.bodycam.client.render.ScholarRenderer;
import dev.ClasherHD.bodycam.client.render.WatchPointDummyRenderer;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.LogBookItem;
import dev.ClasherHD.bodycam.item.LoggerItem;
import dev.ClasherHD.bodycam.mixin.ItemPropertiesAccessor;
import dev.ClasherHD.bodycam.mixin.LivingEntityRendererAccessor;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModMenus;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class ClientSetupHandler {
    public static final List<EntityType<?>> HUMANOID_TRIM_ENTITY_TYPES = List.of(
            EntityType.ARMOR_STAND,
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.DROWNED,
            EntityType.STRAY,
            EntityType.HUSK,
            EntityType.WITHER_SKELETON,
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.ZOMBIFIED_PIGLIN
    );

    private ClientSetupHandler() {
    }

    public static void registerMenusAndRenderers() {
        MenuRegistry.registerScreenFactory(ModMenus.ANCIENT_TABLE.get(), AncientTableScreen::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.PODEST.get(), PodestBlockEntityRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.BODYCAM_DUMMY, BodycamDummyRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.HOLOGRAM_DUMMY, BodycamDummyRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.WATCH_POINT_DUMMY, WatchPointDummyRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.SCHOLAR, ScholarRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.CREEPING_DEATH, CreepingDeathRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void attachPlayerLayers(PlayerRenderer renderer, ItemInHandRenderer itemInHandRenderer, boolean addViaAccessor) {
        if (renderer == null) {
            return;
        }
        List<RenderLayer<?, ?>> layers = ((LivingEntityRendererAccessor) renderer).bodycam$getLayers();
        layers.removeIf(layer -> layer.getClass() == PlayerItemInHandLayer.class);
        if (addViaAccessor && !hasLayer(layers, BodycamItemInHandLayer.class)) {
            layers.add(new BodycamItemInHandLayer<>(renderer, itemInHandRenderer));
        }
        if (addViaAccessor) {
            tryAddGlowingTrimLayer(renderer);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void tryAddGlowingTrimLayer(LivingEntityRenderer<?, ?> renderer) {
        if (renderer == null || !(renderer.getModel() instanceof HumanoidModel<?>)) {
            return;
        }
        List<RenderLayer<?, ?>> layers = ((LivingEntityRendererAccessor) renderer).bodycam$getLayers();
        if (!hasLayer(layers, GlowingArmorTrimLayer.class)) {
            layers.add(new GlowingArmorTrimLayer(renderer));
        }
    }

    private static boolean hasLayer(List<RenderLayer<?, ?>> layers, Class<?> layerClass) {
        for (RenderLayer<?, ?> layer : layers) {
            if (layerClass.isInstance(layer)) {
                return true;
            }
        }
        return false;
    }

    public static void initClient() {
        ModClientConfig.load();
        BodycamClientEvents.register();

        RenderTypeRegistry.register(
                RenderType.cutout(),
                ModBlocks.HOLOGRAM_BLOCK.get(),
                ModBlocks.PODEST.get(),
                ModBlocks.ANCIENT_TABLE.get(),
                ModBlocks.WATCH_POINT.get()
        );

        ResourceLocation emptyProperty = ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "empty");
        ItemPropertiesRegistry.register(ModItems.BODYCAM_MONITOR.get(), emptyProperty, (stack, level, entity, seed) -> ItemDurabilityHelper.isEmpty(stack) ? 1.0F : 0.0F);
        ItemPropertiesRegistry.register(ModItems.DIMENSION_LOCATOR.get(), emptyProperty, (stack, level, entity, seed) -> ItemDurabilityHelper.isEmpty(stack) ? 1.0F : 0.0F);
        ItemPropertiesRegistry.register(ModItems.JAMMER.get(), emptyProperty, (stack, level, entity, seed) -> ItemDurabilityHelper.isEmpty(stack) ? 1.0F : 0.0F);
        ItemPropertiesRegistry.register(ModItems.ANONYMIZER.get(), emptyProperty, (stack, level, entity, seed) -> ItemDurabilityHelper.isEmpty(stack) ? 1.0F : 0.0F);
        ItemPropertiesRegistry.register(ModItems.PLAYER_LOCATOR_COMPASS.get(), emptyProperty, (stack, level, entity, seed) -> ItemDurabilityHelper.isEmpty(stack) ? 1.0F : 0.0F);
        ItemPropertiesRegistry.register(ModItems.LOGGER.get(), emptyProperty, (stack, level, entity, seed) -> ItemDurabilityHelper.isEmpty(stack) ? 1.0F : 0.0F);

        ItemPropertiesRegistry.register(
                ModItems.LOGGER.get(),
                ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "active"),
                (stack, level, entity, seed) -> LoggerItem.isActive(stack) ? 1.0F : 0.0F
        );

        ItemPropertiesRegistry.register(
                ModItems.LOG_BOOK.get(),
                ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "pages_empty"),
                (stack, level, entity, seed) -> LogBookItem.pageCount(stack) == 0 ? 1.0F : 0.0F
        );

        ItemPropertiesRegistry.register(
                ModItems.LOG_BOOK.get(),
                ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "locked"),
                (stack, level, entity, seed) -> LogBookItem.hasPin(stack) ? 1.0F : 0.0F
        );

        ItemPropertiesRegistry.register(
                ModItems.ALL_SEEING_EYE.get(),
                ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "deactivated"),
                (stack, level, entity, seed) -> AllSeeingEyeItem.isActivated(stack) ? 0.0F : 1.0F
        );

        registerUnclampedProperty(
                ModItems.JAMMER.get(),
                ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "mode"),
                (stack, level, entity, seed) -> {
                    CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
                    if (tag != null && tag.contains("JammerMode")) {
                        return tag.getInt("JammerMode");
                    }
                    return 0.0F;
                }
        );

        ItemPropertiesRegistry.register(
                ModItems.PORTABLE_NETHER_PORTAL.get(),
                ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "charged"),
                (stack, level, entity, seed) -> {
                    CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
                    if (tag != null && tag.contains("PortalCharges")) {
                        return Mth.clamp(tag.getInt("PortalCharges"), 0, 2) > 0 ? 1.0F : 0.0F;
                    }
                    return stack.isDamageableItem() && !ItemDurabilityHelper.isEmpty(stack) ? 1.0F : 0.0F;
                }
        );

        ItemPropertiesRegistry.register(
                ModItems.ANONYMIZER.get(),
                ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "active"),
                (stack, level, entity, seed) -> {
                    CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
                    return tag != null && tag.contains("AnonymizerActive") && tag.getBoolean("AnonymizerActive") ? 1.0F : 0.0F;
                }
        );

        ItemPropertiesRegistry.register(
                ModItems.PLAYER_LOCATOR_COMPASS.get(),
                ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> {
                    CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
                    if (tag == null) {
                        return null;
                    }
                    int state = tag.getInt("LocatorState");
                    if (state == 1 && tag.hasUUID("LocatorTargetUUID")) {
                        UUID targetUUID = tag.getUUID("LocatorTargetUUID");
                        BlockPos targetPos = ClientBodycamCache.positions.get(targetUUID);
                        String targetDim = ClientBodycamCache.dimensions.get(targetUUID);
                        ResourceLocation dimRl = targetDim != null ? ResourceLocation.tryParse(targetDim) : null;
                        if (targetPos != null && dimRl != null) {
                            return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimRl), targetPos);
                        }
                    } else if (state == 3) {
                        if (ClientLocatorCache.structureTarget != null && ClientLocatorCache.structureTargetDimension != null) {
                            return GlobalPos.of(ClientLocatorCache.structureTargetDimension, ClientLocatorCache.structureTarget);
                        }
                    }
                    return null;
                })
        );

        registerUnclampedProperty(
                ModItems.PLAYER_LOCATOR_COMPASS.get(),
                ResourceLocation.fromNamespaceAndPath(bodycam.MOD_ID, "state"),
                (stack, level, entity, seed) -> {
                    CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
                    if (tag != null && tag.contains("LocatorState")) {
                        return (float) tag.getInt("LocatorState");
                    }
                    return 0.0F;
                }
        );

        ClientGuiEvent.RENDER_HUD.register((graphics, deltaTracker) -> {
            PortalChannelOverlay.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        });
    }

    private static void registerUnclampedProperty(Item item, ResourceLocation id, ItemPropertyFunction function) {
        ItemPropertiesAccessor.bodycam$getProperties()
                .computeIfAbsent(item, k -> new HashMap<>())
                .put(id, function);
    }
}
