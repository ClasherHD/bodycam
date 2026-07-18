package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.component.ModDataComponents;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.util.BodycamHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class JammerItem extends Item {
    public JammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stackInHand = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (ModServerConfig.OP_ONLY_MODE.get() && !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
                player.displayClientMessage(Component.literal("This feature is restricted to Server Operators.").withStyle(ChatFormatting.RED), false);
                return InteractionResult.FAIL;
            }
            if (!ModServerConfig.ENABLE_JAMMER.get()) {
                player.displayClientMessage(Component.literal("The Jammer is disabled on this server.").withStyle(ChatFormatting.RED), false);
                return InteractionResult.FAIL;
            }

            int currentMode = stackInHand.getOrDefault(ModDataComponents.JAMMER_MODE.get(), 0);
            int nextMode = (currentMode + 1) % 3;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.getItem() instanceof JammerItem) {
                    invStack.set(ModDataComponents.JAMMER_MODE.get(), nextMode);
                    invStack.set(ModDataComponents.IS_MASTER.get(), invStack == stackInHand);
                }
            }

            String key = nextMode == 0 ? "message.bodycam.jammer.off"
                    : (nextMode == 1 ? "message.bodycam.jammer.on" : "message.bodycam.jammer.limited");
            ChatFormatting color = nextMode == 0 ? ChatFormatting.RED
                    : (nextMode == 1 ? ChatFormatting.GREEN : ChatFormatting.BLUE);
            player.displayClientMessage(Component.translatable(key).withStyle(color), true);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        if (entity instanceof Player player) {
            ItemStack masterStack = null;
            ItemStack firstFound = null;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.getItem() instanceof JammerItem) {
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
                int masterMode = masterStack.getOrDefault(ModDataComponents.JAMMER_MODE.get(), 0);
                stack.set(ModDataComponents.JAMMER_MODE.get(), masterMode);
                stack.set(ModDataComponents.IS_MASTER.get(), false);
            }

            if (masterStack != null) {
                long currentTime = level.getGameTime();
                if (BodycamHelper.getPersistentData(player).getLongOr("bodycam_jammer_last_tick", 0L) != currentTime) {
                    int mode = masterStack.getOrDefault(ModDataComponents.JAMMER_MODE.get(), 0);
                    if (mode > 0) {
                        BodycamHelper.getPersistentData(player).putLong("bodycam_jammer_heartbeat", currentTime);
                        BodycamHelper.getPersistentData(player).putInt("bodycam_jammer_mode", mode);
                    }
                    BodycamHelper.getPersistentData(player).putLong("bodycam_jammer_last_tick", currentTime);
                }
            }
        }
    }
}