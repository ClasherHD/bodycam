package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.util.PersistentData;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BodycamSetCameraPacket {

    public static final ConcurrentHashMap<UUID, Vec3> ORIGINAL_POS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Vec2> ORIGINAL_ROT = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, String> ORIGINAL_DIM = new ConcurrentHashMap<>();

    public final UUID targetId;

    public BodycamSetCameraPacket(UUID targetId) {
        this.targetId = targetId;
    }

    public static void encode(BodycamSetCameraPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetId);
    }

    public static BodycamSetCameraPacket decode(FriendlyByteBuf buf) {
        return new BodycamSetCameraPacket(buf.readUUID());
    }

    public static void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler,
            FriendlyByteBuf buf, PacketSender responseSender) {
        BodycamSetCameraPacket msg = decode(buf);
        server.execute(() -> {
            if (player == null)
                return;
            ServerPlayer target = player.server.getPlayerList().getPlayer(msg.targetId);
            if (target == null)
                return;

            if (player.isSpectator()) {
                return;
            }

            boolean hasReach = false;
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            if (mainHand.getItem() instanceof dev.ClasherHD.bodycam.item.BodycamMonitorItem
                    && net.minecraft.world.item.enchantment.EnchantmentHelper
                            .getItemEnchantmentLevel(bodycam.REACH_ENCHANTMENT, mainHand) > 0) {
                hasReach = true;
            } else if (offHand.getItem() instanceof dev.ClasherHD.bodycam.item.BodycamMonitorItem
                    && net.minecraft.world.item.enchantment.EnchantmentHelper
                            .getItemEnchantmentLevel(bodycam.REACH_ENCHANTMENT, offHand) > 0) {
                hasReach = true;
            }

            if (!hasReach) {
                if (player.level() != target.level() || player.distanceTo(target) > 500.0D) {
                    player.sendSystemMessage(Component.literal(
                            "§cSignal zu schwach! Ziel ist zu weit entfernt oder in einer anderen Dimension."));
                    ((PersistentData) player).bodycam$getPersistentData().putBoolean("bodycam_active", false);
                    return;
                }
            }

            if (((PersistentData) player).bodycam$getPersistentData().contains("bodycam_dummy_uuid")) {
                UUID oldId = ((PersistentData) player).bodycam$getPersistentData().getUUID("bodycam_dummy_uuid");
                for (ServerLevel lvl : player.server.getAllLevels()) {
                    Entity e = lvl.getEntity(oldId);
                    if (e != null)
                        e.discard();
                }
            }

            ORIGINAL_DIM.put(player.getUUID(), player.level().dimension().location().toString());
            ORIGINAL_POS.put(player.getUUID(), player.position());
            ORIGINAL_ROT.put(player.getUUID(), new Vec2(player.getXRot(), player.getYRot()));

            BodycamDummyEntity dummy = new BodycamDummyEntity(bodycam.BODYCAM_DUMMY, player.serverLevel());
            dummy.setPos(player.getX(), player.getY(), player.getZ());
            dummy.setYRot(player.getYRot());
            dummy.setXRot(player.getXRot());
            dummy.setYHeadRot(player.getYHeadRot());

            dummy.getEntityData().set(BodycamDummyEntity.OWNER_UUID, Optional.of(player.getUUID()));
            dummy.getEntityData().set(BodycamDummyEntity.OWNER_NAME, player.getGameProfile().getName());
            dummy.setCustomName(Component.literal(player.getGameProfile().getName()));
            dummy.setCustomNameVisible(true);

            if (player.getAttribute(Attributes.ARMOR) != null
                    && dummy.getAttribute(Attributes.ARMOR) != null) {
                dummy.getAttribute(Attributes.ARMOR).setBaseValue(
                        player.getAttributeValue(Attributes.ARMOR));
            }
            if (player.getAttribute(Attributes.ARMOR_TOUGHNESS) != null
                    && dummy.getAttribute(Attributes.ARMOR_TOUGHNESS) != null) {
                dummy.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(
                        player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            }
            if (player.getAttribute(Attributes.MAX_HEALTH) != null
                    && dummy.getAttribute(Attributes.MAX_HEALTH) != null) {
                dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                        player.getAttributeValue(Attributes.MAX_HEALTH));
            }

            dummy.setHealth(player.getHealth());
            dummy.setDeltaMovement(player.getDeltaMovement());
            dummy.fallDistance = player.fallDistance;

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                dummy.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }

            player.serverLevel().addFreshEntity(dummy);
            ((PersistentData) player).bodycam$getPersistentData().putUUID("bodycam_dummy_uuid", dummy.getUUID());

            ((PersistentData) player).bodycam$getPersistentData().putBoolean("bodycam_active", true);
            player.setGameMode(GameType.SPECTATOR);

            if (player.level() != target.level()) {
                player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(),
                        target.getXRot());
                player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(),
                        target.getXRot());
            } else {
                player.connection.teleport(target.getX(), target.getY(), target.getZ(), target.getYRot(),
                        target.getXRot());
            }

            player.setCamera(target);
        });
    }
}
