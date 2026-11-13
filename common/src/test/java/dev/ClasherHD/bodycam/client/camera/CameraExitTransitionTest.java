package dev.ClasherHD.bodycam.client.camera;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CameraExitTransitionTest {
    private UUID playerId;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        CameraSnapState.clearAll();
        playerId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        CameraSnapState.clearAll();
    }

    private Player createMockPlayer(int entityId) {
        Player player = mock(Player.class);
        when(player.getId()).thenReturn(entityId);
        when(player.getPose()).thenReturn(Pose.STANDING);
        when(player.getDimensions(any())).thenReturn(EntityDimensions.scalable(0.6F, 1.8F));
        return player;
    }

    @Test
    void recordAtomicExit_storesExactCoordinatesAndZeroLerpDeltaAcrossAllPartialTicks() {
        double targetX = 128.5D;
        double targetY = 64.0D;
        double targetZ = -256.25D;
        float targetYaw = 135.0F;
        float targetPitch = -22.5F;
        int dummyId = 42;

        CameraSnapState.recordAtomicExit(playerId, targetX, targetY, targetZ, targetYaw, targetPitch, dummyId, null);

        CameraSnapState.SnapRecord record = CameraSnapState.peekPending(playerId);
        assertThat(record).isNotNull();
        assertThat(record.x()).isEqualTo(targetX);
        assertThat(record.y()).isEqualTo(targetY);
        assertThat(record.z()).isEqualTo(targetZ);
        assertThat(record.yRot()).isEqualTo(targetYaw);
        assertThat(record.xRot()).isEqualTo(targetPitch);
        assertThat(record.removedDummyEntityId()).isEqualTo(dummyId);

        float[] partialTicks = new float[]{0.0F, 0.25F, 0.5F, 0.75F, 1.0F};
        for (float pt : partialTicks) {
            double lerpedX = record.x() + (record.x() - record.x()) * pt;
            double lerpedY = record.y() + (record.y() - record.y()) * pt;
            double lerpedZ = record.z() + (record.z() - record.z()) * pt;
            assertThat(lerpedX - targetX).isEqualTo(0.0D);
            assertThat(lerpedY - targetY).isEqualTo(0.0D);
            assertThat(lerpedZ - targetZ).isEqualTo(0.0D);
        }
    }

    @Test
    void consumePending_removesSnapRecordAtomicallyOnFirstRead() {
        CameraSnapState.recordAtomicExit(playerId, 10.0D, 70.0D, 20.0D, 90.0F, 0.0F, 99, null);
        CameraSnapState.SnapRecord first = CameraSnapState.consumePending(playerId);
        CameraSnapState.SnapRecord second = CameraSnapState.consumePending(playerId);

        assertThat(first).isNotNull();
        assertThat(first.removedDummyEntityId()).isEqualTo(99);
        assertThat(second).isNull();
    }

    @Test
    void recordAtomicExit_withEntity_snapsAllPreviousAndCurrentTransformFields() {
        Player player = createMockPlayer(77);
        player.xo = 999.0D;
        player.yo = 999.0D;
        player.zo = 999.0D;
        player.xOld = 999.0D;
        player.yOld = 999.0D;
        player.zOld = 999.0D;
        player.yRotO = 15.0F;
        player.xRotO = 15.0F;
        player.yBodyRotO = 15.0F;
        player.yHeadRotO = 15.0F;
        player.bob = 0.75F;
        player.oBob = 0.75F;
        CameraSnapState.setObserverInMonitorSession(true);

        CameraSnapState.recordAtomicExit(player, 12.5D, 64.0D, -30.25D, 180.0F, -15.0F, 0.1D, -0.05D, 0.2D, 2.5F, 55);

        assertThat(CameraSnapState.isObserverInMonitorSession()).isFalse();
        assertThat(CameraSnapState.isPendingSnap()).isTrue();
        assertThat(CameraSnapState.getLastRemovedDummyId()).isEqualTo(55);

        assertThat(player.xo).isEqualTo(12.5D);
        assertThat(player.yo).isEqualTo(64.0D);
        assertThat(player.zo).isEqualTo(-30.25D);
        assertThat(player.xOld).isEqualTo(12.5D);
        assertThat(player.yOld).isEqualTo(64.0D);
        assertThat(player.zOld).isEqualTo(-30.25D);
        assertThat(player.yRotO).isEqualTo(180.0F);
        assertThat(player.xRotO).isEqualTo(-15.0F);
        assertThat(player.yBodyRot).isEqualTo(180.0F);
        assertThat(player.yBodyRotO).isEqualTo(180.0F);
        assertThat(player.yHeadRot).isEqualTo(180.0F);
        assertThat(player.yHeadRotO).isEqualTo(180.0F);
        assertThat(player.bob).isEqualTo(0.0F);
        assertThat(player.oBob).isEqualTo(0.0F);
        assertThat(player.fallDistance).isEqualTo(2.5F);

        verify(player).setPosRaw(12.5D, 64.0D, -30.25D);
        verify(player).setYRot(180.0F);
        verify(player).setXRot(-15.0F);
        verify(player).lerpTo(12.5D, 64.0D, -30.25D, 180.0F, -15.0F, 0);
        verify(player).lerpHeadTo(180.0F, 0);
        verify(player).syncPacketPositionCodec(12.5D, 64.0D, -30.25D);
        verify(player).setDeltaMovement(0.1D, -0.05D, 0.2D);
        verify(player).setInvisible(false);
    }

    @Test
    void enforceRenderFrameGuard_consumesOneShotWithoutLockingSubsequentFrames() {
        Player player = createMockPlayer(101);
        when(player.getX()).thenReturn(100.0D);
        when(player.getY()).thenReturn(70.0D);
        when(player.getZ()).thenReturn(200.0D);
        when(player.getYRot()).thenReturn(45.0F);
        when(player.getXRot()).thenReturn(12.0F);

        CameraSnapState.recordAtomicExit(player, 100.0D, 70.0D, 200.0D, 45.0F, 12.0F, 0.0D, 0.0D, 0.0D, 0.0F, 88);

        player.xo = 500.0D;
        player.yo = 500.0D;
        player.zo = 500.0D;
        player.yRotO = 0.0F;
        player.xRotO = 0.0F;

        CameraSnapState.enforceRenderFrameGuard(player);

        assertThat(CameraSnapState.isPendingSnap()).isFalse();
        assertThat(player.xo).isEqualTo(100.0D);
        assertThat(player.yo).isEqualTo(70.0D);
        assertThat(player.zo).isEqualTo(200.0D);
        assertThat(player.yRotO).isEqualTo(45.0F);
        assertThat(player.xRotO).isEqualTo(12.0F);

        player.xo = 105.0D;
        player.yRotO = 90.0F;
        CameraSnapState.enforceRenderFrameGuard(player);
        assertThat(player.xo).isEqualTo(105.0D);
        assertThat(player.yRotO).isEqualTo(90.0F);
    }

    @Test
    void tickFrameGuard_consumesPendingOnceAndAllowsFreeMovementAfterward() {
        Player player = createMockPlayer(202);
        when(player.getX()).thenReturn(50.0D);
        when(player.getY()).thenReturn(80.0D);
        when(player.getZ()).thenReturn(-10.0D);
        when(player.getYRot()).thenReturn(-90.0F);
        when(player.getXRot()).thenReturn(30.0F);

        CameraSnapState.recordAtomicExit(player, 50.0D, 80.0D, -10.0D, -90.0F, 30.0F, 0.0D, 0.0D, 0.0D, 0.0F, 77);

        player.xo = 999.0D;
        player.yo = 999.0D;
        player.zo = 999.0D;
        player.yRotO = 0.0F;
        player.xRotO = 0.0F;
        CameraSnapState.tickFrameGuard(player);
        assertThat(CameraSnapState.isPendingSnap()).isFalse();
        assertThat(player.xo).isEqualTo(50.0D);
        assertThat(player.yo).isEqualTo(80.0D);
        assertThat(player.zo).isEqualTo(-10.0D);
        assertThat(player.yRotO).isEqualTo(-90.0F);
        assertThat(player.xRotO).isEqualTo(30.0F);

        player.xo = 1234.0D;
        player.yRotO = 12.0F;
        CameraSnapState.tickFrameGuard(player);
        assertThat(CameraSnapState.isPendingSnap()).isFalse();
        assertThat(player.xo).isEqualTo(1234.0D);
        assertThat(player.yRotO).isEqualTo(12.0F);
    }

    @Test
    void snapRemoteEntityWithoutInterpolation_clearsLerpAndPreservesCodec() {
        Player remote = createMockPlayer(404);
        remote.xo = 900.0D;
        remote.yo = 120.0D;
        remote.zo = -900.0D;

        CameraSnapState.snapRemoteEntityWithoutInterpolation(remote, 10.0D, 64.0D, 20.0D, 90.0F, 0.0F, 0.0D, 0.0D, 0.0D, 0.0F, false);

        assertThat(remote.xo).isEqualTo(10.0D);
        assertThat(remote.yo).isEqualTo(64.0D);
        assertThat(remote.zo).isEqualTo(20.0D);
        verify(remote).lerpTo(10.0D, 64.0D, 20.0D, 90.0F, 0.0F, 0);
        verify(remote).lerpHeadTo(90.0F, 0);
        verify(remote).setInvisible(false);
        verify(remote, org.mockito.Mockito.never()).syncPacketPositionCodec(10.0D, 64.0D, 20.0D);
    }

    @Test
    void snapRemoteObserverOnExit_armsGuardAndCancelsVanillaPositionLerpAndHeadLerp() {
        Player remote = createMockPlayer(505);
        when(remote.lerpTargetX()).thenReturn(10.0D);
        when(remote.lerpTargetY()).thenReturn(64.0D);
        when(remote.lerpTargetZ()).thenReturn(20.0D);
        when(remote.lerpTargetYRot()).thenReturn(90.0F);
        when(remote.lerpTargetXRot()).thenReturn(15.0F);
        when(remote.getDeltaMovement()).thenReturn(net.minecraft.world.phys.Vec3.ZERO);

        CameraSnapState.snapRemoteObserverOnExit(
                remote, 505, 10.0D, 64.0D, 20.0D, 90.0F, 15.0F, 0.0D, 0.0D, 0.0D, 0.0F, false
        );
        assertThat(CameraSnapState.hasRemoteExitGuard(505)).isTrue();

        CameraSnapState.handleRemoteVanillaHeadPacket(remote, 95.0F);
        assertThat(remote.yHeadRot).isEqualTo(95.0F);
        assertThat(remote.yHeadRotO).isEqualTo(95.0F);
        verify(remote).lerpHeadTo(95.0F, 0);

        CameraSnapState.handleRemoteVanillaPositionPacket(remote, true);
        assertThat(CameraSnapState.hasRemoteExitGuard(505)).isFalse();
        assertThat(remote.xo).isEqualTo(10.0D);
        assertThat(remote.yo).isEqualTo(64.0D);
        assertThat(remote.zo).isEqualTo(20.0D);
        verify(remote, org.mockito.Mockito.times(2)).lerpTo(10.0D, 64.0D, 20.0D, 90.0F, 15.0F, 0);
        verify(remote, org.mockito.Mockito.never()).syncPacketPositionCodec(10.0D, 64.0D, 20.0D);
    }

    @Test
    void clearAndReset_leaveNoStaleSnapOrSessionState() {
        Player player = createMockPlayer(303);
        CameraSnapState.setObserverInMonitorSession(true);
        CameraSnapState.recordAtomicExit(playerId, 1.0D, 2.0D, 3.0D, 10.0F, 20.0F, 123, player);
        CameraSnapState.setObserverInMonitorSession(true);

        CameraSnapState.reset();
        assertThat(CameraSnapState.isPendingSnap()).isFalse();
        assertThat(CameraSnapState.getLastRemovedDummyId()).isEqualTo(-1);
        assertThat(CameraSnapState.isObserverInMonitorSession()).isFalse();
        assertThat(CameraSnapState.peekPending(playerId)).isNotNull();

        CameraSnapState.setObserverInMonitorSession(true);
        CameraSnapState.recordAtomicExit(player, 4.0D, 5.0D, 6.0D, 30.0F, 40.0F, 0.0D, 0.0D, 0.0D, 0.0F, 456);
        CameraSnapState.clearAll();
        assertThat(CameraSnapState.isPendingSnap()).isFalse();
        assertThat(CameraSnapState.getLastRemovedDummyId()).isEqualTo(-1);
        assertThat(CameraSnapState.isObserverInMonitorSession()).isFalse();
        assertThat(CameraSnapState.peekPending(playerId)).isNull();
    }
}
