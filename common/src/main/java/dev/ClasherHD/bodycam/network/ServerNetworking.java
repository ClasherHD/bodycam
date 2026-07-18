package dev.ClasherHD.bodycam.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import dev.ClasherHD.bodycam.Bodycam;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.component.ModDataComponents;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.HologramDummyEntity;
import dev.ClasherHD.bodycam.entity.CompassDummyEntity;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.util.BodycamHelper;
import dev.ClasherHD.bodycam.util.BodycamLifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

public class ServerNetworking {

    public static void handleSetCamera(BodycamSetCameraPacket payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            ServerPlayer target = player.getServer().getPlayerList().getPlayer(payload.targetId());
            if (target == null || player.isSpectator()) return;

            if (ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("This feature is restricted to Server Operators.").withStyle(ChatFormatting.RED));
                return;
            }

            long lastJammer = BodycamHelper.getPersistentData(target).getLong("bodycam_jammer_heartbeat");
            int jammerMode = BodycamHelper.getPersistentData(target).getInt("bodycam_jammer_mode");
            boolean isJammerActive = (target.level().getGameTime() - lastJammer) <= 10;
            if (!isJammerActive) jammerMode = 0;

            if (jammerMode == 1) {
                player.sendSystemMessage(Component.translatable("message.bodycam.jammer_blocked").withStyle(ChatFormatting.RED));
                NetworkManager.sendToPlayer(player, new BodycamForceClosePacket());
                return;
            }

            boolean hasReachEnchant = false;
            if (ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()) {
                Optional<Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> reachEnchant = player.level().registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("bodycam", "reach")));
                
                if (reachEnchant.isPresent()) {
                    hasReachEnchant = player.getMainHandItem().getEnchantments().getLevel(reachEnchant.get()) > 0 ||
                                      player.getOffhandItem().getEnchantments().getLevel(reachEnchant.get()) > 0;
                }
            }

            boolean canMonitor = payload.hasReach() && hasReachEnchant;
            if (!canMonitor) {
                if (player.level() != target.level() || player.distanceTo(target) > ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                    player.sendSystemMessage(Component.translatable("message.bodycam.signal_weak").withStyle(ChatFormatting.RED));
                    NetworkManager.sendToPlayer(player, new BodycamForceClosePacket());
                    return;
                }
            }

            boolean isOnHolo = payload.isOnHologram();
            if (isOnHolo) {
                boolean nearHologram = false;
                BlockPos playerPos = player.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-6, -6, -6), playerPos.offset(6, 6, 6))) {
                    if (player.level().getBlockState(pos).is(Bodycam.HOLOGRAM_BLOCK.get())) {
                        nearHologram = true;
                        break;
                    }
                }
                if (!nearHologram) {
                    isOnHolo = false;
                }
            }

            cleanupDummy(player);
            saveOriginalState(player);

            BodycamDummyEntity dummy;
            if (isOnHolo) {
                dummy = new HologramDummyEntity(Bodycam.HOLOGRAM_DUMMY.get(), player.serverLevel());
                dummy.setInvulnerable(true);
            } else {
                dummy = new CompassDummyEntity(Bodycam.COMPASS_DUMMY.get(), player.serverLevel());
            }

            dummy.setPos(player.getX(), player.getY(), player.getZ());
            dummy.setYRot(player.getYRot());
            dummy.setXRot(player.getXRot());
            dummy.setYHeadRot(player.getYHeadRot());
            dummy.setDeltaMovement(player.getDeltaMovement());
            dummy.fallDistance = player.fallDistance;
            dummy.getEntityData().set(BodycamDummyEntity.OWNER_UUID, Optional.of(player.getUUID()));
            dummy.getEntityData().set(BodycamDummyEntity.OWNER_NAME, player.getName().getString());
            dummy.setCustomName(player.getName());
            dummy.setCustomNameVisible(true);

            if (player.getAttribute(Attributes.ARMOR) != null) dummy.getAttribute(Attributes.ARMOR).setBaseValue(player.getAttributeValue(Attributes.ARMOR));
            dummy.setHealth(player.getHealth());
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                dummy.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }

            player.serverLevel().addFreshEntity(dummy);
            BodycamHelper.getPersistentData(player).putUUID("bodycam_dummy_uuid", dummy.getUUID());

            BodycamHelper.getPersistentData(player).putBoolean("bodycam_active", true);
            BodycamHelper.getPersistentData(player).putUUID("bodycam_target_uuid", payload.targetId());
            BodycamHelper.getPersistentData(player).putBoolean("bodycam_bypass_dist", canMonitor || isOnHolo);
            player.setGameMode(GameType.SPECTATOR);

            if (player.level().dimension() != target.level().dimension()) {
                BodycamHelper.getPersistentData(player).putBoolean("bodycam_allow_teleport", true);
                player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
                BodycamHelper.getPersistentData(player).remove("bodycam_allow_teleport");
            }
            player.setCamera(target);
            
            NetworkManager.sendToPlayer(target, new CrossObservationSyncPacket(player.getUUID(), true));
        });
    }

    public static void executeReset(ServerPlayer player) {
        if (!BodycamHelper.getPersistentData(player).getBoolean("bodycam_active")) return;

        UUID targetId = null;
        if (BodycamHelper.getPersistentData(player).contains("bodycam_target_uuid")) {
            targetId = BodycamHelper.getPersistentData(player).getUUID("bodycam_target_uuid");
        }
        if (targetId != null) {
            ServerPlayer target = player.getServer().getPlayerList().getPlayer(targetId);
            if (target != null) {
                NetworkManager.sendToPlayer(target, new CrossObservationSyncPacket(player.getUUID(), false));
            }
        }

        UUID dummyUUID = null;
        if (BodycamHelper.getPersistentData(player).contains("bodycam_dummy_uuid")) {
            dummyUUID = BodycamHelper.getPersistentData(player).getUUID("bodycam_dummy_uuid");
        }

        dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy = null;
        if (dummyUUID != null) {
            for (ServerLevel lvl : player.getServer().getAllLevels()) {
                net.minecraft.world.entity.Entity e = lvl.getEntity(dummyUUID);
                if (e instanceof dev.ClasherHD.bodycam.entity.BodycamDummyEntity d) {
                    dummy = d;
                    break;
                }
            }
        }

        double x, y, z;
        float yaw, pitch;
        Vec3 motion;
        float fallDist;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> origDimKey;

        if (dummy != null) {
            x = dummy.getX();
            y = dummy.getY();
            z = dummy.getZ();
            yaw = dummy.getYRot();
            pitch = dummy.getXRot();
            motion = dummy.getDeltaMovement();
            fallDist = dummy.fallDistance;
            origDimKey = dummy.level().dimension();
        } else {
            Vec3 dummyPos = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.get(player.getUUID());
            Vec3 dummyMotion = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_MOTION.get(player.getUUID());
            Float dummyFall = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_FALL.get(player.getUUID());

            if (dummyPos != null) {
                x = dummyPos.x;
                y = dummyPos.y;
                z = dummyPos.z;
            } else {
                x = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_x");
                y = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_y");
                z = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_z");
            }

            yaw = BodycamHelper.getPersistentData(player).getFloat("bodycam_orig_yrot");
            pitch = BodycamHelper.getPersistentData(player).getFloat("bodycam_orig_xrot");

            if (dummyMotion != null) {
                motion = dummyMotion;
            } else {
                double mx = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_motx");
                double my = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_moty");
                double mz = BodycamHelper.getPersistentData(player).getDouble("bodycam_orig_motz");
                motion = new Vec3(mx, my, mz);
            }

            if (dummyFall != null) {
                fallDist = dummyFall;
            } else {
                fallDist = BodycamHelper.getPersistentData(player).getFloat("bodycam_orig_fall");
            }

            String origDimStr = BodycamHelper.getPersistentData(player).getString("bodycam_orig_dim");
            origDimKey = player.level().dimension();
            for (net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key : player.getServer().levelKeys()) {
                if (key.location().toString().equals(origDimStr)) {
                    origDimKey = key;
                    break;
                }
            }
        }

        int gmId = BodycamHelper.getPersistentData(player).getInt("bodycam_original_gamemode");

        BodycamLifecycle.POSITION_LOCKS.put(player.getUUID(), new BodycamLifecycle.LockData(
                origDimKey, x, y, z, yaw, pitch,
                GameType.byId(gmId),
                dummyUUID,
                origDimKey,
                motion,
                fallDist
        ));

        dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.remove(player.getUUID());
        dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_FALL.remove(player.getUUID());
        dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_MOTION.remove(player.getUUID());

        BodycamHelper.getPersistentData(player).remove("bodycam_active");
        BodycamHelper.getPersistentData(player).remove("bodycam_target_uuid");
        BodycamHelper.getPersistentData(player).remove("bodycam_dummy_uuid");
        player.setCamera(player);
        NetworkManager.sendToPlayer(player, new BodycamResetCameraS2CPacket());
    }

    public static void cleanupDummy(ServerPlayer player) {
        if (BodycamHelper.getPersistentData(player).contains("bodycam_dummy_uuid")) {
            UUID oldId = BodycamHelper.getPersistentData(player).getUUID("bodycam_dummy_uuid");
            for (ServerLevel lvl : player.getServer().getAllLevels()) {
                net.minecraft.world.entity.Entity e = lvl.getEntity(oldId);
                if (e != null) e.discard();
            }
        }
    }

    private static void saveOriginalState(ServerPlayer player) {
        BodycamHelper.getPersistentData(player).putInt("bodycam_original_gamemode", player.gameMode.getGameModeForPlayer().getId());
        BodycamHelper.getPersistentData(player).putString("bodycam_orig_dim", player.level().dimension().location().toString());
        BodycamHelper.getPersistentData(player).putDouble("bodycam_orig_x", player.getX());
        BodycamHelper.getPersistentData(player).putDouble("bodycam_orig_y", player.getY());
        BodycamHelper.getPersistentData(player).putDouble("bodycam_orig_z", player.getZ());
        BodycamHelper.getPersistentData(player).putFloat("bodycam_orig_yrot", player.getYRot());
        BodycamHelper.getPersistentData(player).putFloat("bodycam_orig_xrot", player.getXRot());
        BodycamHelper.getPersistentData(player).putDouble("bodycam_orig_motx", player.getDeltaMovement().x);
        BodycamHelper.getPersistentData(player).putDouble("bodycam_orig_moty", player.getDeltaMovement().y);
        BodycamHelper.getPersistentData(player).putDouble("bodycam_orig_motz", player.getDeltaMovement().z);
        BodycamHelper.getPersistentData(player).putFloat("bodycam_orig_fall", player.fallDistance);
    }

    public static void handleSyncRequest(SyncBodycamRequestC2SPacket payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("This feature is restricted to Server Operators.").withStyle(ChatFormatting.RED));
                return;
            }
            
            boolean isOnHolo = payload.isOnHologram();
            if (isOnHolo) {
                boolean nearHologram = false;
                BlockPos playerPos = player.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-6, -6, -6), playerPos.offset(6, 6, 6))) {
                    if (player.level().getBlockState(pos).is(Bodycam.HOLOGRAM_BLOCK.get())) {
                        nearHologram = true;
                        break;
                    }
                }
                if (!nearHologram) {
                    isOnHolo = false;
                }
            }

            if (isOnHolo && !ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get()) {
                player.sendSystemMessage(Component.literal("The Hologram Block is disabled on this server.").withStyle(ChatFormatting.RED));
                return;
            }

            boolean hasReach = payload.hasReach();
            if (!ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()) {
                hasReach = false;
            }

            sendSyncPacket(player, hasReach, isOnHolo);
        });
    }

    private static void sendSyncPacket(ServerPlayer player, boolean hasReach, boolean isOnHologram) {
        Map<UUID, Integer> jammers = new HashMap<>();
        Map<UUID, UUID> targets = new HashMap<>();
        Map<UUID, String> dimensions = new HashMap<>();
        Map<UUID, BlockPos> positions = new HashMap<>();
        Map<UUID, Boolean> anonymizers = new HashMap<>();

        for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
            int mode = 0;
            boolean hasAnonymizer = false;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack stack = p.getInventory().getItem(i);
                if (stack.getItem() instanceof JammerItem) {
                    mode = Math.max(mode, stack.getOrDefault(ModDataComponents.JAMMER_MODE.get(), 0));
                }
                if (stack.getItem() instanceof AnonymizerItem) {
                    if (stack.getOrDefault(ModDataComponents.ANONYMIZER_ACTIVE.get(), false)) {
                        hasAnonymizer = true;
                    }
                }
            }
            jammers.put(p.getUUID(), mode);
            anonymizers.put(p.getUUID(), hasAnonymizer);

            if (BodycamHelper.getPersistentData(p).getBoolean("bodycam_active") && BodycamHelper.getPersistentData(p).contains("bodycam_target_uuid")) {
                targets.put(p.getUUID(), BodycamHelper.getPersistentData(p).getUUID("bodycam_target_uuid"));
            }

            dimensions.put(p.getUUID(), p.level().dimension().location().toString());
            positions.put(p.getUUID(), p.blockPosition());
        }

        NetworkManager.sendToPlayer(player, new SyncBodycamStatesS2CPacket(jammers, targets, dimensions, positions, anonymizers, hasReach, isOnHologram));
    }
}
