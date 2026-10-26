package dev.ClasherHD.bodycam.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModClientConfigTest {

    @Test
    @DisplayName("parseHexColor should parse 6-character hex with hash prefix")
    void parseHexColor_withHashPrefix_returnsParsedInteger() {
        int white = ModClientConfig.parseHexColor("#FFFFFF", 0);
        assertThat(white).isEqualTo(0xFFFFFF);

        int black = ModClientConfig.parseHexColor("#000000", -1);
        assertThat(black).isEqualTo(0x000000);

        int green = ModClientConfig.parseHexColor("#55FF55", 0);
        assertThat(green).isEqualTo(0x55FF55);
    }

    @Test
    @DisplayName("parseHexColor should parse 6-character hex without hash prefix")
    void parseHexColor_withoutHashPrefix_returnsParsedInteger() {
        int white = ModClientConfig.parseHexColor("FFFFFF", 0);
        assertThat(white).isEqualTo(0xFFFFFF);

        int blue = ModClientConfig.parseHexColor("5555FF", 0);
        assertThat(blue).isEqualTo(0x5555FF);

        int orange = ModClientConfig.parseHexColor("FFAA00", 0);
        assertThat(orange).isEqualTo(0xFFAA00);
    }

    @Test
    @DisplayName("parseHexColor should parse lowercase hex strings")
    void parseHexColor_withLowercaseHex_returnsParsedInteger() {
        int white = ModClientConfig.parseHexColor("#ffffff", 0);
        assertThat(white).isEqualTo(0xFFFFFF);

        int custom = ModClientConfig.parseHexColor("aabbcc", 0);
        assertThat(custom).isEqualTo(0xAABBCC);
    }

    @Test
    @DisplayName("parseHexColor should handle 8-character hex with alpha channel")
    void parseHexColor_withAlphaChannel_returnsSigned32BitInt() {
        int fullAlphaWhite = ModClientConfig.parseHexColor("#FFFFFFFF", 0);
        assertThat(fullAlphaWhite).isEqualTo(-1);
    }

    @Test
    @DisplayName("parseHexColor should return fallback on null or empty input")
    void parseHexColor_withNullOrEmptyInput_returnsFallback() {
        int fallback = 12345;
        assertThat(ModClientConfig.parseHexColor(null, fallback)).isEqualTo(fallback);
        assertThat(ModClientConfig.parseHexColor("", fallback)).isEqualTo(fallback);
    }

    @Test
    @DisplayName("parseHexColor should return fallback on invalid characters")
    void parseHexColor_withInvalidCharacters_returnsFallback() {
        int fallback = -1;
        assertThat(ModClientConfig.parseHexColor("not_a_color", fallback)).isEqualTo(fallback);
        assertThat(ModClientConfig.parseHexColor("#GGGGGG", fallback)).isEqualTo(fallback);
        assertThat(ModClientConfig.parseHexColor("#12345Z", fallback)).isEqualTo(fallback);
        assertThat(ModClientConfig.parseHexColor("  123456  ", fallback)).isEqualTo(fallback);
    }

    @Test
    @DisplayName("setActiveModConfig and reloadFromDisk should execute without exception when null")
    void configLifecycle_whenActiveConfigIsNull_executesSafely() {
        ModClientConfig.setActiveModConfig(null);
        boolean reloaded = ModClientConfig.reloadFromDisk();
        assertThat(reloaded).isTrue();
    }
}
