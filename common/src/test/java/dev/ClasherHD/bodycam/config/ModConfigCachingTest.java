package dev.ClasherHD.bodycam.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModConfigCachingTest {
    @Test
    void parseHexColor_returnsParsedValueOrFallbackOnMalformedString() {
        assertThat(ModClientConfig.parseHexColor("#FF3300", 0xFFFFFF)).isEqualTo(0xFF3300);
        assertThat(ModClientConfig.parseHexColor("0x00FF88", 0xFFFFFF)).isEqualTo(0x00FF88);
        assertThat(ModClientConfig.parseHexColor("NOT_HEX", 0x123456)).isEqualTo(0x123456);
        assertThat(ModClientConfig.parseHexColor(null, 0x654321)).isEqualTo(0x654321);
        assertThat(ModClientConfig.parseHexColor("", 0x654321)).isEqualTo(0x654321);
        assertThat(ModClientConfig.parseHexColor("#GGGGGG", 0x112233)).isEqualTo(0x112233);
    }

    @Test
    void forgeDefaultHexColorsAndOverlayToggles_matchExpectedDefaults() {
        assertThat(ModClientConfig.SHOW_NAME_OVERLAY.getDefault()).isTrue();
        assertThat(ModClientConfig.SHOW_HEALTH_OVERLAY.getDefault()).isTrue();
        assertThat(ModClientConfig.SHOW_SHIFT_OVERLAY.getDefault()).isTrue();
        assertThat(ModClientConfig.SHOW_DAMAGE_VIGNETTE.getDefault()).isTrue();

        assertThat(ModClientConfig.COLOR_STANDARD.getDefault()).isEqualTo("FFFFFF");
        assertThat(ModClientConfig.COLOR_BLOCKED.getDefault()).isEqualTo("FF5555");
        assertThat(ModClientConfig.COLOR_OBSERVING.getDefault()).isEqualTo("5555FF");
        assertThat(ModClientConfig.COLOR_DIMENSION.getDefault()).isEqualTo("55FF55");
        assertThat(ModClientConfig.COLOR_OUT_OF_RANGE.getDefault()).isEqualTo("FFFF55");
        assertThat(ModClientConfig.COLOR_OTHER_DIMENSION.getDefault()).isEqualTo("FFAA00");

        assertThat(ModClientConfig.parseHexColor(ModClientConfig.COLOR_STANDARD.getDefault(), 0)).isEqualTo(0xFFFFFF);
        assertThat(ModClientConfig.parseHexColor(ModClientConfig.COLOR_BLOCKED.getDefault(), 0)).isEqualTo(0xFF5555);
        assertThat(ModClientConfig.parseHexColor(ModClientConfig.COLOR_OBSERVING.getDefault(), 0)).isEqualTo(0x5555FF);
        assertThat(ModClientConfig.parseHexColor(ModClientConfig.COLOR_DIMENSION.getDefault(), 0)).isEqualTo(0x55FF55);
        assertThat(ModClientConfig.parseHexColor(ModClientConfig.COLOR_OUT_OF_RANGE.getDefault(), 0)).isEqualTo(0xFFFF55);
        assertThat(ModClientConfig.parseHexColor(ModClientConfig.COLOR_OTHER_DIMENSION.getDefault(), 0)).isEqualTo(0xFFAA00);
    }
}
