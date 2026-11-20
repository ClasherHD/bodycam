package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.ModAdvancements;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class LoggerItem extends Item {

    public static final String TAG_ACTIVE = "LoggerActive";
    public static final String TAG_ENTRIES = "LoggerEntries";
    public static final int MAX_ENTRIES = 5;
    public static final double FAR_DISTANCE = 500.0D;

    public LoggerItem(Properties properties) {
        super(properties);
    }

    public static boolean isActive(ItemStack stack) {
        return ItemNbtAccess.getBoolean(stack, TAG_ACTIVE);
    }

    private static void resetSession(ItemStack stack) {
        ItemNbtAccess.updateTag(stack, tag -> {
            tag.putBoolean(TAG_ACTIVE, false);
            tag.remove(TAG_ENTRIES);
        });
    }

    private static void syncConfiguredMaxDamage(ItemStack stack) {
        if (!ModServerConfig.isLoaded()) {
            return;
        }
        int configured = Math.max(1, ItemDurabilityHelper.getLoggerMaxDurability());
        Integer current = stack.get(DataComponents.MAX_DAMAGE);
        if (current == null || current != configured) {
            stack.set(DataComponents.MAX_DAMAGE, configured);
            if (stack.getDamageValue() > configured) {
                stack.setDamageValue(configured);
            }
        }
    }

    public static ItemStack refuel(ItemStack stack, int paperCount) {
        syncConfiguredMaxDamage(stack);
        if (paperCount <= 0) {
            return stack;
        }
        int max = Math.max(1, stack.getMaxDamage());
        int currentDamage = Math.min(stack.getDamageValue(), max);
        int perPaper = Math.max(1, max / 2);
        int newDamage = Math.max(0, currentDamage - (perPaper * paperCount));
        stack.setDamageValue(newDamage);
        return stack;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        if (!player.level().isClientSide()) {
            syncConfiguredMaxDamage(stack);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        syncConfiguredMaxDamage(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            if (ItemDurabilityHelper.isEmpty(stack) && !isActive(stack) && !player.isShiftKeyDown()) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sPlayer)) {
            return InteractionResult.PASS;
        }
        syncConfiguredMaxDamage(stack);

        if (ModServerConfig.OP_ONLY_MODE.get() && !sPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (ModServerConfig.isDimensionBlacklisted(level)) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!ModServerConfig.ENABLE_LOGGER.get()) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.logger_disabled").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            if (isActive(stack)) {
                resetSession(stack);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOGGER_CANCEL.get(), SoundSource.PLAYERS, 0.6F, 0.6F);
                sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.cancelled").withStyle(ChatFormatting.RED), true);
            } else {
                sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.not_active").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (!isActive(stack)) {
            if (ItemDurabilityHelper.isEmpty(stack)) {
                sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.empty").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            ItemNbtAccess.updateTag(stack, tag -> {
                tag.putBoolean(TAG_ACTIVE, true);
                tag.put(TAG_ENTRIES, new ListTag());
            });
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOGGER_START.get(), SoundSource.PLAYERS, 0.6F, 1.2F);
            sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.started").withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        }

        CompoundTag stackTag = ItemNbtAccess.getReadOnlyTag(stack);
        ListTag entries = stackTag != null ? stackTag.getListOrEmpty(TAG_ENTRIES).copy() : new ListTag();

        ItemStack paper = new ItemStack(ModItems.LOG_PAPER.get());
        ItemNbtAccess.updateTag(paper, paperTag -> {
            paperTag.putString(LogPaperItem.TAG_AUTHOR, sPlayer.getName().getString());
            PersistentDataAccess.putUUID(paperTag, LogPaperItem.TAG_AUTHOR_UUID, sPlayer.getUUID());
            paperTag.put(LogPaperItem.TAG_ENTRIES, entries);
        });
        int recorded = entries.size();

        resetSession(stack);
        ItemDurabilityHelper.damagePerUse(stack, sPlayer, 1);

        if (!sPlayer.getInventory().add(paper)) {
            sPlayer.drop(paper, false);
        }
        if (recorded > 0) {
            ModAdvancements.award(sPlayer, ModAdvancements.CAUGHT_ON_LOGGER);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOGGER_STOP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.stopped", recorded).withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.SUCCESS;
    }

    public static void recordObservation(ServerPlayer target, ServerPlayer observer, String observerDimension, Vec3 observerPosition) {
        List<ItemStack> candidates = new ArrayList<>();
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            ItemStack stack = target.getInventory().getItem(i);
            if (stack.getItem() instanceof LoggerItem) {
                candidates.add(stack);
            }
        }
        candidates.addAll(BodycamPlatform.getEquippedAccessoryItems(target, ModItems.LOGGER.get()));

        boolean sameDim = observerDimension.equals(target.level().dimension().identifier().toString());
        boolean far = !sameDim || observerPosition.distanceTo(target.position()) > FAR_DISTANCE;
        long now = System.currentTimeMillis();

        for (ItemStack stack : candidates) {
            if (!isActive(stack)) {
                continue;
            }
            ItemNbtAccess.updateTag(stack, tag -> {
                ListTag entries = tag.getListOrEmpty(TAG_ENTRIES);
                CompoundTag entry = new CompoundTag();
                entry.putString(LogPaperItem.ENTRY_NAME, observer.getName().getString());
                PersistentDataAccess.putUUID(entry, LogPaperItem.ENTRY_UUID, observer.getUUID());
                entry.putLong(LogPaperItem.ENTRY_TIME, now);
                entry.putBoolean(LogPaperItem.ENTRY_FAR, far);
                entry.putBoolean(LogPaperItem.ENTRY_SAME_DIM, sameDim);
                entries.add(entry);
                while (entries.size() > MAX_ENTRIES) {
                    entries.remove(0);
                }
                tag.put(TAG_ENTRIES, entries);
            });
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged() && !ItemDurabilityHelper.isEmpty(stack);
    }
}
