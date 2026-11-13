package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
    void clear_resetsAllMapsAndSessionFlags() {
        UUID id = UUID.randomUUID();
        ClientBodycamCache.update(
                Map.of(id, 1),
                Map.of(id, id),
                Map.of(id, "minecraft:overworld"),
                Map.of(id, BlockPos.ZERO),
                Map.of(id, true),
                Set.of(id),
                Map.of(id, "WatchPoint")
        );
        ClientBodycamCache.REACH_LEVEL = 2;
        ClientBodycamCache.IS_ON_HOLOGRAM = true;
        ClientBodycamCache.IS_ALL_SEEING_EYE = true;

        ClientBodycamCache.clear();

        assertThat(ClientBodycamCache.jammers).isEmpty();
        assertThat(ClientBodycamCache.targets).isEmpty();
        assertThat(ClientBodycamCache.dimensions).isEmpty();
        assertThat(ClientBodycamCache.positions).isEmpty();
        assertThat(ClientBodycamCache.anonymizers).isEmpty();
        assertThat(ClientBodycamCache.hidden).isEmpty();
        assertThat(ClientBodycamCache.watchPoints).isEmpty();
        assertThat(ClientBodycamCache.REACH_LEVEL).isZero();
        assertThat(ClientBodycamCache.IS_ON_HOLOGRAM).isFalse();
        assertThat(ClientBodycamCache.IS_ALL_SEEING_EYE).isFalse();
    }

    @Test
    void update_replacesOldEntriesAndHandlesNullCollectionsSafely() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        Map<UUID, Integer> jammers = new HashMap<>();
        jammers.put(player1, 1);
        Set<UUID> hidden = new HashSet<>();
        hidden.add(player1);

        ClientBodycamCache.update(
                jammers,
                Map.of(player1, player2),
                Map.of(player1, "minecraft:overworld"),
                Map.of(player1, new BlockPos(10, 20, 30)),
                Map.of(player1, true),
                hidden,
                Map.of(player1, "WP1")
        );

        jammers.put(UUID.randomUUID(), 2);
        hidden.clear();
        assertThat(ClientBodycamCache.jammers).hasSize(1).containsEntry(player1, 1);
        assertThat(ClientBodycamCache.hidden).containsExactly(player1);

        ClientBodycamCache.update(
                Map.of(player2, 2),
                Collections.emptyMap(),
                Map.of(player2, "minecraft:the_end"),
                Map.of(player2, new BlockPos(40, 50, 60)),
                Map.of(player2, false),
                Set.of(player2),
                Map.of(player2, "WP2")
        );

        assertThat(ClientBodycamCache.jammers).doesNotContainKey(player1).containsEntry(player2, 2);
        assertThat(ClientBodycamCache.dimensions).doesNotContainKey(player1).containsEntry(player2, "minecraft:the_end");
        assertThat(ClientBodycamCache.positions).doesNotContainKey(player1).containsEntry(player2, new BlockPos(40, 50, 60));
        assertThat(ClientBodycamCache.watchPoints).doesNotContainKey(player1).containsEntry(player2, "WP2");

        ClientBodycamCache.update(null, null, null, null, null, null, null);
        assertThat(ClientBodycamCache.jammers).isEmpty();
        assertThat(ClientBodycamCache.dimensions).isEmpty();
        assertThat(ClientBodycamCache.watchPoints).isEmpty();
    }
}
