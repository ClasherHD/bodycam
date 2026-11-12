package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class LoggerItemTest {

    private static LoggerItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (LoggerItem) ModItems.LOGGER.get();
        com.electronwill.nightconfig.core.CommentedConfig serverConfig = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        ModServerConfig.SPEC.acceptConfig(serverConfig);
    }

    @Test
    @DisplayName("LoggerItem should have expected durability and enchantability")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.getMaxDamage(stack)).isEqualTo(ItemDurabilityHelper.LOGGER_DURABILITY);
        assertThat(item.isEnchantable(stack)).isFalse();
        assertThat(item.isRepairable(stack)).isFalse();
        assertThat(item.canApplyAtEnchantingTable(stack, null)).isFalse();
        assertThat(item.isBookEnchantable(stack, ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("isActive should reflect LoggerActive tag")
    void isActive_evaluatesTag() {
        ItemStack stack = new ItemStack(item);
        assertThat(LoggerItem.isActive(stack)).isFalse();

        stack.getOrCreateTag().putBoolean(LoggerItem.TAG_ACTIVE, true);
        assertThat(LoggerItem.isActive(stack)).isTrue();

        stack.getTag().putBoolean(LoggerItem.TAG_ACTIVE, false);
        assertThat(LoggerItem.isActive(stack)).isFalse();
    }

    @Test
    @DisplayName("isBarVisible should be true only when damaged and not empty")
    void isBarVisible_evaluatesCorrectly() {
        ItemStack stack = new ItemStack(item);
        assertThat(item.isBarVisible(stack)).isFalse();

        stack.setDamageValue(2);
        assertThat(item.isBarVisible(stack)).isTrue();

        ItemDurabilityHelper.setEmpty(stack);
        assertThat(item.isBarVisible(stack)).isFalse();
    }

    @Test
    @DisplayName("TAG_ENTRIES list should be clamped to MAX_ENTRIES")
    void entries_areClampedToMax() {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        ListTag entries = new ListTag();
        for (int i = 0; i < 10; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Name", "Player" + i);
            entries.add(entry);
        }
        tag.put(LoggerItem.TAG_ENTRIES, entries);

        while (entries.size() > LoggerItem.MAX_ENTRIES) {
            entries.remove(0);
        }
        assertThat(entries).hasSize(LoggerItem.MAX_ENTRIES);
    }
}
