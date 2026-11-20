package dev.ClasherHD.bodycam.event;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.commands.BodycamCommands;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.enchantment.NightspyEffectHandler;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.CherryHouseExplorerMapItem;
import dev.ClasherHD.bodycam.item.CreepingDeathItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.LogBookItem;
import dev.ClasherHD.bodycam.item.LogPaperItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.CrossObservationSyncPacket;
import dev.ClasherHD.bodycam.network.locator.DimensionLocatorRequestPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorSelectC2SPacket;
import dev.ClasherHD.bodycam.network.logbook.LogBookActionC2SPacket;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import dev.ClasherHD.bodycam.platform.PlatformCommonHelper;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.util.ModAdvancements;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.LootEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.level.entity.trade.TradeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerEvents {
    public static final ConcurrentHashMap<UUID, LockData> POSITION_LOCKS = new ConcurrentHashMap<>();

    public static class LockData {
        public final ResourceKey<Level> originalDim;
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;
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

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> BodycamCommands.register(dispatcher));
        LifecycleEvent.SERVER_STOPPED.register(ServerEvents::onServerStopped);
        PlayerEvent.PLAYER_JOIN.register(ServerEvents::onPlayerLogin);
        PlayerEvent.PLAYER_QUIT.register(ServerEvents::onPlayerLogout);
        TickEvent.SERVER_POST.register(CreepingDeathEntity::tickServerQueues);
        TickEvent.SERVER_LEVEL_POST.register(serverLevel -> {
            if (!serverLevel.isClientSide() && serverLevel.getGameTime() % 40L == 0L) {
                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof Villager villager
                            && villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
                        clampLibrarianEnchantmentOffers(villager);
                    }
                }
            }
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && !player.level().isClientSide()) {
                onServerPlayerTick(serverPlayer);
            }
        });
        InteractionEvent.RIGHT_CLICK_BLOCK.register(ServerEvents::onRightClickBlock);
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            if (!player.level().isClientSide() && entity instanceof Villager villager) {
                clampLibrarianEnchantmentOffers(villager);
            }
            return EventResult.pass();
        });
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (!entity.level().isClientSide() && entity instanceof ServerPlayer deadPlayer) {
                UUID assassinId = CreepingDeathEntity.ACTIVE_HUNTS_BY_TARGET.remove(deadPlayer.getUUID());
                if (assassinId != null && deadPlayer.level().getServer() != null) {
                    for (ServerLevel lvl : deadPlayer.level().getServer().getAllLevels()) {
                        Entity e = lvl.getEntity(assassinId);
                        if (e instanceof CreepingDeathEntity assassin) {
                            assassin.onTargetKilled();
                            break;
                        }
                    }
                }
                CompoundTag deadData = PersistentDataAccess.get(deadPlayer);
                if (deadData.getBooleanOr("bodycam_active", false)) {
                    BodycamResetCameraPacket.executeReset(deadPlayer);
                    PacketHandler.sendToPlayer(deadPlayer, new BodycamForceClosePacket());
                }
                if (deadPlayer.level().getServer() != null) {
                    UUID deadId = deadPlayer.getUUID();
                    for (ServerPlayer online : deadPlayer.level().getServer().getPlayerList().getPlayers()) {
                        CompoundTag obsData = PersistentDataAccess.get(online);
                        if (obsData.getBooleanOr("bodycam_active", false)
                                && !obsData.getBooleanOr("bodycam_watching_watchpoint", false)
                                && PersistentDataAccess.hasUUID(obsData, "bodycam_target_uuid")
                                && deadId.equals(PersistentDataAccess.getUUID(obsData, "bodycam_target_uuid"))) {
                            BodycamResetCameraPacket.executeReset(online);
                            PacketHandler.sendToPlayer(online, new BodycamForceClosePacket());
                        }
                    }
                    PlatformCommonHelper.clearEntityChunks(deadPlayer.level().getServer(), deadId);
                }
            }
            return EventResult.pass();
        });
        PlayerEvent.CHANGE_DIMENSION.register((targetPlayer, oldLevel, newLevel) -> {
            if (!targetPlayer.level().isClientSide()) {
                UUID assassinId = CreepingDeathEntity.ACTIVE_HUNTS_BY_TARGET.get(targetPlayer.getUUID());
                if (assassinId != null && targetPlayer.level().getServer() != null) {
                    for (ServerLevel lvl : targetPlayer.level().getServer().getAllLevels()) {
                        Entity e = lvl.getEntity(assassinId);
                        if (e instanceof CreepingDeathEntity assassin && assassin.isAlive() && !assassin.isDeadOrDying()) {
                            assassin.teleportToTargetDimension(targetPlayer.level(), targetPlayer);
                            break;
                        }
                    }
                }
                if (targetPlayer.level().getServer() != null) {
                    UUID targetId = targetPlayer.getUUID();
                    for (ServerPlayer online : targetPlayer.level().getServer().getPlayerList().getPlayers()) {
                        if (online == targetPlayer) {
                            continue;
                        }
                        CompoundTag obsData = PersistentDataAccess.get(online);
                        if (obsData.getBooleanOr("bodycam_active", false)
                                && !obsData.getBooleanOr("bodycam_watching_watchpoint", false)
                                && PersistentDataAccess.hasUUID(obsData, "bodycam_target_uuid")
                                && targetId.equals(PersistentDataAccess.getUUID(obsData, "bodycam_target_uuid"))) {
                            boolean isAllSeeing = obsData.getBooleanOr("bodycam_all_seeing_active", false);
                            int reachLevel = isAllSeeing ? 2 : (ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()
                                    ? obsData.getIntOr("bodycam_reach_level", 0) : 0);
                            if (reachLevel < 2) {
                                String origDimStr = BodycamSetCameraPacket.ORIGINAL_DIM.get(online.getUUID());
                                ResourceKey<Level> dummyDim = online.level().dimension();
                                if (origDimStr != null && !origDimStr.isEmpty()) {
                                    Identifier rl = Identifier.tryParse(origDimStr);
                                    if (rl != null) {
                                        dummyDim = ResourceKey.create(Registries.DIMENSION, rl);
                                    }
                                }
                                if (dummyDim != newLevel) {
                                    BodycamResetCameraPacket.executeReset(online);
                                    PacketHandler.sendToPlayer(online, new BodycamForceClosePacket());
                                    online.sendSystemMessage(Component.translatable("message.bodycam.signal_weak").withStyle(ChatFormatting.RED));
                                }
                            }
                        }
                    }
                }
            }
        });
        LootEvent.MODIFY_LOOT_TABLE.register((id, context, builtin) -> {
            if (id.identifier().equals(Identifier.withDefaultNamespace("chests/village/village_cartographer"))) {
                context.addPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.CHERRY_HOUSE_EXPLORER_MAP.get()).setWeight(1))
                        .add(EmptyLootItem.emptyItem().setWeight(6)));
            }
        });
        TradeRegistry.registerVillagerTrade(VillagerProfession.CARTOGRAPHER, 3, (sLevel, trader, random) -> {
            if (sLevel == null) {
                return null;
            }
            ItemStack map = CherryHouseExplorerMapItem.createExplorerMap(sLevel, trader.blockPosition());
            if (map.isEmpty()) {
                return null;
            }
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, 14),
                    Optional.of(new ItemCost(Items.COMPASS, 1)),
                    map,
                    0, 12, 10, 0.2F
            );
        });
        TradeRegistry.registerTradeForWanderingTrader(TradeRegistry.WandererTradeType.RARE_TRADES, (sLevel, trader, random) -> {
            if (sLevel == null) {
                return null;
            }
            ItemStack map = CherryHouseExplorerMapItem.createExplorerMap(sLevel, trader.blockPosition());
            if (map.isEmpty()) {
                return null;
            }
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, 16),
                    Optional.of(new ItemCost(Items.COMPASS, 1)),
                    map,
                    0, 2, 10, 0.05F
            );
        });
    }

    private static InteractionResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (level.getBlockState(pos).is(Blocks.CARTOGRAPHY_TABLE)) {
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
                book = ItemStack.EMPTY;
                other = ItemStack.EMPTY;
            }
            if (!book.isEmpty() && (other.getItem() instanceof LogPaperItem || other.getItem() instanceof ShearsItem)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    LogBookItem.handleCartographyInteraction(sp, book, other);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp && !sp.isSpectator()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                CompoundTag beData = PersistentDataAccess.get(be);
                if (beData.getBooleanOr("BodycamEmptyCherryChest", false)) {
                    beData.remove("BodycamEmptyCherryChest");
                    be.setChanged();
                    ModAdvancements.award(sp, ModAdvancements.EMPTY_CHERRY_CHEST);
                }
                if (beData.getBooleanOr("BodycamUltraRareCherryChest", false)) {
                    beData.remove("BodycamUltraRareCherryChest");
                    be.setChanged();
                    ModAdvancements.award(sp, ModAdvancements.ULTRA_RARE_CHEST);
                }
            }
        }
        return InteractionResult.PASS;
    }

    private static void clampLibrarianEnchantmentOffers(Villager villager) {
        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) {
            return;
        }
        Registry<Enchantment> registry = villager.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> nightspy = registry.getOrThrow(ModEnchantments.NIGHTSPY);
        Holder<Enchantment> anonymity = registry.getOrThrow(ModEnchantments.ANONYMITY);
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            ItemStack result = offer.getResult();
            if (!result.is(Items.ENCHANTED_BOOK)) {
                continue;
            }
            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(result);
            boolean needsReplace = enchants.getLevel(nightspy) > 1 || enchants.getLevel(anonymity) > 0;
            if (needsReplace) {
                ItemStack clampedBook = EnchantmentHelper.createBook(new EnchantmentInstance(nightspy, 1));
                ItemCost costA = offer.getItemCostA();
                if (costA.count() > 19) {
                    costA = new ItemCost(costA.item(), 14, costA.components());
                }
                MerchantOffer replacement = new MerchantOffer(
                        costA,
                        offer.getItemCostB(),
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

    public static void onServerStopped(MinecraftServer server) {
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
        PlayerLocatorSelectC2SPacket.clearCooldowns();
        DimensionLocatorRequestPacket.clearCooldowns();
        LogBookActionC2SPacket.clearCooldowns();
        LogBookItem.clearStaticState();
        CreepingDeathItem.invalidateKnownPlayersCache();
        CreepingDeathEntity.clearStaticState();
        PlatformCommonHelper.clearAllForcedChunks(server);
    }

    public static void onPlayerLogin(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || player.level().getServer() == null) {
            return;
        }
        CompoundTag data = PersistentDataAccess.get(player);
        if (data.getBooleanOr("bodycam_active", false)
                || PersistentDataAccess.hasUUID(data, "bodycam_target_uuid")
                || PersistentDataAccess.hasUUID(data, "bodycam_dummy_uuid")) {

            if (data.contains("bodycam_orig_dim")) {
                String dimStr = data.getStringOr("bodycam_orig_dim", "");
                Identifier rl = Identifier.tryParse(dimStr);
                if (rl != null) {
                    ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, rl);
                    ServerLevel origLvl = player.level().getServer().getLevel(dimKey);
                    if (origLvl != null) {
                        double ox = data.getDoubleOr("bodycam_orig_x", 0.0D);
                        double oy = data.getDoubleOr("bodycam_orig_y", 0.0D);
                        double oz = data.getDoubleOr("bodycam_orig_z", 0.0D);
                        float oyrot = data.getFloatOr("bodycam_orig_yrot", 0.0F);
                        float oxrot = data.getFloatOr("bodycam_orig_xrot", 0.0F);
                        player.teleportTo(origLvl, ox, oy, oz, Set.of(), oyrot, oxrot, true);
                        player.setOldPosAndRot();
                    }
                }
            }

            if (PersistentDataAccess.hasUUID(data, "bodycam_dummy_uuid")) {
                UUID dummyId = PersistentDataAccess.getUUID(data, "bodycam_dummy_uuid");
                for (ServerLevel lvl : player.level().getServer().getAllLevels()) {
                    Entity e = lvl.getEntity(dummyId);
                    if (e != null) {
                        if (!data.contains("bodycam_orig_dim")) {
                            player.teleportTo(lvl, e.getX(), e.getY(), e.getZ(), Set.of(), e.getYRot(), e.getXRot(), true);
                            player.setOldPosAndRot();
                        }
                        e.discard();
                        break;
                    }
                }
            }

            if (PersistentDataAccess.hasUUID(data, "bodycam_target_uuid")) {
                UUID oldTargetId = PersistentDataAccess.getUUID(data, "bodycam_target_uuid");
                if (oldTargetId != null) {
                    ServerPlayer oldTarget = player.level().getServer().getPlayerList().getPlayer(oldTargetId);
                    if (oldTarget != null) {
                        PacketHandler.sendToPlayer(oldTarget, new CrossObservationSyncPacket(player.getUUID(), false));
                    }
                }
            }

            if (data.contains("bodycam_original_gamemode")) {
                int gameModeId = data.getIntOr("bodycam_original_gamemode", 0);
                player.setGameMode(GameType.byId(gameModeId));
            }
            player.setInvisible(false);
            BodycamResetCameraPacket.cleanupPlayerNbtAndMaps(player);
            PlatformCommonHelper.clearEntityChunks(player.level().getServer(), player.getUUID());
        } else if (data.getBooleanOr("bodycam_all_seeing_active", false) || data.getBooleanOr("bodycam_all_seeing_pending", false)) {
            data.remove("bodycam_all_seeing_active");
            data.remove("bodycam_all_seeing_pending");
        }
    }

    public static void onPlayerLogout(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || player.level().getServer() == null) {
            return;
        }
        UUID id = player.getUUID();
        PlayerLocatorSelectC2SPacket.clearCooldown(id);
        DimensionLocatorRequestPacket.clearCooldown(id);
        LogBookActionC2SPacket.clearCooldown(id);
        POSITION_LOCKS.remove(id);

        CompoundTag data = PersistentDataAccess.get(player);
        if (data.getBooleanOr("bodycam_active", false) || PersistentDataAccess.hasUUID(data, "bodycam_target_uuid") || PersistentDataAccess.hasUUID(data, "bodycam_dummy_uuid")) {
            BodycamResetCameraPacket.executeReset(player);
        }

        BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(id);
        BodycamSetCameraPacket.ORIGINAL_DIM.remove(id);
        BodycamSetCameraPacket.ORIGINAL_POS.remove(id);
        BodycamSetCameraPacket.ORIGINAL_ROT.remove(id);
        BodycamDummyEntity.DUMMY_POS.remove(id);
        BodycamDummyEntity.DUMMY_FALL.remove(id);
        BodycamDummyEntity.DUMMY_MOTION.remove(id);
        PlatformCommonHelper.clearEntityChunks(player.level().getServer(), id);
        data.remove("bodycam_all_seeing_active");
        data.remove("bodycam_all_seeing_pending");
    }

    private static ServerLevel resolveFogLevel(ServerPlayer observer, String dimStr) {
        if (dimStr != null && !dimStr.isEmpty() && observer.level().getServer() != null) {
            Identifier rl = Identifier.tryParse(dimStr);
            if (rl != null) {
                ServerLevel found = observer.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, rl));
                if (found != null) {
                    return found;
                }
            }
        }
        return observer.level();
    }

    public static void onServerPlayerTick(ServerPlayer observer) {
        if (observer.tickCount % 20 == 0 && ItemDurabilityHelper.hasFullObservationCrystalTrimSet(observer)) {
            ModAdvancements.award(observer, ModAdvancements.CRYSTAL_TRIM_SET);
        }

        ItemStack carried = observer.containerMenu.getCarried();
        CompoundTag data = PersistentDataAccess.get(observer);

        int allSeeingFogTicks = data.getIntOr("bodycam_all_seeing_fog_ticks", 0);
        if (allSeeingFogTicks > 0) {
            data.putInt("bodycam_all_seeing_fog_ticks", allSeeingFogTicks - 1);
            ServerLevel fogLevel = resolveFogLevel(observer, data.getStringOr("bodycam_fog_dim", ""));
            double fx = data.getDoubleOr("bodycam_fog_x", 0.0D);
            double fy = data.getDoubleOr("bodycam_fog_y", 0.0D);
            double fz = data.getDoubleOr("bodycam_fog_z", 0.0D);
            fogLevel.sendParticles(ParticleTypes.END_ROD, fx, fy + 1.0D, fz, 25, 0.85D, 0.85D, 0.85D, 0.03D);
        }

        int cdFogTicks = data.getIntOr("bodycam_creeping_death_fog_ticks", 0);
        if (cdFogTicks > 0) {
            data.putInt("bodycam_creeping_death_fog_ticks", cdFogTicks - 1);
            ServerLevel fogLevel = resolveFogLevel(observer, data.getStringOr("bodycam_cd_fog_dim", ""));
            double fx = data.getDoubleOr("bodycam_cd_fog_x", 0.0D);
            double fy = data.getDoubleOr("bodycam_cd_fog_y", 0.0D);
            double fz = data.getDoubleOr("bodycam_cd_fog_z", 0.0D);
            fogLevel.sendParticles(ParticleTypes.LARGE_SMOKE, fx, fy + 1.0D, fz, 18, 0.75D, 0.85D, 0.75D, 0.02D);
            fogLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, fx, fy + 1.0D, fz, 8, 0.65D, 0.75D, 0.65D, 0.01D);
            fogLevel.sendParticles(ParticleTypes.SCULK_SOUL, fx, fy + 1.0D, fz, 12, 0.75D, 0.85D, 0.75D, 0.03D);
        }

        if (PersistentDataAccess.hasUUID(data, "bodycam_active_jammer_id")) {
            boolean hasJammer = (carried.getItem() instanceof JammerItem && !ItemDurabilityHelper.isEmpty(carried));
            if (!hasJammer) {
                for (ItemStack accStack : BodycamPlatform.getEquippedAccessoryItems(observer, ModItems.JAMMER.get())) {
                    if (!ItemDurabilityHelper.isEmpty(accStack)) {
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
                data.putInt("bodycam_jammer_missing_ticks", 0);
            } else {
                data.remove("bodycam_active_jammer_id");
                data.remove("bodycam_jammer_mode");
                data.remove("bodycam_jammer_missing_ticks");
            }
        }

        if (PersistentDataAccess.hasUUID(data, "bodycam_active_anonymizer_id")) {
            boolean hasAnonymizer = (carried.getItem() instanceof AnonymizerItem && !ItemDurabilityHelper.isEmpty(carried));
            if (!hasAnonymizer) {
                for (ItemStack accStack : BodycamPlatform.getEquippedAccessoryItems(observer, ModItems.ANONYMIZER.get())) {
                    if (!ItemDurabilityHelper.isEmpty(accStack)) {
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
                data.putInt("bodycam_anonymizer_missing_ticks", 0);
            } else {
                data.remove("bodycam_active_anonymizer_id");
                data.remove("bodycam_anonymizer_missing_ticks");
            }
        }

        if (!carried.isEmpty() && (carried.getItem() instanceof JammerItem || carried.getItem() instanceof AnonymizerItem)) {
            carried.getItem().inventoryTick(carried, observer.level(), observer, null);
        }
        for (ItemStack accStack : BodycamPlatform.getEquippedAccessoryItems(observer, ModItems.JAMMER.get())) {
            accStack.getItem().inventoryTick(accStack, observer.level(), observer, null);
        }
        for (ItemStack accStack : BodycamPlatform.getEquippedAccessoryItems(observer, ModItems.ANONYMIZER.get())) {
            accStack.getItem().inventoryTick(accStack, observer.level(), observer, null);
        }

        LockData lockData = POSITION_LOCKS.get(observer.getUUID());
        if (lockData != null) {
            if (observer.level().dimension() == lockData.originalDim) {
                observer.teleportTo(observer.level(), lockData.x, lockData.y, lockData.z, Set.of(), lockData.yaw, lockData.pitch, true);
                observer.hurtMarked = true;
                lockData.lockTicks--;
                if (lockData.lockTicks <= 0) {
                    POSITION_LOCKS.remove(observer.getUUID());
                }
            } else {
                POSITION_LOCKS.remove(observer.getUUID());
            }
        }

        if (data.getBooleanOr("bodycam_active", false) && PersistentDataAccess.hasUUID(data, "bodycam_target_uuid") && observer.level().getServer() != null) {
            UUID targetId = PersistentDataAccess.getUUID(data, "bodycam_target_uuid");

            BodycamDummyEntity dummy = null;
            if (PersistentDataAccess.hasUUID(data, "bodycam_dummy_uuid")) {
                UUID dummyId = PersistentDataAccess.getUUID(data, "bodycam_dummy_uuid");
                for (ServerLevel lvl : observer.level().getServer().getAllLevels()) {
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
                    Identifier rl = Identifier.tryParse(dimStr);
                    if (rl != null) {
                        dummyDim = ResourceKey.create(Registries.DIMENSION, rl);
                    }
                }
            }
            if (dummyPosVal == null) {
                dummyPosVal = BodycamSetCameraPacket.ORIGINAL_POS.get(observer.getUUID());
            }
            if (dummyDim == null) {
                dummyDim = observer.level().dimension();
            }

            boolean isAllSeeing = data.getBooleanOr("bodycam_all_seeing_active", false);

            if (data.getBooleanOr("bodycam_watching_watchpoint", false)) {
                WatchPointDummyEntity wp = WatchPointDummyEntity.findByUUID(observer.level().getServer(), targetId);
                if (wp == null || !wp.isAlive() || wp.isRemoved() || !(wp.level() instanceof ServerLevel wpLevel)) {
                    BodycamResetCameraPacket.executeReset(observer);
                    return;
                }
                int reachLevel = isAllSeeing ? 2 : (ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()
                        ? data.getIntOr("bodycam_reach_level", 0) : 0);
                if (reachLevel < 2) {
                    boolean outOfRange = dummyDim != wp.level().dimension();
                    if (!outOfRange && reachLevel < 1 && dummyPosVal != null) {
                        double maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
                        outOfRange = dummyPosVal.distanceToSqr(wp.position()) > maxDist * maxDist;
                    }
                    if (outOfRange) {
                        BodycamResetCameraPacket.executeReset(observer);
                        PacketHandler.sendToPlayer(observer, new BodycamForceClosePacket());
                        observer.sendSystemMessage(Component.translatable("message.bodycam.signal_weak").withStyle(ChatFormatting.RED));
                        return;
                    }
                }
                NightspyEffectHandler.tick(observer, wp, dummyPosVal, dummyDim);
                if (observer.level().dimension() == wp.level().dimension()) {
                    observer.setPos(wp.getX(), wp.getY(), wp.getZ());
                } else {
                    observer.teleportTo(wpLevel, wp.getX(), wp.getY(), wp.getZ(), Set.of(), observer.getYRot(), observer.getXRot(), true);
                    observer.setOldPosAndRot();
                }
                observer.setCamera(observer);
                return;
            }

            ServerPlayer target = observer.level().getServer().getPlayerList().getPlayer(targetId);
            if (target != null && target.isAlive() && !target.isRemoved()) {
                data.putInt("bodycam_disconnect_ticks", 0);

                if (!isAllSeeing && EnchantmentLevels.isHiddenFromSelection(target)) {
                    BodycamResetCameraPacket.executeReset(observer);
                    PacketHandler.sendToPlayer(observer, new BodycamForceClosePacket());
                    return;
                }

                if (!isAllSeeing) {
                    CompoundTag targetData = PersistentDataAccess.get(target);
                    long lastJammer = targetData.getLongOr("bodycam_jammer_heartbeat", 0L);
                    int currentJammerMode = targetData.getIntOr("bodycam_jammer_mode", 0);
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
                            PacketHandler.sendToPlayer(observer, new BodycamForceClosePacket());
                            observer.sendSystemMessage(Component.translatable("message.bodycam.jammer_blocked").withStyle(ChatFormatting.RED));
                            return;
                        }
                    } else if (currentJammerMode == 1) {
                        BodycamResetCameraPacket.executeReset(observer);
                        PacketHandler.sendToPlayer(observer, new BodycamForceClosePacket());
                        observer.sendSystemMessage(Component.translatable("message.bodycam.jammer_blocked").withStyle(ChatFormatting.RED));
                        return;
                    }

                    int reachLevel = ModServerConfig.ENABLE_REACH_ENCHANTMENT.get() ? data.getIntOr("bodycam_reach_level", 0) : 0;
                    if (reachLevel < 2) {
                        boolean outOfRange2 = dummyDim != target.level().dimension();
                        if (!outOfRange2 && reachLevel < 1 && dummyPosVal != null) {
                            double maxDist2 = ModServerConfig.MAX_MONITOR_DISTANCE.get();
                            outOfRange2 = dummyPosVal.distanceToSqr(target.position()) > maxDist2 * maxDist2;
                        }
                        if (outOfRange2) {
                            BodycamResetCameraPacket.executeReset(observer);
                            PacketHandler.sendToPlayer(observer, new BodycamForceClosePacket());
                            observer.sendSystemMessage(Component.translatable("message.bodycam.signal_weak").withStyle(ChatFormatting.RED));
                            return;
                        }
                    }
                }

                NightspyEffectHandler.tick(observer, target, dummyPosVal, dummyDim);
                if (observer.level().dimension() == target.level().dimension()) {
                    observer.setPos(target.getX(), target.getY(), target.getZ());
                } else {
                    observer.teleportTo(target.level(), target.getX(), target.getY(), target.getZ(), Set.of(), target.getYRot(), target.getXRot(), true);
                    observer.setOldPosAndRot();
                }
                observer.setCamera(target);
            } else {
                int ticks = data.getIntOr("bodycam_disconnect_ticks", 0) + 1;
                data.putInt("bodycam_disconnect_ticks", ticks);
                if (ticks >= 40) {
                    BodycamResetCameraPacket.executeReset(observer);
                }
            }
        }
    }
}
