package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.config.ServerConfigSnapshot;
import dev.ClasherHD.bodycam.network.bodycam.BodycamExitSyncS2CPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.CrossObservationSyncPacket;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamRequestC2SPacket;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamStatesS2CPacket;
import dev.ClasherHD.bodycam.network.config.OpenClientConfigS2CPacket;
import dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket;
import dev.ClasherHD.bodycam.network.config.ReloadClientConfigS2CPacket;
import dev.ClasherHD.bodycam.network.config.SaveServerConfigC2SPacket;
import dev.ClasherHD.bodycam.network.locator.DimensionLocatorRequestPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class PacketCodecRegressionTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
    }

    private RegistryFriendlyByteBuf newRegistryBuffer() {
        FriendlyByteBuf friendly = new FriendlyByteBuf(Unpooled.buffer());
        return new RegistryFriendlyByteBuf(friendly, RegistryAccess.EMPTY);
    }

    private <T> void assertEmptyPayloadCodecRoundtrip(
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            T firstInstance,
            T secondInstance,
            Class<T> expectedClass
    ) {
        RegistryFriendlyByteBuf buf = newRegistryBuffer();
        codec.encode(buf, firstInstance);
        codec.encode(buf, secondInstance);

        T decodedFirst = codec.decode(buf);
        T decodedSecond = codec.decode(buf);

        assertThat(decodedFirst).isNotNull().isInstanceOf(expectedClass);
        assertThat(decodedSecond).isNotNull().isInstanceOf(expectedClass);
        assertThat(decodedFirst).isNotSameAs(firstInstance);
        assertThat(decodedSecond).isNotSameAs(secondInstance);
        assertThat(buf.readableBytes()).isZero();
    }

    @Test
    void emptyPayloadPackets_roundtripDistinctInstancesWithoutUnitCrash() {
        assertEmptyPayloadCodecRoundtrip(
                BodycamResetCameraPacket.STREAM_CODEC,
                new BodycamResetCameraPacket(),
                new BodycamResetCameraPacket(),
                BodycamResetCameraPacket.class
        );
        assertEmptyPayloadCodecRoundtrip(
                BodycamForceClosePacket.STREAM_CODEC,
                new BodycamForceClosePacket(),
                new BodycamForceClosePacket(),
                BodycamForceClosePacket.class
        );
        assertEmptyPayloadCodecRoundtrip(
                OpenClientConfigS2CPacket.STREAM_CODEC,
                new OpenClientConfigS2CPacket(),
                new OpenClientConfigS2CPacket(),
                OpenClientConfigS2CPacket.class
        );
        assertEmptyPayloadCodecRoundtrip(
                ReloadClientConfigS2CPacket.STREAM_CODEC,
                new ReloadClientConfigS2CPacket(),
                new ReloadClientConfigS2CPacket(),
                ReloadClientConfigS2CPacket.class
        );
        assertEmptyPayloadCodecRoundtrip(
                DimensionLocatorRequestPacket.STREAM_CODEC,
                new DimensionLocatorRequestPacket(),
                new DimensionLocatorRequestPacket(),
                DimensionLocatorRequestPacket.class
        );
    }

    @Test
    void bodycamSetCameraPacket_streamCodecRoundtripPreservesFields() {
        UUID targetId = UUID.randomUUID();
        BodycamSetCameraPacket original = new BodycamSetCameraPacket(targetId, true, true);
        RegistryFriendlyByteBuf buf = newRegistryBuffer();

        BodycamSetCameraPacket.STREAM_CODEC.encode(buf, original);
        BodycamSetCameraPacket decoded = BodycamSetCameraPacket.STREAM_CODEC.decode(buf);

        assertThat(decoded.targetId).isEqualTo(targetId);
        assertThat(decoded.isOnHologram).isTrue();
        assertThat(decoded.isAllSeeingEye).isTrue();
        assertThat(buf.readableBytes()).isZero();
    }

    @Test
    void bodycamExitSyncPacket_streamCodecRoundtripPreservesTransformAndDummyId() {
        BodycamExitSyncS2CPacket original = new BodycamExitSyncS2CPacket(
                123.5D, 64.25D, -456.75D, 135.0F, -20.0F, 0.1D, -0.2D, 0.3D, 1.5F, 0, 84, 312, false
        );
        RegistryFriendlyByteBuf buf = newRegistryBuffer();

        BodycamExitSyncS2CPacket.STREAM_CODEC.encode(buf, original);
        BodycamExitSyncS2CPacket decoded = BodycamExitSyncS2CPacket.STREAM_CODEC.decode(buf);

        assertThat(decoded).isEqualTo(original);
        assertThat(buf.readableBytes()).isZero();
    }

    @Test
    void syncBodycamPackets_streamCodecRoundtripPreservesState() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID wp = UUID.randomUUID();

        CrossObservationSyncPacket crossOriginal = new CrossObservationSyncPacket(p1, true);
        RegistryFriendlyByteBuf crossBuf = newRegistryBuffer();
        CrossObservationSyncPacket.STREAM_CODEC.encode(crossBuf, crossOriginal);
        CrossObservationSyncPacket crossDecoded = CrossObservationSyncPacket.STREAM_CODEC.decode(crossBuf);
        assertThat(crossDecoded.observerId).isEqualTo(p1);
        assertThat(crossDecoded.observing).isTrue();
        assertThat(crossBuf.readableBytes()).isZero();

        SyncBodycamRequestC2SPacket reqOriginal = new SyncBodycamRequestC2SPacket(true);
        RegistryFriendlyByteBuf reqBuf = newRegistryBuffer();
        SyncBodycamRequestC2SPacket.STREAM_CODEC.encode(reqBuf, reqOriginal);
        SyncBodycamRequestC2SPacket reqDecoded = SyncBodycamRequestC2SPacket.STREAM_CODEC.decode(reqBuf);
        assertThat(reqDecoded.isOnHologram).isTrue();
        assertThat(reqBuf.readableBytes()).isZero();

        SyncBodycamStatesS2CPacket statesOriginal = new SyncBodycamStatesS2CPacket(
                Map.of(p1, 2),
                Map.of(p1, p2),
                Map.of(p1, "minecraft:overworld"),
                Map.of(p1, new BlockPos(10, 64, -20)),
                Map.of(p2, true),
                Set.of(p1),
                Map.of(wp, "Gate"),
                3,
                true,
                true
        );
        RegistryFriendlyByteBuf statesBuf = newRegistryBuffer();
        SyncBodycamStatesS2CPacket.STREAM_CODEC.encode(statesBuf, statesOriginal);
        SyncBodycamStatesS2CPacket statesDecoded = SyncBodycamStatesS2CPacket.STREAM_CODEC.decode(statesBuf);

        assertThat(statesDecoded.jammers).containsExactlyInAnyOrderEntriesOf(statesOriginal.jammers);
        assertThat(statesDecoded.targets).containsExactlyInAnyOrderEntriesOf(statesOriginal.targets);
        assertThat(statesDecoded.dimensions).containsExactlyInAnyOrderEntriesOf(statesOriginal.dimensions);
        assertThat(statesDecoded.positions).containsExactlyInAnyOrderEntriesOf(statesOriginal.positions);
        assertThat(statesDecoded.anonymizers).containsExactlyInAnyOrderEntriesOf(statesOriginal.anonymizers);
        assertThat(statesDecoded.hidden).containsExactlyInAnyOrderElementsOf(statesOriginal.hidden);
        assertThat(statesDecoded.watchPoints).containsExactlyInAnyOrderEntriesOf(statesOriginal.watchPoints);
        assertThat(statesDecoded.reachLevel).isEqualTo(3);
        assertThat(statesDecoded.isOnHologram).isTrue();
        assertThat(statesDecoded.isAllSeeingEye).isTrue();
        assertThat(statesBuf.readableBytes()).isZero();
    }

    @Test
    void serverConfigSnapshotPackets_streamCodecRoundtripPreservesAllOptions() {
        ServerConfigSnapshot snapshot = new ServerConfigSnapshot(
                500, 5, 50,
                true, false, true, false, true, false,
                true, false, true, false, true, false,
                true, false, true
        );

        RegistryFriendlyByteBuf openBuf = newRegistryBuffer();
        OpenServerConfigS2CPacket.STREAM_CODEC.encode(openBuf, new OpenServerConfigS2CPacket(snapshot));
        OpenServerConfigS2CPacket openDecoded = OpenServerConfigS2CPacket.STREAM_CODEC.decode(openBuf);
        assertThat(openDecoded.snapshot).isEqualTo(snapshot);
        assertThat(openBuf.readableBytes()).isZero();

        RegistryFriendlyByteBuf saveBuf = newRegistryBuffer();
        SaveServerConfigC2SPacket.STREAM_CODEC.encode(saveBuf, new SaveServerConfigC2SPacket(snapshot));
        SaveServerConfigC2SPacket saveDecoded = SaveServerConfigC2SPacket.STREAM_CODEC.decode(saveBuf);
        assertThat(saveDecoded.snapshot).isEqualTo(snapshot);
        assertThat(saveBuf.readableBytes()).isZero();
    }
}
