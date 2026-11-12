package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamRequestC2SPacket;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class AllSeeingEyeItem extends Item {

    public static final String TAG_DEACTIVATED = "Deactivated";
    public static final int FOG_DURATION_TICKS = 20;

    public AllSeeingEyeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isActivated(ItemStack stack) {
        return !stack.hasTag() || !stack.getTag().getBoolean(TAG_DEACTIVATED);
    }

    public static void setActivated(ItemStack stack, boolean activated) {
        if (activated) {
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                tag.remove(TAG_DEACTIVATED);
                if (tag.isEmpty()) {
                    stack.setTag(null);
                }
            }
        } else {
            stack.getOrCreateTag().putBoolean(TAG_DEACTIVATED, true);
        }
    }

    public static ItemStack createActivatedStack() {
        return new ItemStack(ModItems.ALL_SEEING_EYE.get());
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
        if (dev.ClasherHD.bodycam.entity.CreepingDeathEntity.blockObservationIfHunted(sPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        sPlayer.getPersistentData().putBoolean("bodycam_all_seeing_pending", true);
        SyncBodycamRequestC2SPacket.sendAllSeeingEyeState(sPlayer);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
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
}
