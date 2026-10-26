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

class BodycamReachEnchantmentTest {

    private static BodycamReachEnchantment enchantment;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        enchantment = (BodycamReachEnchantment) ModEnchantments.REACH_ENCHANTMENT.get();
    }

    @Test
    @DisplayName("Reach enchantment should have max level 2")
    void properties_haveExpectedDefaults() {
        assertThat(enchantment.getMaxLevel()).isEqualTo(2);
    }

    @ParameterizedTest(name = "level={0} -> minCost={1}, maxCost={2}")
    @CsvSource({
            "1, 15, 65",
            "2, 30, 80"
    })
    @DisplayName("Costs should scale according to formula: min=15+(level-1)*15, max=min+50")
    void costs_scaleAccurately(int level, int expectedMin, int expectedMax) {
        assertThat(enchantment.getMinCost(level)).isEqualTo(expectedMin);
        assertThat(enchantment.getMaxCost(level)).isEqualTo(expectedMax);
    }

    @Test
    @DisplayName("canEnchant and canApplyAtEnchantingTable should accept BodycamMonitorItem and PlayerLocatorCompassItem")
    void itemApplicability_acceptsMonitorAndCompass() {
        ItemStack monitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());
        ItemStack compass = new ItemStack(ModItems.PLAYER_LOCATOR_COMPASS.get());
        ItemStack anonymizer = new ItemStack(ModItems.ANONYMIZER.get());
        ItemStack stick = new ItemStack(Items.STICK);

        assertThat(enchantment.canEnchant(monitor)).isTrue();
        assertThat(enchantment.canApplyAtEnchantingTable(monitor)).isTrue();

        assertThat(enchantment.canEnchant(compass)).isTrue();
        assertThat(enchantment.canApplyAtEnchantingTable(compass)).isTrue();

        assertThat(enchantment.canEnchant(anonymizer)).isFalse();
        assertThat(enchantment.canApplyAtEnchantingTable(anonymizer)).isFalse();

        assertThat(enchantment.canEnchant(stick)).isFalse();
        assertThat(enchantment.canApplyAtEnchantingTable(stick)).isFalse();
    }
}
