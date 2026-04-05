package dev.ClasherHD.bodycam.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class JammerItem extends Item {
    public JammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("This feature is restricted to Server Operators.")
                                .withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_JAMMER.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component
                        .literal("The Jammer is disabled on this server.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            int mode = stack.hasTag() ? stack.getTag().getInt("JammerMode") : 0;
            mode = (mode + 1) % 3;
            stack.getOrCreateTag().putInt("JammerMode", mode);

            java.util.UUID activeId = null;
            if (mode > 0) {
                activeId = java.util.UUID.randomUUID();
                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putUUID("bodycam_active_jammer_id",
                        activeId);
                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putLong("bodycam_jammer_heartbeat",
                        level.getGameTime());
            }

            for (net.minecraft.world.item.ItemStack invStack : player.getInventory().items) {
                if (invStack.getItem() instanceof JammerItem) {
                    invStack.getOrCreateTag().putInt("JammerMode", mode);
                    if (activeId != null)
                        invStack.getOrCreateTag().putUUID("active_id", activeId);
                    else
                        invStack.getOrCreateTag().remove("active_id");
                }
            }
            for (net.minecraft.world.item.ItemStack invStack : player.getInventory().armor) {
                if (invStack.getItem() instanceof JammerItem) {
                    invStack.getOrCreateTag().putInt("JammerMode", mode);
                    if (activeId != null)
                        invStack.getOrCreateTag().putUUID("active_id", activeId);
                    else
                        invStack.getOrCreateTag().remove("active_id");
                }
            }
            for (net.minecraft.world.item.ItemStack invStack : player.getInventory().offhand) {
                if (invStack.getItem() instanceof JammerItem) {
                    invStack.getOrCreateTag().putInt("JammerMode", mode);
                    if (activeId != null)
                        invStack.getOrCreateTag().putUUID("active_id", activeId);
                    else
                        invStack.getOrCreateTag().remove("active_id");
                }
            }

            String key = mode == 0 ? "message.bodycam.jammer.off"
                    : (mode == 1 ? "message.bodycam.jammer.on" : "message.bodycam.jammer.limited");
            net.minecraft.ChatFormatting color = mode == 0 ? net.minecraft.ChatFormatting.RED
                    : (mode == 1 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.BLUE);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key).withStyle(color), true);

            if (mode > 0) {
                net.minecraft.server.MinecraftServer server = level.getServer();
                if (server != null) {
                    for (net.minecraft.server.level.ServerPlayer observer : server.getPlayerList().getPlayers()) {
                        if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(observer)
                                .getBoolean("bodycam_active")) {
                            java.util.UUID targetUuid = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(observer)
                                    .getUUID("bodycam_target_uuid");
                            if (player.getUUID().equals(targetUuid)) {
                                dev.ClasherHD.bodycam.network.BodycamResetCameraPacket.executeReset(observer);
                                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(observer,
                                        dev.ClasherHD.bodycam.network.BodycamPacketIDs.RESET_CAMERA_PACKET_ID,
                                        net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
                            }
                        }
                    }
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId,
            boolean isSelected) {
        if (!level.isClientSide() && entity instanceof net.minecraft.server.level.ServerPlayer) {
            int mode = stack.hasTag() && stack.getTag().contains("JammerMode") ? stack.getTag().getInt("JammerMode")
                    : 0;
            if (mode > 0) {
                boolean isValid = false;
                if (stack.hasTag() && stack.getTag().hasUUID("active_id") && dev.ClasherHD.bodycam.BodycamHelper
                        .getPersistentData(entity).hasUUID("bodycam_active_jammer_id")) {
                    java.util.UUID itemUUID = stack.getTag().getUUID("active_id");
                    java.util.UUID playerUUID = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(entity)
                            .getUUID("bodycam_active_jammer_id");
                    long lastHeartbeat = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(entity)
                            .getLong("bodycam_jammer_heartbeat");
                    boolean isCreative = ((net.minecraft.server.level.ServerPlayer) entity).isCreative();
                    isValid = itemUUID.equals(playerUUID)
                            && (isCreative || (level.getGameTime() - lastHeartbeat <= 10));
                }

                if (isValid) {
                    dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(entity).putLong("bodycam_jammer_heartbeat",
                            level.getGameTime());
                    dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(entity).putInt("bodycam_jammer_mode", mode);
                } else {
                    stack.getOrCreateTag().putInt("JammerMode", 0);
                    stack.getOrCreateTag().remove("active_id");
                }
            }
        }
    }
}
