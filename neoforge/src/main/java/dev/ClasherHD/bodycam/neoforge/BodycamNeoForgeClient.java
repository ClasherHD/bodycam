package dev.ClasherHD.bodycam.neoforge;

import dev.ClasherHD.bodycam.client.event.ClientSetupHandler;
import dev.ClasherHD.bodycam.client.gui.AncientTableScreen;
import dev.ClasherHD.bodycam.client.gui.ClientConfigScreen;
import dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer;
import dev.ClasherHD.bodycam.client.render.CreepingDeathRenderer;
import dev.ClasherHD.bodycam.client.render.PodestBlockEntityRenderer;
import dev.ClasherHD.bodycam.client.render.ScholarRenderer;
import dev.ClasherHD.bodycam.client.render.WatchPointDummyRenderer;
import dev.ClasherHD.bodycam.client.render.item.BodycamItemModelProperties;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModMenus;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@SuppressWarnings("null")
public final class BodycamNeoForgeClient {
    private BodycamNeoForgeClient() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        modBus.addListener(BodycamNeoForgeClient::onRegisterMenuScreens);
        modBus.addListener(BodycamNeoForgeClient::onRegisterRenderers);
        modBus.addListener(BodycamNeoForgeClient::onRegisterConditionalItemProperties);
        modBus.addListener(BodycamNeoForgeClient::onRegisterRangeSelectItemProperties);
        modBus.addListener(BodycamNeoForgeClient::onAddLayers);
        modBus.addListener(BodycamNeoForgeClient::onClientSetup);
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

    private static void onRegisterConditionalItemProperties(RegisterConditionalItemModelPropertyEvent event) {
        BodycamItemModelProperties.registerConditional(event::register);
    }

    private static void onRegisterRangeSelectItemProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        BodycamItemModelProperties.registerRangeSelect(event::register);
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> playerRenderer = event.getPlayerRenderer(skin);
            if (playerRenderer != null) {
                ClientSetupHandler.tryAddGlowingTrimLayer(playerRenderer, event.getEntityModels());
            }
            AvatarRenderer<ClientMannequin> mannequinRenderer = event.getMannequinRenderer(skin);
            if (mannequinRenderer != null) {
                ClientSetupHandler.tryAddGlowingTrimLayer(mannequinRenderer, event.getEntityModels());
            }
        }
        for (EntityType<?> entityType : ClientSetupHandler.HUMANOID_TRIM_ENTITY_TYPES) {
            EntityRenderer<?, ?> renderer = event.getRenderer(entityType);
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer) {
                ClientSetupHandler.tryAddGlowingTrimLayer(livingRenderer, event.getEntityModels());
            }
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetupHandler::initClient);
    }
}
