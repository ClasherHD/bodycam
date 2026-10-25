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
public class AnonymizerItem extends Item {

    public AnonymizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ItemDurabilityHelper.isEmpty(stack)) {
            if (!level.isClientSide() && stack.hasTag() && stack.getTag().getBoolean("AnonymizerActive")) {
                stack.getTag().putBoolean("AnonymizerActive", false);
                stack.getTag().remove("active_id");
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (dev.ClasherHD.bodycam.config.ModServerConfig.isDimensionBlacklisted(level)) {
                player.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_ANONYMIZER.get()) {
                player.sendSystemMessage(Component.translatable("message.bodycam.anonymizer_disabled").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            boolean currentState = stack.hasTag() && stack.getTag().contains("AnonymizerActive")
                    && stack.getTag().getBoolean("AnonymizerActive");

            boolean newState = !currentState;

            UUID activeId = null;
            if (newState) {
                activeId = UUID.randomUUID();
                player.getPersistentData().putUUID("bodycam_active_anonymizer_id", activeId);
                player.getPersistentData().putLong("bodycam_anonymizer_heartbeat", level.getGameTime());
            } else {
                player.getPersistentData().remove("bodycam_active_anonymizer_id");
            }

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.getItem() instanceof AnonymizerItem) {
                    if (ItemDurabilityHelper.isEmpty(invStack)) {
                        invStack.getOrCreateTag().putBoolean("AnonymizerActive", false);
                        invStack.getOrCreateTag().remove("active_id");
                    } else {
                        invStack.getOrCreateTag().putBoolean("AnonymizerActive", newState);
                        if (activeId != null) {
                            invStack.getOrCreateTag().putUUID("active_id", activeId);
                        } else {
                            invStack.getOrCreateTag().remove("active_id");
                        }
                    }
                }
            }

            if (newState) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        dev.ClasherHD.bodycam.registry.ModSounds.ANONYMIZER_USED2.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.25F, 0.25F);
                player.displayClientMessage(Component.translatable("message.bodycam.anonymizer_on").withStyle(ChatFormatting.GREEN), true);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        dev.ClasherHD.bodycam.registry.ModSounds.ANONYMIZER_USED1.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.25F, 0.75F);
                player.displayClientMessage(Component.translatable("message.bodycam.anonymizer_off").withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ItemDurabilityHelper.getAnonymizerMaxDurability();
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

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (ItemDurabilityHelper.isEmpty(stack)) {
                if (stack.hasTag() && stack.getTag().getBoolean("AnonymizerActive")) {
                    stack.getTag().putBoolean("AnonymizerActive", false);
                    stack.getTag().remove("active_id");
                }
                return;
            }

            boolean hasActivePlayerAnonymizer = false;
            UUID playerActiveId = null;
            if (entity.getPersistentData().hasUUID("bodycam_active_anonymizer_id")) {
                long lastHeartbeat = entity.getPersistentData().getLong("bodycam_anonymizer_heartbeat");
                boolean isCreative = player.isCreative();
                if (isCreative || (level.getGameTime() - lastHeartbeat <= 10)) {
                    hasActivePlayerAnonymizer = true;
                    playerActiveId = entity.getPersistentData().getUUID("bodycam_active_anonymizer_id");
                }
            }

            boolean itemActive = stack.hasTag() && stack.getTag().contains("AnonymizerActive") && stack.getTag().getBoolean("AnonymizerActive");
            UUID itemActiveId = stack.hasTag() && stack.getTag().hasUUID("active_id") ? stack.getTag().getUUID("active_id") : null;

            if (!hasActivePlayerAnonymizer && player.isCreative() && itemActive) {
                hasActivePlayerAnonymizer = true;
                playerActiveId = itemActiveId != null ? itemActiveId : UUID.randomUUID();
                entity.getPersistentData().putUUID("bodycam_active_anonymizer_id", playerActiveId);
            }

            if (hasActivePlayerAnonymizer) {
                if (!playerActiveId.equals(itemActiveId)) {
                    stack.getOrCreateTag().putBoolean("AnonymizerActive", true);
                    stack.getOrCreateTag().putUUID("active_id", playerActiveId);
                }

                long gameTime = level.getGameTime();
                int interval = Math.max(1, dev.ClasherHD.bodycam.config.ModServerConfig.ANONYMIZER_INTERVAL_TICKS.get());
                if (!player.isCreative() && gameTime % interval == 0
                        && entity.getPersistentData().getLong("bodycam_anonymizer_last_durability_tick") != gameTime) {
                    entity.getPersistentData().putLong("bodycam_anonymizer_last_durability_tick", gameTime);
                    ItemDurabilityHelper.damagePerTime(stack, player, 1);
                    if (ItemDurabilityHelper.isEmpty(stack)) {
                        stack.getOrCreateTag().putBoolean("AnonymizerActive", false);
                        stack.getOrCreateTag().remove("active_id");
                        if (!hasAnyChargedActiveAnonymizer(player)) {
                            entity.getPersistentData().remove("bodycam_active_anonymizer_id");
                            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    dev.ClasherHD.bodycam.registry.ModSounds.ANONYMIZER_USED1.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.25F, 0.75F);
                            player.displayClientMessage(Component.translatable("message.bodycam.anonymizer_off").withStyle(ChatFormatting.RED), true);
                        }
                        return;
                    }
                }

                entity.getPersistentData().putLong("bodycam_anonymizer_heartbeat", level.getGameTime());
            } else {
                if (itemActive) {
                    stack.getOrCreateTag().putBoolean("AnonymizerActive", false);
                    stack.getOrCreateTag().remove("active_id");
                }
            }
        }
    }

    private static boolean hasAnyChargedActiveAnonymizer(ServerPlayer player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.getItem() instanceof AnonymizerItem && !ItemDurabilityHelper.isEmpty(carried)) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (inv.getItem() instanceof AnonymizerItem && !ItemDurabilityHelper.isEmpty(inv)) {
                return true;
            }
        }
        for (ItemStack curio : CuriosCompat.getEquippedItems(player, ModItems.ANONYMIZER.get())) {
            if (!ItemDurabilityHelper.isEmpty(curio)) {
                return true;
            }
        }
        return false;
    }
}
