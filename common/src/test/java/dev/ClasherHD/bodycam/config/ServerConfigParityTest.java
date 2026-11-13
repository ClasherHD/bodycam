package dev.ClasherHD.bodycam.config;

import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServerConfigParityTest {

    @AfterEach
    void restoreDefaults() {
        ModServerConfig.MAX_MONITOR_DISTANCE.set(ModServerConfig.MAX_MONITOR_DISTANCE.getDefault());
        ModServerConfig.OP_ONLY_MODE.set(ModServerConfig.OP_ONLY_MODE.getDefault());
        ModServerConfig.BLACKLISTED_DIMENSIONS.set(new ArrayList<>());
    }

    @Test
    void serverConfigDefaults_matchForgeParity() {
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
        assertThat(ModServerConfig.ENABLE_LOGGER.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_ALL_SEEING_EYE.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_CREEPING_DEATH.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_PORTABLE_NETHER_PORTAL.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_SCHOLAR_CURSE.getDefault()).isTrue();
        assertThat(ModServerConfig.ENABLE_CREEPING_DEATH_NUKE.getDefault()).isTrue();

        assertThat(ModServerConfig.SPYGLASS_DURABILITY.getDefault()).isEqualTo(16);
        assertThat(ModServerConfig.DIMENSION_LOCATOR_DURABILITY.getDefault()).isEqualTo(8);
        assertThat(ModServerConfig.PLAYER_LOCATOR_DURABILITY.getDefault()).isEqualTo(560);
        assertThat(ModServerConfig.ANONYMIZER_DURABILITY.getDefault()).isEqualTo(448);
        assertThat(ModServerConfig.JAMMER_DURABILITY.getDefault()).isEqualTo(336);
        assertThat(ModServerConfig.LOGGER_DURABILITY.getDefault()).isEqualTo(8);
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
        assertThat(ModServerConfig.BLACKLISTED_DIMENSIONS.getDefault()).isEmpty();

        ServerConfigSnapshot snapshot = ServerConfigSnapshot.defaults();
        assertThat(snapshot.maxMonitorDistance()).isEqualTo(500);
        assertThat(snapshot.maxWatchPointsPerPlayer()).isEqualTo(5);
        assertThat(snapshot.maxWatchPointsTotal()).isEqualTo(50);
        assertThat(snapshot.opOnlyMode()).isFalse();
    }

    @Test
    void blacklistedDimensions_saveAndLoadRoundtrip(@TempDir Path tempDir) {
        try (MockedStatic<BodycamPlatform> platformMock = Mockito.mockStatic(BodycamPlatform.class)) {
            platformMock.when(BodycamPlatform::getConfigDirectory).thenReturn(tempDir);

            ModServerConfig.MAX_MONITOR_DISTANCE.set(750);
            ModServerConfig.BLACKLISTED_DIMENSIONS.set(List.of("minecraft:the_nether", "  custom:arena  ", ""));
            ModServerConfig.save();

            Path configFile = tempDir.resolve("bodycam-server.json");
            assertThat(Files.exists(configFile)).isTrue();

            ModServerConfig.MAX_MONITOR_DISTANCE.set(1);
            ModServerConfig.BLACKLISTED_DIMENSIONS.set(new ArrayList<>());
            assertThat(ModServerConfig.isDimensionBlacklisted("minecraft:the_nether")).isFalse();

            ModServerConfig.load();

            assertThat(ModServerConfig.isLoaded()).isTrue();
            assertThat(ModServerConfig.MAX_MONITOR_DISTANCE.get()).isEqualTo(750);
            assertThat(ModServerConfig.BLACKLISTED_DIMENSIONS.get()).containsExactly("minecraft:the_nether", "custom:arena");
            assertThat(ModServerConfig.isDimensionBlacklisted("minecraft:the_nether")).isTrue();
            assertThat(ModServerConfig.isDimensionBlacklisted("MINECRAFT:THE_NETHER")).isTrue();
            assertThat(ModServerConfig.isDimensionBlacklisted("custom:arena")).isTrue();
            assertThat(ModServerConfig.isDimensionBlacklisted("minecraft:overworld")).isFalse();
            assertThat(ModServerConfig.isDimensionBlacklisted((String) null)).isFalse();
            assertThat(ModServerConfig.isDimensionBlacklisted("")).isFalse();
            assertThat(ModServerConfig.isDimensionBlacklisted((net.minecraft.world.level.Level) null)).isFalse();
        }
    }
}
