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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class AllSeeingEyeItem extends Item {

    public static final String TAG_DEACTIVATED = "Deactivated";
    public static final String TAG_CHARGED = "AllSeeingEyeCharged";
    public static final String TAG_FOG_START_TICK = "FogStartTick";
    public static final int FOG_DURATION_TICKS = 20;
    private static final Identifier ALL_SEEING_EYE_ACTIVATED_ADVANCEMENT = bodycam.id("all_seeing_eye_activated");

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
        long elapsed = gameTime - tag.getLongOr(TAG_FOG_START_TICK, 0L);
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
        double elapsed = (gameTime - tag.getLongOr(TAG_FOG_START_TICK, 0L)) + partialTick;
        if (elapsed < 0.0D || elapsed >= FOG_DURATION_TICKS) {
            return 0.0F;
        }
        return (float) (elapsed / FOG_DURATION_TICKS);
    }

    public static boolean isActivated(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        return tag == null || !tag.getBooleanOr(TAG_DEACTIVATED, false);
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
            return Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        }
        return Component.translatable("item.bodycam.all_seeing_eye.deactivated").withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity instanceof ServerPlayer sPlayer && entity.tickCount % 20 == 0 && isActivated(stack)) {
            ModAdvancements.award(sPlayer, ALL_SEEING_EYE_ACTIVATED_ADVANCEMENT);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isActivated(stack)) {
            if (!level.isClientSide() && player instanceof ServerPlayer sPlayer) {
                sPlayer.sendOverlayMessage(
                        Component.translatable("message.bodycam.all_seeing_eye.deactivated").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer sPlayer)) {
            return InteractionResult.PASS;
        }

        if (ModServerConfig.OP_ONLY_MODE.get() && !sPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (ModServerConfig.isDimensionBlacklisted(level)) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!ModServerConfig.ENABLE_ALL_SEEING_EYE.get()) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.all_seeing_eye_disabled").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (CreepingDeathEntity.blockObservationIfHunted(sPlayer)) {
            return InteractionResult.FAIL;
        }

        ModAdvancements.award(sPlayer, ALL_SEEING_EYE_ACTIVATED_ADVANCEMENT);
        PersistentDataAccess.get(sPlayer).putBoolean("bodycam_all_seeing_pending", true);
        SyncBodycamRequestC2SPacket.sendAllSeeingEyeState(sPlayer);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        if (isActivated(stack)) {
            tooltipAdder.accept(Component.translatable("tooltip.bodycam.all_seeing_eye.activated").withStyle(ChatFormatting.GOLD));
        } else {
            tooltipAdder.accept(Component.translatable("tooltip.bodycam.all_seeing_eye.deactivated").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
