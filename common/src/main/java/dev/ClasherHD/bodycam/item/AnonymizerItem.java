package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class AnonymizerItem extends Item {

    public AnonymizerItem(Properties properties) {
        super(properties);
    }

    private static void syncConfiguredMaxDamage(ItemStack stack) {
        if (!ModServerConfig.isLoaded() || stack == null || stack.isEmpty() || !stack.has(DataComponents.MAX_DAMAGE)) {
            return;
        }
        int configured = ItemDurabilityHelper.getAnonymizerMaxDurability();
        int currentMax = stack.getOrDefault(DataComponents.MAX_DAMAGE, configured);
        if (configured > 0 && currentMax != configured) {
            boolean wasEmpty = stack.getDamageValue() >= currentMax;
            stack.set(DataComponents.MAX_DAMAGE, configured);
            if (wasEmpty || stack.getDamageValue() > configured) {
                stack.setDamageValue(configured);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            syncConfiguredMaxDamage(stack);
        }
        if (ItemDurabilityHelper.isEmpty(stack)) {
            if (!level.isClientSide() && ItemNbtAccess.getBoolean(stack, "AnonymizerActive")) {
                ItemNbtAccess.updateTag(stack, tag -> {
                    tag.putBoolean("AnonymizerActive", false);
                    tag.remove("active_id");
                });
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (ModServerConfig.OP_ONLY_MODE.get() && !sp.hasPermissions(2)) {
                sp.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (ModServerConfig.isDimensionBlacklisted(level)) {
                sp.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (!ModServerConfig.ENABLE_ANONYMIZER.get()) {
                sp.sendSystemMessage(Component.translatable("message.bodycam.anonymizer_disabled").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }

            boolean currentState = ItemNbtAccess.getBoolean(stack, "AnonymizerActive");
            boolean newState = !currentState;

            CompoundTag pData = PersistentDataAccess.get(sp);
            UUID activeId = null;
            if (newState) {
                activeId = UUID.randomUUID();
                pData.putUUID("bodycam_active_anonymizer_id", activeId);
                pData.putLong("bodycam_anonymizer_heartbeat", level.getGameTime());
            } else {
                pData.remove("bodycam_active_anonymizer_id");
            }

            final UUID finalActiveId = activeId;
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack invStack = sp.getInventory().getItem(i);
                if (invStack.getItem() instanceof AnonymizerItem) {
                    if (ItemDurabilityHelper.isEmpty(invStack)) {
                        ItemNbtAccess.updateTag(invStack, tag -> {
                            tag.putBoolean("AnonymizerActive", false);
                            tag.remove("active_id");
                        });
                    } else {
                        ItemNbtAccess.updateTag(invStack, tag -> {
                            tag.putBoolean("AnonymizerActive", newState);
                            if (finalActiveId != null) {
                                tag.putUUID("active_id", finalActiveId);
                            } else {
                                tag.remove("active_id");
                            }
                        });
                    }
                }
            }

            if (newState) {
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        ModSounds.ANONYMIZER_USED2.get(), SoundSource.PLAYERS, 0.25F, 0.25F);
                sp.displayClientMessage(Component.translatable("message.bodycam.anonymizer_on").withStyle(ChatFormatting.GREEN), true);
            } else {
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        ModSounds.ANONYMIZER_USED1.get(), SoundSource.PLAYERS, 0.25F, 0.75F);
                sp.displayClientMessage(Component.translatable("message.bodycam.anonymizer_off").withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            syncConfiguredMaxDamage(stack);
            if (ItemDurabilityHelper.isEmpty(stack)) {
                if (ItemNbtAccess.getBoolean(stack, "AnonymizerActive")) {
                    ItemNbtAccess.updateTag(stack, tag -> {
                        tag.putBoolean("AnonymizerActive", false);
                        tag.remove("active_id");
                    });
                }
                return;
            }

            CompoundTag pData = PersistentDataAccess.get(player);
            boolean hasActivePlayerAnonymizer = false;
            UUID playerActiveId = null;
            if (pData.hasUUID("bodycam_active_anonymizer_id")) {
                long lastHeartbeat = pData.getLong("bodycam_anonymizer_heartbeat");
                boolean isCreative = player.isCreative();
                if (isCreative || (level.getGameTime() - lastHeartbeat <= 10)) {
                    hasActivePlayerAnonymizer = true;
                    playerActiveId = pData.getUUID("bodycam_active_anonymizer_id");
                }
            }

            boolean itemActive = ItemNbtAccess.getBoolean(stack, "AnonymizerActive");
            UUID itemActiveId = ItemNbtAccess.getUUID(stack, "active_id");

            if (hasActivePlayerAnonymizer) {
                final UUID finalPlayerActiveId = playerActiveId;
                if (!playerActiveId.equals(itemActiveId)) {
                    ItemNbtAccess.updateTag(stack, tag -> {
                        tag.putBoolean("AnonymizerActive", true);
                        tag.putUUID("active_id", finalPlayerActiveId);
                    });
                }

                long gameTime = level.getGameTime();
                int interval = Math.max(1, ModServerConfig.ANONYMIZER_INTERVAL_TICKS.get());
                if (!player.isCreative() && gameTime % interval == 0
                        && pData.getLong("bodycam_anonymizer_last_durability_tick") != gameTime) {
                    pData.putLong("bodycam_anonymizer_last_durability_tick", gameTime);
                    ItemDurabilityHelper.damagePerTime(stack, player, 1);
                    if (ItemDurabilityHelper.isEmpty(stack)) {
                        ItemNbtAccess.updateTag(stack, tag -> {
                            tag.putBoolean("AnonymizerActive", false);
                            tag.remove("active_id");
                        });
                        if (!hasAnyChargedActiveAnonymizer(player)) {
                            pData.remove("bodycam_active_anonymizer_id");
                            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    ModSounds.ANONYMIZER_USED1.get(), SoundSource.PLAYERS, 0.25F, 0.75F);
                            player.displayClientMessage(Component.translatable("message.bodycam.anonymizer_off").withStyle(ChatFormatting.RED), true);
                        }
                        return;
                    }
                }

                pData.putLong("bodycam_anonymizer_heartbeat", level.getGameTime());
            } else {
                if (itemActive) {
                    ItemNbtAccess.updateTag(stack, tag -> {
                        tag.putBoolean("AnonymizerActive", false);
                        tag.remove("active_id");
                    });
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
        for (ItemStack acc : BodycamPlatform.getEquippedAccessoryItems(player, ModItems.ANONYMIZER.get())) {
            if (!ItemDurabilityHelper.isEmpty(acc)) {
                return true;
            }
        }
        return false;
    }
}
