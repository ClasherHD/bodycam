package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.util.ModAdvancements;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SuppressWarnings("null")
public class CreepingDeathEntity extends PathfinderMob {

    public static final EntityDataAccessor<Optional<UUID>> SKIN_UUID = SynchedEntityData.defineId(
            CreepingDeathEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<String> SKIN_NAME = SynchedEntityData.defineId(
            CreepingDeathEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Optional<UUID>> TARGET_UUID = SynchedEntityData.defineId(
            CreepingDeathEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<String> SCHOLAR_SKIN = SynchedEntityData.defineId(
            CreepingDeathEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> ULTRA_RARE_CURSE = SynchedEntityData.defineId(
            CreepingDeathEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int MAX_LIFETIME_TICKS = 20 * 60 * 5;
    public static final int SPAWN_DELAY_TICKS = 20 * 10;
    public static final int OSC_NUKE_DELAY_TICKS = 50;

    public static final Map<UUID, UUID> ACTIVE_HUNTS_BY_TARGET = new ConcurrentHashMap<>();
    public static final List<PendingSpawn> PENDING_SPAWNS = new ArrayList<>();
    public static final List<PendingOscNuke> PENDING_OSC_NUKES = new ArrayList<>();

    public static class PendingSpawn {
        public final UUID targetUUID;
        public final UUID skinUUID;
        public final String skinName;
        public final String scholarSkin;
        public final boolean ultraRareCurse;
        public int ticksRemaining;
        public int retryTicks;

        public PendingSpawn(UUID targetUUID, UUID skinUUID, String skinName, int ticksRemaining) {
            this(targetUUID, skinUUID, skinName, "", false, ticksRemaining);
        }

        public PendingSpawn(UUID targetUUID, UUID skinUUID, String skinName, String scholarSkin, boolean ultraRareCurse, int ticksRemaining) {
            this.targetUUID = targetUUID;
            this.skinUUID = skinUUID;
            this.skinName = skinName;
            this.scholarSkin = scholarSkin != null ? scholarSkin : "";
            this.ultraRareCurse = ultraRareCurse;
            this.ticksRemaining = ticksRemaining;
            this.retryTicks = 200;
        }
    }

    public static class PendingOscNuke {
        public final ResourceKey<Level> dimension;
        public final Vec3 pos;
        public int ticksRemaining;

        public PendingOscNuke(ResourceKey<Level> dimension, Vec3 pos, int ticksRemaining) {
            this.dimension = dimension;
            this.pos = pos;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private enum MaceState {
        NONE,
        LAUNCHING,
        DESCENDING
    }

    static final double PEARL_LAUNCH_SPEED = 1.65D;
    static final double PEARL_GRAVITY = 0.03D;
    static final double PEARL_AIR_DRAG = 0.99D;
    static final int PEARL_MAX_SIM_TICKS = 60;
    static final int PEARL_RECOVERY_MAX_SIM_TICKS = 160;
    static final int PEARL_RECOVERY_MIN_MARGIN_TICKS = 4;
    static final double PEARL_MISS_DROP_THRESHOLD = 8.0D;
    static final double PEARL_MISS_FAR_DROP_THRESHOLD = 6.0D;
    static final double PEARL_MISS_FAR_DIST_MARGIN = 4.0D;
    static final double PEARL_RECOVERY_MAX_DROP_BELOW_REF = 4.0D;
    static final double PEARL_MIN_TRAVEL_DIST = 5.0D;
    static final int PEARL_MIN_FLIGHT_TICKS = 3;
    private static final int PEARL_RETRY_COOLDOWN = 25;

    @FunctionalInterface
    interface PearlRayCollider {
        PearlHit clip(Vec3 from, Vec3 to);
    }

    @FunctionalInterface
    interface PearlLandingResolver {
        Vec3 resolveSafeLanding(Vec3 impactPos, Direction hitFace, BlockPos hitBlockPos);
    }

    record PearlHit(Vec3 location, Direction face, BlockPos blockPos, boolean hitTargetEntity) {
    }

    record PearlCandidate(Vec3 launchVelocity, Vec3 landingPos, double travelDist, double progress) {
    }

    record LivePearlPrediction(Vec3 impactPos, Vec3 landingPos, int remainingTicks, boolean unsafeOrVoid, boolean hitTargetEntity) {
    }

    record RecoveryPearlCandidate(Vec3 launchVelocity, Vec3 landingPos, int flightTicks, double score) {
    }

    private ItemStack godSword = ItemStack.EMPTY;
    private ItemStack godMace = ItemStack.EMPTY;
    private ItemStack windChargeStack = ItemStack.EMPTY;
    private ItemStack pickaxeStack = ItemStack.EMPTY;
    private ItemStack axeStack = ItemStack.EMPTY;
    private ItemStack shovelStack = ItemStack.EMPTY;
    private ItemStack cobbleStack = ItemStack.EMPTY;
    private ItemStack elytraStack = ItemStack.EMPTY;
    private ItemStack trimmedChestStack = ItemStack.EMPTY;

    private int attackCooldown = 0;
    private int pearlCooldown = 60;
    private int fallRecoveryCooldown = 0;
    private UUID trackedOffensivePearlUUID = null;
    private boolean trackedPearlRecoveryThrown = false;
    private Vec3 trackedPearlAnchorPos = null;
    private UUID activeRecoveryPearlUUID = null;
    private int buildCooldown = 0;
    private int pillaringTicks = 0;
    private int bridgingTicks = 0;
    private int windChargeCooldown = 0;
    private int windPillarTicks = 0;
    private double windPillarX = 0.0D;
    private double windPillarZ = 0.0D;
    private MaceState maceState = MaceState.NONE;
    private int maceTicks = 0;
    private int maceCooldown = 60;
    private int maceChainCount = 0;
    private Vec3 lastTargetPos = null;
    private Vec3 smoothedTargetVel = Vec3.ZERO;
    private double targetStartYWindow = 0.0D;
    private int targetYWindowTicks = 0;
    private int targetBuildingUpTicks = 0;
    private int targetRunningAwayTicks = 0;
    private int fireworkTicks = 0;
    private BlockPos miningPos = null;
    private int miningTicks = 0;
    private Vec3 lastCheckPos = Vec3.ZERO;
    private int stuckTicks = 0;
    private UUID ownBoatUUID = null;
    private Vec3 lastTargetBoatPos = Vec3.ZERO;
    private int targetBoatStationaryTicks = 0;
    private boolean voidRecoveryFlight = false;
    private int voidRecoveryTicks = 0;
    private BlockPos voidLandingPos = null;
    private Integer forcedChunkX = null;
    private Integer forcedChunkZ = null;
    private ResourceKey<Level> forcedChunkDim = null;
    private boolean missionAccomplished = false;
    private boolean nukeScheduled = false;

    public CreepingDeathEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 256.0D)
                .add(Attributes.STEP_HEIGHT, 1.25D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_UUID, Optional.empty());
        builder.define(SKIN_NAME, "");
        builder.define(TARGET_UUID, Optional.empty());
        builder.define(SCHOLAR_SKIN, "");
        builder.define(ULTRA_RARE_CURSE, false);
    }

    public void initializeAssassin(UUID targetUUID, UUID skinUUID, String skinName) {
        initializeAssassin(targetUUID, skinUUID, skinName, "", false);
    }

    public void initializeAssassin(UUID targetUUID, UUID skinUUID, String skinName, String scholarSkin, boolean ultraRareCurse) {
        this.entityData.set(TARGET_UUID, Optional.ofNullable(targetUUID));
        this.entityData.set(SKIN_UUID, Optional.ofNullable(skinUUID));
        this.entityData.set(SKIN_NAME, skinName != null ? skinName : "");
        this.entityData.set(SCHOLAR_SKIN, scholarSkin != null ? scholarSkin : "");
        this.entityData.set(ULTRA_RARE_CURSE, ultraRareCurse);
        if (skinName != null && !skinName.isEmpty()) {
            this.setCustomName(Component.literal(skinName));
            this.setCustomNameVisible(false);
        }
        this.setupEquipment(this.level().registryAccess());
        this.setHealth(this.getMaxHealth());
        if (ultraRareCurse) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MAX_LIFETIME_TICKS + 200, 1, false, false));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MAX_LIFETIME_TICKS + 200, 1, false, false));
        }
        if (targetUUID != null) {
            ACTIVE_HUNTS_BY_TARGET.put(targetUUID, this.getUUID());
        }
    }

    private static Holder<Enchantment> getEnchantHolder(HolderLookup.Provider registries, ResourceKey<Enchantment> key) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    public void setupEquipment(RegistryAccess registryAccess) {
        Holder<Enchantment> sharpness = getEnchantHolder(registryAccess, Enchantments.SHARPNESS);
        Holder<Enchantment> unbreaking = getEnchantHolder(registryAccess, Enchantments.UNBREAKING);
        Holder<Enchantment> mending = getEnchantHolder(registryAccess, Enchantments.MENDING);
        Holder<Enchantment> looting = getEnchantHolder(registryAccess, Enchantments.LOOTING);
        Holder<Enchantment> fireAspect = getEnchantHolder(registryAccess, Enchantments.FIRE_ASPECT);
        Holder<Enchantment> vanishing = getEnchantHolder(registryAccess, Enchantments.VANISHING_CURSE);

        this.godSword = new ItemStack(Items.NETHERITE_SWORD);
        this.godSword.enchant(sharpness, 5);
        this.godSword.enchant(unbreaking, 3);
        this.godSword.enchant(mending, 1);
        this.godSword.enchant(getEnchantHolder(registryAccess, Enchantments.SWEEPING_EDGE), 3);
        this.godSword.enchant(looting, 3);
        this.godSword.enchant(fireAspect, 2);
        this.godSword.enchant(vanishing, 1);

        this.godMace = new ItemStack(Items.MACE);
        this.godMace.enchant(getEnchantHolder(registryAccess, Enchantments.DENSITY), 5);
        this.godMace.enchant(getEnchantHolder(registryAccess, Enchantments.WIND_BURST), 1);
        this.godMace.enchant(unbreaking, 3);
        this.godMace.enchant(mending, 1);
        this.godMace.enchant(fireAspect, 2);
        this.godMace.enchant(vanishing, 1);

        this.windChargeStack = new ItemStack(Items.WIND_CHARGE, 64);

        Holder<Enchantment> eff = getEnchantHolder(registryAccess, Enchantments.EFFICIENCY);

        this.pickaxeStack = new ItemStack(Items.NETHERITE_PICKAXE);
        this.pickaxeStack.enchant(eff, 10);
        this.pickaxeStack.enchant(unbreaking, 3);
        this.pickaxeStack.enchant(vanishing, 1);

        this.axeStack = new ItemStack(Items.NETHERITE_AXE);
        this.axeStack.enchant(eff, 10);
        this.axeStack.enchant(unbreaking, 3);
        this.axeStack.enchant(vanishing, 1);

        this.shovelStack = new ItemStack(Items.NETHERITE_SHOVEL);
        this.shovelStack.enchant(eff, 10);
        this.shovelStack.enchant(unbreaking, 3);
        this.shovelStack.enchant(vanishing, 1);

        this.cobbleStack = new ItemStack(Items.COBBLESTONE, 64);
        this.elytraStack = new ItemStack(Items.ELYTRA);
        this.elytraStack.enchant(vanishing, 1);

        ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
        helmet.enchant(vanishing, 1);
        ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
        chest.enchant(vanishing, 1);
        ItemStack legs = new ItemStack(Items.NETHERITE_LEGGINGS);
        legs.enchant(vanishing, 1);
        ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
        boots.enchant(getEnchantHolder(registryAccess, Enchantments.DEPTH_STRIDER), 3);
        boots.enchant(vanishing, 1);

        try {
            Optional<? extends HolderLookup.RegistryLookup<TrimMaterial>> matRegOpt = registryAccess.lookup(Registries.TRIM_MATERIAL);
            Optional<? extends HolderLookup.RegistryLookup<TrimPattern>> patRegOpt = registryAccess.lookup(Registries.TRIM_PATTERN);
            if (matRegOpt.isPresent() && patRegOpt.isPresent()) {
                Optional<Holder.Reference<TrimMaterial>> matHolder = matRegOpt.get().get(
                        ResourceKey.create(Registries.TRIM_MATERIAL, bodycam.id("observation_crystal")));
                Optional<Holder.Reference<TrimPattern>> patHolder = patRegOpt.get().get(TrimPatterns.EYE);
                if (matHolder.isPresent() && patHolder.isPresent()) {
                    chest.set(DataComponents.TRIM, new ArmorTrim(matHolder.get(), patHolder.get()));
                }
            }
        } catch (Exception ignored) {
        }

        this.trimmedChestStack = chest.copy();

        this.setItemSlot(EquipmentSlot.MAINHAND, this.godSword.copy());
        this.setItemSlot(EquipmentSlot.HEAD, helmet);
        this.setItemSlot(EquipmentSlot.CHEST, chest);
        this.setItemSlot(EquipmentSlot.LEGS, legs);
        this.setItemSlot(EquipmentSlot.FEET, boots);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    public UUID getSkinUUID() {
        return this.entityData.get(SKIN_UUID).orElse(null);
    }

    public String getSkinName() {
        return this.entityData.get(SKIN_NAME);
    }

    public UUID getTargetUUID() {
        return this.entityData.get(TARGET_UUID).orElse(null);
    }

    public String getScholarSkin() {
        return this.entityData.get(SCHOLAR_SKIN);
    }

    public boolean isUltraRareCurse() {
        return this.entityData.get(ULTRA_RARE_CURSE);
    }

    public boolean isSlimModel() {
        String s = getScholarSkin();
        return "common".equals(s) || "uncommon".equals(s);
    }

    public static boolean isTargetAlreadyHunted(MinecraftServer server, UUID targetUUID) {
        if (targetUUID == null) {
            return false;
        }
        synchronized (PENDING_SPAWNS) {
            for (PendingSpawn p : PENDING_SPAWNS) {
                if (targetUUID.equals(p.targetUUID)) {
                    return true;
                }
            }
        }
        UUID assassinId = ACTIVE_HUNTS_BY_TARGET.get(targetUUID);
        if (assassinId == null) {
            return false;
        }
        if (server != null) {
            for (ServerLevel lvl : server.getAllLevels()) {
                Entity e = lvl.getEntity(assassinId);
                if (e instanceof CreepingDeathEntity assassin && assassin.isAlive() && !assassin.isDeadOrDying() && !assassin.isRemoved()) {
                    return true;
                }
            }
            ACTIVE_HUNTS_BY_TARGET.remove(targetUUID, assassinId);
            return false;
        }
        return true;
    }

    private static final String[] NO_ESCAPE_KEYS = new String[] {
            "message.bodycam.creeping_death.no_escape_1",
            "message.bodycam.creeping_death.no_escape_2",
            "message.bodycam.creeping_death.no_escape_3",
            "message.bodycam.creeping_death.no_escape_4"
    };

    public static void sendHuntNoEscapeTitle(ServerPlayer player) {
        if (player == null) {
            return;
        }
        long now = player.level().getGameTime();
        CompoundTag pData = PersistentDataAccess.get(player);
        long last = pData.getLong("bodycam_cd_title_cd");
        if (now - last < 40L && now >= last) {
            return;
        }
        pData.putLong("bodycam_cd_title_cd", now);
        String key = NO_ESCAPE_KEYS[player.getRandom().nextInt(NO_ESCAPE_KEYS.length)];
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable(key).withStyle(ChatFormatting.GRAY)));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("message.bodycam.creeping_death.no_escape_title")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CREEPING_DEATH_HEARTBEAT.get(), SoundSource.HOSTILE, 1.5F, 0.7F);
    }

    public static boolean blockObservationIfHunted(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return false;
        }
        if (!isTargetAlreadyHunted(player.getServer(), player.getUUID())) {
            return false;
        }
        CompoundTag pData = PersistentDataAccess.get(player);
        if (pData.getBoolean("bodycam_active")) {
            BodycamResetCameraPacket.executeReset(player);
        }
        pData.remove("bodycam_all_seeing_pending");
        PacketHandler.sendToPlayer(player, new BodycamForceClosePacket());
        sendHuntNoEscapeTitle(player);
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected int decreaseAirSupply(int currentAir) {
        return currentAir;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public void setAsInsidePortal(Portal portal, BlockPos pos) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && this.tickCount < MAX_LIFETIME_TICKS) {
            this.voidRecoveryFlight = true;
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (!(attacker instanceof Player) && !(direct instanceof Player)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    private void updateForcedChunk(ServerLevel currentLevel) {
        int cx = SectionPos.blockToSectionCoord(this.getBlockX());
        int cz = SectionPos.blockToSectionCoord(this.getBlockZ());
        ResourceKey<Level> dim = currentLevel.dimension();
        if (this.forcedChunkX == null || this.forcedChunkZ == null
                || this.forcedChunkX != cx || this.forcedChunkZ != cz
                || !dim.equals(this.forcedChunkDim)) {
            releaseForcedChunk();
            BodycamPlatform.forceChunk(currentLevel, this.getUUID(), cx, cz, true);
            this.forcedChunkX = cx;
            this.forcedChunkZ = cz;
            this.forcedChunkDim = dim;
        }
    }

    private void releaseForcedChunk() {
        if (this.forcedChunkX != null && this.forcedChunkZ != null && this.forcedChunkDim != null
                && this.level().getServer() != null) {
            ServerLevel lvl = this.level().getServer().getLevel(this.forcedChunkDim);
            if (lvl != null) {
                BodycamPlatform.forceChunk(lvl, this.getUUID(), this.forcedChunkX, this.forcedChunkZ, false);
            }
        }
        this.forcedChunkX = null;
        this.forcedChunkZ = null;
        this.forcedChunkDim = null;
    }

    public void teleportToTargetDimension(ServerLevel targetLevel, ServerPlayer target) {
        if (this.level().isClientSide() || this.isRemoved() || this.isDeadOrDying()) {
            return;
        }
        UUID targetId = getTargetUUID();
        UUID skinId = getSkinUUID();
        String skinName = getSkinName();
        String scholarSkin = getScholarSkin();
        boolean ultraRare = isUltraRareCurse();
        float currentHealth = this.getHealth();
        int currentTickCount = this.tickCount;

        discardOwnBoat();
        clearTrackedOffensivePearl();
        this.activeRecoveryPearlUUID = null;
        setSwimmingPose(false);
        releaseForcedChunk();

        if (this.level() == targetLevel) {
            BlockPos dest = findSafeLandingNearTarget(targetLevel, target, 4.0D);
            double tx = dest != null ? dest.getX() + 0.5D : target.getX();
            double ty = dest != null ? dest.getY() : target.getY();
            double tz = dest != null ? dest.getZ() + 0.5D : target.getZ();
            this.teleportTo(tx, ty, tz);
            updateForcedChunk(targetLevel);
            return;
        }

        BlockPos dest = findSafeLandingNearTarget(targetLevel, target, 4.0D);
        double tx = dest != null ? dest.getX() + 0.5D : target.getX();
        double ty = dest != null ? dest.getY() : target.getY();
        double tz = dest != null ? dest.getZ() + 0.5D : target.getZ();

        CreepingDeathEntity transferred = new CreepingDeathEntity(ModEntityTypes.CREEPING_DEATH.get(), targetLevel);
        Vec3 toTarget = target.position().subtract(tx, ty, tz);
        float yaw = toTarget.lengthSqr() > 0.001D
                ? (float) (Mth.atan2(toTarget.z, toTarget.x) * (double) (180F / (float) Math.PI)) - 90.0F
                : target.getYRot();
        transferred.moveTo(tx, ty, tz, yaw, 0.0F);
        transferred.setYHeadRot(yaw);
        transferred.setYBodyRot(yaw);
        transferred.initializeAssassin(targetId, skinId, skinName, scholarSkin, ultraRare);
        transferred.setHealth(currentHealth);
        transferred.tickCount = currentTickCount;
        targetLevel.addFreshEntity(transferred);
        transferred.updateForcedChunk(targetLevel);

        this.missionAccomplished = true;
        this.discard();
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        if (!this.level().isClientSide()) {
            if (this.ownBoatUUID == null || !vehicle.getUUID().equals(this.ownBoatUUID)) {
                return false;
            }
        }
        return super.startRiding(vehicle, force);
    }

    private void discardOwnBoat() {
        if (this.isPassenger()) {
            Entity vehicle = this.getVehicle();
            this.stopRiding();
            if (vehicle instanceof Boat && this.ownBoatUUID != null && vehicle.getUUID().equals(this.ownBoatUUID)) {
                vehicle.discard();
            }
        }
        if (this.ownBoatUUID != null && this.level() instanceof ServerLevel sLevel) {
            Entity e = sLevel.getEntity(this.ownBoatUUID);
            if (e instanceof Boat) {
                e.discard();
            }
        }
        this.ownBoatUUID = null;
    }

    private void setSwimmingPose(boolean swimming) {
        if (swimming) {
            this.setSprinting(true);
            this.setSwimming(true);
            if (this.getPose() != Pose.SWIMMING) {
                this.setPose(Pose.SWIMMING);
            }
        } else {
            if (this.isSwimming()) {
                this.setSwimming(false);
            }
            if (this.getPose() == Pose.SWIMMING) {
                this.setPose(Pose.STANDING);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            discardOwnBoat();
            releaseForcedChunk();
            UUID targetId = getTargetUUID();
            if (targetId != null) {
                ACTIVE_HUNTS_BY_TARGET.remove(targetId, this.getUUID());
            }
            if (!this.missionAccomplished && !this.nukeScheduled) {
                this.nukeScheduled = true;
                if (source.getEntity() instanceof ServerPlayer killer) {
                    ModAdvancements.award(killer, ModAdvancements.SURVIVE_CREEPING_DEATH);
                }
                if (targetId != null && this.level() instanceof ServerLevel sLevel) {
                    ServerPlayer hunted = sLevel.getServer().getPlayerList().getPlayer(targetId);
                    if (hunted != null && hunted.isAlive()) {
                        ModAdvancements.award(hunted, ModAdvancements.SURVIVE_CREEPING_DEATH);
                    }
                }
                scheduleOscNuke(this.level().dimension(), this.position());
            }
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            discardOwnBoat();
            releaseForcedChunk();
            UUID targetId = getTargetUUID();
            if (targetId != null) {
                ACTIVE_HUNTS_BY_TARGET.remove(targetId, this.getUUID());
            }
            if (this.miningPos != null && this.level() instanceof ServerLevel sLevel) {
                sLevel.destroyBlockProgress(this.getId(), this.miningPos, -1);
            }
        }
        super.remove(reason);
    }

    public void onTargetKilled() {
        if (this.level().isClientSide() || this.missionAccomplished) {
            return;
        }
        this.missionAccomplished = true;
        discardOwnBoat();
        releaseForcedChunk();
        UUID targetId = getTargetUUID();
        if (targetId != null) {
            ACTIVE_HUNTS_BY_TARGET.remove(targetId, this.getUUID());
        }
        if (this.level() instanceof ServerLevel sLevel) {
            sLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.03D);
            sLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0D, this.getZ(), 25, 0.4D, 0.8D, 0.4D, 0.04D);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !this.isAlive() || this.isDeadOrDying() || this.isRemoved()) {
            return;
        }
        if (!(this.level() instanceof ServerLevel sLevel)) {
            return;
        }

        if (this.godSword.isEmpty() || this.godMace.isEmpty() || this.windChargeStack.isEmpty()) {
            this.setupEquipment(this.level().registryAccess());
        }

        if (this.tickCount >= MAX_LIFETIME_TICKS) {
            if (!this.missionAccomplished && !this.nukeScheduled) {
                this.nukeScheduled = true;
                UUID survivedTargetId = getTargetUUID();
                if (survivedTargetId != null) {
                    ServerPlayer survivedPlayer = sLevel.getServer().getPlayerList().getPlayer(survivedTargetId);
                    if (survivedPlayer != null && survivedPlayer.isAlive()) {
                        ModAdvancements.award(survivedPlayer, ModAdvancements.SURVIVE_CREEPING_DEATH);
                    }
                }
                scheduleOscNuke(sLevel.dimension(), this.position());
            }
            this.discard();
            return;
        }

        UUID targetId = getTargetUUID();
        if (targetId == null) {
            this.discard();
            return;
        }
        ACTIVE_HUNTS_BY_TARGET.put(targetId, this.getUUID());
        updateForcedChunk(sLevel);

        ServerPlayer target = sLevel.getServer().getPlayerList().getPlayer(targetId);
        if (target == null) {
            return;
        }

        if (!target.isAlive() || target.isDeadOrDying()) {
            onTargetKilled();
            return;
        }

        if (PersistentDataAccess.get(target).getBoolean("bodycam_active")) {
            BodycamResetCameraPacket.executeReset(target);
            sendHuntNoEscapeTitle(target);
        }

        if (target.isCreative() || target.isSpectator()) {
            this.missionAccomplished = true;
            discardOwnBoat();
            releaseForcedChunk();
            ACTIVE_HUNTS_BY_TARGET.remove(targetId, this.getUUID());
            this.discard();
            return;
        }

        if (target.level() != this.level()) {
            teleportToTargetDimension(target.serverLevel(), target);
            return;
        }

        this.setTarget(target);

        if (this.attackCooldown > 0) this.attackCooldown--;
        if (this.pearlCooldown > 0) this.pearlCooldown--;
        if (this.fallRecoveryCooldown > 0) this.fallRecoveryCooldown--;
        if (this.buildCooldown > 0) this.buildCooldown--;
        if (this.pillaringTicks > 0) this.pillaringTicks--;
        if (this.bridgingTicks > 0) this.bridgingTicks--;
        if (this.windChargeCooldown > 0) this.windChargeCooldown--;
        if (this.windPillarTicks > 0) this.windPillarTicks--;
        if (this.maceCooldown > 0) this.maceCooldown--;

        if (this.tickCount % 10 == 0) {
            if (this.pillaringTicks <= 0 && this.windPillarTicks <= 0 && this.miningPos == null
                    && this.maceState == MaceState.NONE
                    && !this.voidRecoveryFlight && this.position().distanceToSqr(this.lastCheckPos) < 0.25D) {
                this.stuckTicks += 10;
            } else {
                this.stuckTicks = 0;
            }
            this.lastCheckPos = this.position();
        }

        double dx = target.getX() - this.getX();
        double dy = target.getY() - this.getY();
        double dz = target.getZ() - this.getZ();
        double horizDistSq = dx * dx + dz * dz;
        double totalDistSq = horizDistSq + dy * dy;

        updateTargetBehaviorTracking(target, dx, dz);

        if (this.isUltraRareCurse()) {
            if (!this.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MAX_LIFETIME_TICKS, 1, false, false));
            }
            if (!this.hasEffect(MobEffects.DAMAGE_BOOST)) {
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MAX_LIFETIME_TICKS, 1, false, false));
            }
            if (totalDistSq > 2500.0D) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4, false, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false, true));
            }
        }

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (!this.voidRecoveryFlight) {
            if (this.getY() < sLevel.getMinBuildHeight() + 2
                    || (this.getY() < target.getY() - 18.0D && !hasSolidGroundBelow(sLevel, this.blockPosition()))) {
                this.voidRecoveryFlight = true;
                this.voidRecoveryTicks = 0;
                this.voidLandingPos = findSafeLandingNearTarget(sLevel, target, 20.0D);
            }
        }

        if (this.voidRecoveryFlight) {
            this.voidRecoveryTicks++;
            this.pillaringTicks = 0;
            this.windPillarTicks = 0;
            this.bridgingTicks = 0;
            this.maceState = MaceState.NONE;
            clearTrackedOffensivePearl();
            discardOwnBoat();
            setSwimmingPose(false);
            this.getNavigation().stop();
            clearMining(sLevel);
            equipWeapon(this.godSword);

            if (!this.getSharedFlag(7)) {
                this.setItemSlot(EquipmentSlot.CHEST, this.elytraStack.copy());
                this.setSharedFlag(7, true);
            }

            if (this.voidLandingPos == null || this.voidRecoveryTicks % 20 == 0 || !isValidSpawnSpace(sLevel, this.voidLandingPos)) {
                this.voidLandingPos = findSafeLandingNearTarget(sLevel, target, 20.0D);
            }

            BlockPos safeLanding = this.voidLandingPos != null ? this.voidLandingPos : target.blockPosition();
            double goalX = safeLanding.getX() + 0.5D;
            double goalY = safeLanding.getY();
            double goalZ = safeLanding.getZ() + 0.5D;
            double horizToGoalSq = (goalX - this.getX()) * (goalX - this.getX()) + (goalZ - this.getZ()) * (goalZ - this.getZ());

            boolean reachedGoal = (horizToGoalSq <= 5.0D && Math.abs(this.getY() - goalY) <= 3.0D)
                    || (this.getY() >= sLevel.getMinBuildHeight() + 6 && this.onGround() && hasSolidGroundBelow(sLevel, this.blockPosition()) && this.getY() >= goalY - 3.0D)
                    || this.voidRecoveryTicks >= 140;

            if (reachedGoal) {
                this.setPos(goalX, goalY, goalZ);
                this.setDeltaMovement(Vec3.ZERO);
                this.voidRecoveryFlight = false;
                this.voidRecoveryTicks = 0;
                this.voidLandingPos = null;
                if (!target.isFallFlying()) {
                    this.setSharedFlag(7, false);
                    this.setItemSlot(EquipmentSlot.CHEST, this.trimmedChestStack.copy());
                }
                return;
            }

            double clearanceY = goalY + 5.0D;
            Vec3 flyTarget;
            if (this.getY() < clearanceY && horizToGoalSq > 6.25D) {
                if (hasCeilingAbove(sLevel, this.blockPosition(), (int) Math.ceil(clearanceY))) {
                    Vec3 openColumn = findOpenClimbOffset(sLevel, this.position(), (int) Math.ceil(clearanceY), target.position());
                    flyTarget = new Vec3(openColumn.x, this.getY(), openColumn.z);
                } else {
                    flyTarget = new Vec3(this.getX(), clearanceY + 1.0D, this.getZ());
                }
            } else if (horizToGoalSq > 6.25D) {
                flyTarget = new Vec3(goalX, clearanceY, goalZ);
            } else {
                flyTarget = new Vec3(goalX, goalY + 0.5D, goalZ);
            }

            Vec3 toGoal = flyTarget.subtract(this.position());
            Vec3 dir = toGoal.lengthSqr() > 0.001D ? toGoal.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
            double speed = 1.55D;
            this.setDeltaMovement(dir.scale(speed));
            this.hurtMarked = true;

            float yaw = (float) (Mth.atan2(dir.z, dir.x) * (double) (180F / (float) Math.PI)) - 90.0F;
            float pitch = (float) (-(Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (double) (180F / (float) Math.PI)));
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;

            this.fireworkTicks++;
            if (this.fireworkTicks >= 20) {
                this.fireworkTicks = 0;
                sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1.5F, 1.0F);
                sLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(),
                        12, 0.15D, 0.15D, 0.15D, 0.05D);
            }
            return;
        }

        boolean shouldFlyElytra = target.isFallFlying();

        if (shouldFlyElytra) {
            this.pillaringTicks = 0;
            this.windPillarTicks = 0;
            this.bridgingTicks = 0;
            this.maceState = MaceState.NONE;
            clearTrackedOffensivePearl();
            discardOwnBoat();
            setSwimmingPose(false);
            this.getNavigation().stop();
            clearMining(sLevel);
            if (!this.getSharedFlag(7)) {
                this.setItemSlot(EquipmentSlot.CHEST, this.elytraStack.copy());
                this.setSharedFlag(7, true);
                if (this.onGround()) {
                    this.setPos(this.getX(), this.getY() + 1.2D, this.getZ());
                }
            }
            Vec3 toTarget = target.getEyePosition().subtract(this.position());
            Vec3 dir = toTarget.lengthSqr() > 0.001D ? toTarget.normalize() : Vec3.ZERO;
            double speed = 1.45D;
            this.setDeltaMovement(dir.scale(speed));
            this.hurtMarked = true;

            float yaw = (float) (Mth.atan2(dir.z, dir.x) * (double) (180F / (float) Math.PI)) - 90.0F;
            float pitch = (float) (-(Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (double) (180F / (float) Math.PI)));
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;

            this.fireworkTicks++;
            if (this.fireworkTicks >= 25) {
                this.fireworkTicks = 0;
                sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1.5F, 1.0F);
                sLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(),
                        12, 0.15D, 0.15D, 0.15D, 0.05D);
            }

            if (totalDistSq <= 9.0D && this.attackCooldown <= 0) {
                performMeleeAttack(target);
            }
            return;
        } else if (this.getSharedFlag(7)) {
            this.setSharedFlag(7, false);
            this.setItemSlot(EquipmentSlot.CHEST, this.trimmedChestStack.copy());
        }

        destroyBlockingBoats(sLevel, target);

        boolean shouldUseBoat = false;
        if (target.getVehicle() instanceof Boat targetBoat) {
            boolean targetBoatInWater = isAtWaterSurface(sLevel, targetBoat)
                    || targetBoat.isInWater()
                    || sLevel.getFluidState(targetBoat.blockPosition()).is(FluidTags.WATER)
                    || sLevel.getFluidState(targetBoat.blockPosition().below()).is(FluidTags.WATER);
            double boatDx = targetBoat.getX() - this.lastTargetBoatPos.x;
            double boatDz = targetBoat.getZ() - this.lastTargetBoatPos.z;
            double boatHorizMoveSq = boatDx * boatDx + boatDz * boatDz;
            this.lastTargetBoatPos = targetBoat.position();
            if (boatHorizMoveSq < 0.01D) {
                this.targetBoatStationaryTicks++;
            } else if (boatHorizMoveSq > 0.025D) {
                this.targetBoatStationaryTicks = 0;
            }

            Entity checkEntity = this.getVehicle() instanceof Boat b ? b : this;
            boolean assassinOnWaterSurface = isAtWaterSurface(sLevel, checkEntity);
            if (targetBoatInWater && assassinOnWaterSurface && this.targetBoatStationaryTicks < 40) {
                shouldUseBoat = true;
            }
        } else {
            this.targetBoatStationaryTicks = 0;
        }

        if (shouldUseBoat) {
            this.maceState = MaceState.NONE;
            this.windPillarTicks = 0;
            this.bridgingTicks = 0;
            clearTrackedOffensivePearl();
            setSwimmingPose(false);
            this.getNavigation().stop();

            Boat ownBoat = getOrCreateOwnBoat(sLevel);
            if (ownBoat != null) {
                Vec3 horizVec = new Vec3(dx, 0.0D, dz);
                Vec3 horizDir = horizVec.lengthSqr() > 0.001D ? horizVec.normalize() : Vec3.ZERO;
                float yaw = (float) (Mth.atan2(horizDir.z, horizDir.x) * (double) (180F / (float) Math.PI)) - 90.0F;
                ownBoat.setYRot(yaw);
                this.setYRot(yaw);
                this.yBodyRot = yaw;
                this.yHeadRot = yaw;

                double boatSpeed = 0.44D;
                double boatVy = Math.max(0.0D, ownBoat.getDeltaMovement().y);
                ownBoat.setDeltaMovement(horizDir.x * boatSpeed, boatVy, horizDir.z * boatSpeed);
                ownBoat.hurtMarked = true;
            }

            if (totalDistSq <= 25.0D) {
                equipWeapon(target.isBlocking() ? this.axeStack : this.godSword);
            }
            if (totalDistSq <= 12.0D && this.attackCooldown <= 0) {
                performMeleeAttack(target);
            }
            return;
        } else if (this.ownBoatUUID != null || this.isPassenger()) {
            discardOwnBoat();
        }

        if (handleTrackedPearlRecovery(sLevel, target) && this.maceState == MaceState.NONE && this.windPillarTicks <= 0) {
            return;
        }

        if (this.maceState != MaceState.NONE) {
            if (handleActiveMaceAttack(sLevel, target, dx, dy, dz, horizDistSq, totalDistSq)) {
                return;
            }
        }

        if (this.windPillarTicks > 0) {
            handleActiveWindPillar(sLevel, target);
            return;
        }

        if (canStartWindChargeMace(sLevel, target, dx, dy, dz, horizDistSq)) {
            startWindChargeMace(sLevel, target, dx, dz, horizDistSq);
            return;
        }

        if (totalDistSq <= 25.0D && this.miningPos == null && this.pillaringTicks <= 0 && this.bridgingTicks <= 0) {
            equipWeapon(target.isBlocking() ? this.axeStack : this.godSword);
        }

        if (totalDistSq <= 9.0D && (this.hasLineOfSight(target) || totalDistSq <= 4.5D)) {
            if (this.attackCooldown <= 0) {
                performMeleeAttack(target);
                if (!target.isAlive() || target.isDeadOrDying()) {
                    return;
                }
            }
        }

        if (handleMountainFallRecovery(sLevel, target)) {
            return;
        }

        if (this.pearlCooldown <= 0
                && !hasActiveRecoveryPearl(sLevel)
                && this.miningPos == null
                && !this.isUnderWater()
                && (horizDistSq > 196.0D || (dy > 5.0D && horizDistSq > 36.0D) || this.stuckTicks >= 40)) {
            PearlCandidate pearlCandidate = findViablePearlCandidate(sLevel, target);
            if (pearlCandidate != null) {
                setSwimmingPose(false);
                this.bridgingTicks = 0;
                clearMining(sLevel);
                ThrownEnderpearl thrownPearl = throwEnderPearlAt(sLevel, pearlCandidate.launchVelocity());
                this.trackedOffensivePearlUUID = thrownPearl.getUUID();
                this.trackedPearlRecoveryThrown = false;
                this.trackedPearlAnchorPos = this.position();
                this.pearlCooldown = 90;
                this.stuckTicks = 0;
                this.pillaringTicks = 0;
                return;
            }
            this.pearlCooldown = PEARL_RETRY_COOLDOWN;
        }

        if (this.isInWater()) {
            this.pillaringTicks = 0;
            this.windPillarTicks = 0;
            this.bridgingTicks = 0;
            this.maceState = MaceState.NONE;
            this.getNavigation().stop();

            BlockPos waterObstacle = findBlockingBlock(sLevel, target);
            if (waterObstacle != null) {
                handleMining(sLevel, waterObstacle);
            } else {
                if (this.miningPos != null) {
                    clearMining(sLevel);
                }
                equipCombatWeaponIfHoldingTool(target);
            }

            boolean shouldSwimPose = totalDistSq > 2.25D || this.isUnderWater() || target.isUnderWater();
            setSwimmingPose(shouldSwimPose);

            Vec3 toTarget = new Vec3(dx, target.getEyeY() - this.getEyeY(), dz);
            Vec3 horizVec = new Vec3(dx, 0.0D, dz);
            Vec3 horizDir = horizVec.lengthSqr() > 0.001D ? horizVec.normalize() : Vec3.ZERO;
            double swimSpeed = 0.28D;

            double vy;
            if (target.isUnderWater() || target.getEyeY() < this.getEyeY() - 0.4D) {
                vy = Mth.clamp((target.getEyeY() - this.getEyeY()) * 0.16D, -0.26D, 0.26D);
            } else if (target.getY() > this.getY() + 0.4D) {
                vy = Mth.clamp(dy * 0.16D + 0.08D, -0.20D, 0.28D);
            } else if (this.isUnderWater()) {
                vy = 0.10D;
            } else {
                vy = Math.max(-0.02D, this.getDeltaMovement().y);
            }

            if (this.horizontalCollision && dy >= -0.5D) {
                vy = 0.34D;
            }

            this.setDeltaMovement(horizDir.x * swimSpeed, vy, horizDir.z * swimSpeed);
            this.hurtMarked = true;

            if (toTarget.lengthSqr() > 0.001D) {
                float yaw = (float) (Mth.atan2(toTarget.z, toTarget.x) * (double) (180F / (float) Math.PI)) - 90.0F;
                float pitch = (float) (-(Mth.atan2(toTarget.y, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z)) * (double) (180F / (float) Math.PI)));
                this.setYRot(yaw);
                this.setXRot(pitch);
                this.yBodyRot = yaw;
                this.yHeadRot = yaw;
            }
            return;
        } else {
            setSwimmingPose(false);
        }

        BlockPos pillarFeet = this.blockPosition();
        BlockPos pillarAboveHead = pillarFeet.above(2);
        boolean pillarBlockedByUnbreakable = !isPassableForBody(sLevel, pillarAboveHead)
                && !isBreakableObstacle(sLevel, pillarAboveHead);
        boolean shouldPillar = dy >= 2.0D && horizDistSq < 64.0D
                && (this.onGround() || this.pillaringTicks > 0)
                && !pillarBlockedByUnbreakable;
        if (shouldPillar) {
            this.bridgingTicks = 0;
            this.getNavigation().stop();
            this.setSprinting(false);
            this.xxa = 0.0F;
            this.zza = 0.0F;

            double centerX = pillarFeet.getX() + 0.5D;
            double centerZ = pillarFeet.getZ() + 0.5D;
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);

            if (isBreakableObstacle(sLevel, pillarAboveHead)) {
                this.setPos(centerX, this.getY(), centerZ);
                this.pillaringTicks = 10;
                handleMining(sLevel, pillarAboveHead);
                return;
            }
            if (isBreakableObstacle(sLevel, pillarFeet.above())) {
                this.setPos(centerX, this.getY(), centerZ);
                this.pillaringTicks = 10;
                handleMining(sLevel, pillarFeet.above());
                return;
            }
            BlockState feetState = sLevel.getBlockState(pillarFeet);
            if (!feetState.isAir() && !feetState.canBeReplaced() && isBreakableObstacle(sLevel, pillarFeet)) {
                this.setPos(centerX, this.getY(), centerZ);
                this.pillaringTicks = 10;
                handleMining(sLevel, pillarFeet);
                return;
            }
            clearMining(sLevel);

            if (dy >= 4.0D && this.windChargeCooldown <= 0 && this.onGround()
                    && hasOpenHeadroom(sLevel, pillarFeet, 6)) {
                startWindPillar(sLevel, pillarFeet, centerX, centerZ);
                return;
            }

            if (this.buildCooldown <= 0 && this.onGround()) {
                boolean canPlaceAtFeet = feetState.isAir()
                        || feetState.canBeReplaced()
                        || (feetState.getCollisionShape(sLevel, pillarFeet).isEmpty()
                                && feetState.getDestroySpeed(sLevel, pillarFeet) >= 0.0F);
                if (canPlaceAtFeet) {
                    placeCobbleBlock(sLevel, pillarFeet);
                    this.setPos(centerX, pillarFeet.getY() + 1.0D, centerZ);
                    this.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    this.buildCooldown = 5;
                    this.pillaringTicks = 10;
                }
            } else {
                this.setPos(centerX, this.getY(), centerZ);
            }
            return;
        }

        if (shouldBridge(sLevel, dx, dy, dz, horizDistSq)) {
            handleBridging(sLevel, target, dx, dy, dz, horizDistSq);
            return;
        }

        BlockPos obstacle = findBlockingBlock(sLevel, target);
        if (obstacle != null) {
            this.getNavigation().stop();
            this.setSprinting(false);
            handleMining(sLevel, obstacle);
            return;
        }
        if (this.miningPos != null) {
            clearMining(sLevel);
        }
        equipCombatWeaponIfHoldingTool(target);

        this.getNavigation().moveTo(target, 1.0D);
        this.setSprinting(true);

        if (horizDistSq > 0.25D && !this.hasLineOfSight(target)
                && (this.stuckTicks >= 10 || this.getNavigation().isDone())
                && !isDropAhead(sLevel, this.blockPosition(), dx, dz)) {
            double horizDist = Math.sqrt(horizDistSq);
            double stepX = dx / horizDist;
            double stepZ = dz / horizDist;
            this.getNavigation().stop();
            this.getMoveControl().setWantedPosition(
                    this.getX() + stepX * 1.5D,
                    this.getY(),
                    this.getZ() + stepZ * 1.5D,
                    1.0D
            );
        }
    }

    private static boolean isAtWaterSurface(ServerLevel level, Entity entity) {
        if (entity.isUnderWater()) {
            return false;
        }
        BlockPos pos = entity.blockPosition();
        FluidState above = level.getFluidState(pos.above());
        if (!above.isEmpty()) {
            return false;
        }
        FluidState atPos = level.getFluidState(pos);
        if (atPos.is(FluidTags.WATER) && atPos.isSource()) {
            return true;
        }
        FluidState below = level.getFluidState(pos.below());
        return below.is(FluidTags.WATER) && below.isSource() && atPos.isEmpty();
    }

    private static double findWaterSurfaceY(ServerLevel level, BlockPos pos) {
        FluidState atPos = level.getFluidState(pos);
        if (atPos.is(FluidTags.WATER) && atPos.isSource()) {
            return pos.getY() + 0.9D;
        }
        FluidState below = level.getFluidState(pos.below());
        if (below.is(FluidTags.WATER) && below.isSource()) {
            return pos.below().getY() + 0.9D;
        }
        return pos.getY();
    }

    private static boolean hasCeilingAbove(ServerLevel level, BlockPos fromPos, int maxCheckY) {
        int topY = Math.min(level.getMaxBuildHeight() - 1, maxCheckY);
        for (int y = fromPos.getY() + 1; y <= topY; y++) {
            BlockPos check = new BlockPos(fromPos.getX(), y, fromPos.getZ());
            BlockState state = level.getBlockState(check);
            if (!state.isAir() && !state.getCollisionShape(level, check).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 findOpenClimbOffset(ServerLevel level, Vec3 currentPos, int clearanceY, Vec3 awayCenter) {
        BlockPos base = BlockPos.containing(currentPos);
        for (int r = 2; r <= 24; r += 2) {
            for (int step = 0; step < 16; step++) {
                double angle = (2.0D * Math.PI * step) / 16.0D;
                int cx = base.getX() + (int) Math.round(Math.cos(angle) * r);
                int cz = base.getZ() + (int) Math.round(Math.sin(angle) * r);
                BlockPos col = new BlockPos(cx, base.getY(), cz);
                if (!hasCeilingAbove(level, col, clearanceY)) {
                    return new Vec3(cx + 0.5D, currentPos.y, cz + 0.5D);
                }
            }
        }
        Vec3 away = new Vec3(currentPos.x - awayCenter.x, 0.0D, currentPos.z - awayCenter.z);
        if (away.lengthSqr() < 0.001D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            away = away.normalize();
        }
        return currentPos.add(away.scale(10.0D));
    }

    private static boolean hasSolidGroundBelow(ServerLevel level, BlockPos fromPos) {
        int minY = level.getMinBuildHeight();
        for (int y = fromPos.getY(); y >= minY; y--) {
            BlockPos check = new BlockPos(fromPos.getX(), y, fromPos.getZ());
            BlockState state = level.getBlockState(check);
            if (!state.isAir() && !state.getCollisionShape(level, check).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos findSafeLandingNearTarget(ServerLevel level, ServerPlayer target, double preferredRadius) {
        Vec3 look = target.getLookAngle();
        Vec3 horizBack = new Vec3(-look.x, 0, -look.z);
        if (horizBack.lengthSqr() < 0.001D) {
            horizBack = new Vec3(0, 0, -1);
        } else {
            horizBack = horizBack.normalize();
        }
        double baseAngle = Math.atan2(horizBack.z, horizBack.x);
        double[] radii = preferredRadius >= 15.0D
                ? new double[]{20.0D, 18.0D, 22.0D, 15.0D, 12.0D, 8.0D, 5.0D, 2.0D, 0.0D}
                : new double[]{preferredRadius, 6.0D, 8.0D, 2.0D, 0.0D};

        int maxDy = preferredRadius >= 15.0D ? 25 : 6;
        int minDy = preferredRadius >= 15.0D ? -35 : -8;

        for (boolean requireOpenAbove : new boolean[]{true, false}) {
            for (double radius : radii) {
                int maxStep = radius == 0.0D ? 0 : 12;
                for (int step = 0; step <= maxStep; step++) {
                    int[] signs = (step == 0 || step == 12) ? new int[]{1} : new int[]{1, -1};
                    for (int sign : signs) {
                        double angle = baseAngle + sign * (step * (Math.PI / 12.0D));
                        double rx = target.getX() + Math.cos(angle) * radius;
                        double rz = target.getZ() + Math.sin(angle) * radius;
                        BlockPos ringBase = BlockPos.containing(rx, target.getY(), rz);

                        for (int dy = maxDy; dy >= minDy; dy--) {
                            BlockPos check = ringBase.offset(0, dy, 0);
                            if (isValidSpawnSpace(level, check)) {
                                if (!requireOpenAbove || !hasCeilingAbove(level, check, check.getY() + 6)) {
                                    return check;
                                }
                            }
                        }
                    }
                }
            }
        }
        return target.blockPosition();
    }

    private void performMeleeAttack(ServerPlayer target) {
        boolean blocking = target.isBlocking();
        equipWeapon(blocking ? this.axeStack : this.godSword);
        this.swing(InteractionHand.MAIN_HAND);
        this.doHurtTarget(target);
        if (blocking) {
            target.disableShield();
        }
        this.attackCooldown = 12;
        if (!target.isAlive() || target.isDeadOrDying()) {
            onTargetKilled();
        }
    }

    private void performMaceSmashAttack(ServerLevel level, ServerPlayer target, double dx, double dy, double dz, double horizDistSq) {
        equipWeapon(this.godMace);
        this.swing(InteractionHand.MAIN_HAND);
        boolean hit = this.doHurtTarget(target);
        this.resetFallDistance();
        this.hurtMarked = true;
        this.attackCooldown = 12;
        if (!target.isAlive() || target.isDeadOrDying()) {
            this.maceState = MaceState.NONE;
            onTargetKilled();
            return;
        }
        if (hit && this.maceChainCount < 2 && this.getDeltaMovement().y > 0.20D
                && horizDistSq <= 30.25D && Math.abs(dy) <= 3.0D
                && this.targetRunningAwayTicks < 6 && this.targetBuildingUpTicks < 6
                && !target.isBlocking() && this.hasLineOfSight(target)
                && hasOpenHeadroom(level, this.blockPosition(), 6)
                && hasSafeGroundArea(level, target.blockPosition())) {
            this.maceChainCount++;
            this.maceState = MaceState.LAUNCHING;
            this.maceTicks = 4;
            return;
        }
        endMaceSequence(target, 110);
    }

    private void clearMining(ServerLevel level) {
        if (this.miningPos != null) {
            level.destroyBlockProgress(this.getId(), this.miningPos, -1);
            this.miningPos = null;
            this.miningTicks = 0;
        }
    }

    private void triggerWindChargeBurst(ServerLevel level, double x, double y, double z) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WIND_CHARGE_THROW, SoundSource.HOSTILE, 0.7F, 0.4F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        ExplosionDamageCalculator windCalc = new SimpleExplosionDamageCalculator(
                true,
                false,
                Optional.of(0.0F),
                level.registryAccess().lookupOrThrow(Registries.BLOCK)
                        .get(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS)
                        .map(Function.identity())
        );
        level.explode(
                this,
                null,
                windCalc,
                x,
                y,
                z,
                1.2F,
                false,
                Level.ExplosionInteraction.TRIGGER,
                ParticleTypes.GUST_EMITTER_SMALL,
                ParticleTypes.GUST_EMITTER_LARGE,
                SoundEvents.WIND_CHARGE_BURST
        );
    }

    private void updateTargetBehaviorTracking(ServerPlayer target, double dx, double dz) {
        Vec3 curTargetPos = target.position();
        if (this.lastTargetPos == null) {
            this.lastTargetPos = curTargetPos;
            this.smoothedTargetVel = Vec3.ZERO;
            this.targetStartYWindow = curTargetPos.y;
            this.targetYWindowTicks = 0;
            this.targetBuildingUpTicks = 0;
            this.targetRunningAwayTicks = 0;
            return;
        }

        Vec3 delta = curTargetPos.subtract(this.lastTargetPos);
        if (delta.lengthSqr() > 64.0D) {
            this.smoothedTargetVel = Vec3.ZERO;
            this.targetStartYWindow = curTargetPos.y;
            this.targetYWindowTicks = 0;
        } else {
            this.smoothedTargetVel = this.smoothedTargetVel.scale(0.65D).add(delta.scale(0.35D));
        }
        this.lastTargetPos = curTargetPos;

        double horizDist = Math.sqrt(dx * dx + dz * dz);
        if (horizDist > 0.25D) {
            double awaySpeed = (this.smoothedTargetVel.x * dx + this.smoothedTargetVel.z * dz) / horizDist;
            if (awaySpeed > 0.14D || (target.isSprinting() && awaySpeed > 0.09D)) {
                this.targetRunningAwayTicks = Math.min(30, this.targetRunningAwayTicks + 3);
            } else {
                this.targetRunningAwayTicks = Math.max(0, this.targetRunningAwayTicks - 1);
            }
        } else {
            this.targetRunningAwayTicks = Math.max(0, this.targetRunningAwayTicks - 1);
        }

        this.targetYWindowTicks++;
        if (delta.y > 0.25D) {
            this.targetBuildingUpTicks = Math.min(40, this.targetBuildingUpTicks + 4);
        } else {
            this.targetBuildingUpTicks = Math.max(0, this.targetBuildingUpTicks - 1);
        }
        if (this.targetYWindowTicks >= 20) {
            if (curTargetPos.y - this.targetStartYWindow >= 1.5D) {
                this.targetBuildingUpTicks = Math.max(this.targetBuildingUpTicks, 25);
            }
            this.targetStartYWindow = curTargetPos.y;
            this.targetYWindowTicks = 0;
        }
    }

    private boolean canStartWindChargeMace(ServerLevel level, ServerPlayer target, double dx, double dy, double dz, double horizDistSq) {
        if (this.maceState != MaceState.NONE || this.maceCooldown > 0 || this.windChargeCooldown > 0) {
            return false;
        }
        if (!this.onGround() || this.isInWater() || this.isInLava() || this.pillaringTicks > 0 || this.bridgingTicks > 0) {
            return false;
        }
        if (target.isInWater() || target.isInLava() || target.isFallFlying() || target.isPassenger() || target.isBlocking()) {
            return false;
        }
        if (horizDistSq < 1.2D || horizDistSq > 25.0D || Math.abs(dy) > 1.8D) {
            return false;
        }
        if (this.targetRunningAwayTicks >= 6 || this.targetBuildingUpTicks >= 6) {
            return false;
        }
        double horizDist = Math.sqrt(horizDistSq);
        double awaySpeed = (this.smoothedTargetVel.x * dx + this.smoothedTargetVel.z * dz) / horizDist;
        if (awaySpeed > 0.12D) {
            return false;
        }
        if (!this.hasLineOfSight(target) || isTargetEnclosedOrHiding(level, target)) {
            return false;
        }
        BlockPos selfPos = this.blockPosition();
        BlockPos targetPos = target.blockPosition();
        if (!hasOpenHeadroom(level, selfPos, 7) || !hasOpenHeadroom(level, targetPos, 6)) {
            return false;
        }
        return hasSafeGroundArea(level, selfPos) && hasSafeGroundArea(level, targetPos);
    }

    private void startWindChargeMace(ServerLevel level, ServerPlayer target, double dx, double dz, double horizDistSq) {
        this.getNavigation().stop();
        this.setSprinting(false);
        clearMining(level);
        equipWeapon(this.windChargeStack);
        this.swing(InteractionHand.MAIN_HAND);
        triggerWindChargeBurst(level, this.getX(), this.getY() - 0.15D, this.getZ());

        double horizDist = Math.sqrt(Math.max(0.0001D, horizDistSq));
        double launchHoriz = Mth.clamp(horizDist * 0.11D, 0.06D, 0.30D);
        this.setDeltaMovement((dx / horizDist) * launchHoriz, 1.04D, (dz / horizDist) * launchHoriz);
        this.hurtMarked = true;
        this.resetFallDistance();

        this.maceState = MaceState.LAUNCHING;
        this.maceTicks = 0;
        this.maceChainCount = 0;
        this.windChargeCooldown = 40;
    }

    private boolean handleActiveMaceAttack(ServerLevel level, ServerPlayer target, double dx, double dy, double dz,
            double horizDistSq, double totalDistSq) {
        this.getNavigation().stop();
        clearMining(level);

        if (this.isInWater() || this.isInLava() || target.isInWater() || target.isFallFlying()
                || horizDistSq > 49.0D || dy > 6.0D || this.maceTicks > 60) {
            endMaceSequence(target, 90);
            return false;
        }

        this.maceTicks++;
        Vec3 curVel = this.getDeltaMovement();
        double horizDist = Math.sqrt(horizDistSq);
        if (horizDist > 0.35D) {
            double desiredSpeed = Mth.clamp(
                    horizDist * (this.maceState == MaceState.DESCENDING ? 0.22D : 0.15D),
                    0.05D,
                    0.40D
            );
            double targetVx = (dx / horizDist) * desiredSpeed;
            double targetVz = (dz / horizDist) * desiredSpeed;
            double nextVx = Mth.lerp(0.35D, curVel.x, targetVx);
            double nextVz = Mth.lerp(0.35D, curVel.z, targetVz);
            this.setDeltaMovement(nextVx, curVel.y, nextVz);
            this.hurtMarked = true;
        }

        if (this.maceState == MaceState.LAUNCHING) {
            if (this.maceTicks >= 4) {
                equipWeapon(this.godMace);
            }
            if (this.getDeltaMovement().y <= 0.0D && this.maceTicks >= 4) {
                this.maceState = MaceState.DESCENDING;
            } else if (this.onGround() && this.maceTicks >= 6) {
                endMaceSequence(target, 80);
                return false;
            }
            return true;
        }

        equipWeapon(this.godMace);
        if (MaceItem.canSmashAttack(this) && totalDistSq <= 13.5D
                && (this.hasLineOfSight(target) || totalDistSq <= 6.25D)) {
            performMaceSmashAttack(level, target, dx, dy, dz, horizDistSq);
            return true;
        }

        if (this.onGround()) {
            if (totalDistSq <= 9.0D && this.attackCooldown <= 0) {
                performMeleeAttack(target);
            }
            endMaceSequence(target, 90);
            return false;
        }
        return true;
    }

    private void endMaceSequence(ServerPlayer target, int cooldown) {
        this.maceState = MaceState.NONE;
        this.maceTicks = 0;
        this.maceChainCount = 0;
        this.maceCooldown = cooldown;
        equipWeapon(target.isBlocking() ? this.axeStack : this.godSword);
    }

    private void startWindPillar(ServerLevel level, BlockPos feetPos, double centerX, double centerZ) {
        this.setPos(centerX, this.getY(), centerZ);
        equipWeapon(this.windChargeStack);
        this.swing(InteractionHand.MAIN_HAND);
        triggerWindChargeBurst(level, centerX, this.getY() - 0.15D, centerZ);

        if (level.getBlockState(feetPos).isAir() || level.getBlockState(feetPos).canBeReplaced()) {
            level.setBlock(feetPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
            level.playSound(null, feetPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        this.setPos(centerX, feetPos.getY() + 1.0D, centerZ);
        this.setDeltaMovement(0.0D, 0.96D, 0.0D);
        this.hurtMarked = true;
        this.resetFallDistance();

        this.windPillarX = centerX;
        this.windPillarZ = centerZ;
        this.windPillarTicks = 18;
        this.windChargeCooldown = 36;
        this.pillaringTicks = 22;
        this.stuckTicks = 0;
    }

    private void handleActiveWindPillar(ServerLevel level, ServerPlayer target) {
        this.getNavigation().stop();
        this.setSprinting(false);
        this.xxa = 0.0F;
        this.zza = 0.0F;

        Vec3 curVel = this.getDeltaMovement();
        this.setPos(this.windPillarX, this.getY(), this.windPillarZ);
        this.setDeltaMovement(0.0D, curVel.y, 0.0D);
        this.hurtMarked = true;

        BlockPos feetPos = this.blockPosition();
        placePillarColumnBelow(level, feetPos, target.getBlockY());

        boolean hitCeiling = !isPassableForBody(level, feetPos.above(2));
        if (curVel.y <= 0.06D || this.getY() >= target.getY() || this.onGround() || hitCeiling || this.windPillarTicks <= 1) {
            BlockPos below = feetPos.below();
            if (!hasSolidSupport(level, below)) {
                placeCobbleBlock(level, below);
            }
            this.setPos(this.windPillarX, feetPos.getY(), this.windPillarZ);
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
            this.resetFallDistance();
            this.windPillarTicks = 0;
            this.pillaringTicks = 6;
            this.buildCooldown = 3;
        }
    }

    private void placePillarColumnBelow(ServerLevel level, BlockPos feetPos, int targetBlockY) {
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos pos = feetPos.below(depth);
            if (pos.getY() >= targetBlockY) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.canBeReplaced()) {
                placeCobbleBlock(level, pos);
            } else {
                break;
            }
        }
    }

    private void placeCobbleBlock(ServerLevel level, BlockPos pos) {
        equipWeapon(this.cobbleStack);
        this.swing(InteractionHand.MAIN_HAND);
        level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private boolean hasOpenHeadroom(ServerLevel level, BlockPos basePos, int height) {
        for (int y = 2; y <= height; y++) {
            if (!isPassableForBody(level, basePos.above(y))) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSafeGroundArea(ServerLevel level, BlockPos center) {
        int solidNeighbors = 0;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                BlockPos check = center.offset(ox, 0, oz);
                if (isHazardousDrop(level, check)) {
                    continue;
                }
                if (hasSolidSupport(level, check.below()) || hasSolidSupport(level, check.below(2))) {
                    solidNeighbors++;
                }
            }
        }
        return solidNeighbors >= 6;
    }

    private boolean isHazardousDrop(ServerLevel level, BlockPos pos) {
        for (int d = 1; d <= 5; d++) {
            BlockPos check = pos.below(d);
            FluidState fluid = level.getFluidState(check);
            if (fluid.is(FluidTags.LAVA)) {
                return true;
            }
            if (hasSolidSupport(level, check)) {
                return false;
            }
        }
        return true;
    }

    private boolean isTargetEnclosedOrHiding(ServerLevel level, ServerPlayer target) {
        BlockPos tPos = target.blockPosition();
        if (!hasOpenHeadroom(level, tPos, 5)) {
            return true;
        }
        int walledSides = 0;
        if (!isPassableForBody(level, tPos.north()) || !isPassableForBody(level, tPos.north().above())) walledSides++;
        if (!isPassableForBody(level, tPos.south()) || !isPassableForBody(level, tPos.south().above())) walledSides++;
        if (!isPassableForBody(level, tPos.east()) || !isPassableForBody(level, tPos.east().above())) walledSides++;
        if (!isPassableForBody(level, tPos.west()) || !isPassableForBody(level, tPos.west().above())) walledSides++;
        return walledSides >= 3;
    }

    private boolean shouldBridge(ServerLevel level, double dx, double dy, double dz, double horizDistSq) {
        if (horizDistSq <= 1.44D || dy < -3.0D) {
            return false;
        }
        BlockPos feet = this.blockPosition();
        if (!hasSolidSupport(level, feet.below()) && isDropAt(level, feet)) {
            return true;
        }
        if (isDropAhead(level, feet, dx, dz)) {
            return true;
        }
        return this.bridgingTicks > 0 && isDropAheadWithin(level, feet, dx, dz, 2);
    }

    private void handleBridging(ServerLevel level, ServerPlayer target, double dx, double dy, double dz, double horizDistSq) {
        this.getNavigation().stop();
        this.setSprinting(false);
        this.xxa = 0.0F;
        this.zza = 0.0F;
        this.bridgingTicks = 10;

        BlockPos feet = this.blockPosition();
        double horizDist = Math.sqrt(Math.max(0.0001D, horizDistSq));
        double hx = dx / horizDist;
        double hz = dz / horizDist;

        BlockPos belowFeet = feet.below();
        if (!hasSolidSupport(level, belowFeet)) {
            BlockState belowState = level.getBlockState(belowFeet);
            if (belowState.isAir() || belowState.canBeReplaced()) {
                placeCobbleBlock(level, belowFeet);
                this.setDeltaMovement(0.0D, Math.max(0.0D, this.getDeltaMovement().y), 0.0D);
                this.buildCooldown = 3;
                this.stuckTicks = 0;
                return;
            }
        }

        BlockPos nextStep = BlockPos.containing(feet.getX() + 0.5D + hx, feet.getY(), feet.getZ() + 0.5D + hz);
        if (nextStep.equals(feet)) {
            nextStep = Math.abs(hx) >= Math.abs(hz)
                    ? feet.offset(hx >= 0 ? 1 : -1, 0, 0)
                    : feet.offset(0, 0, hz >= 0 ? 1 : -1);
        }

        if (!isPassableForBody(level, nextStep.above()) && isBreakableObstacle(level, nextStep.above())) {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            handleMining(level, nextStep.above());
            return;
        }
        boolean canStepUpNext = isPassableForBody(level, nextStep.above())
                && isPassableForBody(level, nextStep.above(2))
                && isPassableForBody(level, feet.above(2));
        if (!isPassableForBody(level, nextStep) && !canStepUpNext && isBreakableObstacle(level, nextStep)) {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            handleMining(level, nextStep);
            return;
        }
        clearMining(level);

        boolean isDiagonal = nextStep.getX() != feet.getX() && nextStep.getZ() != feet.getZ();
        boolean cornerReady = true;
        if (isDiagonal) {
            BlockPos cornerX = new BlockPos(nextStep.getX(), feet.getY() - 1, feet.getZ());
            BlockPos cornerZ = new BlockPos(feet.getX(), feet.getY() - 1, nextStep.getZ());
            if (!hasSolidSupport(level, cornerX) && !hasSolidSupport(level, cornerZ)) {
                cornerReady = false;
                BlockPos preferredCorner = Math.abs(hx) >= Math.abs(hz) ? cornerX : cornerZ;
                if (this.buildCooldown <= 0) {
                    BlockState cornerState = level.getBlockState(preferredCorner);
                    if (cornerState.isAir() || cornerState.canBeReplaced()) {
                        placeCobbleBlock(level, preferredCorner);
                        this.buildCooldown = 3;
                        this.stuckTicks = 0;
                        cornerReady = true;
                    }
                }
            }
        }

        BlockPos nextSupport = nextStep.below();
        boolean nextReady = hasSolidSupport(level, nextSupport) || !isPassableForBody(level, nextStep);
        if (!nextReady && this.buildCooldown <= 0) {
            BlockState supState = level.getBlockState(nextSupport);
            if (supState.isAir() || supState.canBeReplaced()) {
                placeCobbleBlock(level, nextSupport);
                this.buildCooldown = 3;
                this.stuckTicks = 0;
                nextReady = true;
            }
        }

        if (nextReady && cornerReady && dy >= 0.9D && dy < 2.2D && this.buildCooldown <= 0
                && isPassableForBody(level, nextStep) && isPassableForBody(level, nextStep.above())
                && isPassableForBody(level, nextStep.above(2)) && isPassableForBody(level, feet.above(2))) {
            BlockState stepState = level.getBlockState(nextStep);
            if (stepState.isAir() || stepState.canBeReplaced()) {
                placeCobbleBlock(level, nextStep);
                this.buildCooldown = 4;
                this.stuckTicks = 0;
            }
        }

        if (nextReady && cornerReady && this.buildCooldown <= 0) {
            BlockPos lookAhead = BlockPos.containing(nextStep.getX() + 0.5D + hx, feet.getY(), nextStep.getZ() + 0.5D + hz);
            BlockPos lookAheadSupport = lookAhead.below();
            if (!hasSolidSupport(level, lookAheadSupport) && isDropAt(level, lookAhead)) {
                BlockState laState = level.getBlockState(lookAheadSupport);
                if (laState.isAir() || laState.canBeReplaced()) {
                    placeCobbleBlock(level, lookAheadSupport);
                    this.buildCooldown = 3;
                    this.stuckTicks = 0;
                }
            }
        }

        if (!nextReady || !cornerReady) {
            double centerX = feet.getX() + 0.5D;
            double centerZ = feet.getZ() + 0.5D;
            double pullX = Mth.clamp((centerX - this.getX()) * 0.25D, -0.10D, 0.10D);
            double pullZ = Mth.clamp((centerZ - this.getZ()) * 0.25D, -0.10D, 0.10D);
            this.setDeltaMovement(pullX, this.getDeltaMovement().y, pullZ);
            this.hurtMarked = true;
            return;
        }

        double goalX = nextStep.getX() + 0.5D;
        double goalZ = nextStep.getZ() + 0.5D;
        double toNextX = goalX - this.getX();
        double toNextZ = goalZ - this.getZ();
        double toNextDist = Math.sqrt(toNextX * toNextX + toNextZ * toNextZ);
        if (toNextDist > 0.05D) {
            double bridgeSpeed = Math.min(0.24D, toNextDist);
            this.setDeltaMovement((toNextX / toNextDist) * bridgeSpeed, this.getDeltaMovement().y, (toNextZ / toNextDist) * bridgeSpeed);
            this.hurtMarked = true;
            float yaw = (float) (Mth.atan2(toNextZ, toNextX) * (double) (180F / (float) Math.PI)) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }
    }

    private static boolean hasSolidSupport(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced() && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isDropAt(ServerLevel level, BlockPos standPos) {
        BlockPos b1 = standPos.below();
        if (hasSolidSupport(level, b1)) {
            return false;
        }
        if (level.getFluidState(b1).is(FluidTags.LAVA)) {
            return true;
        }
        BlockPos b2 = standPos.below(2);
        return !hasSolidSupport(level, b2) || level.getFluidState(b2).is(FluidTags.LAVA);
    }

    private static boolean isDropAhead(ServerLevel level, BlockPos feet, double dx, double dz) {
        return isDropAheadWithin(level, feet, dx, dz, 1);
    }

    private static boolean isDropAheadWithin(ServerLevel level, BlockPos feet, double dx, double dz, int steps) {
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        if (horizDist < 0.1D) {
            return false;
        }
        double hx = dx / horizDist;
        double hz = dz / horizDist;
        for (int step = 1; step <= steps; step++) {
            BlockPos check = BlockPos.containing(feet.getX() + 0.5D + hx * step, feet.getY(), feet.getZ() + 0.5D + hz * step);
            if (!check.equals(feet) && isDropAt(level, check)) {
                return true;
            }
        }
        return false;
    }

    private Boat getOrCreateOwnBoat(ServerLevel level) {
        if (this.getVehicle() instanceof Boat currentBoat && currentBoat.isAlive()
                && this.ownBoatUUID != null && currentBoat.getUUID().equals(this.ownBoatUUID)) {
            return currentBoat;
        }
        discardOwnBoat();
        double surfaceY = findWaterSurfaceY(level, this.blockPosition());
        this.setPos(this.getX(), surfaceY, this.getZ());
        Boat boat = new Boat(level, this.getX(), surfaceY, this.getZ());
        boat.setYRot(this.getYRot());
        this.ownBoatUUID = boat.getUUID();
        level.addFreshEntity(boat);
        this.startRiding(boat, true);
        return boat;
    }

    private void destroyBlockingBoats(ServerLevel level, ServerPlayer target) {
        Entity targetVehicle = target.getVehicle();
        List<Boat> nearbyBoats = level.getEntitiesOfClass(Boat.class, this.getBoundingBox().inflate(0.85D));
        for (Boat boat : nearbyBoats) {
            if (this.ownBoatUUID != null && boat.getUUID().equals(this.ownBoatUUID)) {
                continue;
            }
            if (targetVehicle != null && boat.getUUID().equals(targetVehicle.getUUID())) {
                continue;
            }
            if (boat.getFirstPassenger() instanceof Player) {
                continue;
            }
            this.swing(InteractionHand.MAIN_HAND);
            boat.kill();
        }
    }

    private void equipWeapon(ItemStack stack) {
        ItemStack current = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!ItemStack.isSameItemSameComponents(current, stack)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
        }
    }

    private void equipCombatWeaponIfHoldingTool(ServerPlayer target) {
        ItemStack current = this.getItemBySlot(EquipmentSlot.MAINHAND);
        boolean holdingTool = ItemStack.isSameItemSameComponents(current, this.pickaxeStack)
                || ItemStack.isSameItemSameComponents(current, this.shovelStack)
                || ItemStack.isSameItemSameComponents(current, this.cobbleStack)
                || ItemStack.isSameItemSameComponents(current, this.windChargeStack)
                || (ItemStack.isSameItemSameComponents(current, this.axeStack) && !target.isBlocking());
        if (holdingTool || current.isEmpty()) {
            equipWeapon(target.isBlocking() ? this.axeStack : this.godSword);
        }
    }

    static List<Vec3> buildPearlCandidateVelocities(Vec3 startPos, Vec3 targetAim) {
        double dx = targetAim.x - startPos.x;
        double dy = targetAim.y - startPos.y;
        double dz = targetAim.z - startPos.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        List<Vec3> candidates = new ArrayList<>(3);

        double ballisticLift = Mth.clamp(0.0065D * horizDist * horizDist, 0.0D, horizDist * 0.35D);
        addNormalizedPearlVelocity(candidates, dx, dy + ballisticLift, dz);

        double mediumLift = horizDist * 0.18D;
        if (Math.abs(mediumLift - ballisticLift) > 0.35D) {
            addNormalizedPearlVelocity(candidates, dx, dy + mediumLift, dz);
        }

        if (dy > 2.0D || horizDist > 10.0D) {
            double highLift = (dy > 2.0D || horizDist > 30.0D) ? horizDist * 0.32D : horizDist * 0.24D;
            if (Math.abs(highLift - ballisticLift) > 0.35D && Math.abs(highLift - mediumLift) > 0.35D) {
                addNormalizedPearlVelocity(candidates, dx, dy + highLift, dz);
            }
        }
        return candidates;
    }

    private static void addNormalizedPearlVelocity(List<Vec3> candidates, double vx, double vy, double vz) {
        Vec3 dir = new Vec3(vx, vy, vz);
        if (dir.lengthSqr() > 1.0E-6D) {
            candidates.add(dir.normalize().scale(PEARL_LAUNCH_SPEED));
        }
    }

    static double requiredPearlProgress(double startDist) {
        return Math.min(8.0D, Math.max(4.0D, startDist * 0.25D));
    }

    static PearlCandidate simulatePearlTrajectory(Vec3 startPos, Vec3 launchVelocity, Vec3 selfPos, Vec3 targetPos,
            int minSafeY, PearlRayCollider collider, PearlLandingResolver landingResolver) {
        double startDist = selfPos.distanceTo(targetPos);
        if (startDist < PEARL_MIN_TRAVEL_DIST) {
            return null;
        }

        Vec3 pos = startPos;
        Vec3 vel = launchVelocity;
        for (int tick = 1; tick <= PEARL_MAX_SIM_TICKS; tick++) {
            vel = new Vec3(vel.x, vel.y - PEARL_GRAVITY, vel.z).scale(PEARL_AIR_DRAG);
            Vec3 nextPos = pos.add(vel);
            PearlHit hit = collider.clip(pos, nextPos);
            if (hit != null) {
                double travelDist = startPos.distanceTo(hit.location());
                if (hit.hitTargetEntity()) {
                    if (travelDist < PEARL_MIN_TRAVEL_DIST - 0.5D) {
                        return null;
                    }
                    double progress = startDist - hit.location().distanceTo(targetPos);
                    if (progress < Math.min(4.0D, startDist * 0.5D)) {
                        return null;
                    }
                    return new PearlCandidate(launchVelocity, hit.location(), travelDist, progress);
                }
                if (tick < PEARL_MIN_FLIGHT_TICKS || travelDist < PEARL_MIN_TRAVEL_DIST) {
                    return null;
                }
                Vec3 landingPos = landingResolver.resolveSafeLanding(hit.location(), hit.face(), hit.blockPos());
                if (landingPos == null) {
                    return null;
                }
                double progress = startDist - landingPos.distanceTo(targetPos);
                if (progress < requiredPearlProgress(startDist)) {
                    return null;
                }
                return new PearlCandidate(launchVelocity, landingPos, travelDist, progress);
            }
            if (tick <= PEARL_MIN_FLIGHT_TICKS) {
                PearlHit upperHit = collider.clip(pos.add(0.0D, 0.25D, 0.0D), nextPos.add(0.0D, 0.25D, 0.0D));
                if (upperHit != null && !upperHit.hitTargetEntity()) {
                    return null;
                }
            }
            if (nextPos.y < minSafeY) {
                return null;
            }
            pos = nextPos;
        }
        return null;
    }

    static PearlCandidate selectBestPearlCandidate(Vec3 startPos, Vec3 selfPos, Vec3 targetPos, Vec3 targetAim,
            int minSafeY, PearlRayCollider collider, PearlLandingResolver landingResolver) {
        PearlCandidate best = null;
        for (Vec3 candidateVel : buildPearlCandidateVelocities(startPos, targetAim)) {
            PearlCandidate result = simulatePearlTrajectory(
                    startPos,
                    candidateVel,
                    selfPos,
                    targetPos,
                    minSafeY,
                    collider,
                    landingResolver
            );
            if (result != null && (best == null || result.progress() > best.progress())) {
                best = result;
            }
        }
        return best;
    }

    static LivePearlPrediction simulateLivePearlImpact(Vec3 startPos, Vec3 startVel, int minSafeY,
            PearlRayCollider collider, PearlLandingResolver landingResolver) {
        Vec3 pos = startPos;
        Vec3 vel = startVel;
        for (int tick = 1; tick <= PEARL_RECOVERY_MAX_SIM_TICKS; tick++) {
            vel = new Vec3(vel.x, vel.y - PEARL_GRAVITY, vel.z).scale(PEARL_AIR_DRAG);
            Vec3 nextPos = pos.add(vel);
            PearlHit hit = collider.clip(pos, nextPos);
            if (hit != null) {
                if (hit.hitTargetEntity()) {
                    return new LivePearlPrediction(hit.location(), hit.location(), tick, false, true);
                }
                Vec3 landingPos = landingResolver.resolveSafeLanding(hit.location(), hit.face(), hit.blockPos());
                boolean unsafe = landingPos == null;
                Vec3 effectivePos = landingPos != null ? landingPos : hit.location();
                return new LivePearlPrediction(hit.location(), effectivePos, tick, unsafe, false);
            }
            if (nextPos.y < minSafeY) {
                return new LivePearlPrediction(nextPos, nextPos, tick, true, false);
            }
            pos = nextPos;
        }
        return new LivePearlPrediction(pos, pos, PEARL_RECOVERY_MAX_SIM_TICKS + 1, true, false);
    }

    static boolean isMissedMountainPearl(LivePearlPrediction prediction, Vec3 selfPos, Vec3 targetPos, double referenceY) {
        if (prediction == null || prediction.hitTargetEntity()) {
            return false;
        }
        Vec3 landing = prediction.landingPos();
        double dropBelowRef = referenceY - landing.y;
        if (dropBelowRef >= PEARL_MISS_DROP_THRESHOLD) {
            return true;
        }
        if (prediction.unsafeOrVoid()) {
            return true;
        }
        double currentTargetDist = selfPos.distanceTo(targetPos);
        double predictedTargetDist = landing.distanceTo(targetPos);
        return dropBelowRef >= PEARL_MISS_FAR_DROP_THRESHOLD
                && predictedTargetDist > currentTargetDist + PEARL_MISS_FAR_DIST_MARGIN;
    }

    static int estimateVerticalFlightTicks(double startY, double goalY, double vy) {
        double y = startY;
        double curVy = vy;
        boolean clearedGoal = y >= goalY;
        for (int tick = 1; tick <= PEARL_RECOVERY_MAX_SIM_TICKS; tick++) {
            curVy = (curVy - PEARL_GRAVITY) * PEARL_AIR_DRAG;
            y += curVy;
            if (y >= goalY + 0.5D) {
                clearedGoal = true;
            }
            if (clearedGoal && curVy <= 0.0D && y <= goalY) {
                return tick;
            }
        }
        return -1;
    }

    static double horizontalDragSum(int ticks) {
        return (PEARL_AIR_DRAG / (1.0D - PEARL_AIR_DRAG)) * (1.0D - Math.pow(PEARL_AIR_DRAG, ticks));
    }

    static List<Vec3> buildRecoveryPearlCandidateVelocities(Vec3 startPos, Vec3 anchorPos, Vec3 targetPos) {
        List<Vec3> candidates = new ArrayList<>(16);
        double[] verticalSpeeds = new double[]{0.65D, 0.85D, 1.10D, 1.35D, 1.65D};
        for (double vy : verticalSpeeds) {
            candidates.add(new Vec3(0.0D, vy, 0.0D));
        }

        double[] targetedSpeeds = new double[]{0.65D, 0.85D, 1.10D, 1.35D, 1.55D};
        boolean hasSeparateAnchor = anchorPos != null && anchorPos.distanceToSqr(targetPos) > 0.5625D;
        Vec3 midpoint = hasSeparateAnchor && anchorPos.distanceToSqr(targetPos) > 36.0D
                ? anchorPos.lerp(targetPos, 0.5D)
                : null;

        for (double vy : targetedSpeeds) {
            if (targetPos != null) {
                addTargetedRecoveryVelocity(candidates, startPos, targetPos, vy);
            }
            if (hasSeparateAnchor) {
                addTargetedRecoveryVelocity(candidates, startPos, anchorPos, vy);
            }
            if (midpoint != null) {
                addTargetedRecoveryVelocity(candidates, startPos, midpoint, vy);
            }
        }
        return candidates;
    }

    private static void addTargetedRecoveryVelocity(List<Vec3> candidates, Vec3 startPos, Vec3 goalPos, double vy) {
        double dx = goalPos.x - startPos.x;
        double dz = goalPos.z - startPos.z;
        double horizDistSq = dx * dx + dz * dz;
        if (horizDistSq <= 0.5625D) {
            return;
        }
        int estTicks = estimateVerticalFlightTicks(startPos.y, goalPos.y, vy);
        if (estTicks < PEARL_MIN_FLIGHT_TICKS) {
            return;
        }
        double dragSum = horizontalDragSum(estTicks);
        if (dragSum <= 1.0E-6D) {
            return;
        }
        double vx = dx / dragSum;
        double vz = dz / dragSum;
        double horizSpeed = Math.sqrt(vx * vx + vz * vz);
        if (vy < horizSpeed * 0.75D) {
            return;
        }
        double totalSpeedSq = vx * vx + vy * vy + vz * vz;
        if (totalSpeedSq > PEARL_LAUNCH_SPEED * PEARL_LAUNCH_SPEED) {
            return;
        }
        candidates.add(new Vec3(vx, vy, vz));
    }

    static RecoveryPearlCandidate simulateRecoveryPearlTrajectory(Vec3 startPos, Vec3 launchVelocity,
            Vec3 targetPos, double referenceY, int minRequiredFlightTicks, int minSafeY,
            PearlRayCollider collider, PearlLandingResolver landingResolver) {
        double minFloorY = referenceY - PEARL_RECOVERY_MAX_DROP_BELOW_REF - 1.0D;
        int maxValidTick = estimateVerticalFlightTicks(startPos.y, minFloorY, launchVelocity.y);
        if (maxValidTick < 0 || maxValidTick < minRequiredFlightTicks) {
            return null;
        }
        Vec3 pos = startPos;
        Vec3 vel = launchVelocity;
        for (int tick = 1; tick <= maxValidTick; tick++) {
            vel = new Vec3(vel.x, vel.y - PEARL_GRAVITY, vel.z).scale(PEARL_AIR_DRAG);
            Vec3 nextPos = pos.add(vel);
            PearlHit hit = collider.clip(pos, nextPos);
            if (hit != null) {
                if (tick < minRequiredFlightTicks) {
                    return null;
                }
                if (!hit.hitTargetEntity() && (hit.face() == Direction.DOWN || vel.y > 0.15D)) {
                    return null;
                }
                Vec3 landingPos = hit.hitTargetEntity()
                        ? hit.location()
                        : landingResolver.resolveSafeLanding(hit.location(), hit.face(), hit.blockPos());
                if (landingPos == null) {
                    return null;
                }
                if (landingPos.y < referenceY - PEARL_RECOVERY_MAX_DROP_BELOW_REF) {
                    return null;
                }
                double distToTarget = landingPos.distanceTo(targetPos);
                double heightBonus = Math.min(4.0D, landingPos.y - referenceY) * 2.0D;
                double waitExcess = Math.max(0, tick - minRequiredFlightTicks);
                double score = 100.0D - distToTarget * 1.25D + heightBonus - waitExcess * 0.45D;
                return new RecoveryPearlCandidate(launchVelocity, landingPos, tick, score);
            }
            if (tick <= PEARL_MIN_FLIGHT_TICKS) {
                PearlHit upperHit = collider.clip(pos.add(0.0D, 0.25D, 0.0D), nextPos.add(0.0D, 0.25D, 0.0D));
                if (upperHit != null && !upperHit.hitTargetEntity()) {
                    return null;
                }
            }
            if (nextPos.y < minSafeY || (vel.y <= 0.0D && nextPos.y < minFloorY)) {
                return null;
            }
            pos = nextPos;
        }
        return null;
    }

    static RecoveryPearlCandidate selectRecoveryPearlCandidate(Vec3 startPos, Vec3 anchorPos, Vec3 targetPos,
            double referenceY, int minRequiredFlightTicks, int minSafeY,
            PearlRayCollider collider, PearlLandingResolver landingResolver) {
        if (minRequiredFlightTicks > PEARL_RECOVERY_MAX_SIM_TICKS) {
            return null;
        }
        RecoveryPearlCandidate best = null;
        for (Vec3 candidateVel : buildRecoveryPearlCandidateVelocities(startPos, anchorPos, targetPos)) {
            RecoveryPearlCandidate result = simulateRecoveryPearlTrajectory(
                    startPos,
                    candidateVel,
                    targetPos,
                    referenceY,
                    minRequiredFlightTicks,
                    minSafeY,
                    collider,
                    landingResolver
            );
            if (result != null && (best == null || result.score() > best.score())) {
                best = result;
            }
        }
        return best;
    }

    static RecoveryPearlCandidate evaluatePearlBackRecovery(Vec3 trackedPearlPos, Vec3 trackedPearlVel,
            Vec3 launchStartPos, Vec3 selfPos, Vec3 anchorPos, Vec3 targetPos, double referenceY,
            int minSafeY, PearlRayCollider livePearlCollider, PearlRayCollider recoveryCollider,
            PearlLandingResolver landingResolver) {
        LivePearlPrediction prediction = simulateLivePearlImpact(
                trackedPearlPos,
                trackedPearlVel,
                minSafeY,
                livePearlCollider,
                landingResolver
        );
        if (!isMissedMountainPearl(prediction, selfPos, targetPos, referenceY)) {
            return null;
        }
        int minRequiredFlightTicks = Math.max(
                PEARL_MIN_FLIGHT_TICKS,
                prediction.remainingTicks() + PEARL_RECOVERY_MIN_MARGIN_TICKS
        );
        return selectRecoveryPearlCandidate(
                launchStartPos,
                anchorPos,
                targetPos,
                referenceY,
                minRequiredFlightTicks,
                minSafeY,
                recoveryCollider,
                landingResolver
        );
    }

    private void clearTrackedOffensivePearl() {
        this.trackedOffensivePearlUUID = null;
        this.trackedPearlRecoveryThrown = false;
        this.trackedPearlAnchorPos = null;
    }

    private boolean hasActiveRecoveryPearl(ServerLevel level) {
        if (this.activeRecoveryPearlUUID == null) {
            return false;
        }
        Entity entity = level.getEntity(this.activeRecoveryPearlUUID);
        if (entity instanceof ThrownEnderpearl pearl && pearl.isAlive() && !pearl.isRemoved()) {
            return true;
        }
        this.activeRecoveryPearlUUID = null;
        return false;
    }

    private boolean handleTrackedPearlRecovery(ServerLevel level, ServerPlayer target) {
        if (this.trackedOffensivePearlUUID == null) {
            return false;
        }
        Entity entity = level.getEntity(this.trackedOffensivePearlUUID);
        if (!(entity instanceof ThrownEnderpearl pearl) || !pearl.isAlive() || pearl.isRemoved()) {
            clearTrackedOffensivePearl();
            return false;
        }
        if (this.trackedPearlRecoveryThrown || hasActiveRecoveryPearl(level)) {
            return false;
        }
        if (this.isUnderWater() || this.isInLava()) {
            return false;
        }

        Vec3 anchorPos = this.trackedPearlAnchorPos != null ? this.trackedPearlAnchorPos : this.position();
        double referenceY = Math.min(anchorPos.y, target.getY());
        Vec3 launchStartPos = new Vec3(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        int minSafeY = level.getMinBuildHeight() + 2;
        PearlRayCollider liveCollider = createPearlCollider(level, target);
        PearlRayCollider blockOnlyCollider = createBlockOnlyPearlCollider(level);
        PearlLandingResolver landingResolver = (impactPos, hitFace, hitBlockPos) ->
                resolveSafePearlLanding(level, hitFace, hitBlockPos);

        RecoveryPearlCandidate recovery = evaluatePearlBackRecovery(
                pearl.position(),
                pearl.getDeltaMovement(),
                launchStartPos,
                this.position(),
                anchorPos,
                target.position(),
                referenceY,
                minSafeY,
                liveCollider,
                blockOnlyCollider,
                landingResolver
        );
        if (recovery == null) {
            return false;
        }

        setSwimmingPose(false);
        this.bridgingTicks = 0;
        clearMining(level);
        ThrownEnderpearl recoveryPearl = throwEnderPearlAt(level, recovery.launchVelocity());
        this.trackedPearlRecoveryThrown = true;
        this.activeRecoveryPearlUUID = recoveryPearl.getUUID();
        this.pearlCooldown = Math.max(this.pearlCooldown, 90);
        this.fallRecoveryCooldown = Math.max(this.fallRecoveryCooldown, 70);
        this.stuckTicks = 0;
        this.pillaringTicks = 0;
        return true;
    }

    private boolean handleMountainFallRecovery(ServerLevel level, ServerPlayer target) {
        if (this.fallRecoveryCooldown > 0
                || this.maceState != MaceState.NONE
                || this.windPillarTicks > 0
                || this.voidRecoveryFlight
                || this.getSharedFlag(7)
                || this.isInWater()
                || this.isUnderWater()
                || this.isInLava()
                || this.trackedOffensivePearlUUID != null
                || hasActiveRecoveryPearl(level)) {
            return false;
        }
        if (this.onGround()
                || this.getDeltaMovement().y >= -0.35D
                || this.fallDistance < 3.0F
                || this.getY() >= target.getY() - 5.0D) {
            return false;
        }
        if (!target.onGround() && !hasSolidGroundBelow(level, target.blockPosition())) {
            return false;
        }

        Vec3 launchStartPos = new Vec3(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        Vec3 targetPos = target.position();
        int minSafeY = level.getMinBuildHeight() + 2;
        PearlRayCollider blockOnlyCollider = createBlockOnlyPearlCollider(level);
        PearlLandingResolver landingResolver = (impactPos, hitFace, hitBlockPos) ->
                resolveSafePearlLanding(level, hitFace, hitBlockPos);

        RecoveryPearlCandidate recovery = selectRecoveryPearlCandidate(
                launchStartPos,
                targetPos,
                targetPos,
                targetPos.y,
                PEARL_MIN_FLIGHT_TICKS,
                minSafeY,
                blockOnlyCollider,
                landingResolver
        );
        if (recovery == null) {
            this.fallRecoveryCooldown = 12;
            return false;
        }

        setSwimmingPose(false);
        this.bridgingTicks = 0;
        clearMining(level);
        ThrownEnderpearl recoveryPearl = throwEnderPearlAt(level, recovery.launchVelocity());
        this.activeRecoveryPearlUUID = recoveryPearl.getUUID();
        this.fallRecoveryCooldown = 70;
        this.pearlCooldown = Math.max(this.pearlCooldown, 60);
        this.stuckTicks = 0;
        this.pillaringTicks = 0;
        return true;
    }

    private PearlRayCollider createPearlCollider(ServerLevel level, ServerPlayer target) {
        AABB targetBox = target.getBoundingBox().inflate(0.35D);
        return (from, to) -> {
            BlockHitResult blockHit = level.clip(new ClipContext(
                    from,
                    to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));
            Vec3 end = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : to;
            Optional<Vec3> entityHit = targetBox.clip(from, end);
            if (entityHit.isPresent()) {
                return new PearlHit(entityHit.get(), Direction.UP, BlockPos.containing(entityHit.get()), true);
            }
            if (blockHit.getType() != HitResult.Type.MISS) {
                return new PearlHit(blockHit.getLocation(), blockHit.getDirection(), blockHit.getBlockPos(), false);
            }
            return null;
        };
    }

    private PearlRayCollider createBlockOnlyPearlCollider(ServerLevel level) {
        return (from, to) -> {
            BlockHitResult blockHit = level.clip(new ClipContext(
                    from,
                    to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));
            if (blockHit.getType() != HitResult.Type.MISS) {
                return new PearlHit(blockHit.getLocation(), blockHit.getDirection(), blockHit.getBlockPos(), false);
            }
            return null;
        };
    }

    private PearlCandidate findViablePearlCandidate(ServerLevel level, ServerPlayer target) {
        Vec3 startPos = new Vec3(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        Vec3 selfPos = this.position();
        Vec3 targetPos = target.position();
        Vec3 targetAim = new Vec3(target.getX(), target.getY(0.5D), target.getZ());
        int minSafeY = level.getMinBuildHeight() + 2;
        PearlRayCollider collider = createPearlCollider(level, target);
        PearlLandingResolver landingResolver = (impactPos, hitFace, hitBlockPos) ->
                resolveSafePearlLanding(level, hitFace, hitBlockPos);

        return selectBestPearlCandidate(startPos, selfPos, targetPos, targetAim, minSafeY, collider, landingResolver);
    }

    private Vec3 resolveSafePearlLanding(ServerLevel level, Direction hitFace, BlockPos hitBlockPos) {
        BlockPos standPos;
        if (hitFace == Direction.UP) {
            standPos = hitBlockPos.above();
        } else if (hitFace == Direction.DOWN) {
            BlockPos belowCeiling = hitBlockPos.below();
            standPos = isPassableForBody(level, belowCeiling.below()) ? belowCeiling.below() : belowCeiling;
        } else {
            standPos = hitBlockPos.relative(hitFace);
        }

        if (standPos.getY() <= level.getMinBuildHeight() + 1) {
            return null;
        }
        if (level.getFluidState(standPos).is(FluidTags.LAVA)
                || level.getFluidState(standPos.above()).is(FluidTags.LAVA)) {
            return null;
        }
        if (!isPassableForBody(level, standPos) || !isPassableForBody(level, standPos.above())) {
            return null;
        }

        if (hasSolidSupport(level, standPos.below()) || level.getFluidState(standPos.below()).is(FluidTags.WATER)) {
            if (level.getFluidState(standPos.below()).is(FluidTags.LAVA)) {
                return null;
            }
            return new Vec3(standPos.getX() + 0.5D, standPos.getY(), standPos.getZ() + 0.5D);
        }

        for (int drop = 1; drop <= 12; drop++) {
            BlockPos belowPos = standPos.below(drop);
            if (belowPos.getY() <= level.getMinBuildHeight() + 1) {
                return null;
            }
            if (level.getFluidState(belowPos).is(FluidTags.LAVA)) {
                return null;
            }
            if (hasSolidSupport(level, belowPos) || level.getFluidState(belowPos).is(FluidTags.WATER)) {
                BlockPos landed = belowPos.above();
                if (level.getFluidState(landed).is(FluidTags.LAVA)
                        || !isPassableForBody(level, landed)
                        || !isPassableForBody(level, landed.above())) {
                    return null;
                }
                return new Vec3(landed.getX() + 0.5D, landed.getY(), landed.getZ() + 0.5D);
            }
        }
        return null;
    }

    private ThrownEnderpearl throwEnderPearlAt(ServerLevel level, Vec3 launchVelocity) {
        this.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDER_PEARL_THROW, SoundSource.HOSTILE, 1.0F, 0.8F);

        ThrownEnderpearl pearl = new ThrownEnderpearl(level, this);
        pearl.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        float speed = (float) Math.min(PEARL_LAUNCH_SPEED, Math.max(0.05D, launchVelocity.length()));
        pearl.shoot(launchVelocity.x, launchVelocity.y, launchVelocity.z, speed, 0.0F);
        level.addFreshEntity(pearl);
        return pearl;
    }

    private BlockPos findBlockingBlock(ServerLevel level, ServerPlayer target) {
        BlockPos feet = this.blockPosition();
        BlockPos head = feet.above();
        BlockPos supportBelow = feet.below();

        if (isBreakableObstacle(level, head)) {
            return head;
        }
        if (isBreakableBodyObstacle(level, feet)) {
            return feet;
        }

        double dx = target.getX() - this.getX();
        double dy = target.getY() - this.getY();
        double dz = target.getZ() - this.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        if (horizDist > 0.2D) {
            double stepX = dx / horizDist;
            double stepZ = dz / horizDist;
            BlockPos frontFeet = BlockPos.containing(this.getX() + stepX * 0.9D, this.getY(), this.getZ() + stepZ * 0.9D);
            if (frontFeet.equals(feet)) {
                frontFeet = BlockPos.containing(feet.getX() + 0.5D + stepX, feet.getY(), feet.getZ() + 0.5D + stepZ);
            }
            boolean allowStepUp = this.stuckTicks < 20;

            BlockPos primary = findPassThroughObstacle(level, feet, head, supportBelow, frontFeet, frontFeet.above(), allowStepUp);
            if (primary != null) {
                return primary;
            }

            if (frontFeet.getX() != feet.getX() && frontFeet.getZ() != feet.getZ()) {
                BlockPos cornerXFeet = new BlockPos(frontFeet.getX(), feet.getY(), feet.getZ());
                BlockPos cornerZFeet = new BlockPos(feet.getX(), feet.getY(), frontFeet.getZ());
                boolean cornerXPassable = isPassableForBody(level, cornerXFeet) && isPassableForBody(level, cornerXFeet.above());
                boolean cornerZPassable = isPassableForBody(level, cornerZFeet) && isPassableForBody(level, cornerZFeet.above());
                if ((!cornerXPassable && !cornerZPassable)
                        || ((!cornerXPassable || !cornerZPassable) && (this.horizontalCollision || this.stuckTicks >= 10))) {
                    BlockPos preferredCorner = Math.abs(stepX) >= Math.abs(stepZ) ? cornerXFeet : cornerZFeet;
                    BlockPos fallbackCorner = preferredCorner.equals(cornerXFeet) ? cornerZFeet : cornerXFeet;
                    BlockPos cornerObstacle = findPassThroughObstacle(
                            level,
                            feet,
                            head,
                            supportBelow,
                            preferredCorner,
                            preferredCorner.above(),
                            allowStepUp
                    );
                    if (cornerObstacle != null) {
                        return cornerObstacle;
                    }
                    cornerObstacle = findPassThroughObstacle(
                            level,
                            feet,
                            head,
                            supportBelow,
                            fallbackCorner,
                            fallbackCorner.above(),
                            allowStepUp
                    );
                    if (cornerObstacle != null) {
                        return cornerObstacle;
                    }
                }
            }
        }

        if (dy > 1.5D && (horizDist < 4.0D || (this.stuckTicks >= 20 && !this.hasLineOfSight(target)))) {
            BlockPos above = head.above();
            if (isBreakableObstacle(level, above)) {
                return above;
            }
        } else if (dy < -1.8D && (horizDist < 2.2D || (this.stuckTicks >= 20 && horizDist < 4.5D)) && !this.hasLineOfSight(target)
                && this.bridgingTicks <= 0 && this.pillaringTicks <= 0 && this.windPillarTicks <= 0) {
            if (isBreakableObstacle(level, supportBelow) && isSafeFloorToDigDown(level, supportBelow, target)) {
                return supportBelow;
            }
        }

        return null;
    }

    private BlockPos findPassThroughObstacle(ServerLevel level, BlockPos feet, BlockPos head, BlockPos supportBelow,
            BlockPos targetFeet, BlockPos targetHead, boolean allowStepUp) {
        if (!targetHead.equals(supportBelow) && isBreakableObstacle(level, targetHead)) {
            return targetHead;
        }
        if (!targetFeet.equals(supportBelow) && !targetFeet.equals(feet) && isBreakableObstacle(level, targetFeet)) {
            boolean canStepUp = allowStepUp
                    && isPassableForBody(level, targetFeet.above())
                    && isPassableForBody(level, targetFeet.above(2))
                    && isPassableForBody(level, head.above());
            if (!canStepUp) {
                return targetFeet;
            }
        }
        return null;
    }

    private boolean isPassableForBody(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.getCollisionShape(level, pos).isEmpty();
    }

    private boolean isBreakableBodyObstacle(ServerLevel level, BlockPos pos) {
        if (!isBreakableObstacle(level, pos)) {
            return false;
        }
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return false;
        }
        double topY = pos.getY() + shape.max(Direction.Axis.Y);
        return topY > this.getY() + 0.55D;
    }

    private boolean isSafeFloorToDigDown(ServerLevel level, BlockPos floorPos, ServerPlayer target) {
        int solidNeighbors = 0;
        if (hasSolidSupport(level, floorPos.north())) solidNeighbors++;
        if (hasSolidSupport(level, floorPos.south())) solidNeighbors++;
        if (hasSolidSupport(level, floorPos.east())) solidNeighbors++;
        if (hasSolidSupport(level, floorPos.west())) solidNeighbors++;
        if (solidNeighbors < 2) {
            return false;
        }
        int minSafeY = Math.max(level.getMinBuildHeight() + 2, target.getBlockY() - 4);
        for (int y = floorPos.getY() - 1; y >= minSafeY; y--) {
            BlockPos check = new BlockPos(floorPos.getX(), y, floorPos.getZ());
            if (level.getFluidState(check).is(FluidTags.LAVA)) {
                return false;
            }
            if (hasSolidSupport(level, check)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBreakableObstacle(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced() || state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        float destroySpeed = state.getDestroySpeed(level, pos);
        return destroySpeed >= 0.0F && destroySpeed < 100.0F;
    }

    private void handleMining(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ItemStack bestTool = getBestToolForBlock(state);
        equipWeapon(bestTool);
        this.swing(InteractionHand.MAIN_HAND);

        if (!pos.equals(this.miningPos)) {
            if (this.miningPos != null) {
                level.destroyBlockProgress(this.getId(), this.miningPos, -1);
            }
            this.miningPos = pos.immutable();
            this.miningTicks = 0;
        }

        this.miningTicks++;
        float hardness = Math.max(0.1F, state.getDestroySpeed(level, pos));
        int requiredTicks = Math.max(2, Math.min(20, (int) (hardness * 1.5F)));
        int progressStage = Math.min(9, (this.miningTicks * 10) / requiredTicks);
        level.destroyBlockProgress(this.getId(), this.miningPos, progressStage);

        if (this.miningTicks >= requiredTicks) {
            level.destroyBlockProgress(this.getId(), this.miningPos, -1);
            level.destroyBlock(this.miningPos, true, this);
            this.miningPos = null;
            this.miningTicks = 0;
        }
    }

    private ItemStack getBestToolForBlock(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return this.axeStack;
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return this.shovelStack;
        }
        return this.pickaxeStack;
    }

    public static void scheduleSpawn(UUID targetUUID, UUID skinUUID, String skinName) {
        synchronized (PENDING_SPAWNS) {
            PENDING_SPAWNS.add(new PendingSpawn(targetUUID, skinUUID, skinName, SPAWN_DELAY_TICKS));
        }
    }

    public static boolean scheduleHunt(ServerPlayer owner, ServerPlayer target, UUID skinUuid, String skinName) {
        if (target == null || target.getServer() == null) {
            return false;
        }
        if (isTargetAlreadyHunted(target.getServer(), target.getUUID())) {
            return false;
        }
        scheduleSpawn(target.getUUID(), skinUuid, skinName);
        return true;
    }

    public static void scheduleScholarCurseSpawn(UUID targetUUID, String scholarSkin, boolean ultraRareCurse) {
        synchronized (PENDING_SPAWNS) {
            PENDING_SPAWNS.add(new PendingSpawn(targetUUID, null, "", scholarSkin, ultraRareCurse, SPAWN_DELAY_TICKS));
        }
    }

    public static void scheduleOscNuke(ResourceKey<Level> dimension, Vec3 pos) {
        if (!ModServerConfig.ENABLE_CREEPING_DEATH_NUKE.get()) {
            return;
        }
        synchronized (PENDING_OSC_NUKES) {
            PENDING_OSC_NUKES.add(new PendingOscNuke(dimension, pos, OSC_NUKE_DELAY_TICKS));
        }
    }

    public static void tickServerQueues(MinecraftServer server) {
        tickServer(server);
    }

    public static void clearStaticState() {
        ACTIVE_HUNTS_BY_TARGET.clear();
        synchronized (PENDING_SPAWNS) {
            PENDING_SPAWNS.clear();
        }
        synchronized (PENDING_OSC_NUKES) {
            PENDING_OSC_NUKES.clear();
        }
    }

    public static void tickServer(MinecraftServer server) {
        if (!ACTIVE_HUNTS_BY_TARGET.isEmpty()) {
            ACTIVE_HUNTS_BY_TARGET.entrySet().removeIf(entry -> {
                UUID targetId = entry.getKey();
                UUID assassinId = entry.getValue();
                for (ServerLevel lvl : server.getAllLevels()) {
                    Entity e = lvl.getEntity(assassinId);
                    if (e instanceof CreepingDeathEntity assassin && assassin.isAlive() && !assassin.isDeadOrDying() && !assassin.isRemoved()) {
                        ServerPlayer target = server.getPlayerList().getPlayer(targetId);
                        if (target != null && target.isAlive() && target.level() != assassin.level()) {
                            assassin.teleportToTargetDimension(target.serverLevel(), target);
                        }
                        return false;
                    }
                }
                return true;
            });
        }

        synchronized (PENDING_SPAWNS) {
            Iterator<PendingSpawn> it = PENDING_SPAWNS.iterator();
            while (it.hasNext()) {
                PendingSpawn pending = it.next();
                ServerPlayer target = server.getPlayerList().getPlayer(pending.targetUUID);
                if (target != null && PersistentDataAccess.get(target).getBoolean("bodycam_active")) {
                    BodycamResetCameraPacket.executeReset(target);
                    sendHuntNoEscapeTitle(target);
                }
                if (target == null || !target.isAlive() || target.isCreative() || target.isSpectator()) {
                    it.remove();
                    continue;
                }
                if (pending.ticksRemaining > 0) {
                    if (pending.ultraRareCurse && pending.ticksRemaining == 130) {
                        scheduleOscNuke(target.serverLevel().dimension(), target.position());
                    }
                    if (pending.ticksRemaining % 20 == 0) {
                        target.serverLevel().playSound(
                                null,
                                target.getX(),
                                target.getY(),
                                target.getZ(),
                                ModSounds.CREEPING_DEATH_HEARTBEAT.get(),
                                SoundSource.HOSTILE,
                                2.0F,
                                0.9F
                        );
                    }
                    pending.ticksRemaining--;
                    continue;
                }

                BlockPos spawnPos = findBehindTargetSpawn(target.serverLevel(), target);
                if (spawnPos != null) {
                    spawnAssassinAt(target.serverLevel(), target, pending.skinUUID, pending.skinName,
                            pending.scholarSkin, pending.ultraRareCurse,
                            spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
                    it.remove();
                } else {
                    pending.retryTicks--;
                    if (pending.retryTicks <= 0) {
                        Vec3 ringFallback = getRingFallbackPos(target);
                        spawnAssassinAt(target.serverLevel(), target, pending.skinUUID, pending.skinName,
                                pending.scholarSkin, pending.ultraRareCurse,
                                ringFallback.x, ringFallback.y, ringFallback.z);
                        it.remove();
                    }
                }
            }
        }

        synchronized (PENDING_OSC_NUKES) {
            Iterator<PendingOscNuke> it = PENDING_OSC_NUKES.iterator();
            while (it.hasNext()) {
                PendingOscNuke nuke = it.next();
                ServerLevel level = server.getLevel(nuke.dimension);
                if (level == null) {
                    it.remove();
                    continue;
                }
                if (nuke.ticksRemaining == OSC_NUKE_DELAY_TICKS) {
                    level.playSound(null, nuke.pos.x, nuke.pos.y, nuke.pos.z,
                            ModSounds.CREEPING_DEATH_NUKE.get(), SoundSource.HOSTILE, 5.0F, 0.65F);
                }
                if (nuke.ticksRemaining > 0) {
                    for (int h = 0; h < 60; h += 3) {
                        level.sendParticles(ParticleTypes.END_ROD,
                                nuke.pos.x, nuke.pos.y + h, nuke.pos.z,
                                2, 0.1D, 0.5D, 0.1D, 0.01D);
                    }
                    level.sendParticles(ParticleTypes.SCULK_SOUL,
                            nuke.pos.x, nuke.pos.y + 0.5D, nuke.pos.z,
                            8, 1.2D, 0.2D, 1.2D, 0.02D);
                    nuke.ticksRemaining--;
                    continue;
                }

                executeKubikmeterOscNuke(level, nuke.pos);
                it.remove();
            }
        }
    }

    private static Vec3 getRingFallbackPos(ServerPlayer target) {
        Vec3 look = target.getLookAngle();
        Vec3 horizBack = new Vec3(-look.x, 0, -look.z);
        if (horizBack.lengthSqr() < 0.001D) {
            horizBack = new Vec3(0, 0, -1);
        } else {
            horizBack = horizBack.normalize();
        }
        return target.position().add(horizBack.scale(10.0D));
    }

    private static BlockPos findBehindTargetSpawn(ServerLevel level, ServerPlayer target) {
        Vec3 look = target.getLookAngle();
        Vec3 horizBack = new Vec3(-look.x, 0, -look.z);
        if (horizBack.lengthSqr() < 0.001D) {
            horizBack = new Vec3(0, 0, -1);
        } else {
            horizBack = horizBack.normalize();
        }

        double baseAngle = Math.atan2(horizBack.z, horizBack.x);
        double[] radii = new double[]{10.0D, 9.0D, 11.0D};

        for (double radius : radii) {
            for (int step = 0; step <= 12; step++) {
                int[] signs = (step == 0 || step == 12) ? new int[]{1} : new int[]{1, -1};
                for (int sign : signs) {
                    double angle = baseAngle + sign * (step * (Math.PI / 12.0D));
                    double rx = target.getX() + Math.cos(angle) * radius;
                    double rz = target.getZ() + Math.sin(angle) * radius;
                    BlockPos ringBase = BlockPos.containing(rx, target.getY(), rz);

                    for (int dy = 5; dy >= -6; dy--) {
                        BlockPos check = ringBase.offset(0, dy, 0);
                        if (isValidSpawnSpace(level, check)) {
                            return check;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isValidSpawnSpace(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.isAir() || below.getCollisionShape(level, pos.below()).isEmpty()) {
            return false;
        }
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    private static void spawnAssassinAt(ServerLevel level, ServerPlayer target, UUID skinUUID, String skinName,
            String scholarSkin, boolean ultraRareCurse, double x, double y, double z) {
        CreepingDeathEntity assassin = new CreepingDeathEntity(ModEntityTypes.CREEPING_DEATH.get(), level);
        Vec3 toTarget = target.position().subtract(x, y, z);
        float yaw = (float) (Mth.atan2(toTarget.z, toTarget.x) * (double) (180F / (float) Math.PI)) - 90.0F;
        assassin.moveTo(x, y, z, yaw, 0.0F);
        assassin.setYHeadRot(yaw);
        assassin.setYBodyRot(yaw);
        assassin.initializeAssassin(target.getUUID(), skinUUID, skinName, scholarSkin, ultraRareCurse);
        level.addFreshEntity(assassin);
        assassin.updateForcedChunk(level);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1.0D, z, 30, 0.4D, 0.8D, 0.4D, 0.02D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, y + 1.0D, z, 20, 0.4D, 0.8D, 0.4D, 0.03D);
    }

    public static void executeKubikmeterOscNuke(ServerLevel level, Vec3 center) {
        double dropHeight = center.y + 28.0D;
        int fuseTicks = 18;

        for (int i = 0; i < 8; i++) {
            PrimedTnt coreTnt = new PrimedTnt(level, center.x, dropHeight, center.z, null);
            coreTnt.setFuse(fuseTicks);
            coreTnt.setDeltaMovement(0.0D, -1.55D, 0.0D);
            level.addFreshEntity(coreTnt);
        }

        int innerRingCount = 16;
        for (int i = 0; i < innerRingCount; i++) {
            double angle = (2.0D * Math.PI * i) / innerRingCount;
            double vx = Math.cos(angle) * 0.42D;
            double vz = Math.sin(angle) * 0.42D;
            PrimedTnt ringTnt = new PrimedTnt(level, center.x + Math.cos(angle) * 1.5D, dropHeight, center.z + Math.sin(angle) * 1.5D, null);
            ringTnt.setFuse(fuseTicks);
            ringTnt.setDeltaMovement(vx, -1.55D, vz);
            level.addFreshEntity(ringTnt);
        }

        int outerRingCount = 32;
        for (int i = 0; i < outerRingCount; i++) {
            double angle = (2.0D * Math.PI * i) / outerRingCount;
            double vx = Math.cos(angle) * 0.88D;
            double vz = Math.sin(angle) * 0.88D;
            PrimedTnt ringTnt = new PrimedTnt(level, center.x + Math.cos(angle) * 2.5D, dropHeight, center.z + Math.sin(angle) * 2.5D, null);
            ringTnt.setFuse(fuseTicks);
            ringTnt.setDeltaMovement(vx, -1.55D, vz);
            level.addFreshEntity(ringTnt);
        }

        level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 2.0D, center.z, 5, 1.0D, 1.0D, 1.0D, 0.0D);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0F, 0.5F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (getTargetUUID() != null) {
            tag.putUUID("TargetUUID", getTargetUUID());
        }
        if (getSkinUUID() != null) {
            tag.putUUID("SkinUUID", getSkinUUID());
        }
        tag.putString("SkinName", getSkinName());
        tag.putString("ScholarSkin", getScholarSkin());
        tag.putBoolean("UltraRareCurse", isUltraRareCurse());
        tag.putBoolean("MissionAccomplished", this.missionAccomplished);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetUUID")) {
            UUID tId = tag.getUUID("TargetUUID");
            this.entityData.set(TARGET_UUID, Optional.of(tId));
            ACTIVE_HUNTS_BY_TARGET.put(tId, this.getUUID());
        }
        if (tag.hasUUID("SkinUUID")) {
            this.entityData.set(SKIN_UUID, Optional.of(tag.getUUID("SkinUUID")));
        }
        if (tag.contains("SkinName")) {
            this.entityData.set(SKIN_NAME, tag.getString("SkinName"));
        }
        if (tag.contains("ScholarSkin")) {
            this.entityData.set(SCHOLAR_SKIN, tag.getString("ScholarSkin"));
        }
        if (tag.contains("UltraRareCurse")) {
            this.entityData.set(ULTRA_RARE_CURSE, tag.getBoolean("UltraRareCurse"));
        }
        this.missionAccomplished = tag.getBoolean("MissionAccomplished");
        this.setupEquipment(this.level().registryAccess());
    }
}
