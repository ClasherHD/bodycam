package dev.ClasherHD.bodycam.client.event;

import dev.ClasherHD.bodycam.client.gui.AncientTableScreen;
import dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer;
import dev.ClasherHD.bodycam.client.render.CreepingDeathRenderer;
import dev.ClasherHD.bodycam.client.render.GlowingArmorTrimLayer;
import dev.ClasherHD.bodycam.client.render.PodestBlockEntityRenderer;
import dev.ClasherHD.bodycam.client.render.PortalChannelOverlay;
import dev.ClasherHD.bodycam.client.render.ScholarRenderer;
import dev.ClasherHD.bodycam.client.render.WatchPointDummyRenderer;
import dev.ClasherHD.bodycam.client.render.item.BodycamItemModelProperties;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.ClasherHD.bodycam.mixin.LivingEntityRendererAccessor;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModMenus;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.registry.client.gui.MenuScreenRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.util.List;
import java.util.Set;

@SuppressWarnings("null")
public final class ClientSetupHandler {

    public static final Set<EntityType<?>> HUMANOID_TRIM_ENTITY_TYPES = Set.of(
            EntityTypes.ZOMBIE,
            EntityTypes.HUSK,
            EntityTypes.DROWNED,
            EntityTypes.ZOMBIE_VILLAGER,
            EntityTypes.SKELETON,
            EntityTypes.STRAY,
            EntityTypes.WITHER_SKELETON,
            EntityTypes.BOGGED,
            EntityTypes.PIGLIN,
            EntityTypes.PIGLIN_BRUTE,
            EntityTypes.ZOMBIFIED_PIGLIN,
            EntityTypes.ARMOR_STAND
    );

    private ClientSetupHandler() {
    }

    public static void registerMenusAndRenderers() {
        MenuScreenRegistry.registerScreenFactory(ModMenus.ANCIENT_TABLE.get(), AncientTableScreen::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.PODEST.get(), PodestBlockEntityRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.BODYCAM_DUMMY, BodycamDummyRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.HOLOGRAM_DUMMY, BodycamDummyRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.WATCH_POINT_DUMMY, WatchPointDummyRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.SCHOLAR, ScholarRenderer::new);
        EntityRendererRegistry.register(ModEntityTypes.CREEPING_DEATH, CreepingDeathRenderer::new);
        BodycamItemModelProperties.registerDirect();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void tryAddGlowingTrimLayer(LivingEntityRenderer<?, ?, ?> livingRenderer, EntityModelSet modelSet) {
        if (!(livingRenderer.getModel() instanceof HumanoidModel<?>)) {
            return;
        }
        List<RenderLayer<?, ?>> layers = ((LivingEntityRendererAccessor) livingRenderer).bodycam$getLayers();
        for (RenderLayer<?, ?> layer : layers) {
            if (layer instanceof GlowingArmorTrimLayer<?, ?>) {
                return;
            }
        }
        layers.add(new GlowingArmorTrimLayer<>((RenderLayerParent) livingRenderer, modelSet));
    }

    public static void initClient() {
        ModClientConfig.load();
        BodycamClientEvents.register();
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, deltaTracker) ->
                PortalChannelOverlay.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
    }
}
