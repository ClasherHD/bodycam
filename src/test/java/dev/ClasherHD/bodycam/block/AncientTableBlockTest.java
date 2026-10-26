package dev.ClasherHD.bodycam.block;

import dev.ClasherHD.bodycam.block.entity.AncientTableBlockEntity;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class AncientTableBlockTest {

    private static AncientTableBlock block;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        block = (AncientTableBlock) ModBlocks.ANCIENT_TABLE.get();
    }

    @Test
    @DisplayName("Default state should face NORTH with fuel level 0")
    void defaultState_hasExpectedProperties() {
        BlockState defaultState = block.defaultBlockState();

        assertThat(defaultState.getValue(AncientTableBlock.FACING)).isEqualTo(Direction.NORTH);
        assertThat(defaultState.getValue(AncientTableBlock.FUEL_LEVEL)).isEqualTo(0);
        assertThat(block.getRenderShape(defaultState)).isEqualTo(RenderShape.MODEL);
    }

    @ParameterizedTest
    @EnumSource(Rotation.class)
    @DisplayName("Rotation should update horizontal facing consistently")
    void rotate_updatesFacingCorrectly(Rotation rotation) {
        BlockState state = block.defaultBlockState().setValue(AncientTableBlock.FACING, Direction.NORTH);
        BlockState rotated = block.rotate(state, rotation);

        assertThat(rotated.getValue(AncientTableBlock.FACING)).isEqualTo(rotation.rotate(Direction.NORTH));
    }

    @ParameterizedTest
    @EnumSource(Mirror.class)
    @DisplayName("Mirror should update horizontal facing consistently")
    void mirror_updatesFacingCorrectly(Mirror mirror) {
        BlockState state = block.defaultBlockState().setValue(AncientTableBlock.FACING, Direction.EAST);
        BlockState mirrored = block.mirror(state, mirror);

        assertThat(mirrored.getValue(AncientTableBlock.FACING)).isEqualTo(mirror.mirror(Direction.EAST));
    }

    @Test
    @DisplayName("newBlockEntity should return an instance of AncientTableBlockEntity")
    void newBlockEntity_returnsCorrectType() {
        BlockPos pos = BlockPos.ZERO;
        BlockState state = block.defaultBlockState();
        BlockEntity be = block.newBlockEntity(pos, state);

        assertThat(be).isInstanceOf(AncientTableBlockEntity.class);
    }

    @Test
    @DisplayName("getStateForPlacement faces opposite to context horizontal direction")
    void getStateForPlacement_facesOpposite() {
        BlockPlaceContext context = mock(BlockPlaceContext.class);
        when(context.getHorizontalDirection()).thenReturn(Direction.SOUTH);

        BlockState state = block.getStateForPlacement(context);
        assertThat(state.getValue(AncientTableBlock.FACING)).isEqualTo(Direction.NORTH);
    }

    @Test
    @DisplayName("getTicker returns null on client and valid ticker on server for matching block entity type")
    void getTicker_clientAndServerBehavior() {
        Level clientLevel = mock(Level.class);
        when(clientLevel.isClientSide()).thenReturn(true);
        BlockEntityTicker<AncientTableBlockEntity> clientTicker =
                block.getTicker(clientLevel, block.defaultBlockState(), ModBlockEntities.ANCIENT_TABLE.get());
        assertThat(clientTicker).isNull();

        Level serverLevel = mock(Level.class);
        when(serverLevel.isClientSide()).thenReturn(false);
        BlockEntityTicker<AncientTableBlockEntity> serverTicker =
                block.getTicker(serverLevel, block.defaultBlockState(), ModBlockEntities.ANCIENT_TABLE.get());
        assertThat(serverTicker).isNotNull();

        @SuppressWarnings("unchecked")
        BlockEntityType<BlockEntity> otherType = mock(BlockEntityType.class);
        BlockEntityTicker<BlockEntity> mismatchedTicker =
                block.getTicker(serverLevel, block.defaultBlockState(), otherType);
        assertThat(mismatchedTicker).isNull();
    }

    @Test
    @DisplayName("onRemove drops table contents when block changes")
    void onRemove_dropsContentsWhenBlockChanges() {
        Level level = mock(Level.class);
        BlockPos pos = new BlockPos(1, 2, 3);
        AncientTableBlockEntity table = mock(AncientTableBlockEntity.class);
        when(level.getBlockEntity(pos)).thenReturn(table);

        BlockState oldState = block.defaultBlockState();
        BlockState newState = Blocks.AIR.defaultBlockState();

        block.onRemove(oldState, level, pos, newState, false);
        verify(table).dropContents(level, pos);
    }
}
