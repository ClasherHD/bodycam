package dev.ClasherHD.bodycam.block.entity;

import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AncientTableBlockEntityTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    private AncientTableBlockEntity createEntity() {
        return new AncientTableBlockEntity(BlockPos.ZERO, ModBlocks.ANCIENT_TABLE.get().defaultBlockState());
    }

    @Test
    @DisplayName("Constants should have expected configuration values")
    void constants_haveExpectedValues() {
        assertThat(AncientTableBlockEntity.INPUT_SLOT_COUNT).isEqualTo(3);
        assertThat(AncientTableBlockEntity.OUTPUT_SLOT).isEqualTo(3);
        assertThat(AncientTableBlockEntity.FUEL_SLOT).isEqualTo(4);
        assertThat(AncientTableBlockEntity.SLOT_COUNT).isEqualTo(5);
        assertThat(AncientTableBlockEntity.MAX_FUEL).isEqualTo(5);
        assertThat(AncientTableBlockEntity.DEFAULT_FUEL_PER_CRYSTAL).isEqualTo(112);
        assertThat(AncientTableBlockEntity.ERROR_DISPLAY_TICKS).isEqualTo(40);

        assertThat(AncientTableBlockEntity.DATA_FUEL).isEqualTo(0);
        assertThat(AncientTableBlockEntity.DATA_PROGRESS).isEqualTo(1);
        assertThat(AncientTableBlockEntity.DATA_MAX_PROGRESS).isEqualTo(2);
        assertThat(AncientTableBlockEntity.DATA_ERROR_TICKS).isEqualTo(3);
        assertThat(AncientTableBlockEntity.DATA_COUNT).isEqualTo(4);
    }

    @Test
    @DisplayName("isFuel should return true only for observation crystal")
    void isFuel_evaluatesOnlyObservationCrystal() {
        ItemStack crystal = new ItemStack(ModItems.OBSERVATION_CRYSTAL.get());
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack empty = ItemStack.EMPTY;

        assertThat(AncientTableBlockEntity.isFuel(crystal)).isTrue();
        assertThat(AncientTableBlockEntity.isFuel(stick)).isFalse();
        assertThat(AncientTableBlockEntity.isFuel(empty)).isFalse();
    }

    @Test
    @DisplayName("Fuel slot validity should reject non-crystal items and output slot should reject manual insertion")
    void itemHandlerSlotValidation_evaluatesRules() {
        AncientTableBlockEntity entity = createEntity();
        IItemHandler handler = entity.getItems();

        ItemStack crystal = new ItemStack(ModItems.OBSERVATION_CRYSTAL.get());
        ItemStack stick = new ItemStack(Items.STICK);

        assertThat(handler.isItemValid(AncientTableBlockEntity.FUEL_SLOT, crystal)).isTrue();
        assertThat(handler.isItemValid(AncientTableBlockEntity.FUEL_SLOT, stick)).isFalse();
        assertThat(handler.isItemValid(AncientTableBlockEntity.OUTPUT_SLOT, crystal)).isFalse();
        assertThat(handler.isItemValid(0, stick)).isTrue();
        assertThat(handler.isItemValid(1, stick)).isTrue();
        assertThat(handler.isItemValid(2, stick)).isTrue();
    }

    @ParameterizedTest(name = "fuelUnits={0} -> displayStep={1}")
    @CsvSource({
            "0, 0",
            "-5, 0",
            "1, 1",
            "112, 1",
            "113, 2",
            "224, 2",
            "560, 5",
            "1000, 5"
    })
    @DisplayName("getDisplayFuelStep should map fuelUnits accurately to [0, 5] step display")
    void getDisplayFuelStep_mapsExpectedSteps(int fuelUnits, int expectedStep) {
        AncientTableBlockEntity entity = createEntity();
        MinecraftTestEnvironment.setField(entity, "fuelUnits", fuelUnits);

        assertThat(entity.getDisplayFuelStep()).isEqualTo(expectedStep);
    }

    @Test
    @DisplayName("ContainerData getters and setters should correctly interact with internal fields")
    void containerData_getAndSet_functionsCorrectly() {
        AncientTableBlockEntity entity = createEntity();
        ContainerData data = MinecraftTestEnvironment.getField(entity, "data");

        assertThat(data.getCount()).isEqualTo(4);

        data.set(AncientTableBlockEntity.DATA_FUEL, 3);
        assertThat(data.get(AncientTableBlockEntity.DATA_FUEL)).isEqualTo(3);

        data.set(AncientTableBlockEntity.DATA_PROGRESS, 50);
        assertThat(data.get(AncientTableBlockEntity.DATA_PROGRESS)).isEqualTo(50);

        data.set(AncientTableBlockEntity.DATA_MAX_PROGRESS, 100);
        assertThat(data.get(AncientTableBlockEntity.DATA_MAX_PROGRESS)).isEqualTo(100);

        data.set(AncientTableBlockEntity.DATA_ERROR_TICKS, 25);
        assertThat(data.get(AncientTableBlockEntity.DATA_ERROR_TICKS)).isEqualTo(25);

        assertThat(data.get(999)).isEqualTo(0);
    }

    @Test
    @DisplayName("NBT serialization and deserialization should preserve state across saveAdditional and load")
    void nbtSerialization_roundtrip_preservesState() {
        AncientTableBlockEntity entity = createEntity();
        MinecraftTestEnvironment.setField(entity, "fuelUnits", 250);
        MinecraftTestEnvironment.setField(entity, "progress", 15);
        MinecraftTestEnvironment.setField(entity, "maxProgress", 80);
        MinecraftTestEnvironment.setField(entity, "processType", 2);
        MinecraftTestEnvironment.setField(entity, "activeRepairSlot", 1);
        MinecraftTestEnvironment.setField(entity, "activeRepairAmount", 10);
        MinecraftTestEnvironment.setField(entity, "activeRecipeId", new ResourceLocation("bodycam", "craft_monitor"));
        MinecraftTestEnvironment.setField(entity, "activeFuelCost", 112);

        CompoundTag tag = new CompoundTag();
        entity.saveAdditional(tag);

        AncientTableBlockEntity restored = createEntity();
        restored.load(tag);

        int restoredFuel = MinecraftTestEnvironment.getField(restored, "fuelUnits");
        int restoredProgress = MinecraftTestEnvironment.getField(restored, "progress");
        int restoredMaxProgress = MinecraftTestEnvironment.getField(restored, "maxProgress");
        int restoredProcessType = MinecraftTestEnvironment.getField(restored, "processType");
        int restoredRepairSlot = MinecraftTestEnvironment.getField(restored, "activeRepairSlot");
        int restoredRepairAmount = MinecraftTestEnvironment.getField(restored, "activeRepairAmount");
        ResourceLocation restoredRecipe = MinecraftTestEnvironment.getField(restored, "activeRecipeId");
        int restoredFuelCost = MinecraftTestEnvironment.getField(restored, "activeFuelCost");

        assertThat(restoredFuel).isEqualTo(250);
        assertThat(restoredProgress).isEqualTo(15);
        assertThat(restoredMaxProgress).isEqualTo(80);
        assertThat(restoredProcessType).isEqualTo(2);
        assertThat(restoredRepairSlot).isEqualTo(1);
        assertThat(restoredRepairAmount).isEqualTo(10);
        assertThat(restoredRecipe).isEqualTo(new ResourceLocation("bodycam", "craft_monitor"));
        assertThat(restoredFuelCost).isEqualTo(112);
    }

    @Test
    @DisplayName("itemCapability and getItems should expose valid item handler")
    void itemHandler_exposesValidCapabilities() {
        AncientTableBlockEntity entity = createEntity();
        assertThat(entity.getItems()).isNotNull();
        assertThat(entity.getItems().getSlots()).isEqualTo(AncientTableBlockEntity.SLOT_COUNT);

        net.minecraftforge.common.util.LazyOptional<?> lazyCap = MinecraftTestEnvironment.getField(entity, "itemCapability");
        assertThat(lazyCap.isPresent()).isTrue();
    }

    @Test
    @DisplayName("tryRefuel consumes crystal when space available and ignores when at capacity")
    void tryRefuel_refuelsProperly() throws Exception {
        AncientTableBlockEntity entity = createEntity();
        Method tryRefuel = AncientTableBlockEntity.class.getDeclaredMethod("tryRefuel");
        tryRefuel.setAccessible(true);

        entity.getItems().insertItem(AncientTableBlockEntity.FUEL_SLOT, new ItemStack(ModItems.OBSERVATION_CRYSTAL.get(), 2), false);
        MinecraftTestEnvironment.setField(entity, "fuelUnits", 0);

        tryRefuel.invoke(entity);
        int fuelAfter = MinecraftTestEnvironment.getField(entity, "fuelUnits");
        assertThat(fuelAfter).isEqualTo(112);
        assertThat(entity.getItems().getStackInSlot(AncientTableBlockEntity.FUEL_SLOT).getCount()).isEqualTo(1);

        MinecraftTestEnvironment.setField(entity, "fuelUnits", 560);
        tryRefuel.invoke(entity);
        assertThat(entity.getItems().getStackInSlot(AncientTableBlockEntity.FUEL_SLOT).getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("tryStartRepair sets up repair process for damaged mod item with sufficient fuel")
    void tryStartRepair_configuresProcess() throws Exception {
        AncientTableBlockEntity entity = createEntity();
        Method tryStartRepair = AncientTableBlockEntity.class.getDeclaredMethod("tryStartRepair");
        tryStartRepair.setAccessible(true);

        ItemStack damagedMonitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());
        damagedMonitor.setDamageValue(5);
        entity.getItems().insertItem(0, damagedMonitor, false);
        MinecraftTestEnvironment.setField(entity, "fuelUnits", 500);

        boolean started = (boolean) tryStartRepair.invoke(entity);
        assertThat(started).isTrue();

        int processType = MinecraftTestEnvironment.getField(entity, "processType");
        int repairSlot = MinecraftTestEnvironment.getField(entity, "activeRepairSlot");
        int repairAmount = MinecraftTestEnvironment.getField(entity, "activeRepairAmount");
        assertThat(processType).isEqualTo(1);
        assertThat(repairSlot).isEqualTo(0);
        assertThat(repairAmount).isEqualTo(4);
    }

    @Test
    @DisplayName("cancelProcess resets all process tracking fields to idle state")
    void cancelProcess_resetsFields() throws Exception {
        AncientTableBlockEntity entity = createEntity();
        MinecraftTestEnvironment.setField(entity, "processType", 1);
        MinecraftTestEnvironment.setField(entity, "progress", 10);
        MinecraftTestEnvironment.setField(entity, "maxProgress", 50);
        MinecraftTestEnvironment.setField(entity, "activeRepairSlot", 0);
        MinecraftTestEnvironment.setField(entity, "activeRepairAmount", 5);
        MinecraftTestEnvironment.setField(entity, "activeFuelCost", 112);

        Method cancel = AncientTableBlockEntity.class.getDeclaredMethod("cancelProcess");
        cancel.setAccessible(true);
        cancel.invoke(entity);

        int processType = MinecraftTestEnvironment.getField(entity, "processType");
        int progress = MinecraftTestEnvironment.getField(entity, "progress");
        int maxProgress = MinecraftTestEnvironment.getField(entity, "maxProgress");
        int activeRepairSlot = MinecraftTestEnvironment.getField(entity, "activeRepairSlot");
        int activeFuelCost = MinecraftTestEnvironment.getField(entity, "activeFuelCost");

        assertThat(processType).isEqualTo(0);
        assertThat(progress).isEqualTo(0);
        assertThat(maxProgress).isEqualTo(0);
        assertThat(activeRepairSlot).isEqualTo(-1);
        assertThat(activeFuelCost).isEqualTo(0);
    }

    @Test
    @DisplayName("onCraftButtonPressed sets error ticks when no valid recipe or repair can start")
    void onCraftButtonPressed_setsErrorTicksOnInvalidInput() {
        AncientTableBlockEntity entity = createEntity();
        entity.getItems().insertItem(0, new ItemStack(Items.STICK), false);

        entity.onCraftButtonPressed();

        int errorTicks = MinecraftTestEnvironment.getField(entity, "errorTicks");
        assertThat(errorTicks).isEqualTo(AncientTableBlockEntity.ERROR_DISPLAY_TICKS);
    }
}
