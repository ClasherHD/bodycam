package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.util.PersistentData;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class BodycamResetCameraPacket {

    public BodycamResetCameraPacket() {
    }

    public static void encode(BodycamResetCameraPacket msg, FriendlyByteBuf buf) {
    }

    public static BodycamResetCameraPacket decode(FriendlyByteBuf buf) {
        return new BodycamResetCameraPacket();
    }

    public static void handle(MinecraftServer server, ServerPlayer sender, ServerGamePacketListenerImpl handler,
            FriendlyByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            if (sender != null && sender.server != null) {
                sender.setGameMode(GameType.SURVIVAL);
                sender.setCamera(sender);
                ((PersistentData) sender).bodycam$getPersistentData().putBoolean("bodycam_active", false);

                if (!sender.isAlive()) {
                    return;
                }
                CompoundTag tag = ((PersistentData) sender).bodycam$getPersistentData();

                double x = sender.getX();
                double y = sender.getY();
                double z = sender.getZ();
                float xRot = sender.getXRot();
                float yRot = sender.getYRot();

                ServerLevel originalLevel = sender.serverLevel();
                String dimStr = BodycamSetCameraPacket.ORIGINAL_DIM.getOrDefault(sender.getUUID(), "");
                if (!dimStr.isEmpty()) {
                    ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimStr));
                    originalLevel = sender.server.getLevel(dimKey);
                    if (originalLevel == null) {
                        originalLevel = sender.server.overworld();
                    }
                }

                Vec3 origPos = BodycamSetCameraPacket.ORIGINAL_POS.get(sender.getUUID());
                if (origPos != null) {
                    x = origPos.x;
                    y = origPos.y;
                    z = origPos.z;
                }
                Vec2 origRot = BodycamSetCameraPacket.ORIGINAL_ROT.get(sender.getUUID());
                if (origRot != null) {
                    xRot = origRot.x;
                    yRot = origRot.y;
                }

                BodycamSetCameraPacket.ORIGINAL_DIM.remove(sender.getUUID());
                BodycamSetCameraPacket.ORIGINAL_POS.remove(sender.getUUID());
                BodycamSetCameraPacket.ORIGINAL_ROT.remove(sender.getUUID());

                Vec3 lastPos = BodycamDummyEntity.DUMMY_POS.getOrDefault(sender.getUUID(), new Vec3(x, y, z));
                float lastFall = BodycamDummyEntity.DUMMY_FALL.getOrDefault(sender.getUUID(), 0.0F);
                Vec3 motion = BodycamDummyEntity.DUMMY_MOTION.getOrDefault(sender.getUUID(), Vec3.ZERO);

                sender.teleportTo(originalLevel, lastPos.x, lastPos.y, lastPos.z, yRot, xRot);
                sender.teleportTo(originalLevel, lastPos.x, lastPos.y, lastPos.z, yRot, xRot);

                sender.setDeltaMovement(motion);
                sender.hurtMarked = true;
                sender.fallDistance = lastFall;

                try {
                    UUID dummyId = ((PersistentData) sender).bodycam$getPersistentData().getUUID("bodycam_dummy_uuid");
                    Entity dummy = originalLevel.getEntity(dummyId);
                    if (dummy != null) {
                        dummy.discard();
                    }
                } catch (Exception e) {
                }
                BodycamDummyEntity.DUMMY_POS.remove(sender.getUUID());
                BodycamDummyEntity.DUMMY_FALL.remove(sender.getUUID());
                BodycamDummyEntity.DUMMY_MOTION.remove(sender.getUUID());
            }
        });
    }
}
