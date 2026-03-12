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
                if (mc.player.isDeadOrDying()) {
                    dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring = false;
                    return;
                }
                if (dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring && mc.screen == null) {
                    mc.setScreen(new dev.ClasherHD.bodycam.client.gui.BodycamViewScreen(
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetUuid,
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetNameStatic, 
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.lastHasReach));
                }
            }
        }
    }
}
