package dev.ClasherHD.bodycam.network.bodycam;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.enchantment.NightspyEffectHandler;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.event.ServerEvents;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

@SuppressWarnings("null")
public class BodycamResetCameraPacket implements CustomPacketPayload {
    public static final Type<BodycamResetCameraPacket> TYPE = new Type<>(bodycam.id("bodycam_reset_camera"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodycamResetCameraPacket> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> {
            }, buf -> new BodycamResetCameraPacket());

    public BodycamResetCameraPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BodycamResetCameraPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sender) {
                executeReset(sender);
            }
        });
    }

    public static void executeReset(ServerPlayer sender) {
        if (sender == null || sender.server == null) {
            return;
        }
        CompoundTag data = PersistentDataAccess.get(sender);
        boolean hasActiveSession = data.getBoolean("bodycam_active")
                || data.contains("bodycam_dummy_uuid")
                || data.contains("bodycam_target_uuid")
                || BodycamSetCameraPacket.ORIGINAL_POS.containsKey(sender.getUUID());
        if (!hasActiveSession) {
            data.remove("bodycam_all_seeing_pending");
            return;
        }

        NightspyEffectHandler.end(sender);

        BodycamDummyEntity dummy = null;
        if (data.contains("bodycam_dummy_uuid")) {
            UUID dummyId = data.getUUID("bodycam_dummy_uuid");
            for (ServerLevel lvl : sender.server.getAllLevels()) {
                Entity e = lvl.getEntity(dummyId);
                if (e instanceof BodycamDummyEntity d) {
                    dummy = d;
                    break;
                }
            }
        }

        int removedDummyEntityId = dummy != null ? dummy.getId() : -1;

        int defaultGm = data.contains("bodycam_original_gamemode")
                ? data.getInt("bodycam_original_gamemode")
                : GameType.SURVIVAL.getId();
        int gameModeId = BodycamSetCameraPacket.ORIGINAL_GAMEMODE.getOrDefault(sender.getUUID(), defaultGm);
        GameType originalGameType = GameType.byId(gameModeId);

        if (!sender.isAlive()) {
            if (dummy != null) {
                dummy.discard();
            }
            sender.setGameMode(originalGameType != null ? originalGameType : GameType.SURVIVAL);
            sender.setCamera(sender);
            notifyOldTargetObservationEnd(sender, data);
            cleanupPlayerNbtAndMaps(sender);
            return;
        }

        ServerLevel originalLevel = sender.serverLevel();
        double x = sender.getX();
        double y = sender.getY();
        double z = sender.getZ();
        float xRot = sender.getXRot();
        float yRot = sender.getYRot();
        Vec3 lastPos;
        float lastFall;
        Vec3 motion;

        if (dummy != null && dummy.level() instanceof ServerLevel dummyLevel) {
            originalLevel = dummyLevel;
            lastPos = dummy.position();
            lastFall = dummy.fallDistance;
            motion = dummy.getDeltaMovement();
            yRot = dummy.getYRot();
            xRot = dummy.getXRot();
        } else {
            String dimStr = BodycamSetCameraPacket.ORIGINAL_DIM.getOrDefault(
                    sender.getUUID(), data.getString("bodycam_orig_dim"));
            if (dimStr != null && !dimStr.isEmpty()) {
                ResourceLocation rl = ResourceLocation.tryParse(dimStr);
                if (rl != null) {
                    ServerLevel resolved = sender.server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
                    originalLevel = resolved != null ? resolved : sender.server.overworld();
                } else {
                    originalLevel = sender.server.overworld();
                }
            }
            Vec3 origPos = BodycamSetCameraPacket.ORIGINAL_POS.get(sender.getUUID());
            if (origPos != null) {
                x = origPos.x;
                y = origPos.y;
                z = origPos.z;
            } else if (data.contains("bodycam_orig_x")) {
                x = data.getDouble("bodycam_orig_x");
                y = data.getDouble("bodycam_orig_y");
                z = data.getDouble("bodycam_orig_z");
            }
            Vec2 origRot = BodycamSetCameraPacket.ORIGINAL_ROT.get(sender.getUUID());
            if (origRot != null) {
                xRot = origRot.x;
                yRot = origRot.y;
            } else if (data.contains("bodycam_orig_yrot")) {
                yRot = data.getFloat("bodycam_orig_yrot");
                xRot = data.getFloat("bodycam_orig_xrot");
            }
            lastPos = BodycamDummyEntity.DUMMY_POS.getOrDefault(sender.getUUID(), new Vec3(x, y, z));
            lastFall = BodycamDummyEntity.DUMMY_FALL.getOrDefault(sender.getUUID(), 0.0F);
            motion = BodycamDummyEntity.DUMMY_MOTION.getOrDefault(sender.getUUID(), Vec3.ZERO);
        }

        boolean crossDim = !originalLevel.dimension().equals(sender.level().dimension());
        boolean involvesCustDim = !originalLevel.dimension().location().getNamespace().equals("minecraft")
                || !sender.level().dimension().location().getNamespace().equals("minecraft");
        if (crossDim && involvesCustDim) {
            ServerEvents.POSITION_LOCKS.put(sender.getUUID(), new ServerEvents.LockData(
                    originalLevel.dimension(),
                    lastPos.x,
                    lastPos.y,
                    lastPos.z,
                    yRot,
                    xRot
            ));
            sender.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false, false));
            sender.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 255, false, false, false));
        }

        GameType targetGameMode = originalGameType != null ? originalGameType : GameType.SURVIVAL;
        sender.teleportTo(originalLevel, lastPos.x, lastPos.y, lastPos.z, Set.of(), yRot, xRot);
        sender.setCamera(sender);
        sender.setOldPosAndRot();
        sender.yBodyRot = yRot;
        sender.yBodyRotO = yRot;
        sender.yHeadRot = yRot;
        sender.yHeadRotO = yRot;
        sender.setDeltaMovement(motion);
        sender.hurtMarked = true;
        sender.fallDistance = lastFall;

        BodycamExitSyncS2CPacket remoteSnapPacket = new BodycamExitSyncS2CPacket(
                lastPos.x,
                lastPos.y,
                lastPos.z,
                yRot,
                xRot,
                motion.x,
                motion.y,
                motion.z,
                lastFall,
                targetGameMode.getId(),
                removedDummyEntityId,
                sender.getId(),
                false
        );
        for (ServerPlayer other : originalLevel.players()) {
            if (other != sender && other.distanceToSqr(lastPos.x, lastPos.y, lastPos.z) <= 512.0D * 512.0D) {
                PacketHandler.sendToPlayer(other, remoteSnapPacket);
            }
        }

        if (dummy != null) {
            dummy.discard();
        }

        sender.setInvisible(false);
        sender.setGameMode(targetGameMode);

        PacketHandler.sendToPlayer(sender, new BodycamExitSyncS2CPacket(
                lastPos.x,
                lastPos.y,
                lastPos.z,
                yRot,
                xRot,
                motion.x,
                motion.y,
                motion.z,
                lastFall,
                targetGameMode.getId(),
                removedDummyEntityId,
                sender.getId(),
                false
        ));
        PacketHandler.sendToPlayer(sender, new BodycamForceClosePacket());

        notifyOldTargetObservationEnd(sender, data);
        cleanupPlayerNbtAndMaps(sender);
    }

    private static void notifyOldTargetObservationEnd(ServerPlayer sender, CompoundTag data) {
        if (data.contains("bodycam_target_uuid")) {
            UUID oldTargetId = data.getUUID("bodycam_target_uuid");
            if (oldTargetId != null && sender.server != null) {
                ServerPlayer oldTarget = sender.server.getPlayerList().getPlayer(oldTargetId);
                if (oldTarget != null) {
                    PacketHandler.sendToPlayer(oldTarget, new CrossObservationSyncPacket(sender.getUUID(), false));
                }
            }
        }
    }

    public static void cleanupPlayerNbtAndMaps(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag data = PersistentDataAccess.get(player);
        data.remove("bodycam_active");
        data.remove("bodycam_watching_watchpoint");
        data.remove("bodycam_target_uuid");
        data.remove("bodycam_disconnect_ticks");
        data.remove("bodycam_reach_level");
        data.remove("bodycam_dummy_uuid");
        data.remove("bodycam_original_gamemode");
        data.remove("bodycam_orig_dim");
        data.remove("bodycam_orig_x");
        data.remove("bodycam_orig_y");
        data.remove("bodycam_orig_z");
        data.remove("bodycam_orig_yrot");
        data.remove("bodycam_orig_xrot");
        data.remove("bodycam_original_dimension");
        data.remove("bodycam_on_hologram");
        if (data.getBoolean("bodycam_all_seeing_active")) {
            data.remove("bodycam_all_seeing_active");
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ALL_SEEING_EYE_EXPIRE.get(), SoundSource.PLAYERS, 0.9F, 1.2F);
            player.sendSystemMessage(Component.translatable("message.bodycam.all_seeing_eye.expired").withStyle(ChatFormatting.GOLD));
        }
        data.remove("bodycam_all_seeing_pending");
        data.remove("bodycam_all_seeing_fog_ticks");
        NightspyEffectHandler.clear(player);

        UUID id = player.getUUID();
        BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(id);
        BodycamSetCameraPacket.ORIGINAL_DIM.remove(id);
        BodycamSetCameraPacket.ORIGINAL_POS.remove(id);
        BodycamSetCameraPacket.ORIGINAL_ROT.remove(id);
        BodycamDummyEntity.DUMMY_POS.remove(id);
        BodycamDummyEntity.DUMMY_FALL.remove(id);
        BodycamDummyEntity.DUMMY_MOTION.remove(id);
        ServerEvents.POSITION_LOCKS.remove(id);
    }
}
