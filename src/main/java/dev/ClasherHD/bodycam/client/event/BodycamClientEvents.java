package dev.ClasherHD.bodycam.client.event;

import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "bodycam", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BodycamClientEvents {

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                if (dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring && mc.screen == null) {
                    mc.setScreen(new dev.ClasherHD.bodycam.client.gui.BodycamViewScreen(
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetUuid,
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetNameStatic, 
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.lastHasReach,
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.lastIsOnHologram));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        if (dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring) {
            net.minecraft.resources.ResourceLocation id = event.getOverlay().id();
            if (id.equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.PLAYER_HEALTH.id()) ||
                id.equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.FOOD_LEVEL.id()) ||
                id.equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.ARMOR_LEVEL.id()) ||
                id.equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.EXPERIENCE_BAR.id()) ||
                id.equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id()) ||
                id.equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.id())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        handleHologramInteraction(event);
    }

    @SubscribeEvent
    public static void onRightClickEmpty(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickEmpty event) {
        handleHologramInteraction(event);
    }

    private static void handleHologramInteraction(net.minecraftforge.event.entity.player.PlayerInteractEvent event) {
        if (!event.getEntity().isCrouching()) return;
        boolean onHologram = event.getLevel().getBlockState(event.getEntity().blockPosition().below()).is(dev.ClasherHD.bodycam.bodycam.HOLOGRAM_BLOCK.get()) ||
                             event.getLevel().getBlockState(event.getEntity().blockPosition()).is(dev.ClasherHD.bodycam.bodycam.HOLOGRAM_BLOCK.get());
        if (!onHologram) return;
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
        if (event.getLevel().isClientSide()) {
            if (net.minecraft.client.Minecraft.getInstance().screen == null) {
                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.sendToServer(new dev.ClasherHD.bodycam.network.SyncBodycamRequestC2SPacket(true, true));
            }
        }
    }
}
