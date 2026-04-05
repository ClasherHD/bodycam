package dev.ClasherHD.bodycam.network;

import net.minecraft.server.level.ServerPlayer;

public class BodycamResetCameraPacket {

    public static void executeReset(ServerPlayer sender) {
        if (sender != null && sender.server != null) {
            if (!sender.isAlive()) {
                int gameModeId = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.getOrDefault(sender.getUUID(), net.minecraft.world.level.GameType.SURVIVAL.getId());
                net.minecraft.world.level.GameType originalGameType = net.minecraft.world.level.GameType.byId(gameModeId);
                sender.setGameMode(originalGameType != null ? originalGameType : net.minecraft.world.level.GameType.SURVIVAL);
                sender.setCamera(sender);

                if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).contains("bodycam_target_uuid")) {
                    java.util.UUID oldTargetId = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).getUUID("bodycam_target_uuid");
                    if (oldTargetId != null) {
                        net.minecraft.server.level.ServerPlayer oldTarget = sender.server.getPlayerList().getPlayer(oldTargetId);
                        if (oldTarget != null) {
                            net.minecraft.network.FriendlyByteBuf obuf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                            dev.ClasherHD.bodycam.network.CrossObservationSyncPacket.encode(new dev.ClasherHD.bodycam.network.CrossObservationSyncPacket(sender.getUUID(), false), obuf);
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(oldTarget, dev.ClasherHD.bodycam.network.BodycamPacketIDs.CROSS_SYNC_PACKET_ID, obuf);
                        }
                    }
                }

                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).putBoolean("bodycam_active", false);
                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).remove("bodycam_target_uuid");
                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).remove("bodycam_disconnect_ticks");
                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).remove("bodycam_has_reach");
                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).remove("bodycam_original_dimension");
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(sender.getUUID());
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.remove(sender.getUUID());
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.remove(sender.getUUID());
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT.remove(sender.getUUID());
                dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.remove(sender.getUUID());
                dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_FALL.remove(sender.getUUID());
                dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_MOTION.remove(sender.getUUID());
                return;
            }
            
            net.minecraft.server.level.ServerLevel originalLevel = sender.serverLevel();
            String dimStr = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.getOrDefault(sender.getUUID(), "");
            if (!dimStr.isEmpty()) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(dimStr));
                originalLevel = sender.server.getLevel(dimKey);
                if (originalLevel == null) originalLevel = sender.server.overworld();
            }

            double lastX = sender.getX();
            double lastY = sender.getY();
            double lastZ = sender.getZ();

            net.minecraft.world.phys.Vec3 origPos = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.get(sender.getUUID());
            if (origPos != null) {
                lastX = origPos.x;
                lastY = origPos.y;
                lastZ = origPos.z;
            }

            net.minecraft.world.phys.Vec2 origRot = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT.get(sender.getUUID());
            float finalXRot = sender.getXRot();
            float finalYRot = sender.getYRot();
            if (origRot != null) {
                finalXRot = origRot.x;
                finalYRot = origRot.y;
            }

            final net.minecraft.world.phys.Vec3 dummyPos = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.getOrDefault(sender.getUUID(), new net.minecraft.world.phys.Vec3(lastX, lastY, lastZ));
            float lastFall = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_FALL.getOrDefault(sender.getUUID(), 0.0F);
            net.minecraft.world.phys.Vec3 motion = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_MOTION.getOrDefault(sender.getUUID(), net.minecraft.world.phys.Vec3.ZERO);

            boolean crossDim = !originalLevel.dimension().equals(sender.level().dimension());
            boolean involvesCustDim = !originalLevel.dimension().location().getNamespace().equals("minecraft") || !sender.level().dimension().location().getNamespace().equals("minecraft");
            if (crossDim && involvesCustDim) {
                dev.ClasherHD.bodycam.BodycamFabric.POSITION_LOCKS.put(sender.getUUID(), new dev.ClasherHD.bodycam.BodycamFabric.LockData(originalLevel.dimension(), dummyPos.x, dummyPos.y, dummyPos.z, finalYRot, finalXRot));
                sender.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 20, 0, false, false, false));
                sender.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20, 255, false, false, false));
            }

            sender.teleportTo(originalLevel, dummyPos.x, dummyPos.y, dummyPos.z, finalYRot, finalXRot);
            sender.setDeltaMovement(motion);
            sender.hurtMarked = true;
            sender.fallDistance = lastFall;

            try {
                java.util.UUID dummyId = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).getUUID("bodycam_dummy_uuid");
                net.minecraft.world.entity.Entity dummy = originalLevel.getEntity(dummyId);
                if (dummy != null) dummy.discard();
            } catch (Exception e) {}

            int gameModeId = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.getOrDefault(sender.getUUID(), net.minecraft.world.level.GameType.SURVIVAL.getId());
            net.minecraft.world.level.GameType originalGameType = net.minecraft.world.level.GameType.byId(gameModeId);
            sender.setGameMode(originalGameType != null ? originalGameType : net.minecraft.world.level.GameType.SURVIVAL);
            sender.setInvisible(false);
            sender.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);

            if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).contains("bodycam_target_uuid")) {
                java.util.UUID oldTargetId = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).getUUID("bodycam_target_uuid");
                if (oldTargetId != null) {
                    net.minecraft.server.level.ServerPlayer oldTarget = sender.server.getPlayerList().getPlayer(oldTargetId);
                    if (oldTarget != null) {
                        net.minecraft.network.FriendlyByteBuf obuf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                        dev.ClasherHD.bodycam.network.CrossObservationSyncPacket.encode(new dev.ClasherHD.bodycam.network.CrossObservationSyncPacket(sender.getUUID(), false), obuf);
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(oldTarget, dev.ClasherHD.bodycam.network.BodycamPacketIDs.CROSS_SYNC_PACKET_ID, obuf);
                    }
                }
            }

            dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).putBoolean("bodycam_active", false);
            dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).remove("bodycam_target_uuid");
            dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).remove("bodycam_disconnect_ticks");
            dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).remove("bodycam_has_reach");
            dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(sender).remove("bodycam_original_dimension");
            
            dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(sender.getUUID());
            dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.remove(sender.getUUID());
            dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.remove(sender.getUUID());
            dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT.remove(sender.getUUID());
            dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.remove(sender.getUUID());
            dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_FALL.remove(sender.getUUID());
            dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_MOTION.remove(sender.getUUID());

            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sender, dev.ClasherHD.bodycam.network.BodycamPacketIDs.RESET_CAMERA_PACKET_ID, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
        }
    }
}
