package dev.ClasherHD.bodycam.enchantment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnchantmentLevelsTest {

    @Test
    @DisplayName("EnchantmentLevels constants should have expected values")
    void constants_haveExpectedValues() {
        assertThat(EnchantmentLevels.SCRAMBLED_DIMENSION).isEqualTo("bodycam:scrambled");
        assertThat(EnchantmentLevels.MAX_REACH_LEVEL).isEqualTo(2);
        assertThat(EnchantmentLevels.TRUE_ANONYMITY_LEVEL).isEqualTo(2);
    }
}
