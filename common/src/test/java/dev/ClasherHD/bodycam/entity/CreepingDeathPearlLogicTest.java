package dev.ClasherHD.bodycam.entity;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CreepingDeathPearlLogicTest {

    private static final int MIN_SAFE_Y = -62;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void rejectsPearlWhenBlockedByAdjacentWallInCave() {
        Vec3 selfPos = new Vec3(0.5D, 20.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 21.52D, 0.5D);
        Vec3 targetPos = new Vec3(18.5D, 20.0D, 0.5D);
        Vec3 targetAim = new Vec3(18.5D, 20.9D, 0.5D);

        AABB wall = new AABB(2.0D, 18.0D, -4.0D, 6.0D, 26.0D, 4.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> clipBox(wall, Direction.WEST, from, to);
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x - 0.5D, 20.0D, impact.z);

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(candidate).isNull();
    }

    @Test
    void rejectsPearlInLowCeilingTwoBlockTunnel() {
        Vec3 selfPos = new Vec3(0.5D, 20.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 21.52D, 0.5D);
        Vec3 targetPos = new Vec3(22.5D, 20.0D, 0.5D);
        Vec3 targetAim = new Vec3(22.5D, 20.9D, 0.5D);

        AABB ceiling = new AABB(-2.0D, 22.0D, -2.0D, 25.0D, 26.0D, 2.0D);
        AABB floor = new AABB(-2.0D, 19.0D, -2.0D, 25.0D, 20.0D, 2.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> {
            CreepingDeathEntity.PearlHit ceilingHit = clipBox(ceiling, Direction.DOWN, from, to);
            if (ceilingHit != null) {
                return ceilingHit;
            }
            return clipBox(floor, Direction.UP, from, to);
        };
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, 20.0D, impact.z);

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(candidate).isNull();
    }

    @Test
    void rejectsPearlWhenCeilingHitDropsBackWithInsufficientProgress() {
        Vec3 selfPos = new Vec3(0.5D, 20.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 21.52D, 0.5D);
        Vec3 targetPos = new Vec3(10.5D, 50.0D, 0.5D);
        Vec3 targetAim = new Vec3(10.5D, 50.9D, 0.5D);

        AABB caveRoof = new AABB(-10.0D, 27.0D, -10.0D, 30.0D, 35.0D, 10.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> clipBox(caveRoof, Direction.DOWN, from, to);
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, 20.0D, impact.z);

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(candidate).isNull();
    }

    @Test
    void rejectsPearlWhenLandingIsUnsafeLavaOrVoid() {
        Vec3 selfPos = new Vec3(0.5D, 64.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 65.52D, 0.5D);
        Vec3 targetPos = new Vec3(20.5D, 64.0D, 0.5D);
        Vec3 targetAim = new Vec3(20.5D, 64.9D, 0.5D);

        AABB ground = new AABB(-5.0D, 63.0D, -5.0D, 40.0D, 64.0D, 5.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> clipBox(ground, Direction.UP, from, to);
        CreepingDeathEntity.PearlLandingResolver unsafeResolver = (impact, face, blockPos) -> null;

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                unsafeResolver
        );

        assertThat(candidate).isNull();
    }

    @Test
    void rejectsPearlWhenTargetIsTooClose() {
        Vec3 selfPos = new Vec3(0.5D, 64.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 65.52D, 0.5D);
        Vec3 targetPos = new Vec3(4.0D, 64.0D, 0.5D);
        Vec3 targetAim = new Vec3(4.0D, 64.9D, 0.5D);

        AABB ground = new AABB(-5.0D, 63.0D, -5.0D, 40.0D, 64.0D, 5.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> clipBox(ground, Direction.UP, from, to);
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, 64.0D, impact.z);

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(candidate).isNull();
    }

    @Test
    void acceptsPearlInOpenAirPursuit() {
        Vec3 selfPos = new Vec3(0.5D, 64.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 65.52D, 0.5D);
        Vec3 targetPos = new Vec3(20.5D, 64.0D, 0.5D);
        Vec3 targetAim = new Vec3(20.5D, 64.9D, 0.5D);

        AABB ground = new AABB(-5.0D, 63.0D, -5.0D, 50.0D, 64.0D, 5.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> clipBox(ground, Direction.UP, from, to);
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, 64.0D, impact.z);

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(candidate).isNotNull();
        assertThat(candidate.travelDist()).isGreaterThanOrEqualTo(CreepingDeathEntity.PEARL_MIN_TRAVEL_DIST);
        assertThat(candidate.progress()).isGreaterThanOrEqualTo(10.0D);
    }

    @Test
    void acceptsPearlAcrossChasmOntoFarLedge() {
        Vec3 selfPos = new Vec3(0.5D, 64.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 65.52D, 0.5D);
        Vec3 targetPos = new Vec3(16.5D, 64.0D, 0.5D);
        Vec3 targetAim = new Vec3(16.5D, 64.9D, 0.5D);

        AABB farLedge = new AABB(13.0D, 60.0D, -5.0D, 30.0D, 64.0D, 5.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> clipBox(farLedge, Direction.UP, from, to);
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                impact.x >= 13.0D ? new Vec3(impact.x, 64.0D, impact.z) : null;

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(candidate).isNotNull();
        assertThat(candidate.landingPos().x).isGreaterThanOrEqualTo(13.0D);
        assertThat(candidate.progress()).isGreaterThanOrEqualTo(8.0D);
    }

    @Test
    void acceptsPearlUpOntoHighLedge() {
        Vec3 selfPos = new Vec3(0.5D, 64.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 65.52D, 0.5D);
        Vec3 targetPos = new Vec3(10.5D, 71.0D, 0.5D);
        Vec3 targetAim = new Vec3(10.5D, 71.9D, 0.5D);

        AABB highLedge = new AABB(8.0D, 70.0D, -4.0D, 20.0D, 71.0D, 4.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> clipBox(highLedge, Direction.UP, from, to);
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, 71.0D, impact.z);

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(candidate).isNotNull();
        assertThat(candidate.landingPos().y).isEqualTo(71.0D);
        assertThat(candidate.progress()).isGreaterThanOrEqualTo(6.0D);
    }

    @Test
    void selectsHigherArcWhenLowWallBlocksDirectArc() {
        Vec3 selfPos = new Vec3(0.5D, 64.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 65.52D, 0.5D);
        Vec3 targetPos = new Vec3(30.5D, 64.0D, 0.5D);
        Vec3 targetAim = new Vec3(30.5D, 64.9D, 0.5D);

        AABB lowWall = new AABB(9.0D, 64.0D, -4.0D, 10.0D, 66.5D, 4.0D);
        AABB ground = new AABB(-5.0D, 63.0D, -5.0D, 60.0D, 64.0D, 5.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> {
            CreepingDeathEntity.PearlHit wallHit = clipBox(lowWall, Direction.WEST, from, to);
            if (wallHit != null) {
                return wallHit;
            }
            return clipBox(ground, Direction.UP, from, to);
        };
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, 64.0D, impact.z);

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(candidate).isNotNull();
        assertThat(candidate.landingPos().x).isGreaterThan(10.0D);
        assertThat(candidate.progress()).isGreaterThan(10.0D);
    }

    @Test
    void acceptsDirectEntityHitInOpenAir() {
        Vec3 selfPos = new Vec3(0.5D, 64.0D, 0.5D);
        Vec3 startPos = new Vec3(0.5D, 65.52D, 0.5D);
        Vec3 targetPos = new Vec3(14.5D, 64.0D, 0.5D);
        Vec3 targetAim = new Vec3(14.5D, 64.9D, 0.5D);

        AABB targetBox = new AABB(13.85D, 63.65D, -0.15D, 15.15D, 66.15D, 1.15D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> {
            Optional<Vec3> hit = targetBox.clip(from, to);
            return hit.map(loc -> new CreepingDeathEntity.PearlHit(loc, Direction.UP, BlockPos.containing(loc), true))
                    .orElse(null);
        };
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) -> null;

        CreepingDeathEntity.PearlCandidate candidate = CreepingDeathEntity.selectBestPearlCandidate(
                startPos,
                selfPos,
                targetPos,
                targetAim,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(candidate).isNotNull();
        assertThat(candidate.progress()).isGreaterThan(12.0D);
    }

    @Test
    void throwsUpwardRecoveryPearlWhenMissedPearlFliesOffMountainCliff() {
        Vec3 selfPos = new Vec3(0.5D, 100.0D, 0.5D);
        Vec3 launchStartPos = new Vec3(0.5D, 101.52D, 0.5D);
        Vec3 targetPos = new Vec3(14.5D, 100.0D, 0.5D);
        double referenceY = Math.min(selfPos.y, targetPos.y);

        AABB mountainTop = new AABB(-8.0D, 90.0D, -8.0D, 18.0D, 100.0D, 8.0D);
        AABB valleyFloor = new AABB(-20.0D, 63.0D, -20.0D, 120.0D, 64.0D, 20.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> {
            CreepingDeathEntity.PearlHit mountainHit = clipBox(mountainTop, Direction.UP, from, to);
            if (mountainHit != null) {
                return mountainHit;
            }
            return clipBox(valleyFloor, Direction.UP, from, to);
        };
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, impact.y, impact.z);

        Vec3 missedPearlPos = new Vec3(20.0D, 96.0D, 0.5D);
        Vec3 missedPearlVel = new Vec3(1.1D, -0.35D, 0.0D);
        CreepingDeathEntity.LivePearlPrediction prediction = CreepingDeathEntity.simulateLivePearlImpact(
                missedPearlPos,
                missedPearlVel,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(prediction.landingPos().y).isLessThanOrEqualTo(referenceY - CreepingDeathEntity.PEARL_MISS_DROP_THRESHOLD);
        assertThat(CreepingDeathEntity.isMissedMountainPearl(prediction, selfPos, targetPos, referenceY)).isTrue();

        CreepingDeathEntity.RecoveryPearlCandidate recovery = CreepingDeathEntity.evaluatePearlBackRecovery(
                missedPearlPos,
                missedPearlVel,
                launchStartPos,
                selfPos,
                selfPos,
                targetPos,
                referenceY,
                MIN_SAFE_Y,
                collider,
                collider,
                resolver
        );

        assertThat(recovery).isNotNull();
        assertThat(recovery.launchVelocity().y).isGreaterThan(0.5D);
        assertThat(recovery.flightTicks())
                .isGreaterThanOrEqualTo(prediction.remainingTicks() + CreepingDeathEntity.PEARL_RECOVERY_MIN_MARGIN_TICKS);
        assertThat(recovery.landingPos().y).isGreaterThanOrEqualTo(referenceY - CreepingDeathEntity.PEARL_RECOVERY_MAX_DROP_BELOW_REF);
    }

    @Test
    void doesNotThrowRecoveryPearlWhenLivePearlLandsOnMountainNearTarget() {
        Vec3 selfPos = new Vec3(0.5D, 100.0D, 0.5D);
        Vec3 launchStartPos = new Vec3(0.5D, 101.52D, 0.5D);
        Vec3 targetPos = new Vec3(14.5D, 100.0D, 0.5D);
        double referenceY = Math.min(selfPos.y, targetPos.y);

        AABB mountainTop = new AABB(-8.0D, 90.0D, -8.0D, 25.0D, 100.0D, 8.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> clipBox(mountainTop, Direction.UP, from, to);
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, 100.0D, impact.z);

        Vec3 onTargetPearlPos = new Vec3(8.0D, 103.0D, 0.5D);
        Vec3 onTargetPearlVel = new Vec3(0.9D, -0.25D, 0.0D);

        CreepingDeathEntity.RecoveryPearlCandidate recovery = CreepingDeathEntity.evaluatePearlBackRecovery(
                onTargetPearlPos,
                onTargetPearlVel,
                launchStartPos,
                selfPos,
                selfPos,
                targetPos,
                referenceY,
                MIN_SAFE_Y,
                collider,
                collider,
                resolver
        );

        assertThat(recovery).isNull();
    }

    @Test
    void waitsWhenMissedPearlRemainingFlightExceedsMaxRecoveryArcAndThrowsOnceInWindow() {
        Vec3 selfPos = new Vec3(0.5D, 200.0D, 0.5D);
        Vec3 launchStartPos = new Vec3(0.5D, 201.52D, 0.5D);
        Vec3 targetPos = new Vec3(10.5D, 200.0D, 0.5D);
        double referenceY = Math.min(selfPos.y, targetPos.y);

        AABB mountainTop = new AABB(-6.0D, 190.0D, -6.0D, 14.0D, 200.0D, 6.0D);
        AABB deepValleyFloor = new AABB(-20.0D, 29.0D, -20.0D, 220.0D, 30.0D, 20.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> {
            CreepingDeathEntity.PearlHit mountainHit = clipBox(mountainTop, Direction.UP, from, to);
            if (mountainHit != null) {
                return mountainHit;
            }
            return clipBox(deepValleyFloor, Direction.UP, from, to);
        };
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, impact.y, impact.z);

        Vec3 pearlPos = new Vec3(16.0D, 202.0D, 0.5D);
        Vec3 pearlVel = new Vec3(1.35D, 0.10D, 0.0D);

        CreepingDeathEntity.LivePearlPrediction initialPrediction = CreepingDeathEntity.simulateLivePearlImpact(
                pearlPos,
                pearlVel,
                MIN_SAFE_Y,
                collider,
                resolver
        );
        assertThat(initialPrediction.remainingTicks()).isGreaterThan(100);

        CreepingDeathEntity.RecoveryPearlCandidate tooEarly = CreepingDeathEntity.evaluatePearlBackRecovery(
                pearlPos,
                pearlVel,
                launchStartPos,
                selfPos,
                selfPos,
                targetPos,
                referenceY,
                MIN_SAFE_Y,
                collider,
                collider,
                resolver
        );
        assertThat(tooEarly).isNull();

        Vec3 simPos = pearlPos;
        Vec3 simVel = pearlVel;
        CreepingDeathEntity.RecoveryPearlCandidate timedRecovery = null;
        int remainingWhenThrown = -1;
        for (int elapsed = 1; elapsed <= 120; elapsed++) {
            simVel = new Vec3(simVel.x, simVel.y - CreepingDeathEntity.PEARL_GRAVITY, simVel.z)
                    .scale(CreepingDeathEntity.PEARL_AIR_DRAG);
            simPos = simPos.add(simVel);
            CreepingDeathEntity.RecoveryPearlCandidate candidate = CreepingDeathEntity.evaluatePearlBackRecovery(
                    simPos,
                    simVel,
                    launchStartPos,
                    selfPos,
                    selfPos,
                    targetPos,
                    referenceY,
                    MIN_SAFE_Y,
                    collider,
                    collider,
                    resolver
            );
            if (candidate != null) {
                CreepingDeathEntity.LivePearlPrediction predNow = CreepingDeathEntity.simulateLivePearlImpact(
                        simPos,
                        simVel,
                        MIN_SAFE_Y,
                        collider,
                        resolver
                );
                remainingWhenThrown = predNow.remainingTicks();
                timedRecovery = candidate;
                break;
            }
        }

        assertThat(timedRecovery).isNotNull();
        assertThat(timedRecovery.flightTicks())
                .isGreaterThanOrEqualTo(remainingWhenThrown + CreepingDeathEntity.PEARL_RECOVERY_MIN_MARGIN_TICKS);
        assertThat(timedRecovery.landingPos().y).isGreaterThanOrEqualTo(referenceY - CreepingDeathEntity.PEARL_RECOVERY_MAX_DROP_BELOW_REF);
    }

    @Test
    void rejectsRecoveryPearlWhenEnclosedCeilingIsAboveCreepingDeath() {
        Vec3 selfPos = new Vec3(0.5D, 100.0D, 0.5D);
        Vec3 launchStartPos = new Vec3(0.5D, 101.52D, 0.5D);
        Vec3 targetPos = new Vec3(10.5D, 100.0D, 0.5D);
        double referenceY = Math.min(selfPos.y, targetPos.y);

        AABB ceiling = new AABB(-12.0D, 105.0D, -12.0D, 16.0D, 110.0D, 12.0D);
        AABB mountainTop = new AABB(-8.0D, 90.0D, -8.0D, 16.0D, 100.0D, 8.0D);
        AABB valleyFloor = new AABB(-20.0D, 63.0D, -20.0D, 120.0D, 64.0D, 20.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> {
            CreepingDeathEntity.PearlHit ceilingHit = clipBox(ceiling, Direction.DOWN, from, to);
            if (ceilingHit != null) {
                return ceilingHit;
            }
            CreepingDeathEntity.PearlHit mountainHit = clipBox(mountainTop, Direction.UP, from, to);
            if (mountainHit != null) {
                return mountainHit;
            }
            return clipBox(valleyFloor, Direction.UP, from, to);
        };
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, impact.y, impact.z);

        Vec3 missedPearlPos = new Vec3(20.0D, 96.0D, 0.5D);
        Vec3 missedPearlVel = new Vec3(1.0D, -0.35D, 0.0D);

        CreepingDeathEntity.RecoveryPearlCandidate recovery = CreepingDeathEntity.evaluatePearlBackRecovery(
                missedPearlPos,
                missedPearlVel,
                launchStartPos,
                selfPos,
                selfPos,
                targetPos,
                referenceY,
                MIN_SAFE_Y,
                collider,
                collider,
                resolver
        );

        assertThat(recovery).isNull();
    }

    @Test
    void rejectsRecoveryPearlWhenMountainLandingIsUnsafeLavaOrVoid() {
        Vec3 selfPos = new Vec3(0.5D, 100.0D, 0.5D);
        Vec3 launchStartPos = new Vec3(0.5D, 101.52D, 0.5D);
        Vec3 targetPos = new Vec3(12.5D, 100.0D, 0.5D);
        double referenceY = Math.min(selfPos.y, targetPos.y);

        AABB mountainTop = new AABB(-8.0D, 90.0D, -8.0D, 16.0D, 100.0D, 8.0D);
        AABB valleyFloor = new AABB(-20.0D, 63.0D, -20.0D, 120.0D, 64.0D, 20.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> {
            CreepingDeathEntity.PearlHit mountainHit = clipBox(mountainTop, Direction.UP, from, to);
            if (mountainHit != null) {
                return mountainHit;
            }
            return clipBox(valleyFloor, Direction.UP, from, to);
        };
        CreepingDeathEntity.PearlLandingResolver unsafeMountainResolver = (impact, face, blockPos) ->
                impact.y >= 95.0D ? null : new Vec3(impact.x, impact.y, impact.z);

        Vec3 missedPearlPos = new Vec3(20.0D, 96.0D, 0.5D);
        Vec3 missedPearlVel = new Vec3(1.0D, -0.35D, 0.0D);

        CreepingDeathEntity.RecoveryPearlCandidate recovery = CreepingDeathEntity.evaluatePearlBackRecovery(
                missedPearlPos,
                missedPearlVel,
                launchStartPos,
                selfPos,
                selfPos,
                targetPos,
                referenceY,
                MIN_SAFE_Y,
                collider,
                collider,
                unsafeMountainResolver
        );

        assertThat(recovery).isNull();
    }

    @Test
    void selectsUpwardRecoveryPearlOntoMountainLedgeWhenFallingOffCliff() {
        Vec3 launchStartPos = new Vec3(-4.0D, 94.52D, 0.5D);
        Vec3 targetPos = new Vec3(6.5D, 100.0D, 0.5D);

        AABB mountainLedge = new AABB(0.0D, 90.0D, -8.0D, 18.0D, 100.0D, 8.0D);
        AABB valleyFloor = new AABB(-40.0D, 63.0D, -20.0D, 40.0D, 64.0D, 20.0D);
        CreepingDeathEntity.PearlRayCollider collider = (from, to) -> {
            CreepingDeathEntity.PearlHit ledgeHit = clipBox(mountainLedge, Direction.UP, from, to);
            if (ledgeHit != null) {
                return ledgeHit;
            }
            return clipBox(valleyFloor, Direction.UP, from, to);
        };
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) ->
                new Vec3(impact.x, impact.y, impact.z);

        CreepingDeathEntity.RecoveryPearlCandidate recovery = CreepingDeathEntity.selectRecoveryPearlCandidate(
                launchStartPos,
                targetPos,
                targetPos,
                targetPos.y,
                CreepingDeathEntity.PEARL_MIN_FLIGHT_TICKS,
                MIN_SAFE_Y,
                collider,
                resolver
        );

        assertThat(recovery).isNotNull();
        assertThat(recovery.launchVelocity().y).isGreaterThan(0.5D);
        assertThat(recovery.landingPos().y).isEqualTo(100.0D);
        assertThat(recovery.landingPos().x).isGreaterThanOrEqualTo(0.0D);
    }

    @Test
    void skipsRecoveryRaycastsWhileWaitingOutsideTimingWindow() {
        Vec3 selfPos = new Vec3(0.5D, 200.0D, 0.5D);
        Vec3 launchStartPos = new Vec3(0.5D, 201.52D, 0.5D);
        Vec3 targetPos = new Vec3(10.5D, 200.0D, 0.5D);
        int[] recoveryRaycastCount = new int[]{0};

        CreepingDeathEntity.PearlRayCollider countingCollider = (from, to) -> {
            recoveryRaycastCount[0]++;
            return null;
        };
        CreepingDeathEntity.PearlLandingResolver resolver = (impact, face, blockPos) -> impact;

        CreepingDeathEntity.RecoveryPearlCandidate tooFar = CreepingDeathEntity.selectRecoveryPearlCandidate(
                launchStartPos,
                selfPos,
                targetPos,
                selfPos.y,
                120,
                MIN_SAFE_Y,
                countingCollider,
                resolver
        );
        CreepingDeathEntity.RecoveryPearlCandidate pastMaxSim = CreepingDeathEntity.selectRecoveryPearlCandidate(
                launchStartPos,
                selfPos,
                targetPos,
                selfPos.y,
                CreepingDeathEntity.PEARL_RECOVERY_MAX_SIM_TICKS + 5,
                MIN_SAFE_Y,
                countingCollider,
                resolver
        );

        assertThat(tooFar).isNull();
        assertThat(pastMaxSim).isNull();
        assertThat(recoveryRaycastCount[0]).isZero();
    }

    private static CreepingDeathEntity.PearlHit clipBox(AABB box, Direction face, Vec3 from, Vec3 to) {
        Optional<Vec3> hit = box.clip(from, to);
        return hit.map(loc -> new CreepingDeathEntity.PearlHit(loc, face, BlockPos.containing(loc), false))
                .orElse(null);
    }
}
