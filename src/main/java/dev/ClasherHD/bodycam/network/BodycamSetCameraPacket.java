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

    public BodycamSetCameraPacket(UUID targetId) {
        this.targetId = targetId;
    }

    public static void encode(BodycamSetCameraPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetId);
    }

    public static BodycamSetCameraPacket decode(FriendlyByteBuf buf) {
        return new BodycamSetCameraPacket(buf.readUUID());
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

            boolean hasReach = false;
            net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
            net.minecraft.world.item.ItemStack offHand = player.getOffhandItem();

            if (mainHand.getItem() instanceof dev.ClasherHD.bodycam.item.BodycamMonitorItem
                    && mainHand.getEnchantmentLevel(dev.ClasherHD.bodycam.bodycam.REACH_ENCHANTMENT.get()) > 0) {
                hasReach = true;
            } else if (offHand.getItem() instanceof dev.ClasherHD.bodycam.item.BodycamMonitorItem
                    && offHand.getEnchantmentLevel(dev.ClasherHD.bodycam.bodycam.REACH_ENCHANTMENT.get()) > 0) {
                hasReach = true;
            }

            if (!hasReach) {
                if (player.level() != target.level() || player.distanceTo(target) > 500.0D) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED));
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

            dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy = new dev.ClasherHD.bodycam.entity.BodycamDummyEntity(
                    dev.ClasherHD.bodycam.bodycam.BODYCAM_DUMMY.get(), player.serverLevel());
            dummy.setPos(player.getX(), player.getY(), player.getZ());
            dummy.setYRot(player.getYRot());
            dummy.setXRot(player.getXRot());
            dummy.setYHeadRot(player.getYHeadRot());

            dummy.getEntityData().set(dev.ClasherHD.bodycam.entity.BodycamDummyEntity.OWNER_UUID,
                    java.util.Optional.of(player.getUUID()));
            dummy.getEntityData().set(dev.ClasherHD.bodycam.entity.BodycamDummyEntity.OWNER_NAME,
                    player.getGameProfile().getName());
            dummy.setCustomName(net.minecraft.network.chat.Component.literal(player.getGameProfile().getName()));
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
            player.getPersistentData().putUUID("bodycam_dummy_uuid", dummy.getUUID());

            player.getPersistentData().putBoolean("bodycam_active", true);
            player.getPersistentData().putUUID("bodycam_target_uuid", msg.targetId);
            player.getPersistentData().putInt("bodycam_disconnect_ticks", 0);
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);

            if (player.level() != target.level()) {
                player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(),
                        target.getXRot());
                player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(),
                        target.getXRot());
            } else {
                player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(),
                        target.getXRot());
            }

            player.setCamera(target);
        });
        ctx.get().setPacketHandled(true);
    }
}
