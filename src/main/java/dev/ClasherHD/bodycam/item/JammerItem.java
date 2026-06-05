package dev.ClasherHD.bodycam.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class JammerItem extends Item {
    public JammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stackInHand = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.bodycam.op_only").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stackInHand);
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_JAMMER.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.bodycam.jammer_disabled").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stackInHand);
            }
            int currentMode = stackInHand.getOrCreateTag().getInt("JammerMode");
            int nextMode = (currentMode + 1) % 3;

            java.util.UUID activeId = null;
            if (nextMode > 0) {
                activeId = java.util.UUID.randomUUID();
                player.getPersistentData().putUUID("bodycam_active_jammer_id", activeId);
                player.getPersistentData().putLong("bodycam_jammer_heartbeat", level.getGameTime());
                player.getPersistentData().putInt("bodycam_jammer_mode", nextMode);
            } else {
                player.getPersistentData().remove("bodycam_active_jammer_id");
                player.getPersistentData().remove("bodycam_jammer_mode");
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
            boolean hasActivePlayerJammer = false;
            java.util.UUID playerActiveId = null;
            int playerActiveMode = 0;
            if (entity.getPersistentData().hasUUID("bodycam_active_jammer_id")) {
                long lastHeartbeat = entity.getPersistentData().getLong("bodycam_jammer_heartbeat");
                boolean isCreative = ((net.minecraft.server.level.ServerPlayer)entity).isCreative();
                if (isCreative || (level.getGameTime() - lastHeartbeat <= 10)) {
                    hasActivePlayerJammer = true;
                    playerActiveId = entity.getPersistentData().getUUID("bodycam_active_jammer_id");
                    playerActiveMode = entity.getPersistentData().getInt("bodycam_jammer_mode");
                }
            }

            int itemMode = stack.hasTag() && stack.getTag().contains("JammerMode") ? stack.getTag().getInt("JammerMode") : 0;
            java.util.UUID itemActiveId = stack.hasTag() && stack.getTag().hasUUID("active_id") ? stack.getTag().getUUID("active_id") : null;

            if (hasActivePlayerJammer) {
                if (!playerActiveId.equals(itemActiveId)) {
                    stack.getOrCreateTag().putInt("JammerMode", playerActiveMode);
                    stack.getOrCreateTag().putUUID("active_id", playerActiveId);
                } else {
                    entity.getPersistentData().putInt("bodycam_jammer_mode", itemMode);
                }
                entity.getPersistentData().putLong("bodycam_jammer_heartbeat", level.getGameTime());
            } else {
                if (itemMode > 0) {
                    stack.getOrCreateTag().putInt("JammerMode", 0);
                    stack.getOrCreateTag().remove("active_id");
                }
            }
        }
    }
}
