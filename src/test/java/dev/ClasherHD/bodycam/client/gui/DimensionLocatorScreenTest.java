package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DimensionLocatorScreenTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("DimensionLocatorScreen constructor sets dimensions and title")
    void constructor_setsFieldsCorrectly() throws Exception {
        Map<UUID, String> map = new HashMap<>();
        UUID id = UUID.randomUUID();
        map.put(id, "minecraft:the_nether");

        DimensionLocatorScreen screen = new DimensionLocatorScreen(map);

        assertThat(screen.getTitle()).isEqualTo(Component.translatable("gui.bodycam.dimension_locator"));
        assertThat(screen.isPauseScreen()).isFalse();

        Field dimField = DimensionLocatorScreen.class.getDeclaredField("dimensions");
        dimField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, String> stored = (Map<UUID, String>) dimField.get(screen);
        assertThat(stored).containsEntry(id, "minecraft:the_nether");
    }

    @Test
    @DisplayName("DimensionEntry creates narration and handles clicks")
    void dimensionEntry_methods() {
        DimensionLocatorScreen screen = new DimensionLocatorScreen(Map.of());
        DimensionLocatorScreen.DimensionEntry entry = screen.new DimensionEntry(null, "PlayerOne", "minecraft:the_nether");

        assertThat(entry.getNarration().getString()).isEqualTo("PlayerOne in minecraft:the_nether");
        assertThat(entry.mouseClicked(0, 0, 0)).isFalse();
    }

    @Test
    @DisplayName("WINDOW_TEXTURE has correct ResourceLocation")
    void windowTexture_hasExpectedPath() throws Exception {
        Field texField = DimensionLocatorScreen.class.getDeclaredField("WINDOW_TEXTURE");
        texField.setAccessible(true);
        ResourceLocation loc = (ResourceLocation) texField.get(null);
        assertThat(loc).isEqualTo(new ResourceLocation("bodycam", "textures/gui/gui_window.png"));
    }
}
