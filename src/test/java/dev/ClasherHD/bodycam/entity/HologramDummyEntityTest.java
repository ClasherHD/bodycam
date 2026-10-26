package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HologramDummyEntityTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("HologramDummyEntity should be invulnerable, non-pushable, and immune to damage")
    void invulnerabilityAndPushability_evaluatesCorrectly() {
        HologramDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(HologramDummyEntity.class);

        assertThat(dummy.hurt(null, 50.0F)).isFalse();
        assertThat(dummy.isInvulnerable()).isTrue();
        assertThat(dummy.isInvulnerableTo(null)).isTrue();
        assertThat(dummy.isPushable()).isFalse();
        assertThat(dummy).isInstanceOf(BodycamDummyEntity.class);
    }
}
