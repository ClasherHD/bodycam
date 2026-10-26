package dev.ClasherHD.bodycam.client.event;

import dev.ClasherHD.bodycam.client.render.GlowingArmorTrimLayer;
import dev.ClasherHD.bodycam.client.render.PodestBlockEntityRenderer;
import dev.ClasherHD.bodycam.client.render.PortalChannelOverlay;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class ClientSetupHandlerTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("registerGuiOverlays registers portal channel overlay")
    void registerGuiOverlays_registersOverlay() {
        RegisterGuiOverlaysEvent event = mock(RegisterGuiOverlaysEvent.class);
        ClientSetupHandler.registerGuiOverlays(event);

        verify(event).registerAboveAll("portal_channel_overlay", PortalChannelOverlay.INSTANCE);
    }

    @Test
    @DisplayName("registerRenderers registers entity and block entity renderers")
    void registerRenderers_registersAllRenderers() {
        EntityRenderersEvent.RegisterRenderers event = mock(EntityRenderersEvent.RegisterRenderers.class);
        ClientSetupHandler.registerRenderers(event);

        verify(event).registerEntityRenderer(eq(ModEntityTypes.COMPASS_DUMMY.get()), any());
        verify(event).registerEntityRenderer(eq(ModEntityTypes.HOLOGRAM_DUMMY.get()), any());
        verify(event).registerEntityRenderer(eq(ModEntityTypes.WATCH_POINT_DUMMY.get()), any());
        verify(event).registerBlockEntityRenderer(eq(ModBlockEntities.PODEST.get()), any());
    }

    @Test
    @DisplayName("tryAddGlowingTrimLayer handles null, missing armor layer, adding layer, and already existing layer")
    void tryAddGlowingTrimLayer_branchCoverage() throws Exception {
        Method tryAdd = ClientSetupHandler.class.getDeclaredMethod("tryAddGlowingTrimLayer", LivingEntityRenderer.class);
        tryAdd.setAccessible(true);

        assertThatCode(() -> tryAdd.invoke(null, (LivingEntityRenderer<?, ?>) null))
                .doesNotThrowAnyException();

        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, ?> rendererWithoutArmor = mock(LivingEntityRenderer.class);
        List<RenderLayer<?, ?>> layersList = new ArrayList<>();
        MinecraftTestEnvironment.setField(rendererWithoutArmor, "layers", layersList);
        org.mockito.Mockito.doAnswer(inv -> layersList.add(inv.getArgument(0))).when(rendererWithoutArmor).addLayer(any());

        tryAdd.invoke(null, rendererWithoutArmor);
        assertThat(layersList).isEmpty();

        @SuppressWarnings("unchecked")
        HumanoidArmorLayer<?, ?, ?> armorLayer = mock(HumanoidArmorLayer.class);
        layersList.add(armorLayer);

        tryAdd.invoke(null, rendererWithoutArmor);
        assertThat(layersList).hasSize(2);
        assertThat(layersList.get(1)).isInstanceOf(GlowingArmorTrimLayer.class);

        tryAdd.invoke(null, rendererWithoutArmor);
        assertThat(layersList).hasSize(2);
    }

    @Test
    @DisplayName("registerConfigScreen registers extension point with ModLoadingContext")
    void registerConfigScreen_registersExtensionPoint() {
        try (MockedStatic<ModLoadingContext> mockedContext = mockStatic(ModLoadingContext.class)) {
            ModLoadingContext context = mock(ModLoadingContext.class);
            mockedContext.when(ModLoadingContext::get).thenReturn(context);

            ClientSetupHandler.registerConfigScreen();

            verify(context).registerExtensionPoint(eq(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class), any());
        }
    }

    @Test
    @DisplayName("clientSetup enqueues setup work that registers item properties and config screen")
    void clientSetup_enqueuesSetupWork() {
        try (MockedStatic<ModLoadingContext> mockedContext = mockStatic(ModLoadingContext.class)) {
            ModLoadingContext context = mock(ModLoadingContext.class);
            mockedContext.when(ModLoadingContext::get).thenReturn(context);

            FMLClientSetupEvent event = mock(FMLClientSetupEvent.class);
            doAnswer(invocation -> {
                Runnable r = invocation.getArgument(0);
                r.run();
                return null;
            }).when(event).enqueueWork(any(Runnable.class));

            ClientSetupHandler.clientSetup(event);

            verify(event).enqueueWork(any(Runnable.class));
            verify(context).registerExtensionPoint(eq(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class), any());
        }
    }

    @Test
    @DisplayName("Item property functions correctly reflect item state tags")
    void itemPropertyFunctions_reflectState() {
        ItemStack monitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());
        assertThat(ItemDurabilityHelper.isEmpty(monitor)).isFalse();
        ItemDurabilityHelper.setEmpty(monitor);
        assertThat(ItemDurabilityHelper.isEmpty(monitor)).isTrue();

        ItemStack jammer = new ItemStack(ModItems.JAMMER.get());
        jammer.getOrCreateTag().putInt("JammerMode", 2);
        assertThat(jammer.getTag().getInt("JammerMode")).isEqualTo(2);

        ItemStack anonymizer = new ItemStack(ModItems.ANONYMIZER.get());
        anonymizer.getOrCreateTag().putBoolean("AnonymizerActive", true);
        assertThat(anonymizer.getTag().getBoolean("AnonymizerActive")).isTrue();

        ItemStack portal = new ItemStack(ModItems.PORTABLE_NETHER_PORTAL.get());
        assertThat(PortableNetherPortalItem.getCharges(portal) > 0).isFalse();
        PortableNetherPortalItem.setCharges(portal, 2);
        assertThat(PortableNetherPortalItem.getCharges(portal) > 0).isTrue();

        ItemStack compass = new ItemStack(ModItems.PLAYER_LOCATOR_COMPASS.get());
        compass.getOrCreateTag().putInt("LocatorState", 3);
        assertThat(compass.getTag().getInt("LocatorState")).isEqualTo(3);
    }
}
