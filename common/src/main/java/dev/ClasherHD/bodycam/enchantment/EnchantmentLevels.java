package dev.ClasherHD.bodycam.enchantment;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class EnchantmentLevels {

    public static final String SCRAMBLED_DIMENSION = "bodycam:scrambled";
    public static final int MAX_REACH_LEVEL = 2;
    public static final int TRUE_ANONYMITY_LEVEL = 2;

    private EnchantmentLevels() {
    }

    public static int getLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack == null || stack.isEmpty() || key == null) {
            return 0;
        }
        ItemEnchantments enchants = stack.getEnchantments();
        for (Holder<Enchantment> holder : enchants.keySet()) {
            if (holder.is(key)) {
                return enchants.getLevel(holder);
            }
        }
        return 0;
    }

    public static int reach(ItemStack stack) {
        if (!ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()) {
            return 0;
        }
        return Math.min(getLevel(stack, ModEnchantments.REACH), MAX_REACH_LEVEL);
    }

    public static int nightspy(ItemStack stack) {
        return getLevel(stack, ModEnchantments.NIGHTSPY);
    }

    public static int anonymity(ItemStack stack) {
        return getLevel(stack, ModEnchantments.ANONYMITY);
    }

    public static ItemStack heldBodycam(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof BodycamMonitorItem) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof BodycamMonitorItem) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    public static int sessionReach(ServerPlayer player, boolean onHologram) {
        if (onHologram) {
            return ModServerConfig.ENABLE_REACH_ENCHANTMENT.get() ? MAX_REACH_LEVEL : 0;
        }
        return reach(heldBodycam(player));
    }

    public static int sessionNightspy(ServerPlayer player, boolean onHologram) {
        if (onHologram) {
            return 0;
        }
        return nightspy(heldBodycam(player));
    }

    public static int activeAnonymity(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        int highest = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof AnonymizerItem
                    && !ItemDurabilityHelper.isEmpty(stack)
                    && ItemNbtAccess.getBoolean(stack, "AnonymizerActive")) {
                highest = Math.max(highest, anonymity(stack));
            }
        }
        for (ItemStack accStack : BodycamPlatform.getEquippedAccessoryItems(player, ModItems.ANONYMIZER.get())) {
            if (!ItemDurabilityHelper.isEmpty(accStack)
                    && ItemNbtAccess.getBoolean(accStack, "AnonymizerActive")) {
                highest = Math.max(highest, anonymity(accStack));
            }
        }
        highest = Math.max(highest, PodestBlockEntity.getPodestAnonymityLevel(player));
        return highest;
    }

    public static int anonymityLevel(ServerPlayer player) {
        return activeAnonymity(player);
    }

    public static boolean isHiddenFromSelection(ServerPlayer player) {
        return activeAnonymity(player) >= TRUE_ANONYMITY_LEVEL;
    }

    public static boolean isDimensionScrambled(ServerPlayer player) {
        return activeAnonymity(player) >= 1;
    }
}
