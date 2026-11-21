package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class CreepingDeathEntityTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("isValidSpawnSpace returns false when chunk is unloaded without calling getBlockState")
    void isValidSpawnSpace_unloadedChunk_returnsFalseSafely() throws Exception {
        Method method = CreepingDeathEntity.class.getDeclaredMethod("isValidSpawnSpace", ServerLevel.class, BlockPos.class);
        method.setAccessible(true);

        ServerLevel level = mock(ServerLevel.class);
        BlockPos pos = new BlockPos(100, 64, 100);
        when(level.isLoaded(pos)).thenReturn(false);

        boolean result = (boolean) method.invoke(null, level, pos);
        assertThat(result).isFalse();
        verify(level, never()).getBlockState(any());
    }

    @Test
    @DisplayName("isValidSpawnSpace allows passable non-air blocks like grass while rejecting solid obstacles")
    void isValidSpawnSpace_allowsPassableNonAirAndRejectsSolid() throws Exception {
        Method method = CreepingDeathEntity.class.getDeclaredMethod("isValidSpawnSpace", ServerLevel.class, BlockPos.class);
        method.setAccessible(true);

        ServerLevel level = mock(ServerLevel.class);
        BlockPos pos = new BlockPos(10, 65, 10);
        when(level.isLoaded(pos)).thenReturn(true);

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState grass = Blocks.GRASS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        when(level.getBlockState(pos.below())).thenReturn(stone);
        when(level.getBlockState(pos)).thenReturn(grass);
        when(level.getBlockState(pos.above())).thenReturn(air);
        assertThat((boolean) method.invoke(null, level, pos)).isTrue();

        when(level.getBlockState(pos.above())).thenReturn(stone);
        assertThat((boolean) method.invoke(null, level, pos)).isFalse();
    }

    @Test
    @DisplayName("updateForcedChunk and releaseForcedChunk use ForgeChunkManager tickets across moves and release")
    void chunkForcing_usesForgeChunkManagerTickets() throws Exception {
        Method updateMethod = CreepingDeathEntity.class.getDeclaredMethod("updateForcedChunk", ServerLevel.class);
        updateMethod.setAccessible(true);
        Method releaseMethod = CreepingDeathEntity.class.getDeclaredMethod("releaseForcedChunk");
        releaseMethod.setAccessible(true);

        ServerLevel overworld = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        when(overworld.dimension()).thenReturn(Level.OVERWORLD);
        when(overworld.getServer()).thenReturn(server);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(overworld);

        CreepingDeathEntity entity = MinecraftTestEnvironment.allocateInstance(CreepingDeathEntity.class);
        MinecraftTestEnvironment.setField(entity, "level", overworld);
        MinecraftTestEnvironment.setField(entity, "blockPosition", new BlockPos(16, 64, 32));

        try (MockedStatic<ForgeChunkManager> mockedFcm = mockStatic(ForgeChunkManager.class)) {
            mockedFcm.when(() -> ForgeChunkManager.forceChunk(any(), anyString(), any(net.minecraft.world.entity.Entity.class), anyInt(), anyInt(), anyBoolean(), anyBoolean()))
                    .thenReturn(true);

            updateMethod.invoke(entity, overworld);
            mockedFcm.verify(() -> ForgeChunkManager.forceChunk(overworld, "bodycam", entity, 1, 2, true, true));

            MinecraftTestEnvironment.setField(entity, "blockPosition", new BlockPos(48, 64, 64));
            updateMethod.invoke(entity, overworld);
            mockedFcm.verify(() -> ForgeChunkManager.forceChunk(overworld, "bodycam", entity, 1, 2, false, true));
            mockedFcm.verify(() -> ForgeChunkManager.forceChunk(overworld, "bodycam", entity, 3, 4, true, true));

            releaseMethod.invoke(entity);
            mockedFcm.verify(() -> ForgeChunkManager.forceChunk(overworld, "bodycam", entity, 3, 4, false, true));
            assertThat((Object) MinecraftTestEnvironment.getField(entity, "forcedChunkX")).isNull();
            assertThat((Object) MinecraftTestEnvironment.getField(entity, "forcedChunkDim")).isNull();
        }
    }

    @Test
    @DisplayName("setupEquipment enchants all weapons, tools, elytra, and armor with Curse of Vanishing")
    void setupEquipment_enchantsAllItemsWithCurseOfVanishing() throws Exception {
        ServerLevel level = mock(ServerLevel.class);
        when(level.isClientSide()).thenReturn(false);

        CreepingDeathEntity entity = spy(MinecraftTestEnvironment.allocateInstance(CreepingDeathEntity.class));
        MinecraftTestEnvironment.setField(entity, "level", level);
        MinecraftTestEnvironment.setField(entity, "handItems", NonNullList.withSize(2, ItemStack.EMPTY));
        MinecraftTestEnvironment.setField(entity, "armorItems", NonNullList.withSize(4, ItemStack.EMPTY));
        MinecraftTestEnvironment.setField(entity, "handDropChances", new float[2]);
        MinecraftTestEnvironment.setField(entity, "armorDropChances", new float[4]);
        MinecraftTestEnvironment.setField(entity, "firstTick", true);
        doReturn(true).when(entity).isSilent();

        entity.setupEquipment(RegistryAccess.EMPTY);

        ItemStack godSword = MinecraftTestEnvironment.getField(entity, "godSword");
        ItemStack pickaxe = MinecraftTestEnvironment.getField(entity, "pickaxeStack");
        ItemStack axe = MinecraftTestEnvironment.getField(entity, "axeStack");
        ItemStack shovel = MinecraftTestEnvironment.getField(entity, "shovelStack");
        ItemStack elytra = MinecraftTestEnvironment.getField(entity, "elytraStack");
        ItemStack trimmedChest = MinecraftTestEnvironment.getField(entity, "trimmedChestStack");

        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, godSword)).isEqualTo(1);
        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, pickaxe)).isEqualTo(1);
        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, axe)).isEqualTo(1);
        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, shovel)).isEqualTo(1);
        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, elytra)).isEqualTo(1);
        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, trimmedChest)).isEqualTo(1);

        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, entity.getItemBySlot(EquipmentSlot.MAINHAND))).isEqualTo(1);
        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, entity.getItemBySlot(EquipmentSlot.HEAD))).isEqualTo(1);
        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, entity.getItemBySlot(EquipmentSlot.CHEST))).isEqualTo(1);
        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, entity.getItemBySlot(EquipmentSlot.LEGS))).isEqualTo(1);
        assertThat(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, entity.getItemBySlot(EquipmentSlot.FEET))).isEqualTo(1);
    }

    @Test
    @DisplayName("isSafeFloorToDigDown rejects 1x1 pillars, lava, and abyss while accepting real floors")
    void isSafeFloorToDigDown_rejectsPillarLavaAbyssAndAcceptsRealFloor() throws Exception {
        Method method = CreepingDeathEntity.class.getDeclaredMethod("isSafeFloorToDigDown", ServerLevel.class, BlockPos.class, ServerPlayer.class);
        method.setAccessible(true);

        CreepingDeathEntity entity = MinecraftTestEnvironment.allocateInstance(CreepingDeathEntity.class);
        ServerPlayer target = mock(ServerPlayer.class);
        when(target.getBlockY()).thenReturn(60);

        BlockPos floorPos = new BlockPos(0, 64, 0);

        Set<BlockPos> pillarBlocks = Set.of(floorPos, new BlockPos(0, 60, 0));
        ServerLevel pillarLevel = createBlockMapLevel(pillarBlocks, Set.of());
        assertThat((boolean) method.invoke(entity, pillarLevel, floorPos, target)).isFalse();

        Set<BlockPos> floorBlocks = new HashSet<>(Set.of(floorPos, floorPos.north(), floorPos.east(), new BlockPos(0, 60, 0)));
        ServerLevel safeLevel = createBlockMapLevel(floorBlocks, Set.of());
        assertThat((boolean) method.invoke(entity, safeLevel, floorPos, target)).isTrue();

        ServerLevel lavaLevel = createBlockMapLevel(floorBlocks, Set.of(new BlockPos(0, 62, 0)));
        assertThat((boolean) method.invoke(entity, lavaLevel, floorPos, target)).isFalse();

        Set<BlockPos> abyssBlocks = Set.of(floorPos, floorPos.north(), floorPos.east());
        ServerLevel abyssLevel = createBlockMapLevel(abyssBlocks, Set.of());
        assertThat((boolean) method.invoke(entity, abyssLevel, floorPos, target)).isFalse();
    }

    @Test
    @DisplayName("shouldBridge activates on drops ahead and ignores solid ground or steep downward chases")
    void shouldBridge_detectsDropsAheadAndAvoidsUnnecessaryBridging() throws Exception {
        Method method = CreepingDeathEntity.class.getDeclaredMethod("shouldBridge", ServerLevel.class, double.class, double.class, double.class, double.class);
        method.setAccessible(true);

        CreepingDeathEntity entity = MinecraftTestEnvironment.allocateInstance(CreepingDeathEntity.class);
        BlockPos feet = new BlockPos(0, 64, 0);
        MinecraftTestEnvironment.setField(entity, "blockPosition", feet);
        MinecraftTestEnvironment.setField(entity, "bridgingTicks", 0);

        ServerLevel gapAheadLevel = createBlockMapLevel(Set.of(feet.below()), Set.of());
        assertThat((boolean) method.invoke(entity, gapAheadLevel, 5.0D, 0.0D, 0.0D, 25.0D)).isTrue();
        assertThat((boolean) method.invoke(entity, gapAheadLevel, 1.0D, 0.0D, 0.0D, 1.0D)).isFalse();
        assertThat((boolean) method.invoke(entity, gapAheadLevel, 5.0D, -4.0D, 0.0D, 25.0D)).isFalse();

        ServerLevel solidAheadLevel = createBlockMapLevel(Set.of(feet.below(), feet.east().below()), Set.of());
        assertThat((boolean) method.invoke(entity, solidAheadLevel, 5.0D, 0.0D, 0.0D, 25.0D)).isFalse();

        MinecraftTestEnvironment.setField(entity, "bridgingTicks", 5);
        assertThat((boolean) method.invoke(entity, solidAheadLevel, 5.0D, 0.0D, 0.0D, 25.0D)).isTrue();
    }

    @Test
    @DisplayName("findBlockingBlock refuses to mine feet.below while bridging or pillaring and skips step-up-able ledges")
    void findBlockingBlock_antiSelfSpleefAndStepUpLogic() throws Exception {
        Method method = CreepingDeathEntity.class.getDeclaredMethod("findBlockingBlock", ServerLevel.class, ServerPlayer.class);
        method.setAccessible(true);

        BlockPos feet = new BlockPos(0, 64, 0);
        BlockPos below = feet.below();

        Set<BlockPos> realFloor = Set.of(below, below.north(), below.east(), new BlockPos(0, 60, 0));
        ServerLevel floorLevel = createBlockMapLevel(realFloor, Set.of());

        CreepingDeathEntity entity = spy(MinecraftTestEnvironment.allocateInstance(CreepingDeathEntity.class));
        MinecraftTestEnvironment.setField(entity, "blockPosition", feet);
        MinecraftTestEnvironment.setField(entity, "position", new Vec3(0.5D, 64.0D, 0.5D));
        doReturn(false).when(entity).hasLineOfSight(any());

        ServerPlayer targetBelow = mock(ServerPlayer.class);
        when(targetBelow.getX()).thenReturn(0.5D);
        when(targetBelow.getY()).thenReturn(60.0D);
        when(targetBelow.getZ()).thenReturn(0.5D);
        when(targetBelow.getBlockY()).thenReturn(60);

        MinecraftTestEnvironment.setField(entity, "bridgingTicks", 6);
        MinecraftTestEnvironment.setField(entity, "pillaringTicks", 0);
        assertThat((BlockPos) method.invoke(entity, floorLevel, targetBelow)).isNull();

        MinecraftTestEnvironment.setField(entity, "bridgingTicks", 0);
        MinecraftTestEnvironment.setField(entity, "pillaringTicks", 6);
        assertThat((BlockPos) method.invoke(entity, floorLevel, targetBelow)).isNull();

        MinecraftTestEnvironment.setField(entity, "pillaringTicks", 0);
        assertThat((BlockPos) method.invoke(entity, floorLevel, targetBelow)).isEqualTo(below);

        BlockPos frontLedge = feet.east();
        ServerLevel ledgeLevel = createBlockMapLevel(Set.of(below, frontLedge, frontLedge.below()), Set.of());
        ServerPlayer targetAhead = mock(ServerPlayer.class);
        when(targetAhead.getX()).thenReturn(4.5D);
        when(targetAhead.getY()).thenReturn(64.0D);
        when(targetAhead.getZ()).thenReturn(0.5D);
        when(targetAhead.getBlockY()).thenReturn(64);

        MinecraftTestEnvironment.setField(entity, "stuckTicks", 0);
        assertThat((BlockPos) method.invoke(entity, ledgeLevel, targetAhead)).isNull();

        MinecraftTestEnvironment.setField(entity, "stuckTicks", 25);
        assertThat((BlockPos) method.invoke(entity, ledgeLevel, targetAhead)).isEqualTo(frontLedge);
    }

    private static ServerLevel createBlockMapLevel(Set<BlockPos> solidPositions, Set<BlockPos> lavaPositions) {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getMinBuildHeight()).thenReturn(-64);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        FluidState lavaFluid = mock(FluidState.class);
        when(lavaFluid.is(FluidTags.LAVA)).thenReturn(true);
        FluidState emptyFluid = Fluids.EMPTY.defaultFluidState();
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(inv -> {
            BlockPos pos = inv.getArgument(0);
            return solidPositions.contains(pos) ? stone : air;
        });
        when(level.getFluidState(any(BlockPos.class))).thenAnswer(inv -> {
            BlockPos pos = inv.getArgument(0);
            return lavaPositions.contains(pos) ? lavaFluid : emptyFluid;
        });
        return level;
    }
}
