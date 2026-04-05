package dev.ClasherHD.bodycam.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraft.client.Minecraft;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;

public class BodycamVoicechatPlugin implements VoicechatPlugin {

    public static VoicechatApi voicechatApi;

    @Override
    public String getPluginId() {
        return "bodycam";
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public void onClientTick(Minecraft mc) {
        if (mc.screen instanceof BodycamViewScreen viewScreen) {
            java.util.UUID targetId = viewScreen.getTargetId();
            if (targetId != null && mc.level != null) {
                net.minecraft.world.entity.player.Player target = mc.level.getPlayerByUUID(targetId);
                if (target != null && target.isAlive()) {
                    try {
                        org.lwjgl.openal.AL10.alListener3f(org.lwjgl.openal.AL10.AL_POSITION,
                                (float) target.getX(), (float) target.getEyeY(), (float) target.getZ());
                        org.lwjgl.openal.AL10.alListener3f(org.lwjgl.openal.AL10.AL_VELOCITY, 0.0F, 0.0F, 0.0F);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
    }
}
