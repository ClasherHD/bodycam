package dev.ClasherHD.bodycam.block.entity;

import dev.ClasherHD.bodycam.block.PodestBlock;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class PodestBlockEntityTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        com.electronwill.nightconfig.core.CommentedConfig serverConfig = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        ModServerConfig.SPEC.acceptConfig(serverConfig);
    }

    private PodestBlockEntity createEntity() {
        return new PodestBlockEntity(BlockPos.ZERO, ModBlocks.PODEST.get().defaultBlockState());
    }

    @Test
    @DisplayName("Constants and initial state should match specifications")
    void initialState_matchesSpecs() {
        PodestBlockEntity entity = createEntity();

        assertThat(PodestBlockEntity.MAX_CHARGES).isEqualTo(10);
        assertThat(PodestBlockEntity.BLOCKS_PER_CHARGE).isEqualTo(10.0D);
        assertThat(entity.getCharges()).isEqualTo(0);
        assertThat(entity.getSphericalRadius()).isEqualTo(0.0D);
        assertThat(entity.getHeldItem().isEmpty()).isTrue();
        assertThat(entity.getEffectiveJammerMode()).isEqualTo(0);
    }

    @ParameterizedTest(name = "charges={0} -> sphericalRadius={1}")
    @CsvSource({
            "0, 0.0",
            "1, 10.0",
            "5, 50.0",
            "10, 100.0"
    })
    @DisplayName("getSphericalRadius should scale proportionally with charges")
    void getSphericalRadius_scalesWithCharges(int charges, double expectedRadius) {
        PodestBlockEntity entity = createEntity();
        MinecraftTestEnvironment.setField(entity, "charges", charges);

        assertThat(entity.getSphericalRadius()).isEqualTo(expectedRadius);
    }

    @Test
    @DisplayName("Setting null item should safely convert to ItemStack.EMPTY")
    void setHeldItem_withNull_setsEmptyStack() {
        PodestBlockEntity entity = createEntity();
        entity.setHeldItem(null);

        assertThat(entity.getHeldItem()).isEqualTo(ItemStack.EMPTY);
    }

    @Test
    @DisplayName("Setting JammerItem with mode 0 should automatically initialize JammerMode to 1")
    void setHeldItem_withJammerMode0_promotesToMode1() {
        PodestBlockEntity entity = createEntity();
        ItemStack jammer = new ItemStack(ModItems.JAMMER.get());

        entity.setHeldItem(jammer);

        assertThat(entity.getHeldItem().getTag().getInt("JammerMode")).isEqualTo(1);
        assertThat(entity.getEffectiveJammerMode()).isEqualTo(1);
    }

    @Test
    @DisplayName("Setting JammerItem with mode 2 should retain limited jammer mode")
    void setHeldItem_withJammerMode2_retainsMode2() {
        PodestBlockEntity entity = createEntity();
        ItemStack jammer = new ItemStack(ModItems.JAMMER.get());
        jammer.getOrCreateTag().putInt("JammerMode", 2);

        entity.setHeldItem(jammer);

        assertThat(entity.getHeldItem().getTag().getInt("JammerMode")).isEqualTo(2);
        assertThat(entity.getEffectiveJammerMode()).isEqualTo(2);
    }

    @Test
    @DisplayName("removeHeldItem should reset AnonymizerActive and clear active_id on anonymizer")
    void removeHeldItem_cleansAnonymizerTags() {
        PodestBlockEntity entity = createEntity();
        ItemStack anonymizer = new ItemStack(ModItems.ANONYMIZER.get());
        anonymizer.getOrCreateTag().putBoolean("AnonymizerActive", true);
        anonymizer.getOrCreateTag().putUUID("active_id", UUID.randomUUID());

        entity.setHeldItem(anonymizer);
        ItemStack removed = entity.removeHeldItem();

        assertThat(entity.getHeldItem().isEmpty()).isTrue();
        assertThat(removed.getTag().getBoolean("AnonymizerActive")).isFalse();
        assertThat(removed.getTag().contains("active_id")).isFalse();
    }

    @Test
    @DisplayName("cleanPlacedItemTags resets Jammer mode 1 to 0 but preserves mode 2")
    void cleanPlacedItemTags_preservesLimitedMode() throws Exception {
        Method cleanMethod = PodestBlockEntity.class.getDeclaredMethod("cleanPlacedItemTags", ItemStack.class);
        cleanMethod.setAccessible(true);

        ItemStack normalJammer = new ItemStack(ModItems.JAMMER.get());
        normalJammer.getOrCreateTag().putInt("JammerMode", 1);
        normalJammer.getOrCreateTag().putUUID("active_id", UUID.randomUUID());
        cleanMethod.invoke(null, normalJammer);
        assertThat(normalJammer.getTag().getInt("JammerMode")).isEqualTo(0);
        assertThat(normalJammer.getTag().contains("active_id")).isFalse();

        ItemStack limitedJammer = new ItemStack(ModItems.JAMMER.get());
        limitedJammer.getOrCreateTag().putInt("JammerMode", 2);
        limitedJammer.getOrCreateTag().putUUID("active_id", UUID.randomUUID());
        cleanMethod.invoke(null, limitedJammer);
        assertThat(limitedJammer.getTag().getInt("JammerMode")).isEqualTo(2);
        assertThat(limitedJammer.getTag().contains("active_id")).isFalse();
    }

    @Test
    @DisplayName("addCharge increments charges and updates blockstate until max charges")
    void addCharge_incrementsAndClampsAtMax() {
        PodestBlockEntity entity = createEntity();
        Level level = mock(Level.class);
        BlockPos pos = BlockPos.ZERO;
        BlockState state = ModBlocks.PODEST.get().defaultBlockState();
        when(level.hasNeighborSignal(pos)).thenReturn(true);

        entity.addCharge(level, pos, state);
        assertThat(entity.getCharges()).isEqualTo(1);
        verify(level).setBlock(eq(pos), any(BlockState.class), eq(3));

        MinecraftTestEnvironment.setField(entity, "charges", 10);
        entity.addCharge(level, pos, state);
        assertThat(entity.getCharges()).isEqualTo(10);
    }

    @Test
    @DisplayName("getUpdateTag and getUpdatePacket contain charges and held item")
    void updateTagAndPacket_serializeCorrectly() {
        PodestBlockEntity entity = createEntity();
        MinecraftTestEnvironment.setField(entity, "charges", 4);
        ItemStack jammer = new ItemStack(ModItems.JAMMER.get());
        jammer.getOrCreateTag().putInt("JammerMode", 1);
        MinecraftTestEnvironment.setField(entity, "heldItem", jammer);

        CompoundTag tag = entity.getUpdateTag();
        assertThat(tag.getInt("Charges")).isEqualTo(4);
        assertThat(tag.contains("HeldItem")).isTrue();

        ClientboundBlockEntityDataPacket packet = (ClientboundBlockEntityDataPacket) entity.getUpdatePacket();
        assertThat(packet).isNotNull();
        assertThat(packet.getTag().getInt("Charges")).isEqualTo(4);
    }

    @Test
    @DisplayName("isPodestActive returns false when level is null, unpowered, or charges zero")
    void isPodestActive_evaluatesConditions() {
        PodestBlockEntity entity = createEntity();
        assertThat(entity.isPodestActive()).isFalse();

        Level level = mock(Level.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        MinecraftTestEnvironment.setField(entity, "level", level);
        assertThat(entity.isPodestActive()).isFalse();

        BlockState poweredState = ModBlocks.PODEST.get().defaultBlockState()
                .setValue(PodestBlock.POWERED, true)
                .setValue(PodestBlock.CHARGES, 5);
        PodestBlockEntity poweredEntity = new PodestBlockEntity(BlockPos.ZERO, poweredState);
        MinecraftTestEnvironment.setField(poweredEntity, "level", level);
        MinecraftTestEnvironment.setField(poweredEntity, "charges", 5);
        ItemStack jammer = new ItemStack(ModItems.JAMMER.get());
        poweredEntity.setHeldItem(jammer);

        assertThat(poweredEntity.isPodestActive()).isTrue();
    }

    @Test
    @DisplayName("NBT serialization and deserialization should preserve charges and heldItem")
    void nbtRoundtrip_preservesState() {
        PodestBlockEntity entity = createEntity();
        MinecraftTestEnvironment.setField(entity, "charges", 7);
        ItemStack jammer = new ItemStack(ModItems.JAMMER.get());
        jammer.getOrCreateTag().putInt("JammerMode", 2);
        MinecraftTestEnvironment.setField(entity, "heldItem", jammer);

        CompoundTag tag = new CompoundTag();
        entity.saveAdditional(tag);

        PodestBlockEntity restored = createEntity();
        restored.load(tag);

        assertThat(restored.getCharges()).isEqualTo(7);
        assertThat(restored.getHeldItem().getItem()).isEqualTo(ModItems.JAMMER.get());
        assertThat(restored.getHeldItem().getTag().getInt("JammerMode")).isEqualTo(2);
    }

    @Test
    @DisplayName("NBT loading should clamp out-of-range charges between 0 and 10")
    void nbtLoading_clampsCharges() {
        PodestBlockEntity restoredNegative = createEntity();
        CompoundTag negativeTag = new CompoundTag();
        negativeTag.putInt("Charges", -50);
        restoredNegative.load(negativeTag);
        assertThat(restoredNegative.getCharges()).isEqualTo(0);

        PodestBlockEntity restoredExcessive = createEntity();
        CompoundTag excessiveTag = new CompoundTag();
        excessiveTag.putInt("Charges", 999);
        restoredExcessive.load(excessiveTag);
        assertThat(restoredExcessive.getCharges()).isEqualTo(10);
    }
}
