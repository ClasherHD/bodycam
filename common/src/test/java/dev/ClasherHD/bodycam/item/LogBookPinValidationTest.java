package dev.ClasherHD.bodycam.item;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LogBookPinValidationTest {

    @AfterEach
    void tearDown() {
        LogBookItem.clearStaticState();
    }

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

    @Test
    void clearPlayerCooldown_removesOnlyTargetPlayerAndHandlesNull() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        LogBookItem.LAST_ATTEMPT.put(playerA, 100L);
        LogBookItem.LAST_ATTEMPT.put(playerB, 200L);

        LogBookItem.clearPlayerCooldown(null);
        assertThat(LogBookItem.LAST_ATTEMPT).containsKeys(playerA, playerB);

        LogBookItem.clearPlayerCooldown(playerA);
        assertThat(LogBookItem.LAST_ATTEMPT).doesNotContainKey(playerA).containsEntry(playerB, 200L);
    }
}
