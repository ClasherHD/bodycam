package dev.ClasherHD.bodycam.item;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogBookPinValidationTest {
    @Test
    void isValidPin_acceptsOnlyFourAsciiDigits() {
        assertThat(LogBookItem.isValidPin("0000")).isTrue();
        assertThat(LogBookItem.isValidPin("1234")).isTrue();
        assertThat(LogBookItem.isValidPin("9876")).isTrue();

        assertThat(LogBookItem.isValidPin(null)).isFalse();
        assertThat(LogBookItem.isValidPin("")).isFalse();
        assertThat(LogBookItem.isValidPin("123")).isFalse();
        assertThat(LogBookItem.isValidPin("12345")).isFalse();
        assertThat(LogBookItem.isValidPin("12a4")).isFalse();
        assertThat(LogBookItem.isValidPin("١٢٣٤")).isFalse();
    }
}
