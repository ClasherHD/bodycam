package dev.ClasherHD.bodycam.client.event;


import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;

public class BodycamClientEvents {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring && client.screen == null) {
                    client.setScreen(new dev.ClasherHD.bodycam.client.gui.BodycamViewScreen(
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetUuid,
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.targetNameStatic, 
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.lastHasReach,
                            dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.lastIsOnHologram));
                }
            }
        });




        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (handleHologramInteraction(player, world)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    private static boolean handleHologramInteraction(Player player, net.minecraft.world.level.Level level) {
        if (!player.isCrouching()) return false;
        boolean onHologram = level.getBlockState(player.blockPosition().below()).is(dev.ClasherHD.bodycam.BodycamFabric.HOLOGRAM_BLOCK) ||
                             level.getBlockState(player.blockPosition()).is(dev.ClasherHD.bodycam.BodycamFabric.HOLOGRAM_BLOCK);
        if (!onHologram) return false;
        
        if (level.isClientSide()) {
            if (Minecraft.getInstance().screen == null) {
                net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                buf.writeInt(0);
                buf.writeBoolean(true);
                buf.writeBoolean(true);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(dev.ClasherHD.bodycam.network.BodycamPacketIDs.REQUEST_CAMERA_PACKET_ID, buf);
            }
        }
        return true;
    }
}
