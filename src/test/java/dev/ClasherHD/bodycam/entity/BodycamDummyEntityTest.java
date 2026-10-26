package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class BodycamDummyEntityTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("createAttributes should construct all required attributes")
    void createAttributes_buildsExpectedAttributes() {
        AttributeSupplier.Builder builder = BodycamDummyEntity.createAttributes();
        AttributeSupplier supplier = builder.build();

        assertThat(supplier.hasAttribute(Attributes.MAX_HEALTH)).isTrue();
        assertThat(supplier.hasAttribute(Attributes.ARMOR)).isTrue();
        assertThat(supplier.hasAttribute(Attributes.ARMOR_TOUGHNESS)).isTrue();
        assertThat(supplier.hasAttribute(Attributes.KNOCKBACK_RESISTANCE)).isTrue();
        assertThat(supplier.hasAttribute(Attributes.ATTACK_DAMAGE)).isTrue();
    }

    @Test
    @DisplayName("Entity data accessors and static tracking maps should be defined")
    void dataAccessorsAndTrackingMaps_areAvailable() {
        assertThat(BodycamDummyEntity.OWNER_UUID).isNotNull();
        assertThat(BodycamDummyEntity.OWNER_NAME).isNotNull();

        assertThat(BodycamDummyEntity.DUMMY_POS).isNotNull();
        assertThat(BodycamDummyEntity.DUMMY_FALL).isNotNull();
        assertThat(BodycamDummyEntity.DUMMY_MOTION).isNotNull();

        UUID testId = UUID.randomUUID();
        Vec3 pos = new Vec3(10.5, 64.0, -20.5);
        BodycamDummyEntity.DUMMY_POS.put(testId, pos);
        assertThat(BodycamDummyEntity.DUMMY_POS.get(testId)).isEqualTo(pos);
        BodycamDummyEntity.DUMMY_POS.remove(testId);
    }

    @Test
    @DisplayName("markChunkForced and markChunkUnforced should update forcing state flags correctly")
    void chunkForcingState_updatesCorrectly() {
        BodycamDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(BodycamDummyEntity.class);

        dummy.markChunkForced(12, -34);

        int chunkX = MinecraftTestEnvironment.getField(dummy, "currentLoadedChunkX");
        int chunkZ = MinecraftTestEnvironment.getField(dummy, "currentLoadedChunkZ");
        boolean forced = MinecraftTestEnvironment.getField(dummy, "isChunkForced");

        assertThat(chunkX).isEqualTo(12);
        assertThat(chunkZ).isEqualTo(-34);
        assertThat(forced).isTrue();

        dummy.markChunkUnforced();
        boolean unforced = MinecraftTestEnvironment.getField(dummy, "isChunkForced");
        assertThat(unforced).isFalse();
    }

    @Test
    @DisplayName("isLinkedToOwnerHealth should return true on standard BodycamDummyEntity")
    void isLinkedToOwnerHealth_returnsTrue() throws Exception {
        BodycamDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(BodycamDummyEntity.class);
        Method m = BodycamDummyEntity.class.getDeclaredMethod("isLinkedToOwnerHealth");
        m.setAccessible(true);
        boolean linked = (boolean) m.invoke(dummy);

        assertThat(linked).isTrue();
    }
}
