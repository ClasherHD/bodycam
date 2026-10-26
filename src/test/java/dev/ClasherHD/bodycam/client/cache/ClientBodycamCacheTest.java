package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("null")
class ClientBodycamCacheTest {

    @BeforeEach
    void setUp() {
        ClientBodycamCache.clear();
    }

    @AfterEach
    void tearDown() {
        ClientBodycamCache.clear();
    }

    @Test
    @DisplayName("Cache should initially be empty after clear")
    void clear_whenCalled_ensuresAllCacheStructuresAreEmpty() {
        assertThat(ClientBodycamCache.jammers).isEmpty();
        assertThat(ClientBodycamCache.targets).isEmpty();
        assertThat(ClientBodycamCache.dimensions).isEmpty();
        assertThat(ClientBodycamCache.positions).isEmpty();
        assertThat(ClientBodycamCache.anonymizers).isEmpty();
        assertThat(ClientBodycamCache.hidden).isEmpty();
        assertThat(ClientBodycamCache.watchPoints).isEmpty();
    }

    @Test
    @DisplayName("update should populate all cache maps and sets with input data")
    void update_withPopulatedCollections_correctlyPopulatesAllStructures() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        UUID watchPointId = UUID.randomUUID();

        Map<UUID, Integer> jammers = Map.of(playerA, 1, playerB, 2);
        Map<UUID, UUID> targets = Map.of(playerA, playerB);
        Map<UUID, String> dimensions = Map.of(playerA, "minecraft:overworld", playerB, "minecraft:the_nether");
        Map<UUID, BlockPos> positions = Map.of(playerA, new BlockPos(100, 64, -200), playerB, new BlockPos(0, 70, 0));
        Map<UUID, Boolean> anonymizers = Map.of(playerA, true, playerB, false);
        Set<UUID> hidden = Set.of(playerA);
        Map<UUID, String> watchPoints = Map.of(watchPointId, "Alpha Base Camera");

        ClientBodycamCache.update(jammers, targets, dimensions, positions, anonymizers, hidden, watchPoints);

        assertThat(ClientBodycamCache.jammers).containsExactlyInAnyOrderEntriesOf(jammers);
        assertThat(ClientBodycamCache.targets).containsExactlyInAnyOrderEntriesOf(targets);
        assertThat(ClientBodycamCache.dimensions).containsExactlyInAnyOrderEntriesOf(dimensions);
        assertThat(ClientBodycamCache.positions).containsExactlyInAnyOrderEntriesOf(positions);
        assertThat(ClientBodycamCache.anonymizers).containsExactlyInAnyOrderEntriesOf(anonymizers);
        assertThat(ClientBodycamCache.hidden).containsExactlyInAnyOrderElementsOf(hidden);
        assertThat(ClientBodycamCache.watchPoints).containsExactlyInAnyOrderEntriesOf(watchPoints);
    }

    @Test
    @DisplayName("update with empty collections should clear previous state")
    void update_withEmptyCollections_clearsPreviousCacheState() {
        UUID player = UUID.randomUUID();
        ClientBodycamCache.update(
                Map.of(player, 1),
                Map.of(player, player),
                Map.of(player, "minecraft:overworld"),
                Map.of(player, BlockPos.ZERO),
                Map.of(player, true),
                Set.of(player),
                Map.of(player, "Cam")
        );

        assertThat(ClientBodycamCache.jammers).isNotEmpty();

        ClientBodycamCache.update(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap()
        );

        assertThat(ClientBodycamCache.jammers).isEmpty();
        assertThat(ClientBodycamCache.targets).isEmpty();
        assertThat(ClientBodycamCache.dimensions).isEmpty();
        assertThat(ClientBodycamCache.positions).isEmpty();
        assertThat(ClientBodycamCache.anonymizers).isEmpty();
        assertThat(ClientBodycamCache.hidden).isEmpty();
        assertThat(ClientBodycamCache.watchPoints).isEmpty();
    }

    @Test
    @DisplayName("Sequential updates should overwrite previous cache data entirely")
    void update_whenCalledSequentially_replacesOldEntriesWithNewEntries() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        ClientBodycamCache.update(
                Map.of(player1, 1),
                Map.of(player1, player1),
                Map.of(player1, "minecraft:overworld"),
                Map.of(player1, new BlockPos(10, 20, 30)),
                Map.of(player1, true),
                Set.of(player1),
                Map.of(player1, "WP1")
        );

        ClientBodycamCache.update(
                Map.of(player2, 2),
                Map.of(player2, player2),
                Map.of(player2, "minecraft:the_end"),
                Map.of(player2, new BlockPos(40, 50, 60)),
                Map.of(player2, false),
                Set.of(player2),
                Map.of(player2, "WP2")
        );

        assertThat(ClientBodycamCache.jammers).doesNotContainKey(player1).containsEntry(player2, 2);
        assertThat(ClientBodycamCache.targets).doesNotContainKey(player1).containsEntry(player2, player2);
        assertThat(ClientBodycamCache.dimensions).doesNotContainKey(player1).containsEntry(player2, "minecraft:the_end");
        assertThat(ClientBodycamCache.positions).doesNotContainKey(player1).containsEntry(player2, new BlockPos(40, 50, 60));
        assertThat(ClientBodycamCache.anonymizers).doesNotContainKey(player1).containsEntry(player2, false);
        assertThat(ClientBodycamCache.hidden).doesNotContain(player1).contains(player2);
        assertThat(ClientBodycamCache.watchPoints).doesNotContainKey(player1).containsEntry(player2, "WP2");
    }

    @Test
    @DisplayName("External mutation of input collections should not affect cache")
    void update_whenInputCollectionsMutatedAfterwards_cacheRemainsUnaffected() {
        UUID player = UUID.randomUUID();
        Map<UUID, Integer> jammers = new HashMap<>();
        jammers.put(player, 1);
        Set<UUID> hidden = new HashSet<>();
        hidden.add(player);

        ClientBodycamCache.update(
                jammers,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                hidden,
                Collections.emptyMap()
        );

        jammers.put(UUID.randomUUID(), 2);
        hidden.clear();

        assertThat(ClientBodycamCache.jammers).hasSize(1).containsEntry(player, 1);
        assertThat(ClientBodycamCache.hidden).containsExactly(player);
    }

    @Test
    @DisplayName("Boundary values: Integer.MIN_VALUE and MAX_VALUE in BlockPos positions should be retained")
    void update_withExtremeBlockPosCoordinates_storesCoordinatesAccurately() {
        UUID player = UUID.randomUUID();
        BlockPos extremePos = new BlockPos(Integer.MIN_VALUE, -64, Integer.MAX_VALUE);

        ClientBodycamCache.update(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(player, extremePos),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap()
        );

        assertThat(ClientBodycamCache.positions).containsEntry(player, extremePos);
        BlockPos retrieved = ClientBodycamCache.positions.get(player);
        assertThat(retrieved.getX()).isEqualTo(Integer.MIN_VALUE);
        assertThat(retrieved.getY()).isEqualTo(-64);
        assertThat(retrieved.getZ()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("Repeated clear calls should be idempotent")
    void clear_whenCalledRepeatedly_doesNotThrowAndRemainsEmpty() {
        ClientBodycamCache.clear();
        ClientBodycamCache.clear();
        ClientBodycamCache.clear();

        assertThat(ClientBodycamCache.jammers).isEmpty();
        assertThat(ClientBodycamCache.positions).isEmpty();
        assertThat(ClientBodycamCache.hidden).isEmpty();
    }

    @Test
    @DisplayName("update with null parameters should throw NullPointerException")
    void update_withNullParameters_throwsNullPointerException() {
        assertThatThrownBy(() -> ClientBodycamCache.update(
                null,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("update with map entries having null values should be stored correctly")
    void update_withNullMapValues_storesNullValuesInMaps() {
        UUID player = UUID.randomUUID();
        Map<UUID, UUID> targetsWithNull = new HashMap<>();
        targetsWithNull.put(player, null);

        ClientBodycamCache.update(
                Collections.emptyMap(),
                targetsWithNull,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap()
        );

        assertThat(ClientBodycamCache.targets).containsKey(player);
        assertThat(ClientBodycamCache.targets.get(player)).isNull();
    }
}
