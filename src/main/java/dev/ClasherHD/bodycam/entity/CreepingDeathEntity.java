package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
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
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    private ItemStack godSword = ItemStack.EMPTY;
    private ItemStack pickaxeStack = ItemStack.EMPTY;
    private ItemStack axeStack = ItemStack.EMPTY;
    private ItemStack shovelStack = ItemStack.EMPTY;
    private ItemStack cobbleStack = ItemStack.EMPTY;
    private ItemStack elytraStack = ItemStack.EMPTY;
    private ItemStack trimmedChestStack = ItemStack.EMPTY;

    private int attackCooldown = 0;
    private int pearlCooldown = 60;
    private int buildCooldown = 0;
    private int pillaringTicks = 0;
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
        this.setMaxUpStep(1.25F);
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
                .add(Attributes.FOLLOW_RANGE, 256.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN_UUID, Optional.empty());
        this.entityData.define(SKIN_NAME, "");
        this.entityData.define(TARGET_UUID, Optional.empty());
        this.entityData.define(SCHOLAR_SKIN, "");
        this.entityData.define(ULTRA_RARE_CURSE, false);
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

    public void setupEquipment(RegistryAccess registryAccess) {
        this.godSword = new ItemStack(Items.NETHERITE_SWORD);
        this.godSword.enchant(Enchantments.SHARPNESS, 5);
        this.godSword.enchant(Enchantments.UNBREAKING, 3);
        this.godSword.enchant(Enchantments.MENDING, 1);
        this.godSword.enchant(Enchantments.SWEEPING_EDGE, 3);
        this.godSword.enchant(Enchantments.MOB_LOOTING, 3);
        this.godSword.enchant(Enchantments.FIRE_ASPECT, 2);

        this.pickaxeStack = new ItemStack(Items.NETHERITE_PICKAXE);
        this.pickaxeStack.enchant(Enchantments.BLOCK_EFFICIENCY, 10);
        this.pickaxeStack.enchant(Enchantments.UNBREAKING, 3);

        this.axeStack = new ItemStack(Items.NETHERITE_AXE);
        this.axeStack.enchant(Enchantments.BLOCK_EFFICIENCY, 10);
        this.axeStack.enchant(Enchantments.UNBREAKING, 3);

        this.shovelStack = new ItemStack(Items.NETHERITE_SHOVEL);
        this.shovelStack.enchant(Enchantments.BLOCK_EFFICIENCY, 10);
        this.shovelStack.enchant(Enchantments.UNBREAKING, 3);

        this.cobbleStack = new ItemStack(Items.COBBLESTONE, 64);
        this.elytraStack = new ItemStack(Items.ELYTRA);

        ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
        ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
        ItemStack legs = new ItemStack(Items.NETHERITE_LEGGINGS);
        ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
        boots.enchant(Enchantments.DEPTH_STRIDER, 3);

        try {
            Optional<Registry<TrimMaterial>> matRegOpt = registryAccess.registry(Registries.TRIM_MATERIAL);
            Optional<Registry<TrimPattern>> patRegOpt = registryAccess.registry(Registries.TRIM_PATTERN);
            if (matRegOpt.isPresent() && patRegOpt.isPresent()) {
                Optional<Holder.Reference<TrimMaterial>> matHolder = matRegOpt.get().getHolder(
                        ResourceKey.create(Registries.TRIM_MATERIAL, new ResourceLocation(bodycam.MODID, "observation_crystal")));
                Optional<Holder.Reference<TrimPattern>> patHolder = patRegOpt.get().getHolder(TrimPatterns.EYE);
                if (matHolder.isPresent() && patHolder.isPresent()) {
                    ArmorTrim.setTrim(registryAccess, chest, new ArmorTrim(matHolder.get(), patHolder.get()));
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

    public static boolean isTargetAlreadyHunted(net.minecraft.server.MinecraftServer server, UUID targetUUID) {
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
        long last = player.getPersistentData().getLong("bodycam_cd_title_cd");
        if (now - last < 40L && now >= last) {
            return;
        }
        player.getPersistentData().putLong("bodycam_cd_title_cd", now);
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
        if (player == null || player.server == null) {
            return false;
        }
        if (!isTargetAlreadyHunted(player.server, player.getUUID())) {
            return false;
        }
        if (player.getPersistentData().getBoolean("bodycam_active")) {
            BodycamResetCameraPacket.executeReset(player);
        }
        player.getPersistentData().remove("bodycam_all_seeing_pending");
        PacketHandler.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new BodycamForceClosePacket());
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
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void handleInsidePortal(BlockPos pos) {
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
            currentLevel.setChunkForced(cx, cz, true);
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
                lvl.setChunkForced(this.forcedChunkX, this.forcedChunkZ, false);
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

        CreepingDeathEntity transferred = new CreepingDeathEntity(
                dev.ClasherHD.bodycam.registry.ModEntityTypes.CREEPING_DEATH.get(), targetLevel);
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
                    dev.ClasherHD.bodycam.util.ModAdvancements.award(killer, dev.ClasherHD.bodycam.util.ModAdvancements.SURVIVE_CREEPING_DEATH);
                }
                if (targetId != null && this.level() instanceof ServerLevel sLevel) {
                    ServerPlayer hunted = sLevel.getServer().getPlayerList().getPlayer(targetId);
                    if (hunted != null && hunted.isAlive()) {
                        dev.ClasherHD.bodycam.util.ModAdvancements.award(hunted, dev.ClasherHD.bodycam.util.ModAdvancements.SURVIVE_CREEPING_DEATH);
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

        if (this.godSword.isEmpty()) {
            this.setupEquipment(this.level().registryAccess());
        }

        if (this.tickCount >= MAX_LIFETIME_TICKS) {
            if (!this.missionAccomplished && !this.nukeScheduled) {
                this.nukeScheduled = true;
                UUID survivedTargetId = getTargetUUID();
                if (survivedTargetId != null) {
                    ServerPlayer survivedPlayer = sLevel.getServer().getPlayerList().getPlayer(survivedTargetId);
                    if (survivedPlayer != null && survivedPlayer.isAlive()) {
                        dev.ClasherHD.bodycam.util.ModAdvancements.award(survivedPlayer, dev.ClasherHD.bodycam.util.ModAdvancements.SURVIVE_CREEPING_DEATH);
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

        if (target.getPersistentData().getBoolean("bodycam_active")) {
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
        if (this.buildCooldown > 0) this.buildCooldown--;
        if (this.pillaringTicks > 0) this.pillaringTicks--;

        if (this.tickCount % 10 == 0) {
            if (this.pillaringTicks <= 0 && !this.voidRecoveryFlight && this.position().distanceToSqr(this.lastCheckPos) < 0.25D) {
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
            discardOwnBoat();
            setSwimmingPose(false);
            this.getNavigation().stop();
            if (this.miningPos != null) {
                sLevel.destroyBlockProgress(this.getId(), this.miningPos, -1);
                this.miningPos = null;
                this.miningTicks = 0;
            }
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
            discardOwnBoat();
            setSwimmingPose(false);
            this.getNavigation().stop();
            if (this.miningPos != null) {
                sLevel.destroyBlockProgress(this.getId(), this.miningPos, -1);
                this.miningPos = null;
                this.miningTicks = 0;
            }
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

        if (totalDistSq <= 25.0D && this.miningPos == null && this.pillaringTicks <= 0) {
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

        if (this.pearlCooldown <= 0 && (horizDistSq > 196.0D || (dy > 5.0D && horizDistSq > 36.0D) || this.stuckTicks >= 40)) {
            setSwimmingPose(false);
            throwEnderPearlAt(sLevel, target);
            this.pearlCooldown = 90;
            this.stuckTicks = 0;
            this.pillaringTicks = 0;
            return;
        }

        if (this.isInWater()) {
            this.pillaringTicks = 0;
            this.getNavigation().stop();

            BlockPos waterObstacle = findBlockingBlock(sLevel, target);
            if (waterObstacle != null) {
                handleMining(sLevel, waterObstacle);
            } else if (this.miningPos != null) {
                sLevel.destroyBlockProgress(this.getId(), this.miningPos, -1);
                this.miningPos = null;
                this.miningTicks = 0;
                equipWeapon(target.isBlocking() ? this.axeStack : this.godSword);
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

        boolean shouldPillar = dy >= 2.0D && horizDistSq < 64.0D && (this.onGround() || this.pillaringTicks > 0);
        if (shouldPillar) {
            this.getNavigation().stop();
            this.setSprinting(false);
            this.xxa = 0.0F;
            this.zza = 0.0F;

            BlockPos feetPos = this.blockPosition();
            double centerX = feetPos.getX() + 0.5D;
            double centerZ = feetPos.getZ() + 0.5D;
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);

            BlockPos aboveHead = feetPos.above(2);
            if (!sLevel.getBlockState(aboveHead).isAir() && !sLevel.getBlockState(aboveHead).canBeReplaced()) {
                this.setPos(centerX, this.getY(), centerZ);
                this.pillaringTicks = 10;
                handleMining(sLevel, aboveHead);
                return;
            }
            if (this.miningPos != null) {
                sLevel.destroyBlockProgress(this.getId(), this.miningPos, -1);
                this.miningPos = null;
                this.miningTicks = 0;
            }

            if (this.buildCooldown <= 0 && this.onGround()) {
                if (sLevel.getBlockState(feetPos).isAir() || sLevel.getBlockState(feetPos).canBeReplaced()) {
                    equipWeapon(this.cobbleStack);
                    this.swing(InteractionHand.MAIN_HAND);
                    sLevel.setBlock(feetPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    sLevel.playSound(null, feetPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    this.setPos(centerX, feetPos.getY() + 1.0D, centerZ);
                    this.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    this.buildCooldown = 7;
                    this.pillaringTicks = 12;
                }
            } else {
                this.setPos(centerX, this.getY(), centerZ);
            }
            return;
        }

        this.getNavigation().moveTo(target, 1.0D);
        this.setSprinting(true);

        BlockPos obstacle = findBlockingBlock(sLevel, target);
        if (obstacle != null) {
            handleMining(sLevel, obstacle);
            return;
        } else if (this.miningPos != null) {
            sLevel.destroyBlockProgress(this.getId(), this.miningPos, -1);
            this.miningPos = null;
            this.miningTicks = 0;
            equipWeapon(this.godSword);
        }

        if (this.buildCooldown <= 0 && horizDistSq > 9.0D && dy >= -1.0D) {
            Vec3 horizDir = new Vec3(dx, 0, dz).normalize();
            BlockPos aheadPos = BlockPos.containing(this.getX() + horizDir.x * 1.1D, this.getY(), this.getZ() + horizDir.z * 1.1D);
            BlockPos belowAhead = aheadPos.below();
            BlockState belowState = sLevel.getBlockState(belowAhead);
            if (belowState.isAir() || belowState.canBeReplaced()) {
                equipWeapon(this.cobbleStack);
                this.swing(InteractionHand.MAIN_HAND);
                sLevel.setBlock(belowAhead, Blocks.COBBLESTONE.defaultBlockState(), 3);
                sLevel.playSound(null, belowAhead, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.buildCooldown = 6;
            }
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
            target.disableShield(true);
        }
        this.attackCooldown = 12;
        if (!target.isAlive() || target.isDeadOrDying()) {
            onTargetKilled();
        }
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
        if (!ItemStack.isSameItemSameTags(current, stack)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
        }
    }

    private void throwEnderPearlAt(ServerLevel level, LivingEntity target) {
        this.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDER_PEARL_THROW, SoundSource.HOSTILE, 1.0F, 0.8F);

        ThrownEnderpearl pearl = new ThrownEnderpearl(level, this);
        pearl.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5D) - pearl.getY();
        double dz = target.getZ() - this.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        pearl.shoot(dx, dy + horizDist * 0.18D, dz, 1.65F, 1.0F);
        level.addFreshEntity(pearl);
    }

    private BlockPos findBlockingBlock(ServerLevel level, ServerPlayer target) {
        BlockPos feet = this.blockPosition();
        BlockPos head = feet.above();
        if (isBreakableObstacle(level, head)) {
            return head;
        }
        if (isBreakableObstacle(level, feet)) {
            return feet;
        }

        double dx = target.getX() - this.getX();
        double dy = target.getY() - this.getY();
        double dz = target.getZ() - this.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        if (horizDist > 0.2D) {
            double stepX = dx / horizDist;
            double stepZ = dz / horizDist;
            BlockPos frontHead = BlockPos.containing(this.getX() + stepX * 0.9D, this.getY() + 1.0D, this.getZ() + stepZ * 0.9D);
            BlockPos frontFeet = BlockPos.containing(this.getX() + stepX * 0.9D, this.getY(), this.getZ() + stepZ * 0.9D);
            if (isBreakableObstacle(level, frontHead)) {
                return frontHead;
            }
            if (isBreakableObstacle(level, frontFeet)) {
                return frontFeet;
            }
        }

        if (dy > 1.5D && horizDist < 4.0D) {
            BlockPos above = head.above();
            if (isBreakableObstacle(level, above)) {
                return above;
            }
        } else if (dy < -1.5D && horizDist < 4.0D) {
            BlockPos below = feet.below();
            if (isBreakableObstacle(level, below)) {
                return below;
            }
        }

        return null;
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
            equipWeapon(this.godSword);
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

    public static void scheduleScholarCurseSpawn(UUID targetUUID, String scholarSkin, boolean ultraRareCurse) {
        synchronized (PENDING_SPAWNS) {
            PENDING_SPAWNS.add(new PendingSpawn(targetUUID, null, "", scholarSkin, ultraRareCurse, SPAWN_DELAY_TICKS));
        }
    }

    public static void scheduleOscNuke(ResourceKey<Level> dimension, Vec3 pos) {
        synchronized (PENDING_OSC_NUKES) {
            PENDING_OSC_NUKES.add(new PendingOscNuke(dimension, pos, OSC_NUKE_DELAY_TICKS));
        }
    }

    public static void tickServer(net.minecraft.server.MinecraftServer server) {
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
                if (target != null && target.getPersistentData().getBoolean("bodycam_active")) {
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
        CreepingDeathEntity assassin = new CreepingDeathEntity(
                dev.ClasherHD.bodycam.registry.ModEntityTypes.CREEPING_DEATH.get(), level);
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
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 6.0F, 0.5F);
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
