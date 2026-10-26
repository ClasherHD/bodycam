package dev.ClasherHD.bodycam.client.event;

import dev.ClasherHD.bodycam.client.cache.ClientBodycamCache;
import dev.ClasherHD.bodycam.client.cache.ClientLocatorCache;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BodycamClientEventsTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @BeforeEach
    void resetState() {
        BodycamViewScreen.isMonitoring = false;
        BodycamViewScreen.targetUuid = null;
        BodycamViewScreen.targetNameStatic = null;
        BodycamViewScreen.lastIsOnHologram = false;
        BodycamViewScreen.lastIsWatchPoint = false;
        ClientBodycamCache.clear();
        ClientLocatorCache.clear();
        PlayerSelectionScreen.observingMe.clear();
    }

    @Test
    @DisplayName("onClientLogout should reset monitoring state and clear all client caches")
    void onClientLogout_cleansClientState() {
        BodycamViewScreen.isMonitoring = true;
        BodycamViewScreen.targetUuid = UUID.randomUUID();
        BodycamViewScreen.lastIsWatchPoint = true;
        ClientBodycamCache.jammers.put(UUID.randomUUID(), 2);
        ClientLocatorCache.structureTargetDimension = net.minecraft.world.level.Level.NETHER;
        PlayerSelectionScreen.observingMe.add(UUID.randomUUID());

        ClientPlayerNetworkEvent.LoggingOut event = MinecraftTestEnvironment.allocateInstance(ClientPlayerNetworkEvent.LoggingOut.class);
        BodycamClientEvents.onClientLogout(event);

        assertThat(BodycamViewScreen.isMonitoring).isFalse();
        assertThat(BodycamViewScreen.targetUuid).isNull();
        assertThat(BodycamViewScreen.lastIsWatchPoint).isFalse();
        assertThat(ClientBodycamCache.jammers).isEmpty();
        assertThat(ClientLocatorCache.structureTargetDimension).isNull();
        assertThat(PlayerSelectionScreen.observingMe).isEmpty();
    }

    @Test
    @DisplayName("onRenderGuiOverlay cancels vanilla hud elements when monitoring")
    void onRenderGuiOverlay_cancelsVanillaOverlaysWhenMonitoring() {
        BodycamViewScreen.isMonitoring = true;

        NamedGuiOverlay healthOverlay = mock(NamedGuiOverlay.class);
        when(healthOverlay.id()).thenReturn(VanillaGuiOverlay.PLAYER_HEALTH.id());
        RenderGuiOverlayEvent.Pre healthEvent = new RenderGuiOverlayEvent.Pre(null, null, 0.0F, healthOverlay);
        BodycamClientEvents.onRenderGuiOverlay(healthEvent);
        assertThat(healthEvent.isCanceled()).isTrue();

        NamedGuiOverlay foodOverlay = mock(NamedGuiOverlay.class);
        when(foodOverlay.id()).thenReturn(VanillaGuiOverlay.FOOD_LEVEL.id());
        RenderGuiOverlayEvent.Pre foodEvent = new RenderGuiOverlayEvent.Pre(null, null, 0.0F, foodOverlay);
        BodycamClientEvents.onRenderGuiOverlay(foodEvent);
        assertThat(foodEvent.isCanceled()).isTrue();

        NamedGuiOverlay crosshairOverlay = mock(NamedGuiOverlay.class);
        when(crosshairOverlay.id()).thenReturn(VanillaGuiOverlay.CROSSHAIR.id());
        RenderGuiOverlayEvent.Pre crosshairEvent = new RenderGuiOverlayEvent.Pre(null, null, 0.0F, crosshairOverlay);
        BodycamClientEvents.onRenderGuiOverlay(crosshairEvent);
        assertThat(crosshairEvent.isCanceled()).isTrue();

        NamedGuiOverlay chatOverlay = mock(NamedGuiOverlay.class);
        when(chatOverlay.id()).thenReturn(VanillaGuiOverlay.CHAT_PANEL.id());
        RenderGuiOverlayEvent.Pre chatEvent = new RenderGuiOverlayEvent.Pre(null, null, 0.0F, chatOverlay);
        BodycamClientEvents.onRenderGuiOverlay(chatEvent);
        assertThat(chatEvent.isCanceled()).isFalse();

        BodycamViewScreen.isMonitoring = false;
        RenderGuiOverlayEvent.Pre nonMonitoringEvent = new RenderGuiOverlayEvent.Pre(null, null, 0.0F, healthOverlay);
        BodycamClientEvents.onRenderGuiOverlay(nonMonitoringEvent);
        assertThat(nonMonitoringEvent.isCanceled()).isFalse();
    }

    @Test
    @DisplayName("onClientTick ignores start phase and non-monitoring state")
    void onClientTick_conditions() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            LocalPlayer player = mock(LocalPlayer.class);
            mc.player = player;
            mc.screen = null;
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            TickEvent.ClientTickEvent startEvent = new TickEvent.ClientTickEvent(TickEvent.Phase.START);
            BodycamViewScreen.isMonitoring = true;
            BodycamClientEvents.onClientTick(startEvent);
            verify(mc, never()).setScreen(any());

            TickEvent.ClientTickEvent endEvent = new TickEvent.ClientTickEvent(TickEvent.Phase.END);
            BodycamViewScreen.isMonitoring = false;
            BodycamClientEvents.onClientTick(endEvent);
            verify(mc, never()).setScreen(any());
        }
    }

    @Test
    @DisplayName("onRightClickItem and onRightClickEmpty cancel and process when crouching on HologramBlock")
    void handleHologramInteraction_checksCrouchingAndBlock() {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        BlockPos pos = new BlockPos(10, 64, 10);
        when(player.blockPosition()).thenReturn(pos);
        when(player.isCrouching()).thenReturn(true);
        when(player.level()).thenReturn(level);
        when(player.getCommandSenderWorld()).thenReturn(level);
        when(level.isClientSide()).thenReturn(true);
        when(level.getBlockState(pos.below())).thenReturn(ModBlocks.HOLOGRAM_BLOCK.get().defaultBlockState());

        PlayerInteractEvent.RightClickItem itemEvent = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        MinecraftTestEnvironment.setField(itemEvent, "level", level);

        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            mc.screen = null;
            net.minecraft.client.multiplayer.ClientPacketListener connection = mock(net.minecraft.client.multiplayer.ClientPacketListener.class);
            net.minecraft.network.Connection netConnection = mock(net.minecraft.network.Connection.class);
            when(connection.getConnection()).thenReturn(netConnection);
            when(mc.getConnection()).thenReturn(connection);
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            BodycamClientEvents.onRightClickItem(itemEvent);
            assertThat(itemEvent.isCanceled()).isTrue();
        }

        PlayerInteractEvent.RightClickItem offhandEvent = new PlayerInteractEvent.RightClickItem(player, InteractionHand.OFF_HAND);
        MinecraftTestEnvironment.setField(offhandEvent, "level", level);
        BodycamClientEvents.onRightClickItem(offhandEvent);
        assertThat(offhandEvent.isCanceled()).isFalse();

        when(player.isCrouching()).thenReturn(false);
        PlayerInteractEvent.RightClickItem standEvent = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        MinecraftTestEnvironment.setField(standEvent, "level", level);
        BodycamClientEvents.onRightClickItem(standEvent);
        assertThat(standEvent.isCanceled()).isFalse();

        when(player.isCrouching()).thenReturn(true);
        when(level.getBlockState(pos.below())).thenReturn(Blocks.AIR.defaultBlockState());
        when(level.getBlockState(pos)).thenReturn(Blocks.AIR.defaultBlockState());
        PlayerInteractEvent.RightClickItem airEvent = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        MinecraftTestEnvironment.setField(airEvent, "level", level);
        BodycamClientEvents.onRightClickItem(airEvent);
        assertThat(airEvent.isCanceled()).isFalse();
    }
}
