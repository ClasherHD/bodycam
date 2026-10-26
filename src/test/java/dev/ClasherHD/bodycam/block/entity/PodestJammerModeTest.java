package dev.ClasherHD.bodycam.block.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PodestJammerModeTest {

    @ParameterizedTest(name = "combineJammerModes({0}, {1}) -> {2}")
    @CsvSource({
            "0, 0, 0",
            "1, 0, 1",
            "0, 1, 1",
            "2, 0, 2",
            "0, 2, 2",
            "1, 1, 1",
            "2, 2, 2",
            "1, 2, 1",
            "2, 1, 1"
    })
    @DisplayName("combineJammerModes should resolve valid mode combinations correctly with mode 1 taking precedence")
    void combineJammerModes_validInputs_resolvesExpectedMode(int modeA, int modeB, int expected) {
        int result = PodestBlockEntity.combineJammerModes(modeA, modeB);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("combineJammerModes should handle negative and unexpected mode values safely")
    void combineJammerModes_withBoundaryAndNegativeInputs_returnsExpectedFallbackOrPrecedence() {
        assertThat(PodestBlockEntity.combineJammerModes(-1, -1)).isEqualTo(0);
        assertThat(PodestBlockEntity.combineJammerModes(-1, 0)).isEqualTo(0);
        assertThat(PodestBlockEntity.combineJammerModes(3, 4)).isEqualTo(0);
        assertThat(PodestBlockEntity.combineJammerModes(-1, 1)).isEqualTo(1);
        assertThat(PodestBlockEntity.combineJammerModes(1, -100)).isEqualTo(1);
        assertThat(PodestBlockEntity.combineJammerModes(-1, 2)).isEqualTo(2);
        assertThat(PodestBlockEntity.combineJammerModes(Integer.MIN_VALUE, 1)).isEqualTo(1);
        assertThat(PodestBlockEntity.combineJammerModes(Integer.MAX_VALUE, 2)).isEqualTo(2);
    }

    @Test
    @DisplayName("PodestBlockEntity constants and loaded set should behave correctly")
    void constantsAndLoadedSet_behaveAsExpected() {
        assertThat(PodestBlockEntity.MAX_CHARGES).isEqualTo(10);
        assertThat(PodestBlockEntity.BLOCKS_PER_CHARGE).isEqualTo(10.0D);
        assertThat(PodestBlockEntity.LOADED_PODESTS).isNotNull();
    }
}
