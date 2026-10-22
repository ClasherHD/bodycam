package dev.ClasherHD.bodycam.enchantment;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.compat.curios.CuriosCompat;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

@SuppressWarnings("null")
public final class EnchantmentLevels {

    public static final String SCRAMBLED_DIMENSION = "bodycam:scrambled";
    public static final int MAX_REACH_LEVEL = 2;
    public static final int TRUE_ANONYMITY_LEVEL = 2;

    private EnchantmentLevels() {
    }

    private static int level(Enchantment enchantment, ItemStack stack) {
        return stack.getEnchantmentLevel(enchantment);
    }

    public static int reach(ItemStack stack) {
        if (!ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()) {
            return 0;
        }
        return Math.min(level(ModEnchantments.REACH_ENCHANTMENT.get(), stack), MAX_REACH_LEVEL);
    }

    public static int nightspy(ItemStack stack) {
        return level(ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), stack);
    }

    public static int anonymity(ItemStack stack) {
        return level(ModEnchantments.ANONYMITY_ENCHANTMENT.get(), stack);
    }

    public static ItemStack heldBodycam(Player player) {
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
        int highest = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof AnonymizerItem
                    && !ItemDurabilityHelper.isEmpty(stack)
                    && stack.hasTag()
                    && stack.getTag().getBoolean("AnonymizerActive")) {
                highest = Math.max(highest, anonymity(stack));
            }
        }
        for (ItemStack curioStack : CuriosCompat.getEquippedItems(player, ModItems.ANONYMIZER.get())) {
            if (!ItemDurabilityHelper.isEmpty(curioStack)
                    && curioStack.hasTag()
                    && curioStack.getTag().getBoolean("AnonymizerActive")) {
                highest = Math.max(highest, anonymity(curioStack));
            }
        }
        highest = Math.max(highest, PodestBlockEntity.getPodestAnonymityLevel(player));
        return highest;
    }

    public static boolean isHiddenFromSelection(ServerPlayer player) {
        return activeAnonymity(player) >= TRUE_ANONYMITY_LEVEL;
    }

    public static boolean isDimensionScrambled(ServerPlayer player) {
        return activeAnonymity(player) >= 1;
    }
}
