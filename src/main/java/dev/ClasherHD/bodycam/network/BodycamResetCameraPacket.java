package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BodycamResetCameraPacket {

    public BodycamResetCameraPacket() {
    }

    public static void encode(BodycamResetCameraPacket msg, FriendlyByteBuf buf) {
    }

    public static BodycamResetCameraPacket decode(FriendlyByteBuf buf) {
        return new BodycamResetCameraPacket();
    }

    public static void executeReset(ServerPlayer sender) {
        if (sender != null && sender.server != null) {
            if (!sender.isAlive()) {
                int gameModeId = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.getOrDefault(sender.getUUID(), net.minecraft.world.level.GameType.SURVIVAL.getId());
                net.minecraft.world.level.GameType originalGameType = net.minecraft.world.level.GameType.byId(gameModeId);
                sender.setGameMode(originalGameType != null ? originalGameType : net.minecraft.world.level.GameType.SURVIVAL);
                sender.setCamera(sender);

                if (sender.getPersistentData().contains("bodycam_target_uuid")) {
                    java.util.UUID oldTargetId = sender.getPersistentData().getUUID("bodycam_target_uuid");
                    if (oldTargetId != null) {
                        net.minecraft.server.level.ServerPlayer oldTarget = sender.server.getPlayerList().getPlayer(oldTargetId);
                        if (oldTarget != null) {
                            dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> oldTarget),
                                    new dev.ClasherHD.bodycam.network.CrossObservationSyncPacket(sender.getUUID(), false)
                            );
                        }
                    }
                }

                sender.getPersistentData().putBoolean("bodycam_active", false);
                sender.getPersistentData().remove("bodycam_target_uuid");
                sender.getPersistentData().remove("bodycam_disconnect_ticks");
                sender.getPersistentData().remove("bodycam_has_reach");
                sender.getPersistentData().remove("bodycam_original_dimension");
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(sender.getUUID());
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.remove(sender.getUUID());
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.remove(sender.getUUID());
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT.remove(sender.getUUID());
                dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.remove(sender.getUUID());
                dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_FALL.remove(sender.getUUID());
                dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_MOTION.remove(sender.getUUID());
                return;
            }
            net.minecraft.nbt.CompoundTag tag = sender.getPersistentData();

            double x = sender.getX();
            double y = sender.getY();
            double z = sender.getZ();
            float xRot = sender.getXRot();
            float yRot = sender.getYRot();

            net.minecraft.server.level.ServerLevel originalLevel = sender.serverLevel();
            String dimStr = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM
                    .getOrDefault(sender.getUUID(), "");
            if (!dimStr.isEmpty()) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = net.minecraft.resources.ResourceKey
                        .create(net.minecraft.core.registries.Registries.DIMENSION,
                                new net.minecraft.resources.ResourceLocation(dimStr));
                originalLevel = sender.server.getLevel(dimKey);
                if (originalLevel == null) {
                    originalLevel = sender.server.overworld();
                }
            }

            net.minecraft.world.phys.Vec3 origPos = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS
                    .get(sender.getUUID());
            if (origPos != null) {
                x = origPos.x;
                y = origPos.y;
                z = origPos.z;
            }
            net.minecraft.world.phys.Vec2 origRot = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT
                    .get(sender.getUUID());
            if (origRot != null) {
                xRot = origRot.x;
                yRot = origRot.y;
            }

            final float finalXRot = xRot;
            final float finalYRot = yRot;

            dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.remove(sender.getUUID());
            dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.remove(sender.getUUID());
            dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT.remove(sender.getUUID());

            final net.minecraft.world.phys.Vec3 lastPos = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS
                    .getOrDefault(sender.getUUID(), new net.minecraft.world.phys.Vec3(x, y, z));
            float lastFall = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_FALL
                    .getOrDefault(sender.getUUID(), 0.0F);
            net.minecraft.world.phys.Vec3 motion = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_MOTION
                    .getOrDefault(sender.getUUID(), net.minecraft.world.phys.Vec3.ZERO);

            boolean crossDim = !originalLevel.dimension().equals(sender.level().dimension());
            boolean involvesCustDim = !originalLevel.dimension().location().getNamespace().equals("minecraft")
                    || !sender.level().dimension().location().getNamespace().equals("minecraft");
            boolean needsWorkaround = crossDim && involvesCustDim;

            if (needsWorkaround) {
                dev.ClasherHD.bodycam.bodycam.POSITION_LOCKS.put(sender.getUUID(), new dev.ClasherHD.bodycam.bodycam.LockData(
                        originalLevel.dimension(),
                        lastPos.x, lastPos.y, lastPos.z,
                        finalYRot, finalXRot
                ));
                sender.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 20, 0, false, false, false));
                sender.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20, 255, false, false, false));
            }

            sender.teleportTo(originalLevel, lastPos.x, lastPos.y, lastPos.z, finalYRot, finalXRot);

            sender.setDeltaMovement(motion);
            sender.hurtMarked = true;
            sender.fallDistance = lastFall;

            int gameModeId = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.getOrDefault(sender.getUUID(), net.minecraft.world.level.GameType.SURVIVAL.getId());
            net.minecraft.world.level.GameType originalGameType = net.minecraft.world.level.GameType.byId(gameModeId);
            sender.setGameMode(originalGameType != null ? originalGameType : net.minecraft.world.level.GameType.SURVIVAL);

            if (sender.getPersistentData().contains("bodycam_target_uuid")) {
                java.util.UUID oldTargetId = sender.getPersistentData().getUUID("bodycam_target_uuid");
                if (oldTargetId != null) {
                    net.minecraft.server.level.ServerPlayer oldTarget = sender.server.getPlayerList().getPlayer(oldTargetId);
                    if (oldTarget != null) {
                        dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> oldTarget),
                                new dev.ClasherHD.bodycam.network.CrossObservationSyncPacket(sender.getUUID(), false)
                        );
                    }
                }
            }

            sender.getPersistentData().putBoolean("bodycam_active", false);
            sender.getPersistentData().remove("bodycam_target_uuid");
            sender.getPersistentData().remove("bodycam_disconnect_ticks");
            sender.getPersistentData().remove("bodycam_has_reach");
            sender.getPersistentData().remove("bodycam_original_dimension");
            
            dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(sender.getUUID());

            try {
                java.util.UUID dummyId = sender.getPersistentData().getUUID("bodycam_dummy_uuid");
                net.minecraft.world.entity.Entity dummy = originalLevel.getEntity(dummyId);
                if (dummy != null) {
                    dummy.discard();
                }
            } catch (Exception e) {
            }
            dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.remove(sender.getUUID());
            dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_FALL.remove(sender.getUUID());
            dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_MOTION.remove(sender.getUUID());

            dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sender),
                    new dev.ClasherHD.bodycam.network.BodycamForceClosePacket()
            );
        }
    }

    public static void handle(BodycamResetCameraPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            executeReset(sender);
        });
        ctx.get().setPacketHandled(true);
    }
}
