package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
            boolean active = stack.getOrDefault(ModDataComponents.ANONYMIZER_ACTIVE.get(), false);
            boolean nextActive = !active;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.getItem() instanceof AnonymizerItem) {
                    invStack.set(ModDataComponents.ANONYMIZER_ACTIVE.get(), nextActive);
                    invStack.set(ModDataComponents.IS_MASTER.get(), invStack == stack);
                }
            }
            
            Component msg = Component.translatable(nextActive ? "message.bodycam.anonymizer_on" : "message.bodycam.anonymizer_off")
                    .withStyle(nextActive ? ChatFormatting.GREEN : ChatFormatting.RED);
            player.displayClientMessage(msg, true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player) {
            ItemStack masterStack = null;
            ItemStack firstFound = null;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.getItem() instanceof AnonymizerItem) {
                    if (firstFound == null) firstFound = invStack;
                    if (invStack.getOrDefault(ModDataComponents.IS_MASTER.get(), false)) {
                        masterStack = invStack;
                        break;
                    }
                }
            }

            if (masterStack == null && firstFound != null) {
                masterStack = firstFound;
                masterStack.set(ModDataComponents.IS_MASTER.get(), true);
            }

            if (masterStack != null && stack != masterStack) {
                boolean masterActive = masterStack.getOrDefault(ModDataComponents.ANONYMIZER_ACTIVE.get(), false);
                stack.set(ModDataComponents.ANONYMIZER_ACTIVE.get(), masterActive);
                stack.set(ModDataComponents.IS_MASTER.get(), false);
            }
        }
    }
}