package dev.ClasherHD.bodycam.block;

import com.mojang.serialization.MapCodec;
import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("null")
public class WatchPointBlock extends BaseEntityBlock {
    public static final MapCodec<WatchPointBlock> CODEC = simpleCodec(WatchPointBlock::new);

    public WatchPointBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WatchPointBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WatchPointBlockEntity wp) {
            if (placer instanceof Player player) {
                wp.setOwner(player.getUUID(), player.getName().getString());
            }
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                wp.setCustomName(stack.getHoverName().getString());
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof WatchPointBlockEntity wp) {
            wp.onPlayerInteract(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof WatchPointBlockEntity wp) {
            wp.despawnDummy(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
