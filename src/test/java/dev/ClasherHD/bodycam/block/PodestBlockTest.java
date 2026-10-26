package dev.ClasherHD.bodycam.block;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PodestBlockTest {

    private static PodestBlock block;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        block = (PodestBlock) ModBlocks.PODEST.get();
    }

    @Test
    @DisplayName("Default state should face NORTH, be unpowered, and have 0 charges")
    void defaultState_hasExpectedDefaults() {
        BlockState defaultState = block.defaultBlockState();

        assertThat(defaultState.getValue(PodestBlock.FACING)).isEqualTo(Direction.NORTH);
        assertThat(defaultState.getValue(PodestBlock.POWERED)).isFalse();
        assertThat(defaultState.getValue(PodestBlock.CHARGES)).isEqualTo(0);
        assertThat(block.getRenderShape(defaultState)).isEqualTo(RenderShape.MODEL);
        assertThat(block.useShapeForLightOcclusion(defaultState)).isTrue();
        assertThat(block.isPathfindable(defaultState, null, BlockPos.ZERO, PathComputationType.LAND)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10})
    @DisplayName("Analog output signal should reflect block state charges exactly")
    void analogOutputSignal_reflectsCharges(int charges) {
        BlockState state = block.defaultBlockState().setValue(PodestBlock.CHARGES, charges);

        assertThat(block.hasAnalogOutputSignal(state)).isTrue();
        assertThat(block.getAnalogOutputSignal(state, null, BlockPos.ZERO)).isEqualTo(charges);
    }

    @ParameterizedTest
    @EnumSource(Rotation.class)
    @DisplayName("Rotate should update horizontal facing properly")
    void rotate_updatesFacing(Rotation rotation) {
        BlockState state = block.defaultBlockState().setValue(PodestBlock.FACING, Direction.NORTH);
        BlockState rotated = block.rotate(state, rotation);

        assertThat(rotated.getValue(PodestBlock.FACING)).isEqualTo(rotation.rotate(Direction.NORTH));
    }

    @ParameterizedTest
    @EnumSource(Mirror.class)
    @DisplayName("Mirror should update horizontal facing properly")
    void mirror_updatesFacing(Mirror mirror) {
        BlockState state = block.defaultBlockState().setValue(PodestBlock.FACING, Direction.WEST);
        BlockState mirrored = block.mirror(state, mirror);

        assertThat(mirrored.getValue(PodestBlock.FACING)).isEqualTo(mirror.mirror(Direction.WEST));
    }

    @Test
    @DisplayName("getShape should return correct directional Lectern shapes for all cardinal directions")
    void getShape_returnsExpectedDirectionalShapes() {
        BlockState northState = block.defaultBlockState().setValue(PodestBlock.FACING, Direction.NORTH);
        BlockState southState = block.defaultBlockState().setValue(PodestBlock.FACING, Direction.SOUTH);
        BlockState eastState = block.defaultBlockState().setValue(PodestBlock.FACING, Direction.EAST);
        BlockState westState = block.defaultBlockState().setValue(PodestBlock.FACING, Direction.WEST);

        assertThat(block.getShape(northState, null, BlockPos.ZERO, null)).isEqualTo(LecternBlock.SHAPE_NORTH);
        assertThat(block.getShape(southState, null, BlockPos.ZERO, null)).isEqualTo(LecternBlock.SHAPE_SOUTH);
        assertThat(block.getShape(eastState, null, BlockPos.ZERO, null)).isEqualTo(LecternBlock.SHAPE_EAST);
        assertThat(block.getShape(westState, null, BlockPos.ZERO, null)).isEqualTo(LecternBlock.SHAPE_WEST);
    }

    @Test
    @DisplayName("newBlockEntity should return an instance of PodestBlockEntity")
    void newBlockEntity_returnsCorrectType() {
        BlockEntity be = block.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        assertThat(be).isInstanceOf(PodestBlockEntity.class);
    }
}
