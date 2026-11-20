package dev.ClasherHD.bodycam.client.camera;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public final class CameraSnapState {
    private static volatile boolean pendingSnap = false;
    private static volatile int lastRemovedDummyId = -1;
    private static volatile int snapEntityId = -1;
    private static volatile boolean observerInMonitorSession = false;

    public record SnapRecord(double x, double y, double z, float yRot, float xRot, int removedDummyEntityId) {
    }

    private record RemoteSnapGuard(double x, double y, double z, long expireAtMs) {
    }

    private static final Map<UUID, SnapRecord> PENDING_EXITS = new ConcurrentHashMap<>();
    private static final Map<Integer, RemoteSnapGuard> REMOTE_EXIT_GUARDS = new ConcurrentHashMap<>();

    private CameraSnapState() {
    }

    public static void setObserverInMonitorSession(boolean active) {
        observerInMonitorSession = active;
    }

    public static boolean isObserverInMonitorSession() {
        return observerInMonitorSession;
    }

    public static void snapEntityWithoutInterpolation(Entity entity, double x, double y, double z,
                                                      float yRot, float xRot,
                                                      double vx, double vy, double vz,
                                                      double fallDistance) {
        snapEntityState(entity, x, y, z, yRot, xRot, vx, vy, vz, fallDistance, false, true);
    }

    public static void snapRemoteEntityWithoutInterpolation(Entity entity, double x, double y, double z,
                                                            float yRot, float xRot,
                                                            double vx, double vy, double vz,
                                                            double fallDistance, boolean invisible) {
        snapEntityState(entity, x, y, z, yRot, xRot, vx, vy, vz, fallDistance, invisible, false);
    }

    public static void snapRemoteObserverOnExit(Entity observer, int entityId, double x, double y, double z,
                                                float yRot, float xRot,
                                                double vx, double vy, double vz,
                                                double fallDistance, boolean invisible) {
        armRemoteExitSnap(entityId, x, y, z);
        if (observer != null) {
            snapRemoteEntityWithoutInterpolation(observer, x, y, z, yRot, xRot, vx, vy, vz, fallDistance, invisible);
        }
    }

    public static void armRemoteExitSnap(int entityId, double x, double y, double z) {
        if (entityId >= 0) {
            REMOTE_EXIT_GUARDS.put(entityId, new RemoteSnapGuard(x, y, z, System.currentTimeMillis() + 300L));
        }
    }

    public static boolean hasRemoteExitGuard(int entityId) {
        RemoteSnapGuard guard = REMOTE_EXIT_GUARDS.get(entityId);
        if (guard == null) {
            return false;
        }
        if (System.currentTimeMillis() > guard.expireAtMs()) {
            REMOTE_EXIT_GUARDS.remove(entityId, guard);
            return false;
        }
        return true;
    }

    public static void handleRemoteVanillaPositionPacket(Entity entity, boolean positionUpdated) {
        if (entity == null) {
            return;
        }
        RemoteSnapGuard guard = REMOTE_EXIT_GUARDS.get(entity.getId());
        if (guard == null) {
            return;
        }
        if (System.currentTimeMillis() > guard.expireAtMs()) {
            REMOTE_EXIT_GUARDS.remove(entity.getId(), guard);
            return;
        }
        Vec3 interpPos = entity instanceof LivingEntity living ? living.getInterpolation().position() : entity.position();
        double tx = interpPos.x;
        double ty = interpPos.y;
        double tz = interpPos.z;
        float tyRot = entity instanceof LivingEntity living ? living.getInterpolation().yRot() : entity.getYRot();
        float txRot = entity instanceof LivingEntity living ? living.getInterpolation().xRot() : entity.getXRot();
        snapRemoteEntityWithoutInterpolation(
                entity, tx, ty, tz, tyRot, txRot,
                entity.getDeltaMovement().x, entity.getDeltaMovement().y, entity.getDeltaMovement().z,
                entity.fallDistance, entity.isInvisible()
        );
        if (positionUpdated) {
            double dx = tx - guard.x();
            double dy = ty - guard.y();
            double dz = tz - guard.z();
            if (dx * dx + dy * dy + dz * dz <= 4.0D) {
                REMOTE_EXIT_GUARDS.remove(entity.getId(), guard);
            }
        }
    }

    public static void handleRemoteVanillaHeadPacket(Entity entity, float yHeadRot) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        RemoteSnapGuard guard = REMOTE_EXIT_GUARDS.get(entity.getId());
        if (guard == null) {
            return;
        }
        if (System.currentTimeMillis() > guard.expireAtMs()) {
            REMOTE_EXIT_GUARDS.remove(entity.getId(), guard);
            return;
        }
        living.yHeadRot = yHeadRot;
        living.yHeadRotO = yHeadRot;
        living.lerpHeadTo(yHeadRot, 0);
    }

    private static void snapEntityState(Entity entity, double x, double y, double z,
                                        float yRot, float xRot,
                                        double vx, double vy, double vz,
                                        double fallDistance, boolean invisible, boolean syncCodec) {
        if (entity == null) {
            return;
        }
        entity.setPosRaw(x, y, z);
        entity.setBoundingBox(entity.getDimensions(entity.getPose()).makeBoundingBox(x, y, z));
        entity.xo = x;
        entity.yo = y;
        entity.zo = z;
        entity.xOld = x;
        entity.yOld = y;
        entity.zOld = z;
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.yRotO = yRot;
        entity.xRotO = xRot;
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = yRot;
            living.yBodyRotO = yRot;
            living.yHeadRot = yRot;
            living.yHeadRotO = yRot;
            living.getInterpolation().cancel();
            living.lerpHeadTo(yRot, 0);
            if (living.walkAnimation != null) {
                living.walkAnimation.setSpeed(0.0F);
                living.walkAnimation.update(0.0F, 1.0F, 1.0F);
                living.walkAnimation.setSpeed(0.0F);
            }
        }
        if (entity instanceof AbstractClientPlayer clientPlayer) {
            clientPlayer.avatarState().resetBob();
            clientPlayer.avatarState().resetBob();
        }
        if (syncCodec) {
            entity.syncPacketPositionCodec(x, y, z);
        }
        entity.setDeltaMovement(vx, vy, vz);
        entity.fallDistance = fallDistance;
        entity.setInvisible(invisible);
    }

    public static void recordAtomicExit(Entity player, double x, double y, double z,
                                        float yRot, float xRot,
                                        double vx, double vy, double vz,
                                        double fallDistance, int removedDummyId) {
        observerInMonitorSession = false;
        snapEntityWithoutInterpolation(player, x, y, z, yRot, xRot, vx, vy, vz, fallDistance);
        lastRemovedDummyId = removedDummyId;
        snapEntityId = player != null ? player.getId() : -1;
        pendingSnap = player != null;
    }

    public static void enforceRenderFrameGuard(Entity cameraEntity) {
        if (!pendingSnap || cameraEntity == null) {
            return;
        }
        if (snapEntityId >= 0 && cameraEntity.getId() != snapEntityId && !(cameraEntity instanceof Player)) {
            return;
        }
        consumeOneShotSnapHistory(cameraEntity);
    }

    public static void tickFrameGuard(Entity player) {
        if (!pendingSnap) {
            return;
        }
        if (player != null) {
            consumeOneShotSnapHistory(player);
        } else {
            pendingSnap = false;
        }
    }

    private static void consumeOneShotSnapHistory(Entity entity) {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();
        entity.xo = x;
        entity.yo = y;
        entity.zo = z;
        entity.xOld = x;
        entity.yOld = y;
        entity.zOld = z;
        entity.yRotO = yRot;
        entity.xRotO = xRot;
        if (entity instanceof LivingEntity living) {
            living.yBodyRotO = living.yBodyRot;
            living.yHeadRotO = living.yHeadRot;
        }
        if (entity instanceof AbstractClientPlayer clientPlayer) {
            clientPlayer.avatarState().resetBob();
            clientPlayer.avatarState().resetBob();
        }
        pendingSnap = false;
    }

    public static boolean isPendingSnap() {
        return pendingSnap;
    }

    public static int getLastRemovedDummyId() {
        return lastRemovedDummyId;
    }

    public static double getInterpolatedX(Entity entity, float partialTick) {
        enforceRenderFrameGuard(entity);
        return entity.xo + (entity.getX() - entity.xo) * partialTick;
    }

    public static double getInterpolatedY(Entity entity, float partialTick) {
        enforceRenderFrameGuard(entity);
        return entity.yo + (entity.getY() - entity.yo) * partialTick;
    }

    public static double getInterpolatedZ(Entity entity, float partialTick) {
        enforceRenderFrameGuard(entity);
        return entity.zo + (entity.getZ() - entity.zo) * partialTick;
    }

    public static float getInterpolatedYRot(Entity entity, float partialTick) {
        enforceRenderFrameGuard(entity);
        return entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTick;
    }

    public static float getInterpolatedXRot(Entity entity, float partialTick) {
        enforceRenderFrameGuard(entity);
        return entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTick;
    }

    public static void recordAtomicExit(
            UUID playerId,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            int removedDummyId,
            Entity player
    ) {
        if (playerId != null) {
            PENDING_EXITS.put(playerId, new SnapRecord(x, y, z, yRot, xRot, removedDummyId));
        }
        if (player != null) {
            recordAtomicExit(player, x, y, z, yRot, xRot, 0.0D, 0.0D, 0.0D, 0.0D, removedDummyId);
        }
    }

    public static SnapRecord peekPending(UUID playerId) {
        return playerId == null ? null : PENDING_EXITS.get(playerId);
    }

    public static SnapRecord consumePending(UUID playerId) {
        return playerId == null ? null : PENDING_EXITS.remove(playerId);
    }

    public static void clearAll() {
        PENDING_EXITS.clear();
        reset();
    }

    public static void reset() {
        pendingSnap = false;
        lastRemovedDummyId = -1;
        snapEntityId = -1;
        observerInMonitorSession = false;
        REMOTE_EXIT_GUARDS.clear();
    }
}
