package dev.ClasherHD.bodycam.item;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class ItemDurabilityHelperTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("Durability constants should have expected baseline values")
    void constants_haveExpectedValues() {
        assertThat(ItemDurabilityHelper.SPYGLASS_DURABILITY).isEqualTo(16);
        assertThat(ItemDurabilityHelper.DIMENSION_LOCATOR_DURABILITY).isEqualTo(8);
        assertThat(ItemDurabilityHelper.PLAYER_LOCATOR_DURABILITY).isEqualTo(560);
        assertThat(ItemDurabilityHelper.ANONYMIZER_DURABILITY).isEqualTo(448);
        assertThat(ItemDurabilityHelper.JAMMER_DURABILITY).isEqualTo(336);
    }

    @Test
    @DisplayName("Max durability getters should return default constants when config spec is not loaded")
    void getMaxDurability_whenConfigNotLoaded_returnsDefaultConstants() {
        assertThat(ItemDurabilityHelper.getSpyglassMaxDurability()).isEqualTo(ItemDurabilityHelper.SPYGLASS_DURABILITY);
        assertThat(ItemDurabilityHelper.getDimensionLocatorMaxDurability()).isEqualTo(ItemDurabilityHelper.DIMENSION_LOCATOR_DURABILITY);
        assertThat(ItemDurabilityHelper.getPlayerLocatorMaxDurability()).isEqualTo(ItemDurabilityHelper.PLAYER_LOCATOR_DURABILITY);
        assertThat(ItemDurabilityHelper.getAnonymizerMaxDurability()).isEqualTo(ItemDurabilityHelper.ANONYMIZER_DURABILITY);
        assertThat(ItemDurabilityHelper.getJammerMaxDurability()).isEqualTo(ItemDurabilityHelper.JAMMER_DURABILITY);
    }

    @Test
    @DisplayName("ItemStack.EMPTY should be treated as unbreakable=false, empty=false, durability=0")
    void emptyItemStack_properties_behaveSafely() {
        ItemStack empty = ItemStack.EMPTY;

        assertThat(ItemDurabilityHelper.isUnbreakable(empty)).isFalse();
        assertThat(ItemDurabilityHelper.isEmpty(empty)).isFalse();
        assertThat(ItemDurabilityHelper.getRemainingDurability(empty)).isEqualTo(0);
        assertThat(ItemDurabilityHelper.isPerTimeItem(empty)).isFalse();
        assertThat(ItemDurabilityHelper.isPerUseItem(empty)).isFalse();
        assertThat(ItemDurabilityHelper.isRepairableModItem(empty)).isFalse();
    }

    @Test
    @DisplayName("Damage methods on ItemStack.EMPTY should safely execute without error")
    void damageMethods_onEmptyItemStack_executeSafely() {
        ItemStack empty = ItemStack.EMPTY;

        ItemDurabilityHelper.setEmpty(empty);
        ItemDurabilityHelper.damagePerUse(empty, null, 1);
        ItemDurabilityHelper.damagePerUse(empty, null, 0);
        ItemDurabilityHelper.damagePerUse(empty, null, -10);

        ItemDurabilityHelper.damagePerTime(empty, null, 1);
        ItemDurabilityHelper.damagePerTime(empty, null, 0);
        ItemDurabilityHelper.damagePerTime(empty, null, -10);

        assertThat(empty.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("createUnbreakableStack should create an item with Unbreakable=true tag")
    void createUnbreakableStack_setsUnbreakableTag() {
        net.minecraft.world.item.Item dummyItem = net.minecraft.world.item.Items.STICK;
        ItemStack stack = ItemDurabilityHelper.createUnbreakableStack(dummyItem);

        assertThat(ItemDurabilityHelper.isUnbreakable(stack)).isTrue();
        assertThat(ItemDurabilityHelper.isEmpty(stack)).isFalse();
    }

    @Test
    @DisplayName("isUnbreakable should return false when tag is missing or Unbreakable is false")
    void isUnbreakable_withVariousTags_evaluatesCorrectly() {
        net.minecraft.world.item.Item dummyItem = net.minecraft.world.item.Items.STICK;
        ItemStack plainStack = new ItemStack(dummyItem);
        assertThat(ItemDurabilityHelper.isUnbreakable(plainStack)).isFalse();

        plainStack.getOrCreateTag().putBoolean("Unbreakable", false);
        assertThat(ItemDurabilityHelper.isUnbreakable(plainStack)).isFalse();

        plainStack.getOrCreateTag().putBoolean("Unbreakable", true);
        assertThat(ItemDurabilityHelper.isUnbreakable(plainStack)).isTrue();
    }

    @Test
    @DisplayName("canApplyEnchantment should reject Mending unconditionally")
    void canApplyEnchantment_rejectsMending() {
        ItemStack empty = ItemStack.EMPTY;
        assertThat(ItemDurabilityHelper.canApplyEnchantment(empty, net.minecraft.world.item.enchantment.Enchantments.MENDING)).isFalse();
    }

    @Test
    @DisplayName("canApplyEnchantment should reject Unbreaking for items that are not per-time items")
    void canApplyEnchantment_rejectsUnbreakingForNonPerTimeItems() {
        ItemStack empty = ItemStack.EMPTY;
        assertThat(ItemDurabilityHelper.canApplyEnchantment(empty, net.minecraft.world.item.enchantment.Enchantments.UNBREAKING)).isFalse();
    }

    @Test
    @DisplayName("isBookEnchantable with empty book should return false")
    void isBookEnchantable_withEmptyBook_returnsFalse() {
        ItemStack empty = ItemStack.EMPTY;
        ItemStack emptyBook = ItemStack.EMPTY;
        assertThat(ItemDurabilityHelper.isBookEnchantable(empty, emptyBook)).isFalse();
    }
}
