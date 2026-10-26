package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.block.entity.AncientTableBlockEntity;
import dev.ClasherHD.bodycam.menu.AncientTableMenu;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class AncientTableScreenTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("AncientTableScreen has correct texture and rendering constants")
    void constants_haveExpectedValues() throws Exception {
        Field texField = AncientTableScreen.class.getDeclaredField("TEXTURE");
        texField.setAccessible(true);
        ResourceLocation tex = (ResourceLocation) texField.get(null);
        assertThat(tex).isEqualTo(new ResourceLocation("bodycam", "textures/gui/container/ancient_table_gui.png"));

        Field errW = AncientTableScreen.class.getDeclaredField("ERROR_ARROW_WIDTH");
        errW.setAccessible(true);
        assertThat(errW.getInt(null)).isEqualTo(28);

        Field progW = AncientTableScreen.class.getDeclaredField("PROGRESS_ARROW_WIDTH");
        progW.setAccessible(true);
        assertThat(progW.getInt(null)).isEqualTo(28);

        Field fuelW = AncientTableScreen.class.getDeclaredField("FUEL_BAR_WIDTH");
        fuelW.setAccessible(true);
        assertThat(fuelW.getInt(null)).isEqualTo(18);
    }

    @Test
    @DisplayName("AncientTableScreen constructor sets dimensions and title label positions")
    void constructor_setsDimensionsAndLabels() {
        net.minecraft.server.level.ServerPlayer player = MinecraftTestEnvironment.allocateInstance(net.minecraft.server.level.ServerPlayer.class);
        Inventory inventory = new Inventory(player);
        net.minecraft.world.inventory.SimpleContainerData data = new net.minecraft.world.inventory.SimpleContainerData(AncientTableBlockEntity.DATA_COUNT);
        AncientTableMenu menu = new AncientTableMenu(1, inventory, null, data);

        AncientTableScreen screen = new AncientTableScreen(menu, inventory, Component.literal("Ancient Table"));

        assertThat(screen.getXSize()).isEqualTo(176);
        assertThat(screen.getYSize()).isEqualTo(166);
        assertThat(screen.getTitle()).isEqualTo(Component.literal("Ancient Table"));
    }
}
