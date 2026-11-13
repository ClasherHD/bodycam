package dev.ClasherHD.bodycam.neoforge;

import dev.ClasherHD.bodycam.client.event.ClientSetupHandler;
import dev.ClasherHD.bodycam.client.gui.AncientTableScreen;
import dev.ClasherHD.bodycam.client.gui.ClientConfigScreen;
import dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer;
import dev.ClasherHD.bodycam.client.render.CherryHouseMapRenderer;
import dev.ClasherHD.bodycam.client.render.CreepingDeathRenderer;
import dev.ClasherHD.bodycam.client.render.PodestBlockEntityRenderer;
import dev.ClasherHD.bodycam.client.render.ScholarRenderer;
import dev.ClasherHD.bodycam.client.render.WatchPointDummyRenderer;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModMenus;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public final class BodycamNeoForgeClient {
    private BodycamNeoForgeClient() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        modBus.addListener(BodycamNeoForgeClient::onRegisterMenuScreens);
        modBus.addListener(BodycamNeoForgeClient::onRegisterRenderers);
        modBus.addListener(BodycamNeoForgeClient::onAddLayers);
        modBus.addListener(BodycamNeoForgeClient::onClientSetup);
        NeoForge.EVENT_BUS.addListener(BodycamNeoForgeClient::onRenderHand);
        NeoForge.EVENT_BUS.addListener(BodycamNeoForgeClient::onRenderItemInFrame);
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new ClientConfigScreen(parent));
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ANCIENT_TABLE.get(), AncientTableScreen::new);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.BODYCAM_DUMMY.get(), BodycamDummyRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.HOLOGRAM_DUMMY.get(), BodycamDummyRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.WATCH_POINT_DUMMY.get(), WatchPointDummyRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SCHOLAR.get(), ScholarRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CREEPING_DEATH.get(), CreepingDeathRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PODEST.get(), PodestBlockEntityRenderer::new);
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        var itemInHandRenderer = event.getContext().getItemInHandRenderer();
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                ClientSetupHandler.attachPlayerLayers(playerRenderer, itemInHandRenderer, true);
            }
        }
        for (EntityType<?> entityType : ClientSetupHandler.HUMANOID_TRIM_ENTITY_TYPES) {
            EntityRenderer<?> renderer = event.getRenderer(entityType);
            if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
                ClientSetupHandler.tryAddGlowingTrimLayer(livingRenderer);
            }
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetupHandler::initClient);
    }

    private static void onRenderHand(RenderHandEvent event) {
        if (CherryHouseMapRenderer.renderHand(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight(),
                event.getHand(),
                event.getSwingProgress(),
                event.getEquipProgress(),
                event.getInterpolatedPitch(),
                event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    private static void onRenderItemInFrame(RenderItemInFrameEvent event) {
        if (CherryHouseMapRenderer.renderItemInFrame(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight(),
                event.getItemStack())) {
            event.setCanceled(true);
        }
    }
}
