package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.compat.curios.CuriosCompat;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
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
        return stack.hasTag() && stack.getTag().getBoolean(TAG_ACTIVE);
    }

    private static void resetSession(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_ACTIVE, false);
        tag.remove(TAG_ENTRIES);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            if (ItemDurabilityHelper.isEmpty(stack) && !isActive(stack) && !player.isShiftKeyDown()) {
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer sPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        if (ModServerConfig.OP_ONLY_MODE.get() && !sPlayer.hasPermissions(2)) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
        if (ModServerConfig.isDimensionBlacklisted(level)) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
        if (!ModServerConfig.ENABLE_LOGGER.get()) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.logger_disabled").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            if (isActive(stack)) {
                resetSession(stack);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOGGER_CANCEL.get(), SoundSource.PLAYERS, 0.6F, 0.6F);
                sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.cancelled").withStyle(ChatFormatting.RED), true);
            } else {
                sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.not_active").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (!isActive(stack)) {
            if (ItemDurabilityHelper.isEmpty(stack)) {
                sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.empty").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean(TAG_ACTIVE, true);
            tag.put(TAG_ENTRIES, new ListTag());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOGGER_START.get(), SoundSource.PLAYERS, 0.6F, 1.2F);
            sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.started").withStyle(ChatFormatting.GREEN), true);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        ItemStack paper = new ItemStack(ModItems.LOG_PAPER.get());
        CompoundTag paperTag = paper.getOrCreateTag();
        paperTag.putString(LogPaperItem.TAG_AUTHOR, sPlayer.getName().getString());
        paperTag.putUUID(LogPaperItem.TAG_AUTHOR_UUID, sPlayer.getUUID());
        ListTag entries = stack.getOrCreateTag().getList(TAG_ENTRIES, 10).copy();
        paperTag.put(LogPaperItem.TAG_ENTRIES, entries);
        int recorded = entries.size();

        resetSession(stack);
        ItemDurabilityHelper.damagePerUse(stack, sPlayer, 1);

        if (!sPlayer.getInventory().add(paper)) {
            sPlayer.drop(paper, false);
        }
        if (recorded > 0) {
            dev.ClasherHD.bodycam.util.ModAdvancements.award(sPlayer, dev.ClasherHD.bodycam.util.ModAdvancements.CAUGHT_ON_LOGGER);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOGGER_STOP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        sPlayer.displayClientMessage(Component.translatable("message.bodycam.logger.stopped", recorded).withStyle(ChatFormatting.AQUA), true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    public static void recordObservation(ServerPlayer target, ServerPlayer observer, String observerDimension, Vec3 observerPosition) {
        List<ItemStack> candidates = new ArrayList<>();
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            ItemStack stack = target.getInventory().getItem(i);
            if (stack.getItem() instanceof LoggerItem) {
                candidates.add(stack);
            }
        }
        candidates.addAll(CuriosCompat.getEquippedItems(target, ModItems.LOGGER.get()));

        boolean sameDim = observerDimension.equals(target.level().dimension().location().toString());
        boolean far = !sameDim || observerPosition.distanceTo(target.position()) > FAR_DISTANCE;
        long now = System.currentTimeMillis();

        for (ItemStack stack : candidates) {
            if (!isActive(stack)) {
                continue;
            }
            CompoundTag tag = stack.getOrCreateTag();
            ListTag entries = tag.getList(TAG_ENTRIES, 10);
            CompoundTag entry = new CompoundTag();
            entry.putString(LogPaperItem.ENTRY_NAME, observer.getName().getString());
            entry.putUUID(LogPaperItem.ENTRY_UUID, observer.getUUID());
            entry.putLong(LogPaperItem.ENTRY_TIME, now);
            entry.putBoolean(LogPaperItem.ENTRY_FAR, far);
            entry.putBoolean(LogPaperItem.ENTRY_SAME_DIM, sameDim);
            entries.add(entry);
            while (entries.size() > MAX_ENTRIES) {
                entries.remove(0);
            }
            tag.put(TAG_ENTRIES, entries);
        }
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ItemDurabilityHelper.getLoggerMaxDurability();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged() && !ItemDurabilityHelper.isEmpty(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
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
