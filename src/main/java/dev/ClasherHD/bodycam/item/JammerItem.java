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
        ItemStack stackInHand = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This feature is restricted to Server Operators.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stackInHand);
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_JAMMER.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The Jammer is disabled on this server.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stackInHand);
            }
            int currentMode = stackInHand.getOrCreateTag().getInt("JammerMode");
            int nextMode = (currentMode + 1) % 3;

            java.util.UUID activeId = null;
            if (nextMode > 0) {
                activeId = java.util.UUID.randomUUID();
                player.getPersistentData().putUUID("bodycam_active_jammer_id", activeId);
                player.getPersistentData().putLong("bodycam_jammer_heartbeat", level.getGameTime());
            }

            for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof JammerItem) {
                    stack.getOrCreateTag().putInt("JammerMode", nextMode);
                    if (activeId != null) stack.getOrCreateTag().putUUID("active_id", activeId);
                    else stack.getOrCreateTag().remove("active_id");
                }
            }
            for (net.minecraft.world.item.ItemStack stack : player.getInventory().armor) {
                if (stack.getItem() instanceof JammerItem) {
                    stack.getOrCreateTag().putInt("JammerMode", nextMode);
                    if (activeId != null) stack.getOrCreateTag().putUUID("active_id", activeId);
                    else stack.getOrCreateTag().remove("active_id");
                }
            }
            for (net.minecraft.world.item.ItemStack stack : player.getInventory().offhand) {
                if (stack.getItem() instanceof JammerItem) {
                    stack.getOrCreateTag().putInt("JammerMode", nextMode);
                    if (activeId != null) stack.getOrCreateTag().putUUID("active_id", activeId);
                    else stack.getOrCreateTag().remove("active_id");
                }
            }

            String key = nextMode == 0 ? "message.bodycam.jammer.off" : (nextMode == 1 ? "message.bodycam.jammer.on" : "message.bodycam.jammer.limited");
            net.minecraft.ChatFormatting color = nextMode == 0 ? net.minecraft.ChatFormatting.RED : (nextMode == 1 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.BLUE);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key).withStyle(color), true);
        }
        return InteractionResultHolder.sidedSuccess(stackInHand, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof net.minecraft.server.level.ServerPlayer) {
            int mode = stack.hasTag() && stack.getTag().contains("JammerMode") ? stack.getTag().getInt("JammerMode") : 0;
            if (mode > 0) {
                boolean isValid = false;
                if (stack.hasTag() && stack.getTag().hasUUID("active_id") && entity.getPersistentData().hasUUID("bodycam_active_jammer_id")) {
                    java.util.UUID itemUUID = stack.getTag().getUUID("active_id");
                    java.util.UUID playerUUID = entity.getPersistentData().getUUID("bodycam_active_jammer_id");
                    long lastHeartbeat = entity.getPersistentData().getLong("bodycam_jammer_heartbeat");
                    boolean isCreative = ((net.minecraft.server.level.ServerPlayer)entity).isCreative();
                    isValid = itemUUID.equals(playerUUID) && (isCreative || (level.getGameTime() - lastHeartbeat <= 10));
                }

                if (isValid) {
                    entity.getPersistentData().putLong("bodycam_jammer_heartbeat", level.getGameTime());
                    entity.getPersistentData().putInt("bodycam_jammer_mode", mode);
                } else {
                    stack.getOrCreateTag().putInt("JammerMode", 0);
                    stack.getOrCreateTag().remove("active_id");
                }
            }
        }
    }
}
