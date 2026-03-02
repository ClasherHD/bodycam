package dev.ClasherHD.bodycam.voice;


import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.openal.AL10;

import java.util.UUID;

public class BodycamVoicechatPlugin implements VoicechatPlugin {

    public static VoicechatApi voicechatApi;

    public BodycamVoicechatPlugin() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
    }

    @Override
    public String getPluginId() {
        return "bodycam";
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
    }

    private void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BodycamViewScreen viewScreen) {
            UUID targetId = viewScreen.getTargetId();
            if (targetId != null && mc.level != null) {
                Player target = mc.level.getPlayerByUUID(targetId);
                if (target != null && target.isAlive()) {
                    try {
                        AL10.alListener3f(AL10.AL_POSITION,
                                (float) target.getX(), (float) target.getEyeY(), (float) target.getZ());
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}
