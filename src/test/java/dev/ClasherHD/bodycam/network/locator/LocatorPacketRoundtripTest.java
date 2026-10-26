package dev.ClasherHD.bodycam.network.locator;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocatorPacketRoundtripTest {

    @Test
    @DisplayName("DimensionLocatorResponsePacket should encode and decode dimension map")
    void dimensionLocatorResponsePacket_roundtrip_preservesMapEntries() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Map<UUID, String> dimensions = Map.of(id1, "minecraft:overworld", id2, "minecraft:the_nether");

        DimensionLocatorResponsePacket original = new DimensionLocatorResponsePacket(dimensions);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        DimensionLocatorResponsePacket.encode(original, buf);
        DimensionLocatorResponsePacket decoded = DimensionLocatorResponsePacket.decode(buf);

        assertThat(decoded.dimensions).containsExactlyInAnyOrderEntriesOf(dimensions);
    }

    @Test
    @DisplayName("DimensionLocatorResponsePacket should handle empty dimension map")
    void dimensionLocatorResponsePacket_roundtrip_withEmptyMap() {
        DimensionLocatorResponsePacket original = new DimensionLocatorResponsePacket(Collections.emptyMap());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        DimensionLocatorResponsePacket.encode(original, buf);
        DimensionLocatorResponsePacket decoded = DimensionLocatorResponsePacket.decode(buf);

        assertThat(decoded.dimensions).isEmpty();
    }

    @Test
    @DisplayName("PlayerLocatorSelectC2SPacket should encode and decode target UUID and deselect flag (via reflection)")
    void playerLocatorSelectC2SPacket_roundtrip_withTargetUUID() throws Exception {
        UUID targetId = UUID.randomUUID();
        PlayerLocatorSelectC2SPacket original = new PlayerLocatorSelectC2SPacket(targetId, false);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PlayerLocatorSelectC2SPacket.encode(original, buf);
        PlayerLocatorSelectC2SPacket decoded = PlayerLocatorSelectC2SPacket.decode(buf);

        Field targetField = PlayerLocatorSelectC2SPacket.class.getDeclaredField("targetUUID");
        targetField.setAccessible(true);
        UUID actualTarget = (UUID) targetField.get(decoded);

        Field deselectField = PlayerLocatorSelectC2SPacket.class.getDeclaredField("isDeselect");
        deselectField.setAccessible(true);
        boolean actualDeselect = (boolean) deselectField.get(decoded);

        assertThat(actualTarget).isEqualTo(targetId);
        assertThat(actualDeselect).isFalse();
    }

    @Test
    @DisplayName("PlayerLocatorSelectC2SPacket should encode and decode null target UUID and true deselect flag")
    void playerLocatorSelectC2SPacket_roundtrip_withNullTargetAndDeselectTrue() throws Exception {
        PlayerLocatorSelectC2SPacket original = new PlayerLocatorSelectC2SPacket(null, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PlayerLocatorSelectC2SPacket.encode(original, buf);
        PlayerLocatorSelectC2SPacket decoded = PlayerLocatorSelectC2SPacket.decode(buf);

        Field targetField = PlayerLocatorSelectC2SPacket.class.getDeclaredField("targetUUID");
        targetField.setAccessible(true);
        UUID actualTarget = (UUID) targetField.get(decoded);

        Field deselectField = PlayerLocatorSelectC2SPacket.class.getDeclaredField("isDeselect");
        deselectField.setAccessible(true);
        boolean actualDeselect = (boolean) deselectField.get(decoded);

        assertThat(actualTarget).isNull();
        assertThat(actualDeselect).isTrue();
    }

    @Test
    @DisplayName("PlayerLocatorSelectC2SPacket cooldown methods should remove specific player and clear all entries")
    void playerLocatorSelectC2SPacket_cooldownMethods_mutateCooldownMapCorrectly() throws Exception {
        Field cooldownField = PlayerLocatorSelectC2SPacket.class.getDeclaredField("LAST_SELECT_TICK");
        cooldownField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Long> cooldownMap = (Map<UUID, Long>) cooldownField.get(null);

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        cooldownMap.put(player1, 100L);
        cooldownMap.put(player2, 200L);

        PlayerLocatorSelectC2SPacket.clearCooldown(player1);
        assertThat(cooldownMap).doesNotContainKey(player1).containsKey(player2);

        PlayerLocatorSelectC2SPacket.clearCooldown(null);
        assertThat(cooldownMap).containsKey(player2);

        PlayerLocatorSelectC2SPacket.clearCooldowns();
        assertThat(cooldownMap).isEmpty();
    }

    @Test
    @DisplayName("PlayerLocatorStructureUpdateS2CPacket should encode and decode structure position and dimension")
    void playerLocatorStructureUpdateS2CPacket_roundtrip_preservesFields() {
        BlockPos pos = new BlockPos(1024, -32, 2048);
        String dimension = "minecraft:the_nether";

        PlayerLocatorStructureUpdateS2CPacket original = new PlayerLocatorStructureUpdateS2CPacket(pos, dimension);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PlayerLocatorStructureUpdateS2CPacket.encode(original, buf);
        PlayerLocatorStructureUpdateS2CPacket decoded = PlayerLocatorStructureUpdateS2CPacket.decode(buf);

        assertThat(decoded.structurePos).isEqualTo(pos);
        assertThat(decoded.dimension).isEqualTo(dimension);
    }

    @Test
    @DisplayName("PlayerLocatorStructureUpdateS2CPacket should handle boundary coordinates and custom dimension")
    void playerLocatorStructureUpdateS2CPacket_roundtrip_withBoundaryValues() {
        BlockPos borderPos = new BlockPos(30_000_000, 320, -30_000_000);
        String customDim = "custom_mod:deep_dimension";

        PlayerLocatorStructureUpdateS2CPacket original = new PlayerLocatorStructureUpdateS2CPacket(borderPos, customDim);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PlayerLocatorStructureUpdateS2CPacket.encode(original, buf);
        PlayerLocatorStructureUpdateS2CPacket decoded = PlayerLocatorStructureUpdateS2CPacket.decode(buf);

        assertThat(decoded.structurePos.getX()).isEqualTo(30_000_000);
        assertThat(decoded.structurePos.getY()).isEqualTo(320);
        assertThat(decoded.structurePos.getZ()).isEqualTo(-30_000_000);
        assertThat(decoded.dimension).isEqualTo(customDim);
    }

    @Test
    @DisplayName("PlayerLocatorStructureUpdateS2CPacket demonstrates Minecraft network protocol 26-bit BlockPos bit-packing truncation")
    void playerLocatorStructureUpdateS2CPacket_roundtrip_demonstratesProtocolTruncation() {
        BlockPos outOfRangePos = new BlockPos(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);
        PlayerLocatorStructureUpdateS2CPacket original = new PlayerLocatorStructureUpdateS2CPacket(outOfRangePos, "minecraft:overworld");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PlayerLocatorStructureUpdateS2CPacket.encode(original, buf);
        PlayerLocatorStructureUpdateS2CPacket decoded = PlayerLocatorStructureUpdateS2CPacket.decode(buf);

        assertThat(decoded.structurePos.getX()).isEqualTo(0);
        assertThat(decoded.structurePos.getY()).isEqualTo(0);
        assertThat(decoded.structurePos.getZ()).isEqualTo(-1);
    }

    @Test
    @DisplayName("PlayerLocatorSyncS2CPacket should encode and decode fully populated sync state with non-null current target")
    void playerLocatorSyncS2CPacket_roundtrip_withTargetNonNull() {
        UUID p1 = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Map<UUID, Integer> jammers = Map.of(p1, 1);
        Map<UUID, String> dimensions = Map.of(p1, "minecraft:overworld");
        Map<UUID, BlockPos> positions = Map.of(p1, new BlockPos(10, 64, -10));

        PlayerLocatorSyncS2CPacket original = new PlayerLocatorSyncS2CPacket(jammers, dimensions, positions, target, 2);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PlayerLocatorSyncS2CPacket.encode(original, buf);
        PlayerLocatorSyncS2CPacket decoded = PlayerLocatorSyncS2CPacket.decode(buf);

        assertThat(decoded.jammers).containsExactlyInAnyOrderEntriesOf(jammers);
        assertThat(decoded.dimensions).containsExactlyInAnyOrderEntriesOf(dimensions);
        assertThat(decoded.positions).containsExactlyInAnyOrderEntriesOf(positions);
        assertThat(decoded.currentTarget).isEqualTo(target);
        assertThat(decoded.reachLevel).isEqualTo(2);
    }

    @Test
    @DisplayName("PlayerLocatorSyncS2CPacket should encode and decode state with null current target and empty maps")
    void playerLocatorSyncS2CPacket_roundtrip_withNullTargetAndEmptyMaps() {
        PlayerLocatorSyncS2CPacket original = new PlayerLocatorSyncS2CPacket(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null, 0
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PlayerLocatorSyncS2CPacket.encode(original, buf);
        PlayerLocatorSyncS2CPacket decoded = PlayerLocatorSyncS2CPacket.decode(buf);

        assertThat(decoded.jammers).isEmpty();
        assertThat(decoded.dimensions).isEmpty();
        assertThat(decoded.positions).isEmpty();
        assertThat(decoded.currentTarget).isNull();
        assertThat(decoded.reachLevel).isEqualTo(0);
    }

    @Test
    @DisplayName("PlayerLocatorTargetUpdateS2CPacket should encode and decode target UUID, position, and dimension")
    void playerLocatorTargetUpdateS2CPacket_roundtrip_preservesFields() {
        UUID targetId = UUID.randomUUID();
        BlockPos pos = new BlockPos(-150, 72, 300);
        String dim = "minecraft:the_end";

        PlayerLocatorTargetUpdateS2CPacket original = new PlayerLocatorTargetUpdateS2CPacket(targetId, pos, dim);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PlayerLocatorTargetUpdateS2CPacket.encode(original, buf);
        PlayerLocatorTargetUpdateS2CPacket decoded = PlayerLocatorTargetUpdateS2CPacket.decode(buf);

        assertThat(decoded.targetUUID).isEqualTo(targetId);
        assertThat(decoded.pos).isEqualTo(pos);
        assertThat(decoded.dimension).isEqualTo(dim);
    }
}
