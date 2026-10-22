package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.compat.curios.CuriosCompat;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.UUID;

@SuppressWarnings("null")
public class JammerItem extends Item {
    public JammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stackInHand = player.getItemInHand(hand);
        if (ItemDurabilityHelper.isEmpty(stackInHand)) {
            if (!level.isClientSide() && stackInHand.hasTag() && stackInHand.getTag().getInt("JammerMode") != 0) {
                stackInHand.getTag().putInt("JammerMode", 0);
                stackInHand.getTag().remove("active_id");
            }
            return InteractionResultHolder.fail(stackInHand);
        }
        if (!level.isClientSide()) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stackInHand);
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_JAMMER.get()) {
                player.sendSystemMessage(Component.translatable("message.bodycam.jammer_disabled").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stackInHand);
            }
            int currentMode = stackInHand.getOrCreateTag().getInt("JammerMode");
            int nextMode = (currentMode + 1) % 3;

            UUID activeId = null;
            if (nextMode > 0) {
                activeId = UUID.randomUUID();
                player.getPersistentData().putUUID("bodycam_active_jammer_id", activeId);
                player.getPersistentData().putLong("bodycam_jammer_heartbeat", level.getGameTime());
                player.getPersistentData().putInt("bodycam_jammer_mode", nextMode);
            } else {
                player.getPersistentData().remove("bodycam_active_jammer_id");
                player.getPersistentData().remove("bodycam_jammer_mode");
            }

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof JammerItem) {
                    if (ItemDurabilityHelper.isEmpty(stack)) {
                        stack.getOrCreateTag().putInt("JammerMode", 0);
                        stack.getOrCreateTag().remove("active_id");
                    } else {
                        stack.getOrCreateTag().putInt("JammerMode", nextMode);
                        if (activeId != null) {
                            stack.getOrCreateTag().putUUID("active_id", activeId);
                        } else {
                            stack.getOrCreateTag().remove("active_id");
                        }
                    }
                }
            }

            if (nextMode == 1) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        dev.ClasherHD.bodycam.registry.ModSounds.JAMMER_USED.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.0625F, 0.5F);
            } else if (nextMode == 2) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        dev.ClasherHD.bodycam.registry.ModSounds.JAMMER_USED.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.0625F, 0.75F);
            }

            String key = nextMode == 0 ? "message.bodycam.jammer.off" : (nextMode == 1 ? "message.bodycam.jammer.on" : "message.bodycam.jammer.limited");
            ChatFormatting color = nextMode == 0 ? ChatFormatting.RED : (nextMode == 1 ? ChatFormatting.GREEN : ChatFormatting.BLUE);
            player.displayClientMessage(Component.translatable(key).withStyle(color), true);
        }
        return InteractionResultHolder.sidedSuccess(stackInHand, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (ItemDurabilityHelper.isEmpty(stack)) {
                if (stack.hasTag() && stack.getTag().getInt("JammerMode") > 0) {
                    stack.getTag().putInt("JammerMode", 0);
                    stack.getTag().remove("active_id");
                }
                return;
            }

            boolean hasActivePlayerJammer = false;
            UUID playerActiveId = null;
            int playerActiveMode = 0;
            if (entity.getPersistentData().hasUUID("bodycam_active_jammer_id")) {
                long lastHeartbeat = entity.getPersistentData().getLong("bodycam_jammer_heartbeat");
                boolean isCreative = player.isCreative();
                if (isCreative || (level.getGameTime() - lastHeartbeat <= 10)) {
                    hasActivePlayerJammer = true;
                    playerActiveId = entity.getPersistentData().getUUID("bodycam_active_jammer_id");
                    playerActiveMode = entity.getPersistentData().getInt("bodycam_jammer_mode");
                }
            }

            int itemMode = stack.hasTag() && stack.getTag().contains("JammerMode") ? stack.getTag().getInt("JammerMode") : 0;
            UUID itemActiveId = stack.hasTag() && stack.getTag().hasUUID("active_id") ? stack.getTag().getUUID("active_id") : null;

            if (!hasActivePlayerJammer && player.isCreative() && itemMode > 0) {
                hasActivePlayerJammer = true;
                playerActiveId = itemActiveId != null ? itemActiveId : UUID.randomUUID();
                playerActiveMode = itemMode;
                entity.getPersistentData().putUUID("bodycam_active_jammer_id", playerActiveId);
                entity.getPersistentData().putInt("bodycam_jammer_mode", playerActiveMode);
            }

            if (hasActivePlayerJammer) {
                if (!playerActiveId.equals(itemActiveId)) {
                    stack.getOrCreateTag().putInt("JammerMode", playerActiveMode);
                    stack.getOrCreateTag().putUUID("active_id", playerActiveId);
                    itemMode = playerActiveMode;
                } else {
                    entity.getPersistentData().putInt("bodycam_jammer_mode", itemMode);
                    playerActiveMode = itemMode;
                }

                if (!player.isCreative() && playerActiveMode > 0) {
                    int interval = playerActiveMode == 2 ? 40 : 20;
                    long gameTime = level.getGameTime();
                    if (gameTime % interval == 0 && entity.getPersistentData().getLong("bodycam_jammer_last_durability_tick") != gameTime) {
                        entity.getPersistentData().putLong("bodycam_jammer_last_durability_tick", gameTime);
                        ItemDurabilityHelper.damagePerTime(stack, player, 1);
                        if (ItemDurabilityHelper.isEmpty(stack)) {
                            stack.getOrCreateTag().putInt("JammerMode", 0);
                            stack.getOrCreateTag().remove("active_id");
                            if (!hasAnyChargedActiveJammer(player)) {
                                entity.getPersistentData().remove("bodycam_active_jammer_id");
                                entity.getPersistentData().remove("bodycam_jammer_mode");
                                player.displayClientMessage(Component.translatable("message.bodycam.jammer.off").withStyle(ChatFormatting.RED), true);
                            }
                            return;
                        }
                    }
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

    private static boolean hasAnyChargedActiveJammer(ServerPlayer player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.getItem() instanceof JammerItem && !ItemDurabilityHelper.isEmpty(carried)) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (inv.getItem() instanceof JammerItem && !ItemDurabilityHelper.isEmpty(inv)) {
                return true;
            }
        }
        for (ItemStack curio : CuriosCompat.getEquippedItems(player, ModItems.JAMMER.get())) {
            if (!ItemDurabilityHelper.isEmpty(curio)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged() && !ItemDurabilityHelper.isEmpty(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return ItemDurabilityHelper.canApplyEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return ItemDurabilityHelper.isBookEnchantable(stack, book);
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }
}
