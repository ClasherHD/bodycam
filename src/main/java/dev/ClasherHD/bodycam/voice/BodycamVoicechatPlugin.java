package dev.ClasherHD.bodycam.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.Minecraft;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;

@ForgeVoicechatPlugin
public class BodycamVoicechatPlugin implements VoicechatPlugin {

    public static VoicechatApi voicechatApi;

    @Override
    public String getPluginId() {
        return "bodycam";
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void registerEvents(EventRegistration registration) {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof BodycamViewScreen viewScreen) {
                java.util.UUID targetId = viewScreen.getTargetId();
                if (targetId != null && mc.level != null) {
                    net.minecraft.world.entity.player.Player target = mc.level.getPlayerByUUID(targetId);
                    if (target != null && target.isAlive()) {
                        try {
                            org.lwjgl.openal.AL10.alListener3f(org.lwjgl.openal.AL10.AL_POSITION,
                                    (float) target.getX(), (float) target.getEyeY(), (float) target.getZ());
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }
}
