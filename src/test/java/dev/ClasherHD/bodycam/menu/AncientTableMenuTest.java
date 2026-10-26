package dev.ClasherHD.bodycam.menu;

import dev.ClasherHD.bodycam.block.entity.AncientTableBlockEntity;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AncientTableMenuTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    private AncientTableMenu createMenu(SimpleContainerData data) {
        Inventory inv = new Inventory(null);
        return new AncientTableMenu(1, inv, null, data);
    }

    @Test
    @DisplayName("AncientTableMenu slot layout should have 41 total slots with expected table and player slot partitions")
    void slotLayout_hasExpectedPartitions() {
        SimpleContainerData data = new SimpleContainerData(AncientTableBlockEntity.DATA_COUNT);
        AncientTableMenu menu = createMenu(data);

        assertThat(menu.slots).hasSize(41);

        Slot outputSlot = menu.getSlot(AncientTableBlockEntity.OUTPUT_SLOT);
        Slot fuelSlot = menu.getSlot(AncientTableBlockEntity.FUEL_SLOT);

        assertThat(outputSlot.mayPlace(new ItemStack(Items.DIAMOND))).isFalse();
        assertThat(outputSlot.mayPlace(new ItemStack(ModItems.OBSERVATION_CRYSTAL.get()))).isFalse();

        assertThat(fuelSlot.mayPlace(new ItemStack(Items.DIAMOND))).isFalse();
        assertThat(fuelSlot.mayPlace(new ItemStack(ModItems.OBSERVATION_CRYSTAL.get()))).isTrue();
    }

    @ParameterizedTest(name = "progress={0}, max={1} -> expectedRatio={2}")
    @CsvSource({
            "0, 0, 0.0",
            "0, 100, 0.0",
            "50, 100, 0.5",
            "100, 100, 1.0",
            "150, 100, 1.0",
            "-10, 100, -0.1"
    })
    @DisplayName("getProgressRatio should compute clamped ratio or 0.0 when max <= 0")
    void getProgressRatio_computesExpectedRatio(int progress, int maxProgress, float expectedRatio) {
        SimpleContainerData data = new SimpleContainerData(AncientTableBlockEntity.DATA_COUNT);
        data.set(AncientTableBlockEntity.DATA_PROGRESS, progress);
        data.set(AncientTableBlockEntity.DATA_MAX_PROGRESS, maxProgress);

        AncientTableMenu menu = createMenu(data);

        assertThat(menu.getProgressRatio()).isEqualTo(expectedRatio);
    }

    @Test
    @DisplayName("getFuel should reflect DATA_FUEL from ContainerData")
    void getFuel_reflectsDataFuel() {
        SimpleContainerData data = new SimpleContainerData(AncientTableBlockEntity.DATA_COUNT);
        data.set(AncientTableBlockEntity.DATA_FUEL, 4);

        AncientTableMenu menu = createMenu(data);

        assertThat(menu.getFuel()).isEqualTo(4);
    }

    @Test
    @DisplayName("isShowingError should be true when errorTicks > 0 and false otherwise")
    void isShowingError_evaluatesErrorTicks() {
        SimpleContainerData data = new SimpleContainerData(AncientTableBlockEntity.DATA_COUNT);
        AncientTableMenu menu = createMenu(data);

        assertThat(menu.isShowingError()).isFalse();

        data.set(AncientTableBlockEntity.DATA_ERROR_TICKS, 20);
        assertThat(menu.isShowingError()).isTrue();

        data.set(AncientTableBlockEntity.DATA_ERROR_TICKS, 0);
        assertThat(menu.isShowingError()).isFalse();
    }

    @Test
    @DisplayName("clickMenuButton should return false when table is null or button is invalid")
    void clickMenuButton_withNullTable_returnsFalse() {
        SimpleContainerData data = new SimpleContainerData(AncientTableBlockEntity.DATA_COUNT);
        AncientTableMenu menu = createMenu(data);

        assertThat(menu.clickMenuButton(null, AncientTableMenu.BUTTON_CRAFT)).isFalse();
        assertThat(menu.clickMenuButton(null, 99)).isFalse();
    }

    @Test
    @DisplayName("quickMoveStack on empty slot should return ItemStack.EMPTY safely")
    void quickMoveStack_emptySlot_returnsEmpty() {
        SimpleContainerData data = new SimpleContainerData(AncientTableBlockEntity.DATA_COUNT);
        AncientTableMenu menu = createMenu(data);

        ItemStack result = menu.quickMoveStack(null, 0);
        assertThat(result.isEmpty()).isTrue();
    }
}
