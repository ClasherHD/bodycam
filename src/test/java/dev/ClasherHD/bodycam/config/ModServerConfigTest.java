package dev.ClasherHD.bodycam.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;

class ModServerConfigTest {

    @Test
    @DisplayName("isDimensionBlacklisted with null Level should return false")
    void isDimensionBlacklisted_withNullLevel_returnsFalse() {
        assertThat(ModServerConfig.isDimensionBlacklisted((net.minecraft.world.level.Level) null)).isFalse();
    }

    @Test
    @DisplayName("isDimensionBlacklisted with null or empty dimension string should return false")
    void isDimensionBlacklisted_withNullOrEmptyDimensionId_returnsFalse() {
        assertThat(ModServerConfig.isDimensionBlacklisted((String) null)).isFalse();
        assertThat(ModServerConfig.isDimensionBlacklisted("")).isFalse();
    }

    @Test
    @DisplayName("isDimensionBlacklisted should return false when SPEC is not loaded")
    void isDimensionBlacklisted_whenSpecNotLoaded_returnsFalse() {
        MinecraftTestEnvironment.setField(ModServerConfig.SPEC, "childConfig", null);
        assertThat(ModServerConfig.SPEC.isLoaded()).isFalse();
        assertThat(ModServerConfig.isDimensionBlacklisted("minecraft:the_nether")).isFalse();
        assertThat(ModServerConfig.isDimensionBlacklisted("minecraft:the_end")).isFalse();
    }

    @Test
    @DisplayName("isDimensionBlacklisted should check blacklisted dimensions when SPEC is loaded")
    void isDimensionBlacklisted_whenSpecLoaded_checksConfig() {
        com.electronwill.nightconfig.core.CommentedConfig config = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        config.set("dimension_blacklist.blacklistedDimensions", java.util.List.of("minecraft:the_nether"));
        ModServerConfig.SPEC.acceptConfig(config);
        assertThat(ModServerConfig.SPEC.isLoaded()).isTrue();
        assertThat(ModServerConfig.isDimensionBlacklisted("minecraft:the_nether")).isTrue();
        assertThat(ModServerConfig.isDimensionBlacklisted("minecraft:overworld")).isFalse();
    }

    @Test
    @DisplayName("setActiveModConfig and reloadFromDisk should execute safely when null")
    void configLifecycle_whenActiveConfigIsNull_executesSafely() {
        ModServerConfig.setActiveModConfig(null);
        boolean reloaded = ModServerConfig.reloadFromDisk();
        assertThat(reloaded).isTrue();
    }

    @Test
    @DisplayName("Server config spec builder should define expected default values")
    void configSpec_hasExpectedDefaultValues() {
        assertThat(ModServerConfig.OP_ONLY_MODE.getDefault()).isFalse();
        assertThat(ModServerConfig.MAX_MONITOR_DISTANCE.getDefault()).isEqualTo(500);
        assertThat(ModServerConfig.ENABLE_REACH_ENCHANTMENT.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_JAMMER.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_DIMENSION_LOCATOR.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_HOLOGRAM_BLOCK.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_ANONYMIZER.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_PLAYER_LOCATOR.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_WATCH_POINT.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_PODEST.getDefault()).isTrue();

        assertThat(ModServerConfig.SPYGLASS_DURABILITY.getDefault()).isEqualTo(16);
        assertThat(ModServerConfig.DIMENSION_LOCATOR_DURABILITY.getDefault()).isEqualTo(8);
        assertThat(ModServerConfig.PLAYER_LOCATOR_DURABILITY.getDefault()).isEqualTo(560);
        assertThat(ModServerConfig.ANONYMIZER_DURABILITY.getDefault()).isEqualTo(448);
        assertThat(ModServerConfig.JAMMER_DURABILITY.getDefault()).isEqualTo(336);
        assertThat(ModServerConfig.ANCIENT_TABLE_FUEL_PER_CRYSTAL.getDefault()).isEqualTo(112);
        assertThat(ModServerConfig.ANCIENT_TABLE_REPAIR_SECONDS_PER_CRYSTAL.getDefault()).isEqualTo(28);
        assertThat(ModServerConfig.ANCIENT_TABLE_CRAFT_SECONDS_PER_ITEM.getDefault()).isEqualTo(10);

        assertThat(ModServerConfig.MAX_WATCH_POINTS_PER_PLAYER.getDefault()).isEqualTo(5);
        assertThat(ModServerConfig.MAX_WATCH_POINTS_TOTAL.getDefault()).isEqualTo(50);

        assertThat(ModServerConfig.PODEST_BLOCKS_PER_CHARGE.getDefault()).isEqualTo(10.0D);
        assertThat(ModServerConfig.PODEST_JAMMER_NORMAL_INTERVAL_TICKS.getDefault()).isEqualTo(20);
        assertThat(ModServerConfig.PODEST_JAMMER_LIMITED_INTERVAL_TICKS.getDefault()).isEqualTo(40);
        assertThat(ModServerConfig.PODEST_ANONYMIZER_INTERVAL_TICKS.getDefault()).isEqualTo(20);

        assertThat(ModServerConfig.PORTAL_SEARCH_CHUNK_RADIUS.getDefault()).isEqualTo(8);
        assertThat(ModServerConfig.LOCATOR_SCAN_INTERVAL_TICKS.getDefault()).isEqualTo(50);
        assertThat(ModServerConfig.PODEST_SPHERE_CHECK_INTERVAL_TICKS.getDefault()).isEqualTo(1);
        assertThat(ModServerConfig.JAMMER_NORMAL_INTERVAL_TICKS.getDefault()).isEqualTo(20);
        assertThat(ModServerConfig.JAMMER_LIMITED_INTERVAL_TICKS.getDefault()).isEqualTo(40);
        assertThat(ModServerConfig.ANONYMIZER_INTERVAL_TICKS.getDefault()).isEqualTo(20);
        assertThat(ModServerConfig.PLAYER_LOCATOR_DURABILITY_INTERVAL_TICKS.getDefault()).isEqualTo(20);
    }
}
