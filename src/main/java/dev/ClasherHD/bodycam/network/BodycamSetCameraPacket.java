package dev.ClasherHD.bodycam.network;


import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BodycamSetCameraPacket {

    public static final ConcurrentHashMap<UUID, net.minecraft.world.phys.Vec3> ORIGINAL_POS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, net.minecraft.world.phys.Vec2> ORIGINAL_ROT = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, String> ORIGINAL_DIM = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Integer> ORIGINAL_GAMEMODE = new ConcurrentHashMap<>();



    public static void executeSet(ServerPlayer player, UUID targetId, boolean hasReach, boolean isOnHologram) {
        if (player == null)
            return;
        ServerPlayer target = player.server.getPlayerList().getPlayer(targetId);
        if (target == null)
            return;

        if (player.isSpectator()) {
            return;
        }

        if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target).getBoolean("bodycam_active")
                && dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target).contains("bodycam_target_uuid")) {
            java.util.UUID targetUuid = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target)
                    .getUUID("bodycam_target_uuid");
            if (targetUuid != null && targetUuid.equals(player.getUUID())) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED));
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                        BodycamPacketIDs.RESET_CAMERA_PACKET_ID,
                        net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
                return;
            }
        }

        long lastJammer = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target)
                .getLong("bodycam_jammer_heartbeat");
        int jammerMode = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target).getInt("bodycam_jammer_mode");
        boolean isJammerActive = (target.level().getGameTime() - lastJammer) <= 10;
        if (!isJammerActive) {
            jammerMode = 0;
        }

        if (jammerMode == 1) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.bodycam.jammer_blocked").withStyle(net.minecraft.ChatFormatting.RED));
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                    BodycamPacketIDs.RESET_CAMERA_PACKET_ID,
                    net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            return;
        } else if (jammerMode == 2) {
            if (player.level() != target.level() || player.distanceTo(
                    target) > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.bodycam.jammer_blocked").withStyle(net.minecraft.ChatFormatting.RED));
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                        BodycamPacketIDs.RESET_CAMERA_PACKET_ID,
                        net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
                return;
            }
        }

        boolean hasReachConfig = hasReach
                && dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_REACH_ENCHANTMENT.get();

        if (!hasReachConfig) {
            if (player.level() != target.level() || player.distanceTo(
                    target) > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED));
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                        BodycamPacketIDs.RESET_CAMERA_PACKET_ID,
                        net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
                return;
            }
        }

        if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).contains("bodycam_dummy_uuid")) {
            java.util.UUID oldId = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player)
                    .getUUID("bodycam_dummy_uuid");
            for (net.minecraft.server.level.ServerLevel lvl : player.server.getAllLevels()) {
                net.minecraft.world.entity.Entity e = lvl.getEntity(oldId);
                if (e != null)
                    e.discard();
            }
        }

        ORIGINAL_DIM.put(player.getUUID(), player.level().dimension().location().toString());
        ORIGINAL_POS.put(player.getUUID(), player.position());
        ORIGINAL_ROT.put(player.getUUID(), new net.minecraft.world.phys.Vec2(player.getXRot(), player.getYRot()));
        ORIGINAL_GAMEMODE.put(player.getUUID(), player.gameMode.getGameModeForPlayer().getId());

        dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy;
        if (isOnHologram) {
            dummy = new dev.ClasherHD.bodycam.entity.HologramDummyEntity(
                    dev.ClasherHD.bodycam.entity.EntityRegistryFabric.HOLOGRAM_DUMMY, player.serverLevel());
            dummy.setInvulnerable(true);
        } else {
            dummy = new dev.ClasherHD.bodycam.entity.CompassDummyEntity(
                    dev.ClasherHD.bodycam.entity.EntityRegistryFabric.COMPASS_DUMMY, player.serverLevel());
        }

        dummy.setPos(player.getX(), player.getY(), player.getZ());
        dummy.setYRot(player.getYRot());
        dummy.setXRot(player.getXRot());
        dummy.setYHeadRot(player.getYHeadRot());

        dummy.getEntityData().set(dev.ClasherHD.bodycam.entity.BodycamDummyEntity.OWNER_UUID,
                java.util.Optional.of(player.getUUID()));

        dummy.setCustomName(net.minecraft.network.chat.Component.literal(player.getName().getString()));
        dummy.setCustomNameVisible(true);

        try {
            if (player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) != null
                    && dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) != null) {
                dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(
                        player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR));
            }
            if (player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS) != null
                    && dummy.getAttribute(
                            net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS) != null) {
                dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS).setBaseValue(
                        player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
            }
            if (player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null
                    && dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
                dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(
                        player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH));
            }
        } catch (Exception e) {
        }

        dummy.setHealth(player.getHealth());
        dummy.setDeltaMovement(player.getDeltaMovement());
        dummy.fallDistance = player.fallDistance;

        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            dummy.setItemSlot(slot, player.getItemBySlot(slot).copy());
        }

        player.serverLevel().addFreshEntity(dummy);
        dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putUUID("bodycam_dummy_uuid", dummy.getUUID());

        if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).contains("bodycam_target_uuid")) {
            java.util.UUID oldTargetId = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player)
                    .getUUID("bodycam_target_uuid");
            if (oldTargetId != null && !oldTargetId.equals(targetId)) {
                net.minecraft.server.level.ServerPlayer oldTarget = player.server.getPlayerList()
                        .getPlayer(oldTargetId);
                if (oldTarget != null) {
                    net.minecraft.network.FriendlyByteBuf crossBuf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs
                            .create();
                    CrossObservationSyncPacket.encode(new CrossObservationSyncPacket(player.getUUID(), false),
                            crossBuf);
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(oldTarget,
                            BodycamPacketIDs.CROSS_SYNC_PACKET_ID, crossBuf);
                }
            }
        }
        dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putBoolean("bodycam_active", true);
        dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putUUID("bodycam_target_uuid", targetId);
        dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putInt("bodycam_disconnect_ticks", 0);
        dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putBoolean("bodycam_has_reach", hasReachConfig);
        dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putString("bodycam_original_dimension",
                player.level().dimension().location().getPath());

        net.minecraft.network.FriendlyByteBuf crossBuf2 = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        CrossObservationSyncPacket.encode(new CrossObservationSyncPacket(player.getUUID(), true), crossBuf2);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(target, BodycamPacketIDs.CROSS_SYNC_PACKET_ID,
                crossBuf2);

        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        player.setInvisible(true);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.INVISIBILITY, 999999, 0, false, false, false));

        if (player.level().dimension() != target.level().dimension()) {
            dev.ClasherHD.bodycam.BodycamFabric.LockData lockData = dev.ClasherHD.bodycam.BodycamFabric.POSITION_LOCKS
                    .get(player.getUUID());
            if (lockData != null) {
                lockData.x = target.getX();
                lockData.y = target.getY();
                lockData.z = target.getZ();
                lockData.yRot = target.getYRot();
                lockData.xRot = target.getXRot();
            } else {
                dev.ClasherHD.bodycam.BodycamFabric.POSITION_LOCKS.put(player.getUUID(),
                        new dev.ClasherHD.bodycam.BodycamFabric.LockData(target.level().dimension(),
                                target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot()));
            }
            player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(),
                    target.getXRot());
        } else {
            player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(),
                    target.getXRot());
        }
        player.setCamera(target);

        net.minecraft.network.FriendlyByteBuf cbuf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        cbuf.writeInt(target.getId());
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, BodycamPacketIDs.SET_CAMERA_PACKET_ID,
                cbuf);
    }
}
