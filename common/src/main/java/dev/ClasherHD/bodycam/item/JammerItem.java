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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class JammerItem extends Item {

    public JammerItem(Properties properties) {
        super(properties);
    }

    private static void syncConfiguredMaxDamage(ItemStack stack) {
        if (!ModServerConfig.isLoaded() || stack == null || stack.isEmpty() || !stack.has(DataComponents.MAX_DAMAGE)) {
            return;
        }
        int configured = ItemDurabilityHelper.getJammerMaxDurability();
        int currentMax = stack.getOrDefault(DataComponents.MAX_DAMAGE, configured);
        if (configured > 0 && currentMax != configured) {
            boolean wasEmpty = stack.getDamageValue() >= currentMax;
            stack.set(DataComponents.MAX_DAMAGE, configured);
            if (wasEmpty || stack.getDamageValue() > configured) {
                stack.setDamageValue(configured);
            }
        }
    }

    public static int getMode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return ItemNbtAccess.getInt(stack, "JammerMode");
    }

    public static void setMode(ItemStack stack, int mode) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ItemNbtAccess.putInt(stack, "JammerMode", mode);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stackInHand = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            syncConfiguredMaxDamage(stackInHand);
        }
        if (ItemDurabilityHelper.isEmpty(stackInHand)) {
            if (!level.isClientSide() && getMode(stackInHand) != 0) {
                ItemNbtAccess.updateTag(stackInHand, tag -> {
                    tag.putInt("JammerMode", 0);
                    tag.remove("active_id");
                });
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (ModServerConfig.OP_ONLY_MODE.get() && !sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                sp.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (ModServerConfig.isDimensionBlacklisted(level)) {
                sp.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (!ModServerConfig.ENABLE_JAMMER.get()) {
                sp.sendSystemMessage(Component.translatable("message.bodycam.jammer_disabled").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            int currentMode = getMode(stackInHand);
            int nextMode = (currentMode + 1) % 3;

            CompoundTag pData = PersistentDataAccess.get(sp);
            UUID activeId = null;
            if (nextMode > 0) {
                activeId = UUID.randomUUID();
                PersistentDataAccess.putUUID(pData, "bodycam_active_jammer_id", activeId);
                pData.putLong("bodycam_jammer_heartbeat", level.getGameTime());
                pData.putInt("bodycam_jammer_mode", nextMode);
            } else {
                pData.remove("bodycam_active_jammer_id");
                pData.remove("bodycam_jammer_mode");
            }

            final UUID finalActiveId = activeId;
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack stack = sp.getInventory().getItem(i);
                if (stack.getItem() instanceof JammerItem) {
                    if (ItemDurabilityHelper.isEmpty(stack)) {
                        ItemNbtAccess.updateTag(stack, tag -> {
                            tag.putInt("JammerMode", 0);
                            tag.remove("active_id");
                        });
                    } else {
                        ItemNbtAccess.updateTag(stack, tag -> {
                            tag.putInt("JammerMode", nextMode);
                            if (finalActiveId != null) {
                                PersistentDataAccess.putUUID(tag, "active_id", finalActiveId);
                            } else {
                                tag.remove("active_id");
                            }
                        });
                    }
                }
            }

            if (nextMode == 1) {
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        ModSounds.JAMMER_USED.get(), SoundSource.PLAYERS, 0.0625F, 0.5F);
            } else if (nextMode == 2) {
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        ModSounds.JAMMER_USED.get(), SoundSource.PLAYERS, 0.0625F, 0.75F);
            }

            String key = nextMode == 0 ? "message.bodycam.jammer.off" : (nextMode == 1 ? "message.bodycam.jammer.on" : "message.bodycam.jammer.limited");
            ChatFormatting color = nextMode == 0 ? ChatFormatting.RED : (nextMode == 1 ? ChatFormatting.GREEN : ChatFormatting.BLUE);
            sp.sendOverlayMessage(Component.translatable(key).withStyle(color));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity instanceof ServerPlayer player) {
            syncConfiguredMaxDamage(stack);
            if (ItemDurabilityHelper.isEmpty(stack)) {
                if (getMode(stack) > 0) {
                    ItemNbtAccess.updateTag(stack, tag -> {
                        tag.putInt("JammerMode", 0);
                        tag.remove("active_id");
                    });
                }
                return;
            }

            CompoundTag pData = PersistentDataAccess.get(player);
            boolean hasActivePlayerJammer = false;
            UUID playerActiveId = null;
            int playerActiveMode = 0;
            if (PersistentDataAccess.hasUUID(pData, "bodycam_active_jammer_id")) {
                long lastHeartbeat = pData.getLongOr("bodycam_jammer_heartbeat", 0L);
                boolean isCreative = player.isCreative();
                if (isCreative || (level.getGameTime() - lastHeartbeat <= 10)) {
                    hasActivePlayerJammer = true;
                    playerActiveId = PersistentDataAccess.getUUID(pData, "bodycam_active_jammer_id");
                    playerActiveMode = pData.getIntOr("bodycam_jammer_mode", 0);
                }
            }

            int itemMode = getMode(stack);
            UUID itemActiveId = ItemNbtAccess.getUUID(stack, "active_id");

            if (hasActivePlayerJammer) {
                final UUID finalPlayerActiveId = playerActiveId;
                final int finalPlayerActiveMode = playerActiveMode;
                if (!playerActiveId.equals(itemActiveId)) {
                    ItemNbtAccess.updateTag(stack, tag -> {
                        tag.putInt("JammerMode", finalPlayerActiveMode);
                        PersistentDataAccess.putUUID(tag, "active_id", finalPlayerActiveId);
                    });
                    itemMode = playerActiveMode;
                } else {
                    pData.putInt("bodycam_jammer_mode", itemMode);
                    playerActiveMode = itemMode;
                }

                if (!player.isCreative() && playerActiveMode > 0) {
                    int interval = playerActiveMode == 2
                            ? Math.max(1, ModServerConfig.JAMMER_LIMITED_INTERVAL_TICKS.get())
                            : Math.max(1, ModServerConfig.JAMMER_NORMAL_INTERVAL_TICKS.get());
                    long gameTime = level.getGameTime();
                    if (gameTime % interval == 0 && pData.getLongOr("bodycam_jammer_last_durability_tick", 0L) != gameTime) {
                        pData.putLong("bodycam_jammer_last_durability_tick", gameTime);
                        ItemDurabilityHelper.damagePerTime(stack, player, 1);
                        if (ItemDurabilityHelper.isEmpty(stack)) {
                            ItemNbtAccess.updateTag(stack, tag -> {
                                tag.putInt("JammerMode", 0);
                                tag.remove("active_id");
                            });
                            if (!hasAnyChargedActiveJammer(player)) {
                                pData.remove("bodycam_active_jammer_id");
                                pData.remove("bodycam_jammer_mode");
                                player.sendOverlayMessage(Component.translatable("message.bodycam.jammer.off").withStyle(ChatFormatting.RED));
                            }
                            return;
                        }
                    }
                }

                pData.putLong("bodycam_jammer_heartbeat", level.getGameTime());
            } else {
                if (itemMode > 0) {
                    ItemNbtAccess.updateTag(stack, tag -> {
                        tag.putInt("JammerMode", 0);
                        tag.remove("active_id");
                    });
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
        for (ItemStack acc : BodycamPlatform.getEquippedAccessoryItems(player, ModItems.JAMMER.get())) {
            if (!ItemDurabilityHelper.isEmpty(acc)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged() && !ItemDurabilityHelper.isEmpty(stack);
    }
}
