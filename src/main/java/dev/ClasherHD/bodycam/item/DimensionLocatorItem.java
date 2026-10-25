package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("null")
public class DimensionLocatorItem extends Item {

    public DimensionLocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ItemDurabilityHelper.isEmpty(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sPlayer) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (dev.ClasherHD.bodycam.config.ModServerConfig.isDimensionBlacklisted(level)) {
                player.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_DIMENSION_LOCATOR.get()) {
                player.sendSystemMessage(Component.translatable("message.bodycam.locator_disabled").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            ItemDurabilityHelper.damagePerUse(stack, sPlayer, 1);

            Map<UUID, String> dims = new HashMap<>();
            for (ServerPlayer onlinePlayer : sPlayer.server.getPlayerList().getPlayers()) {
                if (!onlinePlayer.getUUID().equals(player.getUUID()) && !EnchantmentLevels.isHiddenFromSelection(onlinePlayer)) {
                    String origDim = onlinePlayer.getPersistentData().contains("bodycam_orig_dim")
                            ? onlinePlayer.getPersistentData().getString("bodycam_orig_dim")
                            : (onlinePlayer.getPersistentData().contains("bodycam_original_dimension")
                                ? onlinePlayer.getPersistentData().getString("bodycam_original_dimension")
                                : null);
                    if (onlinePlayer.getPersistentData().contains("bodycam_target_uuid") && origDim != null) {
                        dims.put(onlinePlayer.getUUID(), origDim);
                    } else {
                        dims.put(onlinePlayer.getUUID(), onlinePlayer.level().dimension().location().toString());
                    }
                    if (EnchantmentLevels.isDimensionScrambled(onlinePlayer)) {
                        dims.put(onlinePlayer.getUUID(), EnchantmentLevels.SCRAMBLED_DIMENSION);
                    }
                }
            }
            dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sPlayer),
                    new dev.ClasherHD.bodycam.network.locator.DimensionLocatorResponsePacket(dims));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        ItemDurabilityHelper.setEmpty(stack);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ItemDurabilityHelper.getDimensionLocatorMaxDurability();
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
