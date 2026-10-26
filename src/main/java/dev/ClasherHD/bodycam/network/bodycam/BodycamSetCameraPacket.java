package dev.ClasherHD.bodycam.network.bodycam;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.enchantment.NightspyEffectHandler;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class BodycamSetCameraPacket {

    public static final ConcurrentHashMap<UUID, net.minecraft.world.phys.Vec3> ORIGINAL_POS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, net.minecraft.world.phys.Vec2> ORIGINAL_ROT = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, String> ORIGINAL_DIM = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Integer> ORIGINAL_GAMEMODE = new ConcurrentHashMap<>();

    public final UUID targetId;
    public final boolean isOnHologram;

    public BodycamSetCameraPacket(UUID targetId, boolean isOnHologram) {
        this.targetId = targetId;
        this.isOnHologram = isOnHologram;
    }

    public static void encode(BodycamSetCameraPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetId);
        buf.writeBoolean(msg.isOnHologram);
    }

    public static BodycamSetCameraPacket decode(FriendlyByteBuf buf) {
        return new BodycamSetCameraPacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(BodycamSetCameraPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new BodycamForceClosePacket());
                ctx.get().setPacketHandled(true);
                return;
            }
            boolean alreadyActive = player.getPersistentData().getBoolean("bodycam_active");
            if (player.isSpectator() && !alreadyActive) {
                ctx.get().setPacketHandled(true);
                return;
            }

            net.minecraft.world.phys.Vec3 observerPos = player.position();
            String observerDim = player.level().dimension().location().toString();
            if (alreadyActive) {
                net.minecraft.world.phys.Vec3 dummyPos = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.get(player.getUUID());
                if (dummyPos != null) {
                    observerPos = dummyPos;
                } else if (ORIGINAL_POS.containsKey(player.getUUID())) {
                    observerPos = ORIGINAL_POS.get(player.getUUID());
                } else if (player.getPersistentData().contains("bodycam_orig_x")) {
                    observerPos = new net.minecraft.world.phys.Vec3(
                            player.getPersistentData().getDouble("bodycam_orig_x"),
                            player.getPersistentData().getDouble("bodycam_orig_y"),
                            player.getPersistentData().getDouble("bodycam_orig_z")
                    );
                }
                if (ORIGINAL_DIM.containsKey(player.getUUID())) {
                    observerDim = ORIGINAL_DIM.get(player.getUUID());
                } else if (player.getPersistentData().contains("bodycam_orig_dim")) {
                    observerDim = player.getPersistentData().getString("bodycam_orig_dim");
                }
            }

            ServerPlayer target = player.server.getPlayerList().getPlayer(msg.targetId);
            WatchPointDummyEntity targetWatchPoint = null;
            if (target == null) {
                if (dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_WATCH_POINT.get()) {
                    targetWatchPoint = WatchPointDummyEntity.findByUUID(player.server, msg.targetId);
                    if (targetWatchPoint != null && !player.getUUID().equals(targetWatchPoint.getOwnerUUID())) {
                        targetWatchPoint = null;
                    }
                }
            }

            Entity targetEntity = target != null ? target : targetWatchPoint;
            if (targetEntity == null || !(targetEntity.level() instanceof ServerLevel targetServerLevel)) {
                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new BodycamForceClosePacket());
                return;
            }

            if (dev.ClasherHD.bodycam.config.ModServerConfig.isDimensionBlacklisted(player.level())
                    || dev.ClasherHD.bodycam.config.ModServerConfig.isDimensionBlacklisted(targetServerLevel)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.bodycam.dimension_blacklisted").withStyle(net.minecraft.ChatFormatting.RED));
                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new BodycamForceClosePacket());
                return;
            }

            if (target != null) {
                if (EnchantmentLevels.isHiddenFromSelection(target)) {
                    dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new BodycamForceClosePacket());
                    return;
                }

                if (target.getPersistentData().getBoolean("bodycam_active") && target.getPersistentData().contains("bodycam_target_uuid")) {
                    UUID targetUuid = target.getPersistentData().getUUID("bodycam_target_uuid");
                    if (targetUuid != null && targetUuid.equals(player.getUUID())) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED));
                        dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                new BodycamForceClosePacket()
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
                jammerMode = PodestBlockEntity.combineJammerModes(jammerMode, PodestBlockEntity.getPodestJammerMode(target));

                if (jammerMode == 1) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "message.bodycam.jammer_blocked").withStyle(net.minecraft.ChatFormatting.RED));
                    dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new BodycamForceClosePacket());
                    return;
                } else if (jammerMode == 2) {
                    String targetDim = target.level().dimension().location().toString();
                    if (!observerDim.equals(targetDim) || observerPos.distanceTo(target.position()) > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.bodycam.jammer_blocked").withStyle(net.minecraft.ChatFormatting.RED));
                        dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new BodycamForceClosePacket());
                        return;
                    }
                }
            }

            boolean isOnHologram;
            if (alreadyActive) {
                isOnHologram = player.getPersistentData().getBoolean("bodycam_on_hologram");
            } else {
                isOnHologram = msg.isOnHologram;
                if (isOnHologram) {
                    if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get()) {
                        isOnHologram = false;
                    } else {
                        boolean onHologramBlock = player.level().getBlockState(player.blockPosition().below()).is(dev.ClasherHD.bodycam.registry.ModBlocks.HOLOGRAM_BLOCK.get()) ||
                                                  player.level().getBlockState(player.blockPosition()).is(dev.ClasherHD.bodycam.registry.ModBlocks.HOLOGRAM_BLOCK.get());
                        if (!onHologramBlock) {
                            isOnHologram = false;
                        }
                    }
                }
            }

            int reachLevel = EnchantmentLevels.sessionReach(player, isOnHologram);

            if (reachLevel < 2) {
                String targetDim = targetEntity.level().dimension().location().toString();
                if (!observerDim.equals(targetDim) || (reachLevel < 1 && observerPos.distanceTo(targetEntity.position()) > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get())) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED));
                    dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new BodycamForceClosePacket());
                    return;
                }
            }

            if (!isOnHologram && !alreadyActive) {
                ItemStack spyglassStack = EnchantmentLevels.heldBodycam(player);
                if (spyglassStack.isEmpty() || ItemDurabilityHelper.isEmpty(spyglassStack)) {
                    dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new BodycamForceClosePacket());
                    return;
                }
                ItemDurabilityHelper.damagePerUse(spyglassStack, player, 1);
            }

            if (!alreadyActive) {
                if (player.getPersistentData().contains("bodycam_dummy_uuid")) {
                    UUID oldId = player.getPersistentData().getUUID("bodycam_dummy_uuid");
                    for (net.minecraft.server.level.ServerLevel lvl : player.server.getAllLevels()) {
                        net.minecraft.world.entity.Entity e = lvl.getEntity(oldId);
                        if (e != null) {
                            e.discard();
                        }
                    }
                }

                ORIGINAL_DIM.put(player.getUUID(), player.level().dimension().location().toString());
                ORIGINAL_POS.put(player.getUUID(), player.position());
                ORIGINAL_ROT.put(player.getUUID(), new net.minecraft.world.phys.Vec2(player.getXRot(), player.getYRot()));
                ORIGINAL_GAMEMODE.put(player.getUUID(), player.gameMode.getGameModeForPlayer().getId());
                player.getPersistentData().putInt("bodycam_original_gamemode", player.gameMode.getGameModeForPlayer().getId());
                player.getPersistentData().putString("bodycam_orig_dim", player.level().dimension().location().toString());
                player.getPersistentData().putDouble("bodycam_orig_x", player.getX());
                player.getPersistentData().putDouble("bodycam_orig_y", player.getY());
                player.getPersistentData().putDouble("bodycam_orig_z", player.getZ());
                player.getPersistentData().putFloat("bodycam_orig_yrot", player.getYRot());
                player.getPersistentData().putFloat("bodycam_orig_xrot", player.getXRot());
                player.getPersistentData().putBoolean("bodycam_on_hologram", isOnHologram);

                dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy;
                if (isOnHologram) {
                    dummy = new dev.ClasherHD.bodycam.entity.HologramDummyEntity(
                            dev.ClasherHD.bodycam.registry.ModEntityTypes.HOLOGRAM_DUMMY.get(), player.serverLevel());
                    dummy.setInvulnerable(true);
                } else {
                    dummy = new dev.ClasherHD.bodycam.entity.CompassDummyEntity(
                            dev.ClasherHD.bodycam.registry.ModEntityTypes.COMPASS_DUMMY.get(), player.serverLevel());
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
                dummy.markChunkForced(chunkX, chunkZ);
                player.getPersistentData().putUUID("bodycam_dummy_uuid", dummy.getUUID());
            }

            if (player.getPersistentData().contains("bodycam_target_uuid")) {
                UUID oldTargetId = player.getPersistentData().getUUID("bodycam_target_uuid");
                if (oldTargetId != null && !oldTargetId.equals(msg.targetId)) {
                    ServerPlayer oldTarget = player.server.getPlayerList().getPlayer(oldTargetId);
                    if (oldTarget != null) {
                        dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> oldTarget),
                                new CrossObservationSyncPacket(player.getUUID(), false));
                    }
                }
            }
            player.getPersistentData().putBoolean("bodycam_active", true);
            player.getPersistentData().putBoolean("bodycam_watching_watchpoint", targetWatchPoint != null);
            player.getPersistentData().putUUID("bodycam_target_uuid", msg.targetId);
            player.getPersistentData().putInt("bodycam_disconnect_ticks", 0);
            player.getPersistentData().putInt("bodycam_reach_level", reachLevel);
            NightspyEffectHandler.begin(player, EnchantmentLevels.sessionNightspy(player, isOnHologram));

            if (target != null) {
                final ServerPlayer syncTarget = target;
                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> syncTarget),
                        new CrossObservationSyncPacket(player.getUUID(), true));
            }
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);

            player.teleportTo(targetServerLevel, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(), targetEntity.getYRot(), targetEntity.getXRot());
            if (target != null) {
                player.setCamera(target);
            } else {
                player.setCamera(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
