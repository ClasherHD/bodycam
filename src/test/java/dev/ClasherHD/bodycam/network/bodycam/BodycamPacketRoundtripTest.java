package dev.ClasherHD.bodycam.network.bodycam;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class BodycamPacketRoundtripTest {

    @Test
    @DisplayName("BodycamSetCameraPacket should encode and decode target UUID and hologram flag")
    void bodycamSetCameraPacket_roundtrip_preservesFields() {
        UUID targetId = UUID.randomUUID();
        BodycamSetCameraPacket originalTrue = new BodycamSetCameraPacket(targetId, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        BodycamSetCameraPacket.encode(originalTrue, buf);
        BodycamSetCameraPacket decodedTrue = BodycamSetCameraPacket.decode(buf);

        assertThat(decodedTrue.targetId).isEqualTo(targetId);
        assertThat(decodedTrue.isOnHologram).isTrue();

        BodycamSetCameraPacket originalFalse = new BodycamSetCameraPacket(targetId, false);
        FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
        BodycamSetCameraPacket.encode(originalFalse, buf2);
        BodycamSetCameraPacket decodedFalse = BodycamSetCameraPacket.decode(buf2);

        assertThat(decodedFalse.targetId).isEqualTo(targetId);
        assertThat(decodedFalse.isOnHologram).isFalse();
    }

    @Test
    @DisplayName("BodycamSetCameraPacket static maps should allow clean storage and cleanup")
    void bodycamSetCameraPacket_staticMaps_storeAndRemoveDataCorrectly() {
        UUID testPlayer = UUID.randomUUID();
        Vec3 pos = new Vec3(10.5, 64.0, -20.5);
        Vec2 rot = new Vec2(45.0F, 90.0F);

        BodycamSetCameraPacket.ORIGINAL_POS.put(testPlayer, pos);
        BodycamSetCameraPacket.ORIGINAL_ROT.put(testPlayer, rot);
        BodycamSetCameraPacket.ORIGINAL_DIM.put(testPlayer, "minecraft:overworld");
        BodycamSetCameraPacket.ORIGINAL_GAMEMODE.put(testPlayer, 0);

        assertThat(BodycamSetCameraPacket.ORIGINAL_POS).containsEntry(testPlayer, pos);
        assertThat(BodycamSetCameraPacket.ORIGINAL_ROT).containsEntry(testPlayer, rot);
        assertThat(BodycamSetCameraPacket.ORIGINAL_DIM).containsEntry(testPlayer, "minecraft:overworld");
        assertThat(BodycamSetCameraPacket.ORIGINAL_GAMEMODE).containsEntry(testPlayer, 0);

        BodycamSetCameraPacket.ORIGINAL_POS.remove(testPlayer);
        BodycamSetCameraPacket.ORIGINAL_ROT.remove(testPlayer);
        BodycamSetCameraPacket.ORIGINAL_DIM.remove(testPlayer);
        BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(testPlayer);

        assertThat(BodycamSetCameraPacket.ORIGINAL_POS).doesNotContainKey(testPlayer);
    }

    @Test
    @DisplayName("BodycamResetCameraPacket should encode and decode empty payload")
    void bodycamResetCameraPacket_roundtrip_createsValidInstance() {
        BodycamResetCameraPacket original = new BodycamResetCameraPacket();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        BodycamResetCameraPacket.encode(original, buf);
        BodycamResetCameraPacket decoded = BodycamResetCameraPacket.decode(buf);

        assertThat(decoded).isNotNull();
    }

    @Test
    @DisplayName("BodycamResetCameraPacket.cleanupPlayerNbtAndMaps with null player should safely return")
    void bodycamResetCameraPacket_cleanupPlayerNbtAndMaps_handlesNullPlayerSafely() {
        BodycamResetCameraPacket.cleanupPlayerNbtAndMaps(null);
    }

    @Test
    @DisplayName("BodycamForceClosePacket should encode and decode empty payload")
    void bodycamForceClosePacket_roundtrip_createsValidInstance() {
        BodycamForceClosePacket original = new BodycamForceClosePacket();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        BodycamForceClosePacket.encode(original, buf);
        BodycamForceClosePacket decoded = BodycamForceClosePacket.decode(buf);

        assertThat(decoded).isNotNull();
    }

    @Test
    @DisplayName("CrossObservationSyncPacket should encode and decode observer UUID and active state")
    void crossObservationSyncPacket_roundtrip_preservesFields() {
        UUID observerId = UUID.randomUUID();
        CrossObservationSyncPacket originalObserving = new CrossObservationSyncPacket(observerId, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        CrossObservationSyncPacket.encode(originalObserving, buf);
        CrossObservationSyncPacket decodedObserving = CrossObservationSyncPacket.decode(buf);

        assertThat(decodedObserving.observerId).isEqualTo(observerId);
        assertThat(decodedObserving.isObserving).isTrue();

        CrossObservationSyncPacket originalNotObserving = new CrossObservationSyncPacket(observerId, false);
        FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
        CrossObservationSyncPacket.encode(originalNotObserving, buf2);
        CrossObservationSyncPacket decodedNotObserving = CrossObservationSyncPacket.decode(buf2);

        assertThat(decodedNotObserving.observerId).isEqualTo(observerId);
        assertThat(decodedNotObserving.isObserving).isFalse();
    }

    @Test
    @DisplayName("SyncBodycamRequestC2SPacket should encode and decode hologram boolean flag")
    void syncBodycamRequestC2SPacket_roundtrip_preservesHologramFlag() {
        SyncBodycamRequestC2SPacket reqTrue = new SyncBodycamRequestC2SPacket(true);
        FriendlyByteBuf bufTrue = new FriendlyByteBuf(Unpooled.buffer());
        SyncBodycamRequestC2SPacket.encode(reqTrue, bufTrue);
        SyncBodycamRequestC2SPacket decodedTrue = SyncBodycamRequestC2SPacket.decode(bufTrue);
        assertThat(decodedTrue.isOnHologram).isTrue();

        SyncBodycamRequestC2SPacket reqFalse = new SyncBodycamRequestC2SPacket(false);
        FriendlyByteBuf bufFalse = new FriendlyByteBuf(Unpooled.buffer());
        SyncBodycamRequestC2SPacket.encode(reqFalse, bufFalse);
        SyncBodycamRequestC2SPacket decodedFalse = SyncBodycamRequestC2SPacket.decode(bufFalse);
        assertThat(decodedFalse.isOnHologram).isFalse();
    }

    @Test
    @DisplayName("SyncBodycamStatesS2CPacket should encode and decode fully populated state")
    void syncBodycamStatesS2CPacket_roundtrip_withPopulatedData() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID wpId = UUID.randomUUID();

        Map<UUID, Integer> jammers = Map.of(p1, 1, p2, 2);
        Map<UUID, UUID> targets = Map.of(p1, p2);
        Map<UUID, String> dimensions = Map.of(p1, "minecraft:overworld", p2, "minecraft:the_nether");
        Map<UUID, BlockPos> positions = Map.of(p1, new BlockPos(100, 64, -200), p2, new BlockPos(-500, 12, 800));
        Map<UUID, Boolean> anonymizers = Map.of(p1, true, p2, false);
        Set<UUID> hidden = Set.of(p1);
        Map<UUID, String> watchPoints = Map.of(wpId, "Watch Point Alpha");

        SyncBodycamStatesS2CPacket original = new SyncBodycamStatesS2CPacket(
                jammers, targets, dimensions, positions, anonymizers, hidden, watchPoints, 2, true
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SyncBodycamStatesS2CPacket.encode(original, buf);
        SyncBodycamStatesS2CPacket decoded = SyncBodycamStatesS2CPacket.decode(buf);

        assertThat(decoded.jammers).containsExactlyInAnyOrderEntriesOf(jammers);
        assertThat(decoded.targets).containsExactlyInAnyOrderEntriesOf(targets);
        assertThat(decoded.dimensions).containsExactlyInAnyOrderEntriesOf(dimensions);
        assertThat(decoded.positions).containsExactlyInAnyOrderEntriesOf(positions);
        assertThat(decoded.anonymizers).containsExactlyInAnyOrderEntriesOf(anonymizers);
        assertThat(decoded.hidden).containsExactlyInAnyOrderElementsOf(hidden);
        assertThat(decoded.watchPoints).containsExactlyInAnyOrderEntriesOf(watchPoints);
        assertThat(decoded.reachLevel).isEqualTo(2);
        assertThat(decoded.isOnHologram).isTrue();
    }

    @Test
    @DisplayName("SyncBodycamStatesS2CPacket should encode and decode empty collections")
    void syncBodycamStatesS2CPacket_roundtrip_withEmptyCollections() {
        SyncBodycamStatesS2CPacket original = new SyncBodycamStatesS2CPacket(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap(),
                0,
                false
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SyncBodycamStatesS2CPacket.encode(original, buf);
        SyncBodycamStatesS2CPacket decoded = SyncBodycamStatesS2CPacket.decode(buf);

        assertThat(decoded.jammers).isEmpty();
        assertThat(decoded.targets).isEmpty();
        assertThat(decoded.dimensions).isEmpty();
        assertThat(decoded.positions).isEmpty();
        assertThat(decoded.anonymizers).isEmpty();
        assertThat(decoded.hidden).isEmpty();
        assertThat(decoded.watchPoints).isEmpty();
        assertThat(decoded.reachLevel).isEqualTo(0);
        assertThat(decoded.isOnHologram).isFalse();
    }

    @Test
    @DisplayName("SyncBodycamStatesS2CPacket should preserve Minecraft boundary coordinates and extreme integer values")
    void syncBodycamStatesS2CPacket_roundtrip_withBoundaryValues() {
        UUID id = UUID.randomUUID();
        BlockPos borderPos = new BlockPos(30_000_000, -64, -30_000_000);

        SyncBodycamStatesS2CPacket original = new SyncBodycamStatesS2CPacket(
                Map.of(id, Integer.MAX_VALUE),
                Collections.emptyMap(),
                Map.of(id, "custom:long_dimension_name_test"),
                Map.of(id, borderPos),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap(),
                Integer.MIN_VALUE,
                false
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SyncBodycamStatesS2CPacket.encode(original, buf);
        SyncBodycamStatesS2CPacket decoded = SyncBodycamStatesS2CPacket.decode(buf);

        assertThat(decoded.jammers).containsEntry(id, Integer.MAX_VALUE);
        assertThat(decoded.reachLevel).isEqualTo(Integer.MIN_VALUE);
        BlockPos decodedPos = decoded.positions.get(id);
        assertThat(decodedPos.getX()).isEqualTo(30_000_000);
        assertThat(decodedPos.getY()).isEqualTo(-64);
        assertThat(decodedPos.getZ()).isEqualTo(-30_000_000);
    }

    @Test
    @DisplayName("SyncBodycamStatesS2CPacket demonstrates Minecraft network protocol 26-bit BlockPos bit-packing truncation")
    void syncBodycamStatesS2CPacket_roundtrip_demonstratesMinecraftProtocol26BitCoordinateTruncation() {
        UUID id = UUID.randomUUID();
        BlockPos outOfRangePos = new BlockPos(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);

        SyncBodycamStatesS2CPacket original = new SyncBodycamStatesS2CPacket(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(id, outOfRangePos),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap(),
                0,
                false
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SyncBodycamStatesS2CPacket.encode(original, buf);
        SyncBodycamStatesS2CPacket decoded = SyncBodycamStatesS2CPacket.decode(buf);

        BlockPos decodedPos = decoded.positions.get(id);
        assertThat(decodedPos.getX()).isEqualTo(0);
        assertThat(decodedPos.getY()).isEqualTo(0);
        assertThat(decodedPos.getZ()).isEqualTo(-1);
    }
}
