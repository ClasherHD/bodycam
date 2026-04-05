package dev.ClasherHD.bodycam.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

public class AnonymizerItem extends Item {

    public AnonymizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This feature is restricted to Server Operators.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_ANONYMIZER.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The Anonymizer is disabled on this server.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            boolean currentState = false;
            if (stack.hasTag() && stack.getTag().contains("AnonymizerActive")) {
                currentState = stack.getTag().getBoolean("AnonymizerActive");
            }
            
            boolean newState = !currentState;
            
            java.util.UUID activeId = null;
            if (newState) {
                activeId = java.util.UUID.randomUUID();
                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putUUID("bodycam_active_anonymizer_id", activeId);
                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putLong("bodycam_anonymizer_heartbeat", level.getGameTime());
            }
            
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.getItem() instanceof AnonymizerItem) {
                    invStack.getOrCreateTag().putBoolean("AnonymizerActive", newState);
                    if (activeId != null) invStack.getOrCreateTag().putUUID("active_id", activeId);
                    else invStack.getOrCreateTag().remove("active_id");
                }
            }

            if (newState) {
                player.displayClientMessage(Component.translatable("message.bodycam.anonymizer_on").withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(Component.translatable("message.bodycam.anonymizer_off").withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof net.minecraft.server.level.ServerPlayer) {
            boolean active = stack.hasTag() && stack.getTag().contains("AnonymizerActive") && stack.getTag().getBoolean("AnonymizerActive");
            if (active) {
                boolean isValid = false;
                if (stack.hasTag() && stack.getTag().hasUUID("active_id") && dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(entity).hasUUID("bodycam_active_anonymizer_id")) {
                    java.util.UUID itemUUID = stack.getTag().getUUID("active_id");
                    java.util.UUID playerUUID = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(entity).getUUID("bodycam_active_anonymizer_id");
                    long lastHeartbeat = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(entity).getLong("bodycam_anonymizer_heartbeat");
                    boolean isCreative = ((net.minecraft.server.level.ServerPlayer)entity).isCreative();
                    isValid = itemUUID.equals(playerUUID) && (isCreative || (level.getGameTime() - lastHeartbeat <= 10));
                }
                
                if (isValid) {
                    dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(entity).putLong("bodycam_anonymizer_heartbeat", level.getGameTime());
                } else {
                    stack.getOrCreateTag().putBoolean("AnonymizerActive", false);
                    stack.getOrCreateTag().remove("active_id");
                }
            }
        }
    }
}
