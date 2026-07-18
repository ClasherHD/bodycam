package dev.ClasherHD.bodycam.util;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.network.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.CrossObservationSyncPacket;
import dev.ClasherHD.bodycam.network.ServerNetworking;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BodycamLifecycle {
    public static final ConcurrentHashMap<UUID, LockData> POSITION_LOCKS = new ConcurrentHashMap<>();

    public static class LockData {
        public final ResourceKey<Level> originalDim;
        public final double x, y, z;
        public final float yaw, pitch;
        public final GameType targetGameMode;
        public final UUID dummyUUID;
        public final ResourceKey<Level> dummyDimKey;
        public final Vec3 motion;
        public final float fallDistance;
        public int lockTicks = 2;

        public LockData(ResourceKey<Level> originalDim, double x, double y, double z, float yaw, float pitch, GameType targetGameMode, UUID dummyUUID, ResourceKey<Level> dummyDimKey, Vec3 motion, float fallDistance) {
            this.originalDim = originalDim;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.targetGameMode = targetGameMode;
            this.dummyUUID = dummyUUID;
            this.dummyDimKey = dummyDimKey;
            this.motion = motion;
            this.fallDistance = fallDistance;
        }
    }

    public static void init() {
        PlayerEvent.PLAYER_JOIN.register(BodycamLifecycle::handlePlayerLogin);
        PlayerEvent.PLAYER_QUIT.register(BodycamLifecycle::handlePlayerLogout);
        TickEvent.PLAYER_POST.register(BodycamLifecycle::handlePlayerTick);
    }

    private static void handlePlayerTick(net.minecraft.world.entity.player.Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer observer)) return;
        
        if (POSITION_LOCKS.containsKey(observer.getUUID())) {
            LockData data = POSITION_LOCKS.get(observer.getUUID());
            if (data != null) {
                if (data.lockTicks > 0) {
                    data.lockTicks--;
                    if (data.lockTicks == 0) {
                        if (data.dummyUUID != null) {
                            ServerLevel dummyLevel = observer.getServer().getLevel(data.dummyDimKey);
                            if (dummyLevel != null) {
                                Entity dummy = dummyLevel.getEntity(data.dummyUUID);
                                if (dummy != null) {
                                    dummy.discard();
                                }
                            }
                        }
                        observer.teleportTo(observer.getServer().getLevel(data.originalDim), data.x, data.y, data.z, data.yaw, data.pitch);
                        observer.setDeltaMovement(data.motion);
                        observer.hurtMarked = true;
                        observer.fallDistance = data.fallDistance;
                        if (data.targetGameMode != null) {
                            observer.setGameMode(data.targetGameMode);
                        }
                        observer.setInvisible(false);
                        POSITION_LOCKS.remove(observer.getUUID());
                    } else {
                        observer.teleportTo(observer.getServer().getLevel(data.originalDim), data.x, data.y, data.z, data.yaw, data.pitch);
                    }
                }
            }
        }

        if (BodycamHelper.getPersistentData(observer).getBoolean("bodycam_active")) {
            if (BodycamHelper.getPersistentData(observer).contains("bodycam_target_uuid")) {
                UUID targetId = BodycamHelper.getPersistentData(observer).getUUID("bodycam_target_uuid");
                if (targetId != null) {
                    ServerPlayer target = observer.getServer().getPlayerList().getPlayer(targetId);
                    if (target == null) {
                        ServerNetworking.executeReset(observer);
                        NetworkManager.sendToPlayer(observer, new BodycamForceClosePacket());
                    } else {
                        boolean forceClose = false;
                        boolean bypassDist = BodycamHelper.getPersistentData(observer).getBoolean("bodycam_bypass_dist");
                        
                        long lastJammer = BodycamHelper.getPersistentData(target).getLong("bodycam_jammer_heartbeat");
                        int jammerMode = BodycamHelper.getPersistentData(target).getInt("bodycam_jammer_mode");
                        boolean isJammerActive = (target.level().getGameTime() - lastJammer) <= 10;
                        if (!isJammerActive) jammerMode = 0;

                        if (jammerMode == 1) {
                            forceClose = true;
                            observer.sendSystemMessage(Component.translatable("message.bodycam.jammer_blocked").withStyle(ChatFormatting.RED));
                        } else if (jammerMode == 2 || !bypassDist) {
                            String origDim = BodycamHelper.getPersistentData(observer).getString("bodycam_orig_dim");
                            double ox = BodycamHelper.getPersistentData(observer).getDouble("bodycam_orig_x");
                            double oy = BodycamHelper.getPersistentData(observer).getDouble("bodycam_orig_y");
                            double oz = BodycamHelper.getPersistentData(observer).getDouble("bodycam_orig_z");

                            UUID dummyUUID = null;
                            if (BodycamHelper.getPersistentData(observer).contains("bodycam_dummy_uuid")) {
                                dummyUUID = BodycamHelper.getPersistentData(observer).getUUID("bodycam_dummy_uuid");
                            }
                            dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy = null;
                            if (dummyUUID != null) {
                                for (ServerLevel lvl : observer.getServer().getAllLevels()) {
                                    Entity e = lvl.getEntity(dummyUUID);
                                    if (e instanceof dev.ClasherHD.bodycam.entity.BodycamDummyEntity d) {
                                        dummy = d;
                                        break;
                                    }
                                }
                            }

                            if (dummy != null) {
                                ox = dummy.getX();
                                oy = dummy.getY();
                                oz = dummy.getZ();
                                origDim = dummy.level().dimension().location().toString();
                            } else if (dummyUUID != null) {
                                Vec3 dummyPos = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.get(observer.getUUID());
                                if (dummyPos != null) {
                                    ox = dummyPos.x;
                                    oy = dummyPos.y;
                                    oz = dummyPos.z;
                                }
                            }

                            if (!origDim.equals(target.level().dimension().location().toString())) {
                                forceClose = true;
                            } else {
                                double distSqr = target.distanceToSqr(ox, oy, oz);
                                double maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
                                if (distSqr > maxDist * maxDist) {
                                    forceClose = true;
                                }
                            }
                            if (forceClose && jammerMode != 2) {
                                 observer.sendSystemMessage(Component.translatable("message.bodycam.signal_weak").withStyle(ChatFormatting.RED));
                            } else if (forceClose) {
                                 observer.sendSystemMessage(Component.translatable("message.bodycam.jammer_blocked").withStyle(ChatFormatting.RED));
                            }
                        }

                        if (forceClose) {
                            ServerNetworking.executeReset(observer);
                            NetworkManager.sendToPlayer(observer, new BodycamForceClosePacket());
                        } else {
                            if (observer.level().dimension() != target.level().dimension()) {
                                BodycamHelper.getPersistentData(observer).putBoolean("bodycam_allow_teleport", true);
                                observer.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
                                BodycamHelper.getPersistentData(observer).remove("bodycam_allow_teleport");
                                observer.setCamera(target);
                            } else if (observer.getCamera() != target) {
                                observer.setCamera(target);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void handlePlayerLogin(ServerPlayer player) {
        if (BodycamHelper.getPersistentData(player).getBoolean("bodycam_active")) {
            double x = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_x");
            double y = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_y");
            double z = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_z");
            float yaw = BodycamHelper.getPersistentData(player).getFloat("bodycam_orig_yrot");
            float pitch = BodycamHelper.getPersistentData(player).getFloat("bodycam_orig_xrot");
            String dim = BodycamHelper.getPersistentData(player).getString("bodycam_orig_dim");
            int gmId = BodycamHelper.getPersistentData(player).getInt("bodycam_original_gamemode");
            double mx = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_motx");
            double my = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_moty");
            double mz = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_motz");
            float fall = BodycamHelper.getPersistentData(player).getFloat("bodycam_orig_fall");

            UUID dummyUUID = null;
            if (BodycamHelper.getPersistentData(player).contains("bodycam_dummy_uuid")) {
                dummyUUID = BodycamHelper.getPersistentData(player).getUUID("bodycam_dummy_uuid");
            }

            ResourceKey<Level> dimKey = Level.OVERWORLD;
            for (ResourceKey<Level> key : player.getServer().levelKeys()) {
                if (key.location().toString().equals(dim)) {
                    dimKey = key;
                    break;
                }
            }

            POSITION_LOCKS.put(player.getUUID(), new LockData(
                    dimKey, x, y, z, yaw, pitch,
                    GameType.byId(gmId),
                    dummyUUID,
                    dimKey,
                    new Vec3(mx, my, mz),
                    fall
            ));

            BodycamHelper.getPersistentData(player).remove("bodycam_active");
            BodycamHelper.getPersistentData(player).remove("bodycam_target_uuid");
            BodycamHelper.getPersistentData(player).remove("bodycam_dummy_uuid");
            player.setCamera(player);
        }
    }

    private static void handlePlayerLogout(ServerPlayer player) {
        POSITION_LOCKS.remove(player.getUUID());
        if (BodycamHelper.getPersistentData(player).getBoolean("bodycam_active")) {
            UUID targetId = BodycamHelper.getPersistentData(player).getUUID("bodycam_target_uuid");
            if (targetId != null) {
                ServerPlayer target = player.getServer().getPlayerList().getPlayer(targetId);
                if (target != null) {
                    NetworkManager.sendToPlayer(target, new CrossObservationSyncPacket(player.getUUID(), false));
                }
            }
            ServerNetworking.cleanupDummy(player);
        }
    }
}
