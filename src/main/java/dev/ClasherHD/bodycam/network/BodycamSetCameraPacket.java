package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;

public class BodycamSetCameraPacket {

    public static final ConcurrentHashMap<UUID, net.minecraft.world.phys.Vec3> ORIGINAL_POS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, net.minecraft.world.phys.Vec2> ORIGINAL_ROT = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, String> ORIGINAL_DIM = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Integer> ORIGINAL_GAMEMODE = new ConcurrentHashMap<>();

    public final UUID targetId;
    public final boolean hasReach;
    public final boolean isOnHologram;

    public BodycamSetCameraPacket(UUID targetId, boolean hasReach, boolean isOnHologram) {
        this.targetId = targetId;
        this.hasReach = hasReach;
        this.isOnHologram = isOnHologram;
    }

    public static void encode(BodycamSetCameraPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetId);
        buf.writeBoolean(msg.hasReach);
        buf.writeBoolean(msg.isOnHologram);
    }

    public static BodycamSetCameraPacket decode(FriendlyByteBuf buf) {
        return new BodycamSetCameraPacket(buf.readUUID(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(BodycamSetCameraPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;
            ServerPlayer target = player.server.getPlayerList().getPlayer(msg.targetId);
            if (target == null)
                return;

            if (player.isSpectator()) {
                ctx.get().setPacketHandled(true);
                return;
            }

            if (target.getPersistentData().getBoolean("bodycam_active") && target.getPersistentData().contains("bodycam_target_uuid")) {
                java.util.UUID targetUuid = target.getPersistentData().getUUID("bodycam_target_uuid");
                if (targetUuid != null && targetUuid.equals(player.getUUID())) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED));
                    dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new dev.ClasherHD.bodycam.network.BodycamForceClosePacket()
                    );
                    return;
                }
            }

            long lastJammer = target.getPersistentData().getLong("bodycam_jammer_heartbeat");
            int jammerMode = target.getPersistentData().getInt("bodycam_jammer_mode");
            boolean isJammerActive = (target.level().getGameTime() - lastJammer) <= 10;
            if (!isJammerActive) {
                jammerMode = 0;
            }

            if (jammerMode == 1) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.bodycam.jammer_blocked").withStyle(net.minecraft.ChatFormatting.RED));
                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new dev.ClasherHD.bodycam.network.BodycamForceClosePacket());
                return;
            } else if (jammerMode == 2) {
                if (player.level() != target.level() || player.distanceTo(target) > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "message.bodycam.jammer_blocked").withStyle(net.minecraft.ChatFormatting.RED));
                    dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new dev.ClasherHD.bodycam.network.BodycamForceClosePacket());
                    return;
                }
            }

            boolean hasReach = msg.hasReach && dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_REACH_ENCHANTMENT.get();

            if (!hasReach) {
                if (player.level() != target.level() || player.distanceTo(target) > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED));
                    dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new dev.ClasherHD.bodycam.network.BodycamForceClosePacket());
                    return;
                }
            }

            if (player.getPersistentData().contains("bodycam_dummy_uuid")) {
                java.util.UUID oldId = player.getPersistentData().getUUID("bodycam_dummy_uuid");
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

            boolean isOnHologram = msg.isOnHologram;
            dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy;
            if (isOnHologram) {
                dummy = new dev.ClasherHD.bodycam.entity.HologramDummyEntity(
                        dev.ClasherHD.bodycam.bodycam.HOLOGRAM_DUMMY.get(), player.serverLevel());
                dummy.setInvulnerable(true);
            } else {
                dummy = new dev.ClasherHD.bodycam.entity.CompassDummyEntity(
                        dev.ClasherHD.bodycam.bodycam.COMPASS_DUMMY.get(), player.serverLevel());
            }

            dummy.setPos(player.getX(), player.getY(), player.getZ());
            dummy.setYRot(player.getYRot());
            dummy.setXRot(player.getXRot());
            dummy.setYHeadRot(player.getYHeadRot());

            dummy.getEntityData().set(dev.ClasherHD.bodycam.entity.BodycamDummyEntity.OWNER_UUID,
                    java.util.Optional.of(player.getUUID()));
            dummy.getEntityData().set(dev.ClasherHD.bodycam.entity.BodycamDummyEntity.OWNER_NAME,
                    player.getName().getString());
            dummy.setCustomName(net.minecraft.network.chat.Component.literal(player.getName().getString()));
            dummy.setCustomNameVisible(true);

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

            dummy.setHealth(player.getHealth());
            dummy.setDeltaMovement(player.getDeltaMovement());
            dummy.fallDistance = player.fallDistance;

            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                dummy.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }

            player.serverLevel().addFreshEntity(dummy);
            int chunkX = net.minecraft.util.Mth.floor(dummy.getX()) >> 4;
            int chunkZ = net.minecraft.util.Mth.floor(dummy.getZ()) >> 4;
            net.minecraftforge.common.world.ForgeChunkManager.forceChunk(
                    player.serverLevel(), "bodycam", dummy, chunkX, chunkZ, true, true);
            player.getPersistentData().putUUID("bodycam_dummy_uuid", dummy.getUUID());

            if (player.getPersistentData().contains("bodycam_target_uuid")) {
                java.util.UUID oldTargetId = player.getPersistentData().getUUID("bodycam_target_uuid");
                if (oldTargetId != null && !oldTargetId.equals(msg.targetId)) {
                    net.minecraft.server.level.ServerPlayer oldTarget = player.server.getPlayerList().getPlayer(oldTargetId);
                    if (oldTarget != null) {
                        dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> oldTarget),
                                new dev.ClasherHD.bodycam.network.CrossObservationSyncPacket(player.getUUID(), false));
                    }
                }
            }
            player.getPersistentData().putBoolean("bodycam_active", true);
            player.getPersistentData().putUUID("bodycam_target_uuid", msg.targetId);
            player.getPersistentData().putInt("bodycam_disconnect_ticks", 0);
            player.getPersistentData().putBoolean("bodycam_has_reach", hasReach);
            player.getPersistentData().putString("bodycam_original_dimension", player.level().dimension().location().getPath());

            dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> target),
                    new dev.ClasherHD.bodycam.network.CrossObservationSyncPacket(player.getUUID(), true));
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);

            if (player.level().dimension() != target.level().dimension()) {
                player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            }
            player.setCamera(target);
        });
        ctx.get().setPacketHandled(true);
    }
}
