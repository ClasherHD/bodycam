package dev.ClasherHD.bodycam.enchantment;

import dev.ClasherHD.bodycam.registry.ModEnchantments;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class BodycamNightspyEnchantmentTest {

    private static BodycamNightspyEnchantment enchantment;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        enchantment = (BodycamNightspyEnchantment) ModEnchantments.NIGHTSPY_ENCHANTMENT.get();
    }

    @Test
    @DisplayName("Nightspy enchantment should have max level 4 and be treasure only")
    void properties_haveExpectedDefaults() {
        assertThat(enchantment.getMaxLevel()).isEqualTo(4);
        assertThat(enchantment.isTreasureOnly()).isTrue();
    }

    @ParameterizedTest(name = "level={0} -> minCost={1}, maxCost={2}")
    @CsvSource({
            "1, 20, 70",
            "2, 30, 80",
            "3, 40, 90",
            "4, 50, 100"
    })
    @DisplayName("Costs should scale according to formula: min=10+10*level, max=min+50")
    void costs_scaleAccurately(int level, int expectedMin, int expectedMax) {
        assertThat(enchantment.getMinCost(level)).isEqualTo(expectedMin);
        assertThat(enchantment.getMaxCost(level)).isEqualTo(expectedMax);
    }

    @Test
    @DisplayName("canEnchant and canApplyAtEnchantingTable should only accept BodycamMonitorItem")
    void itemApplicability_acceptsOnlyMonitor() {
        ItemStack monitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());
        ItemStack anonymizer = new ItemStack(ModItems.ANONYMIZER.get());
        ItemStack stick = new ItemStack(Items.STICK);

        assertThat(enchantment.canEnchant(monitor)).isTrue();
        assertThat(enchantment.canApplyAtEnchantingTable(monitor)).isTrue();

        assertThat(enchantment.canEnchant(anonymizer)).isFalse();
        assertThat(enchantment.canApplyAtEnchantingTable(anonymizer)).isFalse();

        assertThat(enchantment.canEnchant(stick)).isFalse();
        assertThat(enchantment.canApplyAtEnchantingTable(stick)).isFalse();
    }
}
