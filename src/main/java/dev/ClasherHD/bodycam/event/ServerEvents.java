package dev.ClasherHD.bodycam.event;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.commands.BodycamCommands;
import dev.ClasherHD.bodycam.compat.curios.CuriosCompat;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.enchantment.NightspyEffectHandler;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.CrossObservationSyncPacket;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (ItemDurabilityHelper.isRepairableModItem(left) || left.getItem() instanceof PortableNetherPortalItem) {
            if (right.getItem() instanceof EnchantedBookItem || right.isEnchanted()) {
                if (!ItemDurabilityHelper.isBookEnchantable(left, right)) {
                    event.setCanceled(true);
                }
            } else if (ItemStack.isSameItem(left, right)) {
                if (!right.isEnchanted() || !ItemDurabilityHelper.isBookEnchantable(left, right)) {
                    event.setCanceled(true);
                }
            }
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
                player.getPersistentData().remove("bodycam_watching_watchpoint");
                player.getPersistentData().remove("bodycam_dummy_uuid");
                player.getPersistentData().remove("bodycam_disconnect_ticks");
                player.getPersistentData().remove("bodycam_active");
                player.getPersistentData().remove("bodycam_reach_level");
                NightspyEffectHandler.clear(player);
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
                boolean hasJammer = (carried.getItem() instanceof JammerItem && !ItemDurabilityHelper.isEmpty(carried));
                if (!hasJammer) {
                    for (ItemStack curioStack : CuriosCompat.getEquippedItems(observer, ModItems.JAMMER.get())) {
                        if (!ItemDurabilityHelper.isEmpty(curioStack)) {
                            hasJammer = true;
                            break;
                        }
                    }
                }
                if (!hasJammer) {
                    for (int i = 0; i < observer.getInventory().getContainerSize(); i++) {
                        ItemStack invStack = observer.getInventory().getItem(i);
                        if (invStack.getItem() instanceof JammerItem && !ItemDurabilityHelper.isEmpty(invStack)) {
                            hasJammer = true;
                            break;
                        }
                    }
                }
                if (hasJammer) {
                    observer.getPersistentData().putInt("bodycam_jammer_missing_ticks", 0);
                } else {
                    int missing = observer.getPersistentData().getInt("bodycam_jammer_missing_ticks") + 1;
                    if (!observer.isCreative() || missing > 10) {
                        observer.getPersistentData().remove("bodycam_active_jammer_id");
                        observer.getPersistentData().remove("bodycam_jammer_mode");
                        observer.getPersistentData().remove("bodycam_jammer_missing_ticks");
                    } else {
                        observer.getPersistentData().putInt("bodycam_jammer_missing_ticks", missing);
                    }
                }
            }

            if (observer.getPersistentData().hasUUID("bodycam_active_anonymizer_id")) {
                boolean hasAnonymizer = (carried.getItem() instanceof AnonymizerItem && !ItemDurabilityHelper.isEmpty(carried));
                if (!hasAnonymizer) {
                    for (ItemStack curioStack : CuriosCompat.getEquippedItems(observer, ModItems.ANONYMIZER.get())) {
                        if (!ItemDurabilityHelper.isEmpty(curioStack)) {
                            hasAnonymizer = true;
                            break;
                        }
                    }
                }
                if (!hasAnonymizer) {
                    for (int i = 0; i < observer.getInventory().getContainerSize(); i++) {
                        ItemStack invStack = observer.getInventory().getItem(i);
                        if (invStack.getItem() instanceof AnonymizerItem && !ItemDurabilityHelper.isEmpty(invStack)) {
                            hasAnonymizer = true;
                            break;
                        }
                    }
                }
                if (hasAnonymizer) {
                    observer.getPersistentData().putInt("bodycam_anonymizer_missing_ticks", 0);
                } else {
                    int missing = observer.getPersistentData().getInt("bodycam_anonymizer_missing_ticks") + 1;
                    if (!observer.isCreative() || missing > 10) {
                        observer.getPersistentData().remove("bodycam_active_anonymizer_id");
                        observer.getPersistentData().remove("bodycam_anonymizer_missing_ticks");
                    } else {
                        observer.getPersistentData().putInt("bodycam_anonymizer_missing_ticks", missing);
                    }
                }
            }

            if (!carried.isEmpty()) {
                if (carried.getItem() instanceof JammerItem || carried.getItem() instanceof AnonymizerItem) {
                    carried.getItem().inventoryTick(carried, observer.level(), observer, -1, false);
                }
            }

            for (ItemStack curioStack : CuriosCompat.getEquippedItems(observer, ModItems.JAMMER.get())) {
                curioStack.getItem().inventoryTick(curioStack, observer.level(), observer, -1, false);
            }
            for (ItemStack curioStack : CuriosCompat.getEquippedItems(observer, ModItems.ANONYMIZER.get())) {
                curioStack.getItem().inventoryTick(curioStack, observer.level(), observer, -1, false);
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

                if (observer.getPersistentData().getBoolean("bodycam_watching_watchpoint")) {
                    WatchPointDummyEntity wp = WatchPointDummyEntity.findByUUID(observer.server, targetId);
                    if (wp == null || !wp.isAlive() || wp.isRemoved() || !(wp.level() instanceof ServerLevel wpLevel)) {
                        BodycamResetCameraPacket.executeReset(observer);
                        return;
                    }

                    int reachLevel = ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()
                            ? observer.getPersistentData().getInt("bodycam_reach_level") : 0;

                    if (reachLevel < 2) {
                        boolean outOfRange = dummyDim != wp.level().dimension();
                        if (!outOfRange && reachLevel < 1 && dummyPosVal != null) {
                            double maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
                            outOfRange = dummyPosVal.distanceToSqr(wp.position()) > maxDist * maxDist;
                        }
                        if (outOfRange) {
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

                    NightspyEffectHandler.tick(observer, wp, dummyPosVal, dummyDim);

                    if (observer.level().dimension() == wp.level().dimension()) {
                        observer.setPos(wp.getX(), wp.getY(), wp.getZ());
                    } else {
                        observer.teleportTo(wpLevel, wp.getX(), wp.getY(), wp.getZ(), observer.getYRot(), observer.getXRot());
                    }
                    observer.setCamera(observer);
                    return;
                }

                ServerPlayer target = observer.server.getPlayerList().getPlayer(targetId);
                if (target != null && target.isAlive() && !target.isRemoved()) {
                    observer.getPersistentData().putInt("bodycam_disconnect_ticks", 0);

                    if (EnchantmentLevels.isHiddenFromSelection(target)) {
                        BodycamResetCameraPacket.executeReset(observer);
                        PacketHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> observer),
                                new BodycamForceClosePacket()
                        );
                        return;
                    }

                    long lastJammer = target.getPersistentData().getLong("bodycam_jammer_heartbeat");
                    int currentJammerMode = target.getPersistentData().getInt("bodycam_jammer_mode");
                    boolean isJammerActive = (target.level().getGameTime() - lastJammer) <= 10;
                    if (!isJammerActive) {
                        currentJammerMode = 0;
                    }
                    currentJammerMode = PodestBlockEntity.combineJammerModes(currentJammerMode, PodestBlockEntity.getPodestJammerMode(target));

                    if (currentJammerMode == 2) {
                        boolean outOfRange = dummyDim != target.level().dimension();
                        if (!outOfRange && dummyPosVal != null) {
                            double maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
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

                    int reachLevel = ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()
                            ? observer.getPersistentData().getInt("bodycam_reach_level") : 0;

                    if (reachLevel < 2) {
                        boolean outOfRange2 = dummyDim != target.level().dimension();
                        if (!outOfRange2 && reachLevel < 1 && dummyPosVal != null) {
                            double maxDist2 = ModServerConfig.MAX_MONITOR_DISTANCE.get();
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

                    NightspyEffectHandler.tick(observer, target, dummyPosVal, dummyDim);

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
