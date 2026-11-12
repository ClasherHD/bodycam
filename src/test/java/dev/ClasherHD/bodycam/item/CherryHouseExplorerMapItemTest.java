package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class CherryHouseExplorerMapItemTest {

    private static CherryHouseExplorerMapItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (CherryHouseExplorerMapItem) ModItems.CHERRY_HOUSE_EXPLORER_MAP.get();
    }

    @Test
    @DisplayName("CherryHouseExplorerMapItem should be instantiated and have expected tag keys")
    void structureTag_andConstants_areConfigured() {
        assertThat(CherryHouseExplorerMapItem.CHERRY_HOUSE_TAG.location().toString()).isEqualTo("bodycam:cherry_house");
        assertThat(CherryHouseExplorerMapItem.TAG_TARGET_X).isEqualTo("CherryHouseX");
        assertThat(CherryHouseExplorerMapItem.TAG_TARGET_Z).isEqualTo("CherryHouseZ");
        assertThat(CherryHouseExplorerMapItem.TAG_CENTER_X).isEqualTo("CherryHouseCenterX");
        assertThat(CherryHouseExplorerMapItem.TAG_CENTER_Z).isEqualTo("CherryHouseCenterZ");
        assertThat(CherryHouseExplorerMapItem.TAG_SEARCHED).isEqualTo("CherryHouseSearched");

        ItemStack stack = new ItemStack(item);
        assertThat(stack.getItem()).isInstanceOf(CherryHouseExplorerMapItem.class);
    }
}
