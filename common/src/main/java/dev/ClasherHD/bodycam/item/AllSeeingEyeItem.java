package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamRequestC2SPacket;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.ModAdvancements;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class AllSeeingEyeItem extends Item {

    public static final String TAG_DEACTIVATED = "Deactivated";
    public static final String TAG_CHARGED = "AllSeeingEyeCharged";
    public static final String TAG_FOG_START_TICK = "FogStartTick";
    public static final int FOG_DURATION_TICKS = 20;
    private static final ResourceLocation ALL_SEEING_EYE_ACTIVATED_ADVANCEMENT = bodycam.id("all_seeing_eye_activated");

    public AllSeeingEyeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isFogActive(ItemStack stack, long gameTime) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag == null || !tag.contains(TAG_FOG_START_TICK)) {
            return false;
        }
        long elapsed = gameTime - tag.getLong(TAG_FOG_START_TICK);
        return elapsed >= 0 && elapsed < FOG_DURATION_TICKS;
    }

    public static float getFogProgress(ItemStack stack, long gameTime, float partialTick) {
        if (stack == null || stack.isEmpty()) {
            return 0.0F;
        }
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag == null || !tag.contains(TAG_FOG_START_TICK)) {
            return 0.0F;
        }
        double elapsed = (gameTime - tag.getLong(TAG_FOG_START_TICK)) + partialTick;
        if (elapsed < 0.0D || elapsed >= FOG_DURATION_TICKS) {
            return 0.0F;
        }
        return (float) (elapsed / FOG_DURATION_TICKS);
    }

    public static boolean isActivated(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        return tag == null || !tag.getBoolean(TAG_DEACTIVATED);
    }

    public static void setActivated(ItemStack stack, boolean activated) {
        ItemNbtAccess.updateTag(stack, tag -> {
            if (activated) {
                tag.remove(TAG_DEACTIVATED);
                tag.putBoolean(TAG_CHARGED, true);
            } else {
                tag.remove(TAG_CHARGED);
                tag.putBoolean(TAG_DEACTIVATED, true);
            }
        });
    }

    public static ItemStack createActivatedStack() {
        ItemStack stack = new ItemStack(ModItems.ALL_SEEING_EYE.get());
        setActivated(stack, true);
        return stack;
    }

    public static ItemStack createDeactivatedStack() {
        ItemStack stack = new ItemStack(ModItems.ALL_SEEING_EYE.get());
        setActivated(stack, false);
        return stack;
    }

    public static boolean hasActivatedEye(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof AllSeeingEyeItem && isActivated(held)) {
                return true;
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (inv.getItem() instanceof AllSeeingEyeItem && isActivated(inv)) {
                return true;
            }
        }
        return false;
    }

    public static boolean consumeOne(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof AllSeeingEyeItem && isActivated(held)) {
                held.shrink(1);
                return true;
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (inv.getItem() instanceof AllSeeingEyeItem && isActivated(inv)) {
                inv.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isActivated(stack)) {
            return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        }
        return Component.translatable("item.bodycam.all_seeing_eye.deactivated").withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide() && entity instanceof ServerPlayer sPlayer && entity.tickCount % 20 == 0 && isActivated(stack)) {
            ModAdvancements.award(sPlayer, ALL_SEEING_EYE_ACTIVATED_ADVANCEMENT);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isActivated(stack)) {
            if (!level.isClientSide() && player instanceof ServerPlayer sPlayer) {
                sPlayer.displayClientMessage(
                        Component.translatable("message.bodycam.all_seeing_eye.deactivated").withStyle(ChatFormatting.RED),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide()) {
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
        if (!ModServerConfig.ENABLE_ALL_SEEING_EYE.get()) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.all_seeing_eye_disabled").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
        if (CreepingDeathEntity.blockObservationIfHunted(sPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        ModAdvancements.award(sPlayer, ALL_SEEING_EYE_ACTIVATED_ADVANCEMENT);
        PersistentDataAccess.get(sPlayer).putBoolean("bodycam_all_seeing_pending", true);
        SyncBodycamRequestC2SPacket.sendAllSeeingEyeState(sPlayer);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isActivated(stack)) {
            tooltip.add(Component.translatable("tooltip.bodycam.all_seeing_eye.activated").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.bodycam.all_seeing_eye.deactivated").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
