package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerLocatorScreenTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("PlayerLocatorScreen constructor sets maps, currentTarget, reachLevel, and title")
    void constructor_setsFieldsCorrectly() throws Exception {
        UUID targetId = UUID.randomUUID();
        Map<UUID, Integer> jammers = new HashMap<>();
        jammers.put(targetId, 1);

        Map<UUID, String> dimensions = new HashMap<>();
        dimensions.put(targetId, "minecraft:overworld");

        Map<UUID, BlockPos> positions = new HashMap<>();
        positions.put(targetId, new BlockPos(10, 20, 30));

        PlayerLocatorScreen screen = new PlayerLocatorScreen(jammers, dimensions, positions, targetId, 2);

        assertThat(screen.getTitle()).isEqualTo(Component.translatable("gui.bodycam.player_locator"));
        assertThat(screen.isPauseScreen()).isFalse();

        Field reachField = PlayerLocatorScreen.class.getDeclaredField("reachLevel");
        reachField.setAccessible(true);
        assertThat(reachField.getInt(screen)).isEqualTo(2);

        Field targetField = PlayerLocatorScreen.class.getDeclaredField("currentTarget");
        targetField.setAccessible(true);
        assertThat(targetField.get(screen)).isEqualTo(targetId);

        Field jamField = PlayerLocatorScreen.class.getDeclaredField("jammers");
        jamField.setAccessible(true);
        assertThat(jamField.get(screen)).isEqualTo(jammers);
    }

    @Test
    @DisplayName("PlayerLocatorScreen handles null currentTarget gracefully")
    void constructor_handlesNullTarget() throws Exception {
        PlayerLocatorScreen screen = new PlayerLocatorScreen(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null, 0);

        Field targetField = PlayerLocatorScreen.class.getDeclaredField("currentTarget");
        targetField.setAccessible(true);
        assertThat(targetField.get(screen)).isNull();
    }

    @Test
    @DisplayName("PlayerEntry creates narration correctly")
    void playerEntry_narration() {
        PlayerLocatorScreen screen = new PlayerLocatorScreen(Map.of(), Map.of(), Map.of(), null, 0);
        PlayerLocatorScreen.PlayerEntry entry = screen.new PlayerEntry(null, "TargetPlayer", false);

        assertThat(entry.getNarration().getString()).isEqualTo("TargetPlayer");
    }

    @Test
    @DisplayName("WINDOW_TEXTURE has correct ResourceLocation")
    void windowTexture_hasExpectedPath() throws Exception {
        Field texField = PlayerLocatorScreen.class.getDeclaredField("WINDOW_TEXTURE");
        texField.setAccessible(true);
        ResourceLocation loc = (ResourceLocation) texField.get(null);
        assertThat(loc).isEqualTo(new ResourceLocation("bodycam", "textures/gui/gui_window.png"));
    }
}
