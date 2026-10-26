package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class CompassDummyEntityTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("CompassDummyEntity should be subclass of BodycamDummyEntity")
    void inheritance_isSubclassOfBodycamDummyEntity() {
        CompassDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(CompassDummyEntity.class);

        assertThat(dummy).isInstanceOf(BodycamDummyEntity.class);
    }
}
