package dev.ClasherHD.bodycam.event;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.commands.BodycamCommands;
import dev.ClasherHD.bodycam.compat.curios.CuriosCompat;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.enchantment.NightspyEffectHandler;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.entity.ScholarEntity;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.LogBookItem;
import dev.ClasherHD.bodycam.item.LogPaperItem;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.CrossObservationSyncPacket;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = bodycam.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("null")
public class ServerEvents {

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        POSITION_LOCKS.clear();
        BodycamSetCameraPacket.ORIGINAL_POS.clear();
        BodycamSetCameraPacket.ORIGINAL_ROT.clear();
        BodycamSetCameraPacket.ORIGINAL_DIM.clear();
        BodycamSetCameraPacket.ORIGINAL_GAMEMODE.clear();
        BodycamDummyEntity.DUMMY_POS.clear();
        BodycamDummyEntity.DUMMY_FALL.clear();
        BodycamDummyEntity.DUMMY_MOTION.clear();
        WatchPointDummyEntity.ACTIVE_WATCH_POINTS.clear();
        PodestBlockEntity.LOADED_PODESTS.clear();
        CreepingDeathEntity.ACTIVE_HUNTS_BY_TARGET.clear();
        synchronized (CreepingDeathEntity.PENDING_SPAWNS) {
            CreepingDeathEntity.PENDING_SPAWNS.clear();
        }
        synchronized (CreepingDeathEntity.PENDING_OSC_NUKES) {
            CreepingDeathEntity.PENDING_OSC_NUKES.clear();
        }
        dev.ClasherHD.bodycam.network.locator.PlayerLocatorSelectC2SPacket.clearCooldowns();
    }

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
    public static void onCartographyTableInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        Level level = event.getLevel();
        if (!level.getBlockState(event.getPos()).is(Blocks.CARTOGRAPHY_TABLE)) {
            return;
        }
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack book;
        ItemStack other;
        if (mainHand.getItem() instanceof LogBookItem) {
            book = mainHand;
            other = offHand;
        } else if (offHand.getItem() instanceof LogBookItem) {
            book = offHand;
            other = mainHand;
        } else {
            return;
        }
        if (!(other.getItem() instanceof LogPaperItem) && !(other.getItem() instanceof ShearsItem)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            LogBookItem.handleCartographyInteraction(serverPlayer, book, other);
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
            if (player.getPersistentData().getBoolean("bodycam_active")
                    || player.getPersistentData().contains("bodycam_target_uuid")
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
                player.setInvisible(false);
                BodycamResetCameraPacket.cleanupPlayerNbtAndMaps(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity() instanceof ServerPlayer player) {
            dev.ClasherHD.bodycam.network.locator.PlayerLocatorSelectC2SPacket.clearCooldown(player.getUUID());
            POSITION_LOCKS.remove(player.getUUID());
            BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(player.getUUID());
            BodycamSetCameraPacket.ORIGINAL_DIM.remove(player.getUUID());
            BodycamSetCameraPacket.ORIGINAL_POS.remove(player.getUUID());
            BodycamSetCameraPacket.ORIGINAL_ROT.remove(player.getUUID());
            BodycamDummyEntity.DUMMY_POS.remove(player.getUUID());
            BodycamDummyEntity.DUMMY_FALL.remove(player.getUUID());
            BodycamDummyEntity.DUMMY_MOTION.remove(player.getUUID());
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
            if (player.getPersistentData().getBoolean("bodycam_all_seeing_active")
                    || player.getPersistentData().getBoolean("bodycam_all_seeing_pending")) {
                player.getPersistentData().remove("bodycam_all_seeing_active");
                player.getPersistentData().remove("bodycam_all_seeing_pending");
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BodycamCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
            CreepingDeathEntity.tickServer(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer deadPlayer) {
            UUID assassinId = CreepingDeathEntity.ACTIVE_HUNTS_BY_TARGET.remove(deadPlayer.getUUID());
            if (assassinId != null && deadPlayer.server != null) {
                for (ServerLevel lvl : deadPlayer.server.getAllLevels()) {
                    Entity e = lvl.getEntity(assassinId);
                    if (e instanceof CreepingDeathEntity assassin) {
                        assassin.onTargetKilled();
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ScholarEntity) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() && event.getTarget() instanceof Villager villager) {
            clampLibrarianEnchantmentOffers(villager);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity() instanceof Villager villager
                && villager.tickCount % 40 == 0
                && villager.getVillagerData().getProfession() == VillagerProfession.LIBRARIAN) {
            clampLibrarianEnchantmentOffers(villager);
        }
    }

    private static void clampLibrarianEnchantmentOffers(Villager villager) {
        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) {
            return;
        }
        Enchantment nightspy = ModEnchantments.NIGHTSPY_ENCHANTMENT.get();
        Enchantment anonymity = ModEnchantments.ANONYMITY_ENCHANTMENT.get();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            ItemStack result = offer.getResult();
            if (!(result.getItem() instanceof EnchantedBookItem)) {
                continue;
            }
            java.util.Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(result);
            boolean needsReplace = false;
            if (enchants.containsKey(nightspy) && enchants.get(nightspy) > 1) {
                needsReplace = true;
            } else if (enchants.containsKey(anonymity)) {
                needsReplace = true;
            }
            if (needsReplace) {
                ItemStack clampedBook = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(nightspy, 1));
                ItemStack costA = offer.getBaseCostA().copy();
                if (costA.getCount() > 19) {
                    costA.setCount(14);
                }
                MerchantOffer replacement = new MerchantOffer(
                        costA,
                        offer.getCostB().copy(),
                        clampedBook,
                        offer.getUses(),
                        offer.getMaxUses(),
                        offer.getXp(),
                        offer.getPriceMultiplier(),
                        offer.getDemand()
                );
                offers.set(i, replacement);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer targetPlayer) {
            UUID assassinId = CreepingDeathEntity.ACTIVE_HUNTS_BY_TARGET.get(targetPlayer.getUUID());
            if (assassinId != null && targetPlayer.server != null) {
                for (ServerLevel lvl : targetPlayer.server.getAllLevels()) {
                    Entity e = lvl.getEntity(assassinId);
                    if (e instanceof CreepingDeathEntity assassin && assassin.isAlive() && !assassin.isDeadOrDying()) {
                        assassin.teleportToTargetDimension(targetPlayer.serverLevel(), targetPlayer);
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()
                && event.player instanceof ServerPlayer observer) {

            int fogTicks = observer.getPersistentData().getInt("bodycam_all_seeing_fog_ticks");
            if (fogTicks > 0) {
                observer.getPersistentData().putInt("bodycam_all_seeing_fog_ticks", fogTicks - 1);
                String fogDimStr = observer.getPersistentData().getString("bodycam_fog_dim");
                ServerLevel fogLevel = observer.serverLevel();
                if (!fogDimStr.isEmpty()) {
                    ResourceLocation rl = ResourceLocation.tryParse(fogDimStr);
                    if (rl != null) {
                        ServerLevel found = observer.server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
                        if (found != null) {
                            fogLevel = found;
                        }
                    }
                }
                double fx = observer.getPersistentData().getDouble("bodycam_fog_x");
                double fy = observer.getPersistentData().getDouble("bodycam_fog_y");
                double fz = observer.getPersistentData().getDouble("bodycam_fog_z");
                fogLevel.sendParticles(ParticleTypes.END_ROD, fx, fy + 1.0D, fz, 25, 0.85D, 0.85D, 0.85D, 0.03D);
            }

            int cdFogTicks = observer.getPersistentData().getInt("bodycam_creeping_death_fog_ticks");
            if (cdFogTicks > 0) {
                observer.getPersistentData().putInt("bodycam_creeping_death_fog_ticks", cdFogTicks - 1);
                String fogDimStr = observer.getPersistentData().getString("bodycam_cd_fog_dim");
                ServerLevel fogLevel = observer.serverLevel();
                if (!fogDimStr.isEmpty()) {
                    ResourceLocation rl = ResourceLocation.tryParse(fogDimStr);
                    if (rl != null) {
                        ServerLevel found = observer.server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
                        if (found != null) {
                            fogLevel = found;
                        }
                    }
                }
                double fx = observer.getPersistentData().getDouble("bodycam_cd_fog_x");
                double fy = observer.getPersistentData().getDouble("bodycam_cd_fog_y");
                double fz = observer.getPersistentData().getDouble("bodycam_cd_fog_z");
                fogLevel.sendParticles(ParticleTypes.LARGE_SMOKE, fx, fy + 1.0D, fz, 18, 0.75D, 0.85D, 0.75D, 0.02D);
                fogLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, fx, fy + 1.0D, fz, 8, 0.65D, 0.75D, 0.65D, 0.01D);
                fogLevel.sendParticles(ParticleTypes.SCULK_SOUL, fx, fy + 1.0D, fz, 12, 0.75D, 0.85D, 0.75D, 0.03D);
            }

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

                    boolean isAllSeeing = observer.getPersistentData().getBoolean("bodycam_all_seeing_active");

                    if (!isAllSeeing && EnchantmentLevels.isHiddenFromSelection(target)) {
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

                    if (!isAllSeeing && currentJammerMode == 2) {
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
                    } else if (!isAllSeeing && currentJammerMode == 1) {
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

                    int reachLevel = isAllSeeing ? 2 : (ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()
                            ? observer.getPersistentData().getInt("bodycam_reach_level") : 0);

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
