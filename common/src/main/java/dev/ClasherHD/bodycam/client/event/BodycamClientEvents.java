package dev.ClasherHD.bodycam.client.event;

import dev.architectury.event.EventResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import dev.architectury.networking.NetworkManager;
import dev.ClasherHD.bodycam.network.SyncBodycamRequestC2SPacket;
import dev.ClasherHD.bodycam.Bodycam;

public class BodycamClientEvents {
    public static void register() {
        dev.architectury.event.events.common.InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (handleHologramInteraction(player, player.level(), pos)) {
                return EventResult.interruptTrue();
            }
            return EventResult.pass();
        });

        dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.level != null && client.player != null && dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring) {
                if (client.screen == null) {
                    client.setScreen(new dev.ClasherHD.bodycam.client.gui.BodycamViewScreen(
                        dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetUuid,
                        dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetNameStatic,
                        dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.lastHasReach,
                        dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.lastIsOnHologram
                    ));
                }
            }
        });

        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring = false;
            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetUuid = null;
            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetNameStatic = null;
            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.lastHasReach = false;
            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.lastIsOnHologram = false;
            dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen.observingMe.clear();
            dev.ClasherHD.bodycam.client.ClientBodycamCache.clear();
        });
    }

    private static boolean handleHologramInteraction(Player player, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        boolean onHologram = level.getBlockState(pos).is(Bodycam.HOLOGRAM_BLOCK.get()) ||
                             level.getBlockState(player.blockPosition().below()).is(Bodycam.HOLOGRAM_BLOCK.get()) ||
                             level.getBlockState(player.blockPosition()).is(Bodycam.HOLOGRAM_BLOCK.get());
        if (!onHologram) return false;

        if (level.isClientSide()) {
            if (Minecraft.getInstance().screen == null) {
                NetworkManager.sendToServer(new SyncBodycamRequestC2SPacket(true, true));
            }
        }
        return true;
    }
}
