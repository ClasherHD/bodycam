package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class PlayerLocatorCompassItemTest {

    private static PlayerLocatorCompassItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (PlayerLocatorCompassItem) ModItems.PLAYER_LOCATOR_COMPASS.get();
    }

    @Test
    @DisplayName("PlayerLocatorCompassItem should have expected durability and properties")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.getMaxDamage(stack)).isEqualTo(ItemDurabilityHelper.PLAYER_LOCATOR_DURABILITY);
        assertThat(item.isEnchantable(stack)).isTrue();
        assertThat(item.getEnchantmentValue()).isEqualTo(1);
        assertThat(item.isRepairable(stack)).isFalse();
    }

    @Test
    @DisplayName("isBarVisible should be true only when damaged and not empty")
    void isBarVisible_evaluatesCorrectly() {
        ItemStack stack = new ItemStack(item);
        assertThat(item.isBarVisible(stack)).isFalse();

        stack.setDamageValue(15);
        assertThat(item.isBarVisible(stack)).isTrue();

        ItemDurabilityHelper.setEmpty(stack);
        assertThat(item.isBarVisible(stack)).isFalse();
    }

    @Test
    @DisplayName("resetToIdle on fully populated CompoundTag should clean all locator and routing fields")
    void resetToIdle_withPopulatedTag_cleansAllFields() {
        CompoundTag tag = new CompoundTag();
        UUID targetUUID = UUID.randomUUID();
        tag.putUUID("LocatorTargetUUID", targetUUID);
        tag.putLong("LocatorTargetUUIDMost", targetUUID.getMostSignificantBits());
        tag.putLong("LocatorTargetUUIDLeast", targetUUID.getLeastSignificantBits());
        tag.putInt("LocatorState", 3);
        tag.putLong("LocatorStateTimestamp", 12345L);
        tag.putInt("LocatorCachedStructX", 100);
        tag.putInt("LocatorCachedStructZ", -200);
        tag.putInt("LocatorCachedPortalY", 64);
        tag.putBoolean("LocatorCachedIsPortal", true);
        tag.putString("LocatorLastTargetDim", "minecraft:the_nether");
        tag.putString("LocatorLastMyDim", "minecraft:overworld");
        tag.putLong("LocatorLastCheckTick", 500L);
        tag.putLong("LocatorStructSearchFailedTick", 600L);

        item.resetToIdle(tag);

        assertThat(tag.getInt("LocatorState")).isEqualTo(0);
        assertThat(tag.contains("LocatorTargetUUID")).isFalse();
        assertThat(tag.contains("LocatorTargetUUIDMost")).isFalse();
        assertThat(tag.contains("LocatorTargetUUIDLeast")).isFalse();
        assertThat(tag.contains("LocatorStateTimestamp")).isFalse();
        assertThat(tag.contains("LocatorCachedStructX")).isFalse();
        assertThat(tag.contains("LocatorCachedStructZ")).isFalse();
        assertThat(tag.contains("LocatorCachedPortalY")).isFalse();
        assertThat(tag.contains("LocatorCachedIsPortal")).isFalse();
        assertThat(tag.contains("LocatorLastTargetDim")).isFalse();
        assertThat(tag.contains("LocatorLastMyDim")).isFalse();
        assertThat(tag.contains("LocatorLastCheckTick")).isFalse();
        assertThat(tag.contains("LocatorStructSearchFailedTick")).isFalse();
    }

    @Test
    @DisplayName("resetToIdle on empty CompoundTag should initialize state to 0 safely without exceptions")
    void resetToIdle_withEmptyTag_executesSafely() {
        CompoundTag emptyTag = new CompoundTag();

        item.resetToIdle(emptyTag);

        assertThat(emptyTag.getInt("LocatorState")).isEqualTo(0);
    }

    @Test
    @DisplayName("Enchantment rules should allow Unbreaking and reject Mending")
    void enchantmentRules_adhereToPolicy() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.UNBREAKING)).isTrue();
        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.MENDING)).isFalse();
    }

    @Test
    @DisplayName("shouldCauseReequipAnimation should reequip on slot change or different item")
    void reequipAnimation_evaluatesCorrectly() {
        ItemStack stackA = new ItemStack(item);
        ItemStack stackB = new ItemStack(item);
        ItemStack other = new ItemStack(ModItems.ANONYMIZER.get());

        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, false)).isFalse();
        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, true)).isTrue();
        assertThat(item.shouldCauseReequipAnimation(stackA, other, false)).isTrue();
    }
}
