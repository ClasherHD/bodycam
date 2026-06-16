package dev.ClasherHD.bodycam.event;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.CrossObservationSyncPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.commands.BodycamCommands;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = bodycam.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("null")
public class ServerEvents {

    public static final ConcurrentHashMap<UUID, LockData> POSITION_LOCKS = new ConcurrentHashMap<>();

    public static class LockData {
        public final ResourceKey<Level> originalDim;
        public final double x, y, z;
        public final float yaw, pitch;
        public int lockTicks = 10;

        public LockData(ResourceKey<Level> originalDim, double x, double y, double z, float yaw, float pitch) {
            this.originalDim = originalDim;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity() instanceof ServerPlayer player) {
            if (player.getPersistentData().contains("bodycam_target_uuid")
                    || player.getPersistentData().contains("bodycam_dummy_uuid")) {
                
                if (player.getPersistentData().contains("bodycam_orig_dim")) {
                    String dimStr = player.getPersistentData().getString("bodycam_orig_dim");
                    ResourceKey<Level> dimKey = ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.tryParse(dimStr)
                    );
                    ServerLevel origLvl = player.server.getLevel(dimKey);
                    if (origLvl != null) {
                        double ox = player.getPersistentData().getDouble("bodycam_orig_x");
                        double oy = player.getPersistentData().getDouble("bodycam_orig_y");
                        double oz = player.getPersistentData().getDouble("bodycam_orig_z");
                        float oyrot = player.getPersistentData().getFloat("bodycam_orig_yrot");
                        float oxrot = player.getPersistentData().getFloat("bodycam_orig_xrot");
                        player.teleportTo(origLvl, ox, oy, oz, oyrot, oxrot);
                    }
                }

                if (player.getPersistentData().contains("bodycam_dummy_uuid")) {
                    UUID dummyId = player.getPersistentData().getUUID("bodycam_dummy_uuid");
                    for (ServerLevel lvl : player.server.getAllLevels()) {
                        Entity e = lvl.getEntity(dummyId);
                        if (e != null) {
                            if (!player.getPersistentData().contains("bodycam_orig_dim")) {
                                player.teleportTo(lvl, e.getX(), e.getY(), e.getZ(),
                                                e.getYRot(), e.getXRot());
                            }
                            e.discard();
                            break;
                        }
                    }
                }
                if (player.getPersistentData().contains("bodycam_target_uuid")) {
                    UUID oldTargetId = player.getPersistentData().getUUID("bodycam_target_uuid");
                    if (oldTargetId != null) {
                        ServerPlayer oldTarget = player.server.getPlayerList().getPlayer(oldTargetId);
                        if (oldTarget != null) {
                            PacketHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> oldTarget),
                                new CrossObservationSyncPacket(player.getUUID(), false)
                            );
                        }
                    }
                }
                if (player.getPersistentData().contains("bodycam_original_gamemode")) {
                    int gameModeId = player.getPersistentData().getInt("bodycam_original_gamemode");
                    player.setGameMode(GameType.byId(gameModeId));
                }
                player.getPersistentData().remove("bodycam_target_uuid");
                player.getPersistentData().remove("bodycam_dummy_uuid");
                player.getPersistentData().remove("bodycam_disconnect_ticks");
                player.getPersistentData().remove("bodycam_active");
                player.getPersistentData().remove("bodycam_has_reach");
                player.getPersistentData().remove("bodycam_original_dimension");
                player.getPersistentData().remove("bodycam_original_gamemode");
                player.getPersistentData().remove("bodycam_orig_dim");
                player.getPersistentData().remove("bodycam_orig_x");
                player.getPersistentData().remove("bodycam_orig_y");
                player.getPersistentData().remove("bodycam_orig_z");
                player.getPersistentData().remove("bodycam_orig_yrot");
                player.getPersistentData().remove("bodycam_orig_xrot");
                player.setInvisible(false);
                BodycamSetCameraPacket.ORIGINAL_POS.remove(player.getUUID());
                BodycamSetCameraPacket.ORIGINAL_ROT.remove(player.getUUID());
                BodycamSetCameraPacket.ORIGINAL_DIM.remove(player.getUUID());
                BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(player.getUUID());
                POSITION_LOCKS.remove(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity() instanceof ServerPlayer player) {
            if (player.getPersistentData().contains("bodycam_target_uuid")
                    || player.getPersistentData().contains("bodycam_dummy_uuid")) {
                if (player.getPersistentData().contains("bodycam_dummy_uuid")) {
                    UUID dummyId = player.getPersistentData().getUUID("bodycam_dummy_uuid");
                    for (ServerLevel lvl : player.server.getAllLevels()) {
                        Entity e = lvl.getEntity(dummyId);
                        if (e != null) {
                            e.discard();
                            break;
                        }
                    }
                }
                if (player.getPersistentData().contains("bodycam_target_uuid")) {
                    UUID oldTargetId = player.getPersistentData().getUUID("bodycam_target_uuid");
                    if (oldTargetId != null) {
                        ServerPlayer oldTarget = player.server.getPlayerList().getPlayer(oldTargetId);
                        if (oldTarget != null) {
                            PacketHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> oldTarget),
                                new CrossObservationSyncPacket(player.getUUID(), false)
                            );
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BodycamCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()
                && event.player instanceof ServerPlayer observer) {

            ItemStack carried = observer.containerMenu.getCarried();

            if (observer.getPersistentData().hasUUID("bodycam_active_jammer_id")) {
                boolean hasJammer = carried.getItem() instanceof JammerItem;
                if (!hasJammer) {
                    for (ItemStack stack : observer.getInventory().items) {
                        if (stack.getItem() instanceof JammerItem) {
                            hasJammer = true;
                            break;
                        }
                    }
                }
                if (!hasJammer) {
                    for (ItemStack stack : observer.getInventory().armor) {
                        if (stack.getItem() instanceof JammerItem) {
                            hasJammer = true;
                            break;
                        }
                    }
                }
                if (!hasJammer) {
                    for (ItemStack stack : observer.getInventory().offhand) {
                        if (stack.getItem() instanceof JammerItem) {
                            hasJammer = true;
                            break;
                        }
                    }
                }
                if (!hasJammer) {
                    observer.getPersistentData().remove("bodycam_active_jammer_id");
                    observer.getPersistentData().remove("bodycam_jammer_mode");
                }
            }

            if (observer.getPersistentData().hasUUID("bodycam_active_anonymizer_id")) {
                boolean hasAnonymizer = carried.getItem() instanceof AnonymizerItem;
                if (!hasAnonymizer) {
                    for (ItemStack stack : observer.getInventory().items) {
                        if (stack.getItem() instanceof AnonymizerItem) {
                            hasAnonymizer = true;
                            break;
                        }
                    }
                }
                if (!hasAnonymizer) {
                    for (ItemStack stack : observer.getInventory().armor) {
                        if (stack.getItem() instanceof AnonymizerItem) {
                            hasAnonymizer = true;
                            break;
                        }
                    }
                }
                if (!hasAnonymizer) {
                    for (ItemStack stack : observer.getInventory().offhand) {
                        if (stack.getItem() instanceof AnonymizerItem) {
                            hasAnonymizer = true;
                            break;
                        }
                    }
                }
                if (!hasAnonymizer) {
                    observer.getPersistentData().remove("bodycam_active_anonymizer_id");
                }
            }
            if (!carried.isEmpty()) {
                if (carried.getItem() instanceof JammerItem) {
                    if (carried.hasTag() && carried.getTag().contains("JammerMode")
                            && carried.getTag().getInt("JammerMode") > 0) {
                        if (carried.getTag().hasUUID("active_id")
                                && observer.getPersistentData().hasUUID("bodycam_active_jammer_id")) {
                            if (carried.getTag().getUUID("active_id")
                                    .equals(observer.getPersistentData().getUUID("bodycam_active_jammer_id"))) {
                                observer.getPersistentData().putLong(
                                        "bodycam_jammer_heartbeat",
                                        observer.level().getGameTime()
                                );
                            }
                        }
                    }
                } else if (carried.getItem() instanceof AnonymizerItem) {
                    if (carried.hasTag() && carried.getTag().contains("AnonymizerActive")
                            && carried.getTag().getBoolean("AnonymizerActive")) {
                        if (carried.getTag().hasUUID("active_id")
                                && observer.getPersistentData().hasUUID("bodycam_active_anonymizer_id")) {
                            if (carried.getTag().getUUID("active_id").equals(observer
                                    .getPersistentData()
                                    .getUUID("bodycam_active_anonymizer_id"))) {
                                observer.getPersistentData().putLong(
                                        "bodycam_anonymizer_heartbeat",
                                        observer.level().getGameTime()
                                );
                            }
                        }
                    }
                }
            }

            if (POSITION_LOCKS.containsKey(observer.getUUID())) {
                LockData data = POSITION_LOCKS.get(observer.getUUID());
                if (observer.level().dimension() == data.originalDim) {
                    observer.teleportTo(observer.serverLevel(), data.x, data.y, data.z, data.yaw, data.pitch);
                    observer.hurtMarked = true;
                    data.lockTicks--;
                    if (data.lockTicks <= 0) {
                        POSITION_LOCKS.remove(observer.getUUID());
                    }
                }
            }

            if (observer.getPersistentData().getBoolean("bodycam_active")
                    && observer.getPersistentData().contains("bodycam_target_uuid")) {
                UUID targetId = observer.getPersistentData().getUUID("bodycam_target_uuid");
                ServerPlayer target = observer.server.getPlayerList().getPlayer(targetId);
                if (target != null && target.isAlive() && !target.isRemoved()) {
                    observer.getPersistentData().putInt("bodycam_disconnect_ticks", 0);

                    BodycamDummyEntity dummy = null;
                    if (observer.getPersistentData().contains("bodycam_dummy_uuid")) {
                        UUID dummyId = observer.getPersistentData().getUUID("bodycam_dummy_uuid");
                        for (ServerLevel lvl : observer.server.getAllLevels()) {
                            Entity e = lvl.getEntity(dummyId);
                            if (e instanceof BodycamDummyEntity d) {
                                dummy = d;
                                break;
                            }
                        }
                    }

                    Vec3 dummyPosVal = null;
                    ResourceKey<Level> dummyDim = null;

                    if (dummy != null) {
                        dummyPosVal = dummy.position();
                        dummyDim = dummy.level().dimension();
                    } else {
                        dummyPosVal = BodycamDummyEntity.DUMMY_POS.get(observer.getUUID());
                        String dimStr = BodycamSetCameraPacket.ORIGINAL_DIM.get(observer.getUUID());
                        if (dimStr != null && !dimStr.isEmpty()) {
                            dummyDim = ResourceKey.create(
                                Registries.DIMENSION,
                                new ResourceLocation(dimStr)
                            );
                        }
                    }

                    if (dummyPosVal == null) {
                        dummyPosVal = BodycamSetCameraPacket.ORIGINAL_POS.get(observer.getUUID());
                    }
                    if (dummyDim == null) {
                        dummyDim = observer.serverLevel().dimension();
                    }

                    long lastJammer = target.getPersistentData().getLong("bodycam_jammer_heartbeat");
                    int currentJammerMode = target.getPersistentData().getInt("bodycam_jammer_mode");
                    boolean isJammerActive = (target.level().getGameTime() - lastJammer) <= 10;
                    if (!isJammerActive) {
                        currentJammerMode = 0;
                    }

                    if (currentJammerMode == 2) {
                        boolean outOfRange = dummyDim != target.level().dimension();
                        if (!outOfRange && dummyPosVal != null) {
                            double maxDist = (double) ModServerConfig.MAX_MONITOR_DISTANCE.get();
                            outOfRange = dummyPosVal.distanceToSqr(target.position()) > maxDist * maxDist;
                        }
                        if (outOfRange) {
                            BodycamResetCameraPacket.executeReset(observer);
                            PacketHandler.INSTANCE.send(
                                    PacketDistributor.PLAYER.with(() -> observer),
                                    new BodycamForceClosePacket()
                            );
                            observer.sendSystemMessage(Component
                                    .translatable("message.bodycam.jammer_blocked")
                                    .withStyle(ChatFormatting.RED));
                            return;
                        }
                    } else if (currentJammerMode == 1) {
                        BodycamResetCameraPacket.executeReset(observer);
                        PacketHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> observer),
                                new BodycamForceClosePacket()
                        );
                        observer.sendSystemMessage(Component
                                .translatable("message.bodycam.jammer_blocked")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }

                    boolean reachActive = observer.getPersistentData().getBoolean("bodycam_has_reach")
                            && ModServerConfig.ENABLE_REACH_ENCHANTMENT.get();

                    if (!reachActive) {
                        boolean outOfRange2 = dummyDim != target.level().dimension();
                        if (!outOfRange2 && dummyPosVal != null) {
                            double maxDist2 = (double) ModServerConfig.MAX_MONITOR_DISTANCE.get();
                            outOfRange2 = dummyPosVal.distanceToSqr(target.position()) > maxDist2 * maxDist2;
                        }
                        if (outOfRange2) {
                            BodycamResetCameraPacket.executeReset(observer);
                            PacketHandler.INSTANCE.send(
                                    PacketDistributor.PLAYER.with(() -> observer),
                                    new BodycamForceClosePacket()
                            );
                            observer.sendSystemMessage(Component
                                    .translatable("message.bodycam.signal_weak")
                                    .withStyle(ChatFormatting.RED));
                            return;
                        }
                    }

                    if (observer.level().dimension() == target.level().dimension()) {
                        observer.setPos(target.getX(), target.getY(), target.getZ());
                    } else {
                        observer.teleportTo(target.serverLevel(), target.getX(), target.getY(),
                                target.getZ(), target.getYRot(), target.getXRot());
                    }
                    observer.setCamera(target);
                } else {
                    int ticks = observer.getPersistentData().getInt("bodycam_disconnect_ticks");
                    ticks++;
                    observer.getPersistentData().putInt("bodycam_disconnect_ticks", ticks);
                    if (ticks >= 40) {
                        BodycamResetCameraPacket.executeReset(observer);
                    }
                }
            }
        }
    }
}
