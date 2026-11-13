package dev.ClasherHD.bodycam.client.event;

import dev.ClasherHD.bodycam.client.cache.ClientBodycamCache;
import dev.ClasherHD.bodycam.client.cache.ClientLocatorCache;
import dev.ClasherHD.bodycam.client.camera.CameraSnapState;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamRequestC2SPacket;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class BodycamClientEvents {
    private BodycamClientEvents() {
    }

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(BodycamClientEvents::onClientTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            BodycamViewScreen.isMonitoring = false;
            BodycamViewScreen.targetUuid = null;
            BodycamViewScreen.targetNameStatic = null;
            BodycamViewScreen.lastIsOnHologram = false;
            BodycamViewScreen.lastIsWatchPoint = false;
            BodycamViewScreen.lastIsAllSeeingEye = false;
            ClientBodycamCache.clear();
            ClientLocatorCache.clear();
            PlayerSelectionScreen.observingMe.clear();
            CameraSnapState.clearAll();
        });
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            if (handleHologramInteraction(player, hand)) {
                return CompoundEventResult.interruptTrue(player.getItemInHand(hand));
            }
            return CompoundEventResult.pass();
        });
        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register(BodycamClientEvents::handleHologramInteraction);
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            if (handleHologramInteraction(player, hand)) {
                return EventResult.interruptTrue();
            }
            return EventResult.pass();
        });
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.player != null) {
            CameraSnapState.tickFrameGuard(mc.player);
            if (BodycamViewScreen.isMonitoring && mc.screen == null && BodycamViewScreen.targetUuid != null) {
                mc.setScreen(new BodycamViewScreen(
                        BodycamViewScreen.targetUuid,
                        BodycamViewScreen.targetNameStatic != null ? BodycamViewScreen.targetNameStatic : "",
                        BodycamViewScreen.lastIsOnHologram,
                        BodycamViewScreen.lastIsWatchPoint,
                        BodycamViewScreen.lastIsAllSeeingEye
                ));
            }
        }
    }

    private static boolean handleHologramInteraction(Player player, InteractionHand hand) {
        if (player == null || hand != InteractionHand.MAIN_HAND || !player.isCrouching()) {
            return false;
        }
        boolean onHologram = player.level().getBlockState(player.blockPosition().below()).is(ModBlocks.HOLOGRAM_BLOCK.get())
                || player.level().getBlockState(player.blockPosition()).is(ModBlocks.HOLOGRAM_BLOCK.get());
        if (!onHologram) {
            return false;
        }
        if (player.level().isClientSide()) {
            if (Minecraft.getInstance().screen == null) {
                PacketHandler.sendToServer(new SyncBodycamRequestC2SPacket(true));
            }
        }
        return true;
    }
}
