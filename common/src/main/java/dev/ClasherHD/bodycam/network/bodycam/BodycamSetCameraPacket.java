package dev.ClasherHD.bodycam.network.bodycam;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.enchantment.NightspyEffectHandler;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.entity.HologramDummyEntity;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.LoggerItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.util.ModAdvancements;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class BodycamSetCameraPacket implements CustomPacketPayload {
    public static final Type<BodycamSetCameraPacket> TYPE = new Type<>(bodycam.id("bodycam_set_camera"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodycamSetCameraPacket> STREAM_CODEC =
            StreamCodec.of(BodycamSetCameraPacket::encode, BodycamSetCameraPacket::decode);

    public static final ConcurrentHashMap<UUID, Vec3> ORIGINAL_POS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Vec2> ORIGINAL_ROT = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, String> ORIGINAL_DIM = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Integer> ORIGINAL_GAMEMODE = new ConcurrentHashMap<>();

    public final UUID targetId;
    public final boolean isOnHologram;
    public final boolean isAllSeeingEye;

    public BodycamSetCameraPacket(UUID targetId, boolean isOnHologram) {
        this(targetId, isOnHologram, false);
    }

    public BodycamSetCameraPacket(UUID targetId, boolean isOnHologram, boolean isAllSeeingEye) {
        this.targetId = targetId;
        this.isOnHologram = isOnHologram;
        this.isAllSeeingEye = isAllSeeingEye;
    }

    public static void encode(RegistryFriendlyByteBuf buf, BodycamSetCameraPacket msg) {
        buf.writeUUID(msg.targetId);
        buf.writeBoolean(msg.isOnHologram);
        buf.writeBoolean(msg.isAllSeeingEye);
    }

    public static BodycamSetCameraPacket decode(RegistryFriendlyByteBuf buf) {
        return new BodycamSetCameraPacket(buf.readUUID(), buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BodycamSetCameraPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player) || player.server == null) {
                return;
            }
            if (ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                return;
            }
            if (CreepingDeathEntity.blockObservationIfHunted(player)) {
                return;
            }
            CompoundTag playerData = PersistentDataAccess.get(player);
            boolean alreadyActive = playerData.getBoolean("bodycam_active");
            if (player.isSpectator() && !alreadyActive) {
                return;
            }

            boolean isAllSeeingEye;
            if (alreadyActive) {
                isAllSeeingEye = playerData.getBoolean("bodycam_all_seeing_active");
            } else {
                isAllSeeingEye = msg.isAllSeeingEye
                        && ModServerConfig.ENABLE_ALL_SEEING_EYE.get()
                        && playerData.getBoolean("bodycam_all_seeing_pending")
                        && AllSeeingEyeItem.hasActivatedEye(player);
            }
            if (msg.isAllSeeingEye && !isAllSeeingEye) {
                PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                return;
            }

            Vec3 observerPos = player.position();
            String observerDim = player.level().dimension().location().toString();
            if (alreadyActive) {
                Vec3 dummyPos = BodycamDummyEntity.DUMMY_POS.get(player.getUUID());
                if (dummyPos != null) {
                    observerPos = dummyPos;
                } else if (ORIGINAL_POS.containsKey(player.getUUID())) {
                    observerPos = ORIGINAL_POS.get(player.getUUID());
                } else if (playerData.contains("bodycam_orig_x")) {
                    observerPos = new Vec3(
                            playerData.getDouble("bodycam_orig_x"),
                            playerData.getDouble("bodycam_orig_y"),
                            playerData.getDouble("bodycam_orig_z")
                    );
                }
                if (ORIGINAL_DIM.containsKey(player.getUUID())) {
                    observerDim = ORIGINAL_DIM.get(player.getUUID());
                } else if (playerData.contains("bodycam_orig_dim")) {
                    observerDim = playerData.getString("bodycam_orig_dim");
                }
            }

            ServerPlayer target = player.server.getPlayerList().getPlayer(msg.targetId);
            WatchPointDummyEntity targetWatchPoint = null;
            if (target == null) {
                if (ModServerConfig.ENABLE_WATCH_POINT.get()) {
                    targetWatchPoint = WatchPointDummyEntity.findByUUID(player.server, msg.targetId);
                    if (targetWatchPoint != null && !isAllSeeingEye && !player.getUUID().equals(targetWatchPoint.getOwnerUuidOrNull())) {
                        targetWatchPoint = null;
                    }
                }
            }

            Entity targetEntity = target != null ? target : targetWatchPoint;
            if (targetEntity == null || !(targetEntity.level() instanceof ServerLevel targetServerLevel)) {
                PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                return;
            }

            if (ModServerConfig.isDimensionBlacklisted(player.level())
                    || ModServerConfig.isDimensionBlacklisted(targetServerLevel)) {
                player.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
                PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                return;
            }

            if (target != null && !isAllSeeingEye) {
                if (EnchantmentLevels.isHiddenFromSelection(target)) {
                    PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                    return;
                }

                CompoundTag targetData = PersistentDataAccess.get(target);
                if (targetData.getBoolean("bodycam_active") && targetData.contains("bodycam_target_uuid")) {
                    UUID targetUuid = targetData.getUUID("bodycam_target_uuid");
                    if (targetUuid != null && targetUuid.equals(player.getUUID())) {
                        player.sendSystemMessage(Component.translatable("message.bodycam.signal_weak").withStyle(ChatFormatting.RED));
                        PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                        return;
                    }
                }

                long lastJammer = targetData.getLong("bodycam_jammer_heartbeat");
                int jammerMode = targetData.getInt("bodycam_jammer_mode");
                boolean isJammerActive = (target.level().getGameTime() - lastJammer) <= 10;
                if (!isJammerActive) {
                    jammerMode = 0;
                }
                jammerMode = PodestBlockEntity.combineJammerModes(jammerMode, PodestBlockEntity.getPodestJammerMode(target));

                if (jammerMode == 1) {
                    player.sendSystemMessage(Component.translatable("message.bodycam.jammer_blocked").withStyle(ChatFormatting.RED));
                    PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                    return;
                } else if (jammerMode == 2) {
                    String targetDim = target.level().dimension().location().toString();
                    if (!observerDim.equals(targetDim)
                            || observerPos.distanceTo(target.position()) > (double) ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                        player.sendSystemMessage(Component.translatable("message.bodycam.jammer_blocked").withStyle(ChatFormatting.RED));
                        PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                        return;
                    }
                }
            }

            boolean isOnHologram;
            if (isAllSeeingEye) {
                isOnHologram = false;
            } else if (alreadyActive) {
                isOnHologram = playerData.getBoolean("bodycam_on_hologram");
            } else {
                isOnHologram = msg.isOnHologram;
                if (isOnHologram) {
                    if (!ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get()) {
                        isOnHologram = false;
                    } else {
                        boolean onHologramBlock = player.level().getBlockState(player.blockPosition().below()).is(ModBlocks.HOLOGRAM_BLOCK.get())
                                || player.level().getBlockState(player.blockPosition()).is(ModBlocks.HOLOGRAM_BLOCK.get());
                        if (!onHologramBlock) {
                            isOnHologram = false;
                        }
                    }
                }
            }

            int reachLevel = isAllSeeingEye ? 2 : EnchantmentLevels.sessionReach(player, isOnHologram);

            if (!isAllSeeingEye && reachLevel < 2) {
                String targetDim = targetEntity.level().dimension().location().toString();
                if (!observerDim.equals(targetDim)
                        || (reachLevel < 1 && observerPos.distanceTo(targetEntity.position()) > (double) ModServerConfig.MAX_MONITOR_DISTANCE.get())) {
                    player.sendSystemMessage(Component.translatable("message.bodycam.signal_weak").withStyle(ChatFormatting.RED));
                    PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                    return;
                }
            }

            if (!isAllSeeingEye && !isOnHologram && !alreadyActive) {
                ItemStack spyglassStack = EnchantmentLevels.heldBodycam(player);
                if (spyglassStack.isEmpty() || ItemDurabilityHelper.isEmpty(spyglassStack)) {
                    PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
                    return;
                }
                ItemDurabilityHelper.damagePerUse(spyglassStack, player, 1);
            }

            if (!alreadyActive) {
                if (isAllSeeingEye) {
                    playerData.remove("bodycam_all_seeing_pending");
                    playerData.putBoolean("bodycam_all_seeing_active", true);

                    AllSeeingEyeItem.consumeOne(player);

                    player.level().playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.ALL_SEEING_EYE_USE.get(),
                            SoundSource.PLAYERS,
                            3.125F,
                            1.65F
                    );

                    playerData.putInt("bodycam_all_seeing_fog_ticks", AllSeeingEyeItem.FOG_DURATION_TICKS);
                    playerData.putDouble("bodycam_fog_x", player.getX());
                    playerData.putDouble("bodycam_fog_y", player.getY());
                    playerData.putDouble("bodycam_fog_z", player.getZ());
                    playerData.putString("bodycam_fog_dim", player.level().dimension().location().toString());
                    player.serverLevel().sendParticles(
                            ParticleTypes.END_ROD,
                            player.getX(),
                            player.getY() + 1.0D,
                            player.getZ(),
                            35,
                            0.75D,
                            0.85D,
                            0.75D,
                            0.02D
                    );
                }

                if (playerData.contains("bodycam_dummy_uuid")) {
                    UUID oldId = playerData.getUUID("bodycam_dummy_uuid");
                    for (ServerLevel lvl : player.server.getAllLevels()) {
                        Entity e = lvl.getEntity(oldId);
                        if (e != null) {
                            e.discard();
                        }
                    }
                }

                int currentGm = player.gameMode.getGameModeForPlayer().getId();
                String currentDim = player.level().dimension().location().toString();
                Vec3 currentPos = player.position();
                Vec2 currentRot = new Vec2(player.getXRot(), player.getYRot());

                ORIGINAL_DIM.put(player.getUUID(), currentDim);
                ORIGINAL_POS.put(player.getUUID(), currentPos);
                ORIGINAL_ROT.put(player.getUUID(), currentRot);
                ORIGINAL_GAMEMODE.put(player.getUUID(), currentGm);

                playerData.putInt("bodycam_original_gamemode", currentGm);
                playerData.putString("bodycam_orig_dim", currentDim);
                playerData.putDouble("bodycam_orig_x", currentPos.x);
                playerData.putDouble("bodycam_orig_y", currentPos.y);
                playerData.putDouble("bodycam_orig_z", currentPos.z);
                playerData.putFloat("bodycam_orig_yrot", currentRot.y);
                playerData.putFloat("bodycam_orig_xrot", currentRot.x);
                playerData.putBoolean("bodycam_on_hologram", isOnHologram);

                BodycamDummyEntity dummy;
                if (isOnHologram) {
                    dummy = new HologramDummyEntity(ModEntityTypes.HOLOGRAM_DUMMY.get(), player.serverLevel());
                    dummy.setInvulnerable(true);
                } else {
                    dummy = new BodycamDummyEntity(ModEntityTypes.BODYCAM_DUMMY.get(), player.serverLevel());
                }

                dummy.setPos(player.getX(), player.getY(), player.getZ());
                dummy.setYRot(player.getYRot());
                dummy.setXRot(player.getXRot());
                dummy.setYHeadRot(player.getYHeadRot());

                dummy.setOwnerUUID(player.getUUID());
                dummy.setOwnerName(player.getName().getString());
                dummy.setCustomName(Component.literal(player.getName().getString()));
                dummy.setCustomNameVisible(true);

                if (player.getAttribute(Attributes.ARMOR) != null && dummy.getAttribute(Attributes.ARMOR) != null) {
                    dummy.getAttribute(Attributes.ARMOR).setBaseValue(player.getAttributeValue(Attributes.ARMOR));
                }
                if (player.getAttribute(Attributes.ARMOR_TOUGHNESS) != null && dummy.getAttribute(Attributes.ARMOR_TOUGHNESS) != null) {
                    dummy.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
                }
                if (player.getAttribute(Attributes.MAX_HEALTH) != null && dummy.getAttribute(Attributes.MAX_HEALTH) != null) {
                    dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getAttributeValue(Attributes.MAX_HEALTH));
                }

                dummy.setHealth(player.getHealth());
                dummy.setAbsorptionAmount(player.getAbsorptionAmount());
                dummy.setDeltaMovement(player.getDeltaMovement());
                dummy.fallDistance = player.fallDistance;

                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    dummy.setItemSlot(slot, player.getItemBySlot(slot).copy());
                }

                BodycamDummyEntity.DUMMY_POS.put(player.getUUID(), dummy.position());
                BodycamDummyEntity.DUMMY_FALL.put(player.getUUID(), dummy.fallDistance);
                BodycamDummyEntity.DUMMY_MOTION.put(player.getUUID(), dummy.getDeltaMovement());

                player.serverLevel().addFreshEntity(dummy);
                int chunkX = Mth.floor(dummy.getX()) >> 4;
                int chunkZ = Mth.floor(dummy.getZ()) >> 4;
                BodycamPlatform.forceChunk(player.serverLevel(), dummy.getUUID(), chunkX, chunkZ, true);
                dummy.markChunkForced(chunkX, chunkZ);
                playerData.putUUID("bodycam_dummy_uuid", dummy.getUUID());
            }

            UUID oldTargetId = playerData.contains("bodycam_target_uuid")
                    ? playerData.getUUID("bodycam_target_uuid")
                    : null;
            if (oldTargetId != null && !oldTargetId.equals(msg.targetId)) {
                ServerPlayer oldTarget = player.server.getPlayerList().getPlayer(oldTargetId);
                if (oldTarget != null) {
                    PacketHandler.sendToPlayer(oldTarget, new CrossObservationSyncPacket(player.getUUID(), false));
                }
            }

            if (target != null && (!alreadyActive || oldTargetId == null || !oldTargetId.equals(msg.targetId))) {
                LoggerItem.recordObservation(target, player, observerDim, observerPos);
                if (!isAllSeeingEye && ItemDurabilityHelper.hasFullObservationCrystalTrimSet(target)) {
                    target.connection.send(new ClientboundSoundPacket(
                            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.TRIM_OBSERVATION_ALERT.get()),
                            SoundSource.PLAYERS,
                            target.getX(),
                            target.getY(),
                            target.getZ(),
                            0.45F,
                            0.72F,
                            target.getRandom().nextLong()
                    ));
                }
            }

            playerData.putBoolean("bodycam_active", true);
            ModAdvancements.award(player, ModAdvancements.FIRST_OBSERVATION);
            playerData.putBoolean("bodycam_watching_watchpoint", targetWatchPoint != null);
            playerData.putUUID("bodycam_target_uuid", msg.targetId);
            playerData.putInt("bodycam_disconnect_ticks", 0);
            playerData.putInt("bodycam_reach_level", reachLevel);
            NightspyEffectHandler.begin(player, EnchantmentLevels.sessionNightspy(player, isOnHologram));

            if (target != null) {
                PacketHandler.sendToPlayer(target, new CrossObservationSyncPacket(player.getUUID(), true));
            }
            player.setGameMode(GameType.SPECTATOR);

            player.teleportTo(targetServerLevel, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(),
                    Set.of(), targetEntity.getYRot(), targetEntity.getXRot());
            player.setOldPosAndRot();
            BodycamPlatform.stopRidingAndSetCamera(player, target != null ? target : player);
        });
    }
}
