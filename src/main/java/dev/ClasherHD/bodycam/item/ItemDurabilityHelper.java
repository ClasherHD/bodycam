package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.enchantment.BodycamAnonymityEnchantment;
import dev.ClasherHD.bodycam.enchantment.BodycamNightspyEnchantment;
import dev.ClasherHD.bodycam.enchantment.BodycamReachEnchantment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Map;

@SuppressWarnings("null")
public final class ItemDurabilityHelper {

    public static final int SPYGLASS_DURABILITY = 16;
    public static final int DIMENSION_LOCATOR_DURABILITY = 8;
    public static final int PLAYER_LOCATOR_DURABILITY = 560;
    public static final int ANONYMIZER_DURABILITY = 448;
    public static final int JAMMER_DURABILITY = 336;

    private ItemDurabilityHelper() {
    }

    public static boolean isUnbreakable(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("Unbreakable");
    }

    public static ItemStack createUnbreakableStack(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putBoolean("Unbreakable", true);
        return stack;
    }

    public static boolean isEmpty(ItemStack stack) {
        if (isUnbreakable(stack)) {
            return false;
        }
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage();
    }

    public static int getRemainingDurability(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 0;
        }
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    public static void setEmpty(ItemStack stack) {
        if (stack.isDamageableItem()) {
            stack.setDamageValue(stack.getMaxDamage());
        }
    }

    public static ItemStack createEmptyStack(Item item) {
        ItemStack stack = new ItemStack(item);
        setEmpty(stack);
        return stack;
    }

    public static boolean isPerTimeItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof PlayerLocatorCompassItem
                || item instanceof AnonymizerItem
                || item instanceof JammerItem;
    }

    public static boolean isPerUseItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BodycamMonitorItem
                || item instanceof DimensionLocatorItem;
    }

    public static boolean isRepairableModItem(ItemStack stack) {
        return isPerTimeItem(stack) || isPerUseItem(stack);
    }

    public static void damagePerUse(ItemStack stack, ServerPlayer player, int amount) {
        if (player != null && player.isCreative()) {
            return;
        }
        if (isUnbreakable(stack) || !stack.isDamageableItem() || isEmpty(stack) || amount <= 0) {
            return;
        }
        int newDamage = Math.min(stack.getMaxDamage(), stack.getDamageValue() + amount);
        stack.setDamageValue(newDamage);
        if (newDamage >= stack.getMaxDamage() && player != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
    }

    public static void damagePerTime(ItemStack stack, ServerPlayer player, int amount) {
        if (player != null && player.isCreative()) {
            return;
        }
        if (isUnbreakable(stack) || !stack.isDamageableItem() || isEmpty(stack) || amount <= 0) {
            return;
        }
        int unbreaking = stack.getEnchantmentLevel(Enchantments.UNBREAKING);
        int actualDamage = 0;
        for (int i = 0; i < amount; i++) {
            if (unbreaking <= 0 || player == null
                    || !DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(stack, unbreaking, player.getRandom())) {
                actualDamage++;
            }
        }
        if (actualDamage <= 0) {
            return;
        }
        int newDamage = Math.min(stack.getMaxDamage(), stack.getDamageValue() + actualDamage);
        stack.setDamageValue(newDamage);
        if (newDamage >= stack.getMaxDamage() && player != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
    }

    public static boolean canApplyEnchantment(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.MENDING) {
            return false;
        }
        if (enchantment == Enchantments.UNBREAKING) {
            return isPerTimeItem(stack);
        }
        if (enchantment instanceof BodycamReachEnchantment
                || enchantment instanceof BodycamNightspyEnchantment
                || enchantment instanceof BodycamAnonymityEnchantment) {
            return enchantment.canEnchant(stack);
        }
        return false;
    }

    public static boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(book);
        if (enchantments.isEmpty()) {
            return false;
        }
        for (Enchantment enchantment : enchantments.keySet()) {
            if (!canApplyEnchantment(stack, enchantment)) {
                return false;
            }
        }
        return true;
    }
}
