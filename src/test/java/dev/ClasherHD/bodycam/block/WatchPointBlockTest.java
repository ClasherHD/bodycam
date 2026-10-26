package dev.ClasherHD.bodycam.block;

import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class WatchPointBlockTest {

    private static WatchPointBlock block;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        block = (WatchPointBlock) ModBlocks.WATCH_POINT.get();
    }

    @Test
    @DisplayName("WatchPointBlock should have MODEL render shape")
    void renderShape_isModel() {
        BlockState defaultState = block.defaultBlockState();
        assertThat(block.getRenderShape(defaultState)).isEqualTo(RenderShape.MODEL);
    }

    @Test
    @DisplayName("newBlockEntity should return WatchPointBlockEntity")
    void newBlockEntity_returnsCorrectType() {
        BlockEntity be = block.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        assertThat(be).isInstanceOf(WatchPointBlockEntity.class);
    }

    @Test
    @DisplayName("setPlacedBy configures owner and custom name on server side")
    void setPlacedBy_configuresBlockEntity() {
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);

        BlockPos pos = new BlockPos(1, 2, 3);
        WatchPointBlockEntity wp = new WatchPointBlockEntity(pos, block.defaultBlockState());
        when(level.getBlockEntity(pos)).thenReturn(wp);

        ServerPlayer player = mock(ServerPlayer.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUUID()).thenReturn(playerId);
        when(player.getName()).thenReturn(Component.literal("PlayerName"));

        ItemStack stack = new ItemStack(ModBlocks.WATCH_POINT.get().asItem());
        stack.setHoverName(Component.literal("CustomWatchPoint"));

        block.setPlacedBy(level, pos, block.defaultBlockState(), player, stack);

        assertThat(wp.getOwnerUUID()).isEqualTo(playerId);
        assertThat((String) MinecraftTestEnvironment.getField(wp, "ownerName")).isEqualTo("PlayerName");
        assertThat(wp.getCustomName()).isEqualTo("CustomWatchPoint");
    }

    @Test
    @DisplayName("use returns PASS for offhand and delegates to onPlayerInteract for mainhand on server")
    void use_handlesInteraction() {
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);

        BlockPos pos = new BlockPos(1, 2, 3);
        WatchPointBlockEntity wp = mock(WatchPointBlockEntity.class);
        when(level.getBlockEntity(pos)).thenReturn(wp);

        ServerPlayer player = mock(ServerPlayer.class);
        BlockHitResult hit = new BlockHitResult(Vec3.ZERO, net.minecraft.core.Direction.UP, pos, false);

        InteractionResult offhandResult = block.use(block.defaultBlockState(), level, pos, player, InteractionHand.OFF_HAND, hit);
        assertThat(offhandResult).isEqualTo(InteractionResult.PASS);
        verifyNoInteractions(wp);

        InteractionResult mainhandResult = block.use(block.defaultBlockState(), level, pos, player, InteractionHand.MAIN_HAND, hit);
        assertThat(mainhandResult).isEqualTo(InteractionResult.CONSUME);
        verify(wp).onPlayerInteract(player);
    }

    @Test
    @DisplayName("onRemove despawns dummy when block changes on server")
    void onRemove_despawnsDummyOnServer() {
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);

        BlockPos pos = new BlockPos(1, 2, 3);
        WatchPointBlockEntity wp = mock(WatchPointBlockEntity.class);
        when(level.getBlockEntity(pos)).thenReturn(wp);

        BlockState oldState = block.defaultBlockState();
        BlockState newState = Blocks.AIR.defaultBlockState();

        block.onRemove(oldState, level, pos, newState, false);
        verify(wp).despawnDummy(level);
    }
}
