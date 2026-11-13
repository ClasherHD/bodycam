package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class ItemDurabilityHelper {
    public static final int SPYGLASS_DURABILITY = 16;
    public static final int DIMENSION_LOCATOR_DURABILITY = 8;
    public static final int PLAYER_LOCATOR_DURABILITY = 560;
    public static final int ANONYMIZER_DURABILITY = 448;
    public static final int JAMMER_DURABILITY = 336;
    public static final int LOGGER_DURABILITY = 8;

    public static final String CRYSTAL_TRIM_MATERIAL = bodycam.MODID + ":observation_crystal";
    public static final float SAVE_CHANCE_PER_TRIM_PIECE = 0.125F;

    private ItemDurabilityHelper() {
    }

    public static int getLoggerMaxDurability() {
        return ModServerConfig.isLoaded() ? ModServerConfig.LOGGER_DURABILITY.get() : LOGGER_DURABILITY;
    }

    public static int getSpyglassMaxDurability() {
        return ModServerConfig.isLoaded() ? ModServerConfig.SPYGLASS_DURABILITY.get() : SPYGLASS_DURABILITY;
    }

    public static int getDimensionLocatorMaxDurability() {
        return ModServerConfig.isLoaded() ? ModServerConfig.DIMENSION_LOCATOR_DURABILITY.get() : DIMENSION_LOCATOR_DURABILITY;
    }

    public static int getPlayerLocatorMaxDurability() {
        return ModServerConfig.isLoaded() ? ModServerConfig.PLAYER_LOCATOR_DURABILITY.get() : PLAYER_LOCATOR_DURABILITY;
    }

    public static int getAnonymizerMaxDurability() {
        return ModServerConfig.isLoaded() ? ModServerConfig.ANONYMIZER_DURABILITY.get() : ANONYMIZER_DURABILITY;
    }

    public static int getJammerMaxDurability() {
        return ModServerConfig.isLoaded() ? ModServerConfig.JAMMER_DURABILITY.get() : JAMMER_DURABILITY;
    }

    public static ItemStack createUnbreakableStack(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        return stack;
    }

    public static boolean isUnbreakable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.has(DataComponents.UNBREAKABLE);
    }

    public static boolean isEmpty(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        if (isUnbreakable(stack)) {
            return false;
        }
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage();
    }

    public static int getRemainingDurability(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
            return 0;
        }
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    public static void setEmpty(ItemStack stack) {
        if (stack != null && stack.isDamageableItem()) {
            stack.setDamageValue(stack.getMaxDamage());
        }
    }

    public static ItemStack createEmptyStack(Item item) {
        ItemStack stack = new ItemStack(item);
        setEmpty(stack);
        return stack;
    }

    public static boolean isPerTimeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof PlayerLocatorCompassItem
                || item instanceof AnonymizerItem
                || item instanceof JammerItem;
    }

    public static boolean isPerUseItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof BodycamMonitorItem
                || item instanceof DimensionLocatorItem;
    }

    public static boolean isRepairableModItem(ItemStack stack) {
        return isPerTimeItem(stack) || isPerUseItem(stack);
    }

    public static boolean hasObservationCrystalTrim(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim == null) {
            return false;
        }
        if ("observation_crystal".equals(trim.material().value().assetName())) {
            return true;
        }
        return trim.material().unwrapKey().map(ResourceKey::location).map(loc -> loc.equals(bodycam.id("observation_crystal"))).orElse(false);
    }

    public static int getObservationCrystalTrimCount(Player player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (hasObservationCrystalTrim(armorStack)) {
                count++;
            }
        }
        return Math.min(4, count);
    }

    public static float getTrimSaveChance(Player player) {
        return getObservationCrystalTrimCount(player) * SAVE_CHANCE_PER_TRIM_PIECE;
    }

    public static boolean hasFullObservationCrystalTrimSet(Player player) {
        return getObservationCrystalTrimCount(player) >= 4;
    }

    public static boolean shouldSaveDurabilityByTrim(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        float chance = getTrimSaveChance(player);
        return chance > 0.0F && player.getRandom().nextFloat() < chance;
    }

    public static void consumeTickDurability(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isUnbreakable(stack) || !stack.isDamageableItem() || isEmpty(stack)) {
            return;
        }
        int next = stack.getDamageValue() + 1;
        stack.setDamageValue(Math.min(stack.getMaxDamage(), next));
    }

    public static void consumeUse(ItemStack stack, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            damagePerUse(stack, serverPlayer, 1);
            return;
        }
        if (player != null && player.isCreative()) {
            return;
        }
        if (stack == null || stack.isEmpty() || isUnbreakable(stack) || !stack.isDamageableItem() || isEmpty(stack)) {
            return;
        }
        int next = stack.getDamageValue() + 1;
        stack.setDamageValue(Math.min(stack.getMaxDamage(), next));
    }

    public static void damagePerUse(ItemStack stack, ServerPlayer player, int amount) {
        if (player != null && player.isCreative()) {
            return;
        }
        if (stack == null || stack.isEmpty() || isUnbreakable(stack) || !stack.isDamageableItem() || isEmpty(stack) || amount <= 0) {
            return;
        }
        int actualDamage = 0;
        for (int i = 0; i < amount; i++) {
            if (!shouldSaveDurabilityByTrim(player)) {
                actualDamage++;
            }
        }
        if (actualDamage <= 0) {
            return;
        }
        int next = Math.min(stack.getMaxDamage(), stack.getDamageValue() + actualDamage);
        stack.setDamageValue(next);
        if (next >= stack.getMaxDamage() && player != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
    }

    public static void damagePerTime(ItemStack stack, ServerPlayer player, int amount) {
        if (player != null && player.isCreative()) {
            return;
        }
        if (stack == null || stack.isEmpty() || isUnbreakable(stack) || !stack.isDamageableItem() || isEmpty(stack) || amount <= 0) {
            return;
        }
        int unbreaking = EnchantmentLevels.getLevel(stack, Enchantments.UNBREAKING);
        int actualDamage = 0;
        for (int i = 0; i < amount; i++) {
            if (shouldSaveDurabilityByTrim(player)) {
                continue;
            }
            if (unbreaking <= 0 || player == null || player.getRandom().nextInt(unbreaking + 1) == 0) {
                actualDamage++;
            }
        }
        if (actualDamage <= 0) {
            return;
        }
        int next = Math.min(stack.getMaxDamage(), stack.getDamageValue() + actualDamage);
        stack.setDamageValue(next);
        if (next >= stack.getMaxDamage() && player != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
    }

    public static boolean canApplyEnchantment(ItemStack stack, Holder<Enchantment> holder) {
        if (stack == null || stack.isEmpty() || holder == null) {
            return false;
        }
        if (holder.is(Enchantments.MENDING)) {
            return false;
        }
        if (holder.is(Enchantments.UNBREAKING)) {
            return isPerTimeItem(stack);
        }
        if (holder.is(ModEnchantments.REACH) || holder.is(ModEnchantments.NIGHTSPY) || holder.is(ModEnchantments.ANONYMITY)) {
            return holder.value().canEnchant(stack);
        }
        return false;
    }

    public static boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (stack == null || stack.isEmpty() || book == null || book.isEmpty()) {
            return false;
        }
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        if (enchantments.isEmpty()) {
            return false;
        }
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (!canApplyEnchantment(stack, holder)) {
                return false;
            }
        }
        return true;
    }
}
