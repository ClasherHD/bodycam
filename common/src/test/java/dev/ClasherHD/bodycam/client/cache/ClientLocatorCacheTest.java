package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.ResourceLocationException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("null")
class ClientLocatorCacheTest {

    @BeforeEach
    void setUp() {
        ClientLocatorCache.clear();
    }

    @AfterEach
    void tearDown() {
        ClientLocatorCache.clear();
    }

    @Test
    void updateStructure_setsAndClearsTargetAndDimension() {
        BlockPos targetPos = new BlockPos(128, 70, -256);
        ClientLocatorCache.updateStructure(targetPos, "bodycam:scrambled");

        ResourceKey<Level> expectedKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("bodycam", "scrambled"));
        assertThat(ClientLocatorCache.structureTarget).isEqualTo(targetPos);
        assertThat(ClientLocatorCache.structureTargetDimension).isEqualTo(expectedKey);

        ClientLocatorCache.updateStructure(targetPos, null);
        assertThat(ClientLocatorCache.structureTarget).isEqualTo(targetPos);
        assertThat(ClientLocatorCache.structureTargetDimension).isNull();

        ClientLocatorCache.clear();
        assertThat(ClientLocatorCache.structureTarget).isNull();
        assertThat(ClientLocatorCache.structureTargetDimension).isNull();
    }

    @Test
    void updateStructure_withInvalidDimension_throwsResourceLocationException() {
        assertThatThrownBy(() -> ClientLocatorCache.updateStructure(BlockPos.ZERO, "Invalid Dimension"))
                .isInstanceOf(ResourceLocationException.class);
    }
}
