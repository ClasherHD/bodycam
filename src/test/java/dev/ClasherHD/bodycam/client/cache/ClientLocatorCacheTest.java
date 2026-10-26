package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("null")
class ClientLocatorCacheTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @BeforeEach
    void setUp() {
        ClientLocatorCache.clear();
    }

    @AfterEach
    void tearDown() {
        ClientLocatorCache.clear();
    }

    @Test
    @DisplayName("Cache should initially have null values after clear")
    void initialState_isCompletelyNull() {
        assertThat(ClientLocatorCache.structureTarget).isNull();
        assertThat(ClientLocatorCache.structureTargetDimension).isNull();
    }

    @Test
    @DisplayName("updateStructure with valid BlockPos and dimension should set both fields")
    void updateStructure_withValidPosAndDimension_setsBothFieldsCorrectly() {
        BlockPos targetPos = new BlockPos(128, 70, -256);
        String dimensionStr = "minecraft:overworld";

        ClientLocatorCache.updateStructure(targetPos, dimensionStr);

        assertThat(ClientLocatorCache.structureTarget).isEqualTo(targetPos);
        assertThat(ClientLocatorCache.structureTargetDimension).isNotNull();
        assertThat(ClientLocatorCache.structureTargetDimension.location().toString()).isEqualTo("minecraft:overworld");
        assertThat(ClientLocatorCache.structureTargetDimension.registry()).isEqualTo(Registries.DIMENSION.location());
    }

    @Test
    @DisplayName("updateStructure with null dimension should set structureTargetDimension to null")
    void updateStructure_withNullDimension_clearsDimensionResourceKey() {
        BlockPos targetPos = new BlockPos(50, 100, 50);

        ClientLocatorCache.updateStructure(targetPos, "minecraft:the_nether");
        assertThat(ClientLocatorCache.structureTargetDimension).isNotNull();

        ClientLocatorCache.updateStructure(targetPos, null);

        assertThat(ClientLocatorCache.structureTarget).isEqualTo(targetPos);
        assertThat(ClientLocatorCache.structureTargetDimension).isNull();
    }

    @Test
    @DisplayName("updateStructure with null position should set structureTarget to null")
    void updateStructure_withNullPosition_clearsPositionWhileUpdatingDimension() {
        ClientLocatorCache.updateStructure(null, "minecraft:the_end");

        assertThat(ClientLocatorCache.structureTarget).isNull();
        assertThat(ClientLocatorCache.structureTargetDimension).isNotNull();
        assertThat(ClientLocatorCache.structureTargetDimension.location().toString()).isEqualTo("minecraft:the_end");
    }

    @Test
    @DisplayName("updateStructure with custom namespace dimension creates valid ResourceKey")
    void updateStructure_withCustomDimensionNamespace_createsCorrectResourceKey() {
        BlockPos pos = new BlockPos(0, 0, 0);
        String customDim = "bodycam:scrambled";

        ClientLocatorCache.updateStructure(pos, customDim);

        ResourceKey<Level> expectedKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("bodycam", "scrambled"));
        assertThat(ClientLocatorCache.structureTargetDimension).isEqualTo(expectedKey);
    }

    @Test
    @DisplayName("updateStructure with boundary coordinates preserves exact values")
    void updateStructure_withBoundaryCoordinates_preservesCoordinatesExact() {
        BlockPos extremePos = new BlockPos(Integer.MIN_VALUE, -64, Integer.MAX_VALUE);

        ClientLocatorCache.updateStructure(extremePos, "minecraft:overworld");

        assertThat(ClientLocatorCache.structureTarget).isNotNull();
        assertThat(ClientLocatorCache.structureTarget.getX()).isEqualTo(Integer.MIN_VALUE);
        assertThat(ClientLocatorCache.structureTarget.getY()).isEqualTo(-64);
        assertThat(ClientLocatorCache.structureTarget.getZ()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("clear should reset both target position and dimension to null")
    void clear_whenPopulated_resetsAllFieldsToNull() {
        ClientLocatorCache.updateStructure(new BlockPos(10, 20, 30), "minecraft:overworld");
        assertThat(ClientLocatorCache.structureTarget).isNotNull();
        assertThat(ClientLocatorCache.structureTargetDimension).isNotNull();

        ClientLocatorCache.clear();

        assertThat(ClientLocatorCache.structureTarget).isNull();
        assertThat(ClientLocatorCache.structureTargetDimension).isNull();
    }

    @Test
    @DisplayName("Repeated clear calls should be idempotent")
    void clear_whenAlreadyEmpty_isIdempotent() {
        ClientLocatorCache.clear();
        ClientLocatorCache.clear();

        assertThat(ClientLocatorCache.structureTarget).isNull();
        assertThat(ClientLocatorCache.structureTargetDimension).isNull();
    }

    @Test
    @DisplayName("updateStructure with invalid dimension resource location should throw ResourceLocationException")
    void updateStructure_withInvalidDimensionResourceLocation_throwsException() {
        BlockPos pos = new BlockPos(0, 64, 0);
        assertThatThrownBy(() -> ClientLocatorCache.updateStructure(pos, "Invalid Resource Location with Spaces"))
                .isInstanceOf(net.minecraft.ResourceLocationException.class);
    }
}
