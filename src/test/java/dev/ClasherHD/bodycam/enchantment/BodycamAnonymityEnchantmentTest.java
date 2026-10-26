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

class BodycamAnonymityEnchantmentTest {

    private static BodycamAnonymityEnchantment enchantment;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        enchantment = (BodycamAnonymityEnchantment) ModEnchantments.ANONYMITY_ENCHANTMENT.get();
    }

    @Test
    @DisplayName("Anonymity enchantment should have max level 2 and be treasure only")
    void properties_haveExpectedDefaults() {
        assertThat(enchantment.getMaxLevel()).isEqualTo(2);
        assertThat(enchantment.isTreasureOnly()).isTrue();
    }

    @ParameterizedTest(name = "level={0} -> minCost={1}, maxCost={2}")
    @CsvSource({
            "1, 30, 80",
            "2, 45, 95"
    })
    @DisplayName("Costs should scale according to formula: min=15+15*level, max=min+50")
    void costs_scaleAccurately(int level, int expectedMin, int expectedMax) {
        assertThat(enchantment.getMinCost(level)).isEqualTo(expectedMin);
        assertThat(enchantment.getMaxCost(level)).isEqualTo(expectedMax);
    }

    @Test
    @DisplayName("getFullname should switch to true anonymity key at level 2")
    void getFullname_switchesAtLevelTwo() {
        assertThat(enchantment.getFullname(1).getString()).contains("enchantment.bodycam.anonymity");
        assertThat(enchantment.getFullname(2).getString()).contains("enchantment.bodycam.true_anonymity");
    }

    @Test
    @DisplayName("canEnchant and canApplyAtEnchantingTable should only accept AnonymizerItem")
    void itemApplicability_acceptsOnlyAnonymizer() {
        ItemStack anonymizer = new ItemStack(ModItems.ANONYMIZER.get());
        ItemStack monitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());
        ItemStack stick = new ItemStack(Items.STICK);

        assertThat(enchantment.canEnchant(anonymizer)).isTrue();
        assertThat(enchantment.canApplyAtEnchantingTable(anonymizer)).isTrue();

        assertThat(enchantment.canEnchant(monitor)).isFalse();
        assertThat(enchantment.canApplyAtEnchantingTable(monitor)).isFalse();

        assertThat(enchantment.canEnchant(stick)).isFalse();
        assertThat(enchantment.canApplyAtEnchantingTable(stick)).isFalse();
    }
}
