package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import net.minecraft.BlockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
public class PortableNetherPortalItem extends Item {

    public static final String TAG_CHARGES = "PortalCharges";
    public static final int MAX_CHARGES = 2;
    public static final int CHANNEL_TICKS = 80;

    public PortableNetherPortalItem(Properties properties) {
        super(properties);
    }

    public static int getCharges(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_CHARGES)) {
            return 0;
        }
        return Mth.clamp(stack.getTag().getInt(TAG_CHARGES), 0, MAX_CHARGES);
    }

    public static void setCharges(ItemStack stack, int charges) {
        stack.getOrCreateTag().putInt(TAG_CHARGES, Mth.clamp(charges, 0, MAX_CHARGES));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (isNearActiveNetherPortal(level, clickedPos)) {
            return tryBindPortal(level, player, stack, clickedPos);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(stack);
        }

        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hitResult.getType() == HitResult.Type.BLOCK && isNearActiveNetherPortal(level, hitResult.getBlockPos())) {
            InteractionResult bindResult = tryBindPortal(level, player, stack, hitResult.getBlockPos());
            if (bindResult.consumesAction()) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }

        int charges = getCharges(stack);
        if (charges <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.bodycam.portable_portal.not_charged").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        ResourceKey<Level> currentDim = level.dimension();
        if (currentDim != Level.OVERWORLD && currentDim != Level.NETHER) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.bodycam.portable_portal.invalid_dimension").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (player.getPersistentData().getBoolean("bodycam_active")) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResult tryBindPortal(Level level, Player player, ItemStack stack, BlockPos pos) {
        if (player != null && ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        if (getCharges(stack) >= MAX_CHARGES) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            setCharges(stack, MAX_CHARGES);
            level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.bodycam.portable_portal.charged").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private boolean isNearActiveNetherPortal(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
            return true;
        }
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.NETHER_PORTAL)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer player) {
            int charges = getCharges(stack);
            if (charges > 0 && performDimensionTeleport(player)) {
                if (!player.getAbilities().instabuild) {
                    int remaining = charges - 1;
                    if (remaining <= 0) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.9F);
                        stack.shrink(1);
                        return stack;
                    }
                    setCharges(stack, remaining);
                }
            }
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }

    private boolean performDimensionTeleport(ServerPlayer player) {
        ServerLevel sourceLevel = player.serverLevel();
        boolean destIsNether = sourceLevel.dimension() != Level.NETHER;
        ResourceKey<Level> targetDimKey = destIsNether ? Level.NETHER : Level.OVERWORLD;
        ServerLevel targetLevel = player.server.getLevel(targetDimKey);
        if (targetLevel == null || !player.server.isNetherEnabled() || player.isPassenger()) {
            return false;
        }

        WorldBorder border = targetLevel.getWorldBorder();
        double scale = DimensionType.getTeleportationScale(sourceLevel.dimensionType(), targetLevel.dimensionType());
        BlockPos scaledPos = border.clampToBounds(player.getX() * scale, player.getY(), player.getZ() * scale);

        Optional<BlockUtil.FoundRectangle> existingPortal = targetLevel.getPortalForcer().findPortalAround(scaledPos, destIsNether, border);
        if (existingPortal.isPresent()) {
            BlockUtil.FoundRectangle rect = existingPortal.get();
            if (player.isCreative()) {
                float[] rotation = new float[2];
                Vec3 landingVec = findExistingPortalLandingPos(targetLevel, rect, rotation);
                BlockPos landingBlockPos = BlockPos.containing(landingVec);

                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0.0F;
                player.setPortalCooldown(100);
                player.teleportTo(targetLevel, landingVec.x, landingVec.y, landingVec.z, rotation[0], rotation[1]);
                player.setPortalCooldown(100);

                targetLevel.playSound(null, landingBlockPos, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.0F);
                return true;
            } else {
                Direction.Axis portalAxis = targetLevel.getBlockState(rect.minCorner)
                        .getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS)
                        .orElse(Direction.Axis.X);
                PortalInfo portalInfo = PortalShape.createPortalInfo(
                        targetLevel,
                        rect,
                        portalAxis,
                        new Vec3(0.5D, 0.0D, 0.0D),
                        player,
                        Vec3.ZERO,
                        player.getYRot(),
                        player.getXRot()
                );
                BlockPos exitBlockPos = BlockPos.containing(portalInfo.pos);

                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0.0F;
                player.setPortalCooldown();
                player.teleportTo(targetLevel, portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z, portalInfo.yRot, portalInfo.xRot);

                targetLevel.playSound(null, exitBlockPos, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.0F);
                return true;
            }
        }

        Direction.Axis axis = player.getDirection().getClockWise().getAxis();
        BlockPos landingPos = findVanillaPortalLandingPos(targetLevel, scaledPos, axis);
        if (landingPos == null) {
            return false;
        }

        ensureSafeLanding(targetLevel, landingPos);

        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setPortalCooldown(100);
        player.teleportTo(
                targetLevel,
                landingPos.getX() + 0.5D,
                landingPos.getY(),
                landingPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
        player.setPortalCooldown(100);

        targetLevel.playSound(null, landingPos, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.0F);
        return true;
    }

    private Vec3 findExistingPortalLandingPos(ServerLevel level, BlockUtil.FoundRectangle portalRect, float[] outRotation) {
        BlockPos minCorner = portalRect.minCorner;
        BlockState portalState = level.getBlockState(minCorner);
        Direction.Axis portalAxis = portalState.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);

        int midOffset = portalRect.axis1Size / 2;
        BlockPos centerPortalBlock = (portalAxis == Direction.Axis.X)
                ? minCorner.offset(midOffset, 0, 0)
                : minCorner.offset(0, 0, midOffset);

        Direction normalPos = (portalAxis == Direction.Axis.X) ? Direction.SOUTH : Direction.EAST;
        Direction normalNeg = normalPos.getOpposite();

        BlockPos cand1 = centerPortalBlock.relative(normalPos);
        BlockPos cand2 = centerPortalBlock.relative(normalNeg);

        boolean cand1Safe = isSafePassableLanding(level, cand1);
        boolean cand2Safe = isSafePassableLanding(level, cand2);

        BlockPos chosen;
        Direction facing;
        if (cand1Safe) {
            chosen = cand1;
            facing = normalPos;
        } else if (cand2Safe) {
            chosen = cand2;
            facing = normalNeg;
        } else {
            chosen = cand1;
            facing = normalPos;
            ensureSafeLanding(level, chosen);
        }

        outRotation[0] = facing.toYRot();
        outRotation[1] = 0.0F;
        return new Vec3(chosen.getX() + 0.5D, chosen.getY(), chosen.getZ() + 0.5D);
    }

    private boolean isSafePassableLanding(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());

        if (feet.is(Blocks.NETHER_PORTAL) || head.is(Blocks.NETHER_PORTAL)) {
            return false;
        }
        if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) {
            return false;
        }
        if (!feet.getCollisionShape(level, pos).isEmpty() || !head.getCollisionShape(level, pos.above()).isEmpty()) {
            return false;
        }
        return !below.getCollisionShape(level, pos.below()).isEmpty() || below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private BlockPos findVanillaPortalLandingPos(ServerLevel level, BlockPos targetPos, Direction.Axis axis) {
        Direction dir = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        double bestFullDist = -1.0D;
        BlockPos bestFullPos = null;
        double bestPartialDist = -1.0D;
        BlockPos bestPartialPos = null;
        WorldBorder border = level.getWorldBorder();
        int maxHeight = Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + level.getLogicalHeight()) - 1;
        BlockPos.MutableBlockPos tempPos = targetPos.mutable();

        for (BlockPos.MutableBlockPos spiralPos : BlockPos.spiralAround(targetPos, 16, Direction.EAST, Direction.SOUTH)) {
            int startY = Math.min(maxHeight, level.getHeight(Heightmap.Types.MOTION_BLOCKING, spiralPos.getX(), spiralPos.getZ()));
            if (border.isWithinBounds(spiralPos) && border.isWithinBounds(spiralPos.move(dir, 1))) {
                spiralPos.move(dir.getOpposite(), 1);

                for (int y = startY; y >= level.getMinBuildHeight(); y--) {
                    spiralPos.setY(y);
                    if (canPortalReplaceBlock(level, spiralPos)) {
                        int topReplaceableY = y;
                        while (y > level.getMinBuildHeight() && canPortalReplaceBlock(level, spiralPos.move(Direction.DOWN))) {
                            y--;
                        }

                        if (y + 4 <= maxHeight) {
                            int openHeight = topReplaceableY - y;
                            if (openHeight <= 0 || openHeight >= 3) {
                                spiralPos.setY(y);
                                if (canHostFrame(level, spiralPos, tempPos, dir, 0)) {
                                    double distSq = targetPos.distSqr(spiralPos);
                                    if (canHostFrame(level, spiralPos, tempPos, dir, -1) && canHostFrame(level, spiralPos, tempPos, dir, 1)
                                            && (bestFullDist == -1.0D || bestFullDist > distSq)) {
                                        bestFullDist = distSq;
                                        bestFullPos = spiralPos.immutable();
                                    }

                                    if (bestFullDist == -1.0D && (bestPartialDist == -1.0D || bestPartialDist > distSq)) {
                                        bestPartialDist = distSq;
                                        bestPartialPos = spiralPos.immutable();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bestFullDist == -1.0D && bestPartialDist != -1.0D) {
            bestFullPos = bestPartialPos;
            bestFullDist = bestPartialDist;
        }

        if (bestFullDist == -1.0D) {
            int minClampY = Math.max(level.getMinBuildHeight() + 1, 70);
            int maxClampY = maxHeight - 9;
            if (maxClampY < minClampY) {
                return null;
            }
            bestFullPos = new BlockPos(targetPos.getX(), Mth.clamp(targetPos.getY(), minClampY, maxClampY), targetPos.getZ()).immutable();
            if (!border.isWithinBounds(bestFullPos)) {
                return null;
            }
        }

        return bestFullPos;
    }

    private boolean canPortalReplaceBlock(ServerLevel level, BlockPos.MutableBlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    private boolean canHostFrame(ServerLevel level, BlockPos basePos, BlockPos.MutableBlockPos tempPos, Direction dir, int offsetCross) {
        Direction crossDir = dir.getClockWise();
        for (int w = -1; w < 3; w++) {
            for (int h = -1; h < 4; h++) {
                tempPos.setWithOffset(basePos, dir.getStepX() * w + crossDir.getStepX() * offsetCross, h, dir.getStepZ() * w + crossDir.getStepZ() * offsetCross);
                if (h < 0 && !level.getBlockState(tempPos).isFaceSturdy(level, tempPos, Direction.UP)) {
                    return false;
                }
                if (h >= 0 && !canPortalReplaceBlock(level, tempPos)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void ensureSafeLanding(ServerLevel level, BlockPos feetPos) {
        BlockPos headPos = feetPos.above();
        BlockPos belowPos = feetPos.below();

        clearBlockWithoutDrops(level, feetPos);
        clearBlockWithoutDrops(level, headPos);

        BlockState belowState = level.getBlockState(belowPos);
        boolean hasSolidFloor = belowState.isFaceSturdy(level, belowPos, Direction.UP)
                && belowState.getFluidState().isEmpty();

        if (!hasSolidFloor) {
            if (belowState.getDestroySpeed(level, belowPos) >= 0.0F || belowState.isAir() || !belowState.getFluidState().isEmpty()) {
                level.setBlock(belowPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
        }
    }

    private void clearBlockWithoutDrops(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() && state.getFluidState().isEmpty()) {
            return;
        }
        if (state.getDestroySpeed(level, pos) >= 0.0F && level.getBlockEntity(pos) == null) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return CHANNEL_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharges(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharges(stack) / (float) MAX_CHARGES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xAA00FF;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int charges = getCharges(stack);
        if (charges > 0) {
            tooltip.add(Component.translatable("tooltip.bodycam.portable_portal.charges", charges, MAX_CHARGES)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("tooltip.bodycam.portable_portal.uncharged")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
