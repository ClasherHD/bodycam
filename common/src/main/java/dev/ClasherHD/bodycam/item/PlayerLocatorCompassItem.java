package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorStructureUpdateS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorSyncS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorTargetUpdateS2CPacket;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("null")
public class PlayerLocatorCompassItem extends Item {

    public PlayerLocatorCompassItem(Properties properties) {
        super(properties);
    }

    private static void syncConfiguredMaxDamage(ItemStack stack) {
        if (!ModServerConfig.isLoaded()) {
            return;
        }
        int configured = Math.max(1, ItemDurabilityHelper.getPlayerLocatorMaxDurability());
        Integer current = stack.get(DataComponents.MAX_DAMAGE);
        if (current == null || current != configured) {
            stack.set(DataComponents.MAX_DAMAGE, configured);
            if (stack.getDamageValue() > configured) {
                stack.setDamageValue(configured);
            }
        }
    }

    public static boolean isTracking(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag == null) {
            return false;
        }
        int state = tag.getInt("LocatorState");
        return state == 1 || state == 3 || tag.getBoolean("Tracking");
    }

    public static int getTargetX(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag == null) {
            return 0;
        }
        if (tag.contains("LocatorCachedStructX")) {
            return tag.getInt("LocatorCachedStructX");
        }
        return tag.getInt("TargetX");
    }

    public static int getTargetZ(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag == null) {
            return 0;
        }
        if (tag.contains("LocatorCachedStructZ")) {
            return tag.getInt("LocatorCachedStructZ");
        }
        return tag.getInt("TargetZ");
    }

    public static void handleSelection(ServerPlayer player, UUID targetUuid, boolean deselect) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof PlayerLocatorCompassItem)) {
            stack = player.getOffhandItem();
            if (!(stack.getItem() instanceof PlayerLocatorCompassItem)) {
                return;
            }
        }
        PlayerLocatorCompassItem compass = (PlayerLocatorCompassItem) stack.getItem();
        if (deselect || targetUuid == null) {
            compass.resetToIdle(stack);
            return;
        }
        ServerPlayer targetPlayer = player.server.getPlayerList().getPlayer(targetUuid);
        if (targetPlayer == null || EnchantmentLevels.isHiddenFromSelection(targetPlayer)) {
            compass.resetToIdle(stack);
            return;
        }
        ItemNbtAccess.putUUID(stack, "LocatorTargetUUID", targetUuid);
        int reachLevel = EnchantmentLevels.reach(stack);
        compass.calculateState(stack, player, targetPlayer, reachLevel, player.level().getGameTime());
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (!level.isClientSide()) {
            syncConfiguredMaxDamage(stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            syncConfiguredMaxDamage(stack);
        }
        if (ItemDurabilityHelper.isEmpty(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sPlayer) {
            if (ModServerConfig.OP_ONLY_MODE.get() && !sPlayer.hasPermissions(2)) {
                sPlayer.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (ModServerConfig.isDimensionBlacklisted(level)) {
                sPlayer.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (!ModServerConfig.ENABLE_PLAYER_LOCATOR.get()) {
                sPlayer.sendSystemMessage(Component.translatable("message.bodycam.player_locator_disabled").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }

            level.playSound(null, sPlayer.getX(), sPlayer.getY(), sPlayer.getZ(),
                    ModSounds.PLAYER_LOCATOR_USED.get(), SoundSource.PLAYERS, 0.25F, 1.0F);

            Map<UUID, Integer> jammers = new HashMap<>();
            Map<UUID, String> dimensions = new HashMap<>();
            Map<UUID, BlockPos> positions = new HashMap<>();

            int reachLevel = EnchantmentLevels.reach(stack);
            String myDim = sPlayer.level().dimension().location().toString();
            double maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
            double maxDistSq = maxDist * maxDist;

            for (ServerPlayer p : sPlayer.server.getPlayerList().getPlayers()) {
                if (!p.getUUID().equals(sPlayer.getUUID()) && !EnchantmentLevels.isHiddenFromSelection(p)) {
                    int jammerMode = getJammerMode(p);
                    String targetDim = getTargetDimension(p);
                    BlockPos targetPos = getTargetPosition(p);
                    boolean sameDim = myDim.equals(targetDim);
                    double distSq = sPlayer.blockPosition().distSqr(targetPos);
                    boolean withinBaseDist = sameDim && (distSq <= maxDistSq);
                    boolean isJammed = (jammerMode == 1) || (jammerMode == 2 && !withinBaseDist);

                    boolean inReach;
                    if (reachLevel >= 2) {
                        inReach = true;
                    } else if (reachLevel >= 1) {
                        inReach = sameDim;
                    } else {
                        inReach = withinBaseDist;
                    }

                    jammers.put(p.getUUID(), jammerMode);
                    if (isJammed || EnchantmentLevels.isDimensionScrambled(p)) {
                        dimensions.put(p.getUUID(), myDim);
                    } else {
                        dimensions.put(p.getUUID(), targetDim);
                    }
                    if (!isJammed && inReach) {
                        positions.put(p.getUUID(), withinBaseDist ? sPlayer.blockPosition() : new BlockPos(30_000_000, 0, 30_000_000));
                    }
                }
            }

            CompoundTag readTag = ItemNbtAccess.getReadOnlyTag(stack);
            UUID currentTarget = (readTag != null && readTag.hasUUID("LocatorTargetUUID"))
                    ? readTag.getUUID("LocatorTargetUUID")
                    : null;

            PacketHandler.sendToPlayer(
                    sPlayer,
                    new PlayerLocatorSyncS2CPacket(jammers, dimensions, positions, currentTarget, reachLevel)
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer owner)) {
            return;
        }
        syncConfiguredMaxDamage(stack);

        if (ItemDurabilityHelper.isEmpty(stack)) {
            CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
            if (tag != null && (tag.hasUUID("LocatorTargetUUID") || tag.getInt("LocatorState") != 0)) {
                resetToIdle(stack);
            }
            return;
        }

        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag == null || !tag.hasUUID("LocatorTargetUUID")) {
            if (tag != null && tag.getInt("LocatorState") != 0) {
                ItemNbtAccess.putInt(stack, "LocatorState", 0);
            }
            return;
        }

        UUID targetUUID = tag.getUUID("LocatorTargetUUID");
        ServerPlayer targetPlayer = owner.server.getPlayerList().getPlayer(targetUUID);

        if (targetPlayer == null || EnchantmentLevels.isHiddenFromSelection(targetPlayer)) {
            resetToIdle(stack);
            return;
        }

        long currentTick = owner.level().getGameTime();
        int durabilityInterval = Math.max(1, ModServerConfig.PLAYER_LOCATOR_DURABILITY_INTERVAL_TICKS.get());
        if (!owner.isCreative() && currentTick % durabilityInterval == 0) {
            ItemDurabilityHelper.damagePerTime(stack, owner, 1);
            if (ItemDurabilityHelper.isEmpty(stack)) {
                resetToIdle(stack);
                return;
            }
        }

        int currentState = tag.getInt("LocatorState");
        if (currentState == 2 || currentState == 4) {
            long timestamp = tag.getLong("LocatorStateTimestamp");
            if (currentTick - timestamp >= 200) {
                resetToIdle(stack);
                return;
            }
        }

        if (currentState == 1) {
            if (currentTick % 20 == 0) {
                PacketHandler.sendToPlayer(
                        owner,
                        new PlayerLocatorTargetUpdateS2CPacket(targetUUID, getTargetPosition(targetPlayer), getTargetDimension(targetPlayer))
                );
            }
        }

        int reachLevel = EnchantmentLevels.reach(stack);
        long lastCheck = tag.getLong("LocatorLastCheckTick");
        String lastTargetDim = tag.getString("LocatorLastTargetDim");
        String myLastDim = tag.getString("LocatorLastMyDim");

        String currentTargetDim = getTargetDimension(targetPlayer);
        String currentMyDim = owner.level().dimension().location().toString();

        int scanInterval = Math.max(1, ModServerConfig.LOCATOR_SCAN_INTERVAL_TICKS.get());
        boolean dimChanged = !currentTargetDim.equals(lastTargetDim) || !currentMyDim.equals(myLastDim);
        boolean periodic = (currentTick - lastCheck >= scanInterval);

        if (dimChanged || periodic || !tag.contains("LocatorState")) {
            calculateState(stack, owner, targetPlayer, reachLevel, currentTick);
        }
    }

    public void calculateState(ItemStack stack, ServerPlayer owner, ServerPlayer targetPlayer, int reachLevel, long currentTick) {
        ItemNbtAccess.updateTag(stack, tag -> {
            tag.putLong("LocatorLastCheckTick", currentTick);

            String currentTargetDim = getTargetDimension(targetPlayer);
            String currentMyDim = owner.level().dimension().location().toString();

            boolean dimChanged = !currentTargetDim.equals(tag.getString("LocatorLastTargetDim"))
                    || !currentMyDim.equals(tag.getString("LocatorLastMyDim"));

            if (dimChanged) {
                tag.remove("LocatorStructSearchFailedTick");
            }

            tag.putString("LocatorLastTargetDim", currentTargetDim);
            tag.putString("LocatorLastMyDim", currentMyDim);

            int jammerMode = getJammerMode(targetPlayer);
            double distSq = owner.blockPosition().distSqr(getTargetPosition(targetPlayer));
            double maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
            double maxDistSq = maxDist * maxDist;
            boolean sameDim = currentMyDim.equals(currentTargetDim);

            boolean isJammed = (jammerMode == 1) || (jammerMode == 2 && (!sameDim || distSq > maxDistSq));

            ResourceKey<Level> targetDimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(currentTargetDim));

            int newState;
            if (isJammed) {
                newState = 4;
            } else if (sameDim) {
                if (distSq <= maxDistSq || reachLevel >= 1) {
                    newState = 1;
                } else {
                    newState = 2;
                }
            } else {
                if (reachLevel >= 2 && isVanillaDim(owner.level().dimension()) && isVanillaDim(targetDimKey)) {
                    newState = 3;
                } else {
                    newState = 2;
                }
            }

            int previousState = tag.contains("LocatorState") ? tag.getInt("LocatorState") : -1;
            boolean stateOrDimChanged = (newState != previousState) || dimChanged;

            if (newState == 3) {
                updateCrossDimensionRouting(tag, owner, targetDimKey, currentMyDim, currentTick, stateOrDimChanged);
            } else if (stateOrDimChanged) {
                tag.putInt("LocatorState", newState);
                tag.putLong("LocatorStateTimestamp", currentTick);
                clearRoutingCache(tag);

                if (newState == 1) {
                    PacketHandler.sendToPlayer(
                            owner,
                            new PlayerLocatorTargetUpdateS2CPacket(targetPlayer.getUUID(), getTargetPosition(targetPlayer), currentTargetDim)
                    );
                }
            }
        });
    }

    private void updateCrossDimensionRouting(CompoundTag tag, ServerPlayer owner, ResourceKey<Level> targetDim,
                                             String currentMyDim, long currentTick, boolean forceRescan) {
        ServerLevel level = owner.serverLevel();
        Block portalBlock = getRequiredPortalBlock(level.dimension(), targetDim);
        int chunkRadius = getEffectiveChunkRadius(owner);

        if (!forceRescan && isCachedPortalValid(tag, level, owner, portalBlock, chunkRadius)) {
            return;
        }

        BlockPos activePortal = findNearestActivePortal(level, owner, portalBlock, chunkRadius);
        if (activePortal != null) {
            applyRoutingTarget(tag, owner, activePortal, currentMyDim, currentTick, true);
            return;
        }

        clearPortalBlockCache(tag);

        if (!forceRescan && tag.contains("LocatorCachedStructX") && tag.contains("LocatorCachedStructZ")) {
            return;
        }

        if (tag.contains("LocatorStructSearchFailedTick") && (currentTick - tag.getLong("LocatorStructSearchFailedTick") < 1200)) {
            tag.putInt("LocatorState", 2);
            return;
        }

        BlockPos structPos = findRoutingStructure(owner, targetDim);
        if (structPos != null) {
            tag.remove("LocatorStructSearchFailedTick");
            applyRoutingTarget(tag, owner, structPos, currentMyDim, currentTick, false);
        } else {
            clearRoutingCache(tag);
            tag.putLong("LocatorStructSearchFailedTick", currentTick);
            tag.putInt("LocatorState", 2);
            tag.putLong("LocatorStateTimestamp", currentTick);
        }
    }

    private void applyRoutingTarget(CompoundTag tag, ServerPlayer owner, BlockPos targetPos, String currentMyDim,
                                    long currentTick, boolean isActivePortal) {
        boolean posChanged = !tag.contains("LocatorCachedStructX")
                || tag.getInt("LocatorCachedStructX") != targetPos.getX()
                || tag.getInt("LocatorCachedStructZ") != targetPos.getZ()
                || (isActivePortal && (!tag.contains("LocatorCachedPortalY") || tag.getInt("LocatorCachedPortalY") != targetPos.getY()));
        boolean stateChanged = tag.getInt("LocatorState") != 3;

        tag.putInt("LocatorState", 3);
        if (stateChanged) {
            tag.putLong("LocatorStateTimestamp", currentTick);
        }

        tag.putInt("LocatorCachedStructX", targetPos.getX());
        tag.putInt("LocatorCachedStructZ", targetPos.getZ());
        tag.putBoolean("LocatorCachedIsPortal", isActivePortal);
        if (isActivePortal) {
            tag.putInt("LocatorCachedPortalY", targetPos.getY());
        } else {
            tag.remove("LocatorCachedPortalY");
        }

        if (stateChanged || posChanged) {
            PacketHandler.sendToPlayer(
                    owner,
                    new PlayerLocatorStructureUpdateS2CPacket(targetPos, currentMyDim)
            );
        }
    }

    private boolean isCachedPortalValid(CompoundTag tag, ServerLevel level, ServerPlayer owner, Block portalBlock, int chunkRadius) {
        if (portalBlock == null || !tag.getBoolean("LocatorCachedIsPortal")) {
            return false;
        }
        if (!tag.contains("LocatorCachedStructX") || !tag.contains("LocatorCachedPortalY") || !tag.contains("LocatorCachedStructZ")) {
            return false;
        }

        int x = tag.getInt("LocatorCachedStructX");
        int y = tag.getInt("LocatorCachedPortalY");
        int z = tag.getInt("LocatorCachedStructZ");

        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        int playerChunkX = SectionPos.blockToSectionCoord(owner.getBlockX());
        int playerChunkZ = SectionPos.blockToSectionCoord(owner.getBlockZ());

        if (Math.abs(chunkX - playerChunkX) > chunkRadius || Math.abs(chunkZ - playerChunkZ) > chunkRadius) {
            return false;
        }

        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) {
            return false;
        }

        return chunk.getBlockState(new BlockPos(x, y, z)).is(portalBlock);
    }

    private Block getRequiredPortalBlock(ResourceKey<Level> myDim, ResourceKey<Level> targetDim) {
        if (myDim == Level.OVERWORLD) {
            if (targetDim == Level.NETHER) {
                return Blocks.NETHER_PORTAL;
            }
            if (targetDim == Level.END) {
                return Blocks.END_PORTAL;
            }
        } else if (myDim == Level.NETHER) {
            if (targetDim == Level.OVERWORLD || targetDim == Level.END) {
                return Blocks.NETHER_PORTAL;
            }
        } else if (myDim == Level.END) {
            if (targetDim == Level.OVERWORLD || targetDim == Level.NETHER) {
                return Blocks.END_PORTAL;
            }
        }
        return null;
    }

    private int getEffectiveChunkRadius(ServerPlayer owner) {
        int configRadius = ModServerConfig.PORTAL_SEARCH_CHUNK_RADIUS.get();
        int viewDist = owner.server.getPlayerList().getViewDistance();
        int simDist = owner.server.getPlayerList().getSimulationDistance();
        int serverDist = Math.min(viewDist, simDist);
        return Math.max(1, Math.min(configRadius, serverDist));
    }

    private BlockPos findNearestActivePortal(ServerLevel level, ServerPlayer owner, Block targetPortalBlock, int chunkRadius) {
        if (targetPortalBlock == null) {
            return null;
        }

        ServerChunkCache chunkSource = level.getChunkSource();
        BlockPos playerPos = owner.blockPosition();
        int centerChunkX = SectionPos.blockToSectionCoord(playerPos.getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(playerPos.getZ());

        BlockPos nearestPos = null;
        double nearestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                LevelChunk chunk = chunkSource.getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }

                LevelChunkSection[] sections = chunk.getSections();
                int minSection = chunk.getMinSection();
                int chunkMinX = SectionPos.sectionToBlockCoord(cx);
                int chunkMinZ = SectionPos.sectionToBlockCoord(cz);

                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }
                    if (!section.maybeHas(state -> state.is(targetPortalBlock))) {
                        continue;
                    }

                    int sectionMinY = SectionPos.sectionToBlockCoord(minSection + i);
                    for (int lx = 0; lx < 16; lx++) {
                        for (int ly = 0; ly < 16; ly++) {
                            for (int lz = 0; lz < 16; lz++) {
                                if (section.getBlockState(lx, ly, lz).is(targetPortalBlock)) {
                                    int bx = chunkMinX + lx;
                                    int by = sectionMinY + ly;
                                    int bz = chunkMinZ + lz;
                                    mutablePos.set(bx, by, bz);
                                    double distSq = playerPos.distSqr(mutablePos);
                                    if (distSq < nearestDistSq) {
                                        nearestDistSq = distSq;
                                        nearestPos = mutablePos.immutable();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return nearestPos;
    }

    private void clearPortalBlockCache(CompoundTag tag) {
        if (tag.getBoolean("LocatorCachedIsPortal")) {
            tag.remove("LocatorCachedStructX");
            tag.remove("LocatorCachedStructZ");
            tag.remove("LocatorCachedPortalY");
            tag.remove("LocatorCachedIsPortal");
        }
    }

    private void clearRoutingCache(CompoundTag tag) {
        tag.remove("LocatorCachedStructX");
        tag.remove("LocatorCachedStructZ");
        tag.remove("LocatorCachedPortalY");
        tag.remove("LocatorCachedIsPortal");
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged() && !ItemDurabilityHelper.isEmpty(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    public void resetToIdle(ItemStack stack) {
        ItemNbtAccess.updateTag(stack, this::resetToIdle);
    }

    public void resetToIdle(CompoundTag tag) {
        tag.remove("LocatorTargetUUID");
        tag.remove("LocatorTargetUUIDMost");
        tag.remove("LocatorTargetUUIDLeast");
        tag.putInt("LocatorState", 0);
        tag.remove("LocatorStateTimestamp");
        clearRoutingCache(tag);
        tag.remove("LocatorLastTargetDim");
        tag.remove("LocatorLastMyDim");
        tag.remove("LocatorLastCheckTick");
        tag.remove("LocatorStructSearchFailedTick");
    }

    private int getJammerMode(ServerPlayer player) {
        int personalMode = 0;
        CompoundTag pData = PersistentDataAccess.get(player);
        if (pData.hasUUID("bodycam_active_jammer_id")) {
            long lastHeartbeat = pData.getLong("bodycam_jammer_heartbeat");
            boolean isCreative = player.isCreative();
            if (isCreative || (player.level().getGameTime() - lastHeartbeat <= 10)) {
                personalMode = pData.getInt("bodycam_jammer_mode");
            }
        }
        return PodestBlockEntity.combineJammerModes(personalMode, PodestBlockEntity.getPodestJammerMode(player));
    }

    private boolean isVanillaDim(ResourceKey<Level> dim) {
        return dim == Level.OVERWORLD || dim == Level.NETHER || dim == Level.END;
    }

    private BlockPos findRoutingStructure(ServerPlayer owner, ResourceKey<Level> targetDim) {
        ServerLevel level = owner.serverLevel();
        ResourceKey<Level> myDim = level.dimension();
        BlockPos playerPos = owner.blockPosition();

        if (myDim == Level.END) {
            return new BlockPos(0, 64, 0);
        } else if (myDim == Level.NETHER) {
            return level.findNearestMapStructure(StructureTags.RUINED_PORTAL, playerPos, 100, false);
        } else if (myDim == Level.OVERWORLD) {
            if (targetDim == Level.NETHER) {
                return level.findNearestMapStructure(StructureTags.RUINED_PORTAL, playerPos, 100, false);
            } else if (targetDim == Level.END) {
                return level.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, playerPos, 100, false);
            }
        }
        return null;
    }

    private BlockPos getTargetPosition(ServerPlayer p) {
        CompoundTag pData = PersistentDataAccess.get(p);
        if (pData.getBoolean("bodycam_active")) {
            Vec3 dummyVec = BodycamDummyEntity.DUMMY_POS.get(p.getUUID());
            if (dummyVec != null) {
                return BlockPos.containing(dummyVec);
            }
            Vec3 origVec = BodycamSetCameraPacket.ORIGINAL_POS.get(p.getUUID());
            if (origVec != null) {
                return BlockPos.containing(origVec);
            }
            if (pData.contains("bodycam_orig_x")) {
                return BlockPos.containing(
                        pData.getDouble("bodycam_orig_x"),
                        pData.getDouble("bodycam_orig_y"),
                        pData.getDouble("bodycam_orig_z")
                );
            }
        }
        return p.blockPosition();
    }

    private String getTargetDimension(ServerPlayer p) {
        CompoundTag pData = PersistentDataAccess.get(p);
        if (pData.getBoolean("bodycam_active")) {
            String origDim = BodycamSetCameraPacket.ORIGINAL_DIM.get(p.getUUID());
            if (origDim != null) {
                return origDim;
            }
            if (pData.contains("bodycam_orig_dim")) {
                return pData.getString("bodycam_orig_dim");
            }
            if (pData.contains("bodycam_original_dimension")) {
                return pData.getString("bodycam_original_dimension");
            }
        }
        return p.level().dimension().location().toString();
    }
}
