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
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("This feature is restricted to Server Operators.")
                                .withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_ANONYMIZER.get()) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("The Anonymizer is disabled on this server.")
                                .withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
        }

        boolean currentState = stack.hasTag() && stack.getTag().getBoolean("AnonymizerActive");
        boolean newState = !currentState;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.getItem() instanceof AnonymizerItem) {
                invStack.getOrCreateTag().putBoolean("AnonymizerActive", newState);
                invStack.getOrCreateTag().putBoolean("IsMaster", invStack == stack);
            }
        }

        if (!level.isClientSide()) {
            if (newState) {
                player.displayClientMessage(
                        Component.translatable("message.bodycam.anonymizer_on").withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("message.bodycam.anonymizer_off").withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId,
            boolean isSelected) {
        if (entity instanceof Player player) {
            ItemStack masterStack = null;
            ItemStack firstFound = null;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.getItem() instanceof AnonymizerItem) {
                    if (firstFound == null)
                        firstFound = invStack;
                    if (invStack.hasTag() && invStack.getTag().getBoolean("IsMaster")) {
                        masterStack = invStack;
                        break;
                    }
                }
            }

            if (masterStack == null && firstFound != null) {
                masterStack = firstFound;
                masterStack.getOrCreateTag().putBoolean("IsMaster", true);
            }

            if (masterStack != null && stack != masterStack) {
                boolean masterState = masterStack.hasTag() && masterStack.getTag().getBoolean("AnonymizerActive");
                stack.getOrCreateTag().putBoolean("AnonymizerActive", masterState);
                stack.getOrCreateTag().putBoolean("IsMaster", false);
            }

            if (!level.isClientSide() && stack == masterStack) {
                if (stack.hasTag() && stack.getTag().getBoolean("AnonymizerActive")) {
                    dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player)
                            .putLong("bodycam_anonymizer_heartbeat", level.getGameTime());
                }
            }
        }
    }
}