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

    public static void handle(BodycamResetCameraPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.server != null) {
                sender.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                sender.setCamera(sender);
                sender.getPersistentData().putBoolean("bodycam_active", false);

                if (!sender.isAlive()) {
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

                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.remove(sender.getUUID());
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.remove(sender.getUUID());
                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT.remove(sender.getUUID());

                net.minecraft.world.phys.Vec3 lastPos = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS
                        .getOrDefault(sender.getUUID(), new net.minecraft.world.phys.Vec3(x, y, z));
                float lastFall = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_FALL
                        .getOrDefault(sender.getUUID(), 0.0F);
                net.minecraft.world.phys.Vec3 motion = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_MOTION
                        .getOrDefault(sender.getUUID(), net.minecraft.world.phys.Vec3.ZERO);

                sender.teleportTo(originalLevel, lastPos.x, lastPos.y, lastPos.z, yRot, xRot);
                sender.teleportTo(originalLevel, lastPos.x, lastPos.y, lastPos.z, yRot, xRot);

                sender.setDeltaMovement(motion);
                sender.hurtMarked = true;
                sender.fallDistance = lastFall;

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
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
