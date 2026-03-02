package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.util.PersistentData;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BodycamDummyEntity extends LivingEntity {
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData
            .defineId(BodycamDummyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(BodycamDummyEntity.class,
            EntityDataSerializers.STRING);

    public static final Map<UUID, Vec3> DUMMY_POS = new ConcurrentHashMap<>();
    public static final Map<UUID, Float> DUMMY_FALL = new ConcurrentHashMap<>();
    public static final Map<UUID, Vec3> DUMMY_MOTION = new ConcurrentHashMap<>();

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH)
                .add(Attributes.ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE)
                .add(Attributes.ATTACK_DAMAGE);
    }

    private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);

    public BodycamDummyEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = this.getBoundingBox().getSize() * 64.0D;
        if (Double.isNaN(d0)) {
            d0 = 4.0D;
        }
        d0 *= 64.0D * getViewScale();
        return distance < d0 * d0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(OWNER_NAME, "");
    }

    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return this.armorItems;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        switch (slot.getType()) {
            case HAND:
                return this.handItems.get(slot.getIndex());
            case ARMOR:
                return this.armorItems.get(slot.getIndex());
            default:
                return ItemStack.EMPTY;
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        this.verifyEquippedItem(stack);
        switch (slot.getType()) {
            case HAND:
                this.onEquipItem(slot, this.handItems.set(slot.getIndex(), stack), stack);
                break;
            case ARMOR:
                this.onEquipItem(slot, this.armorItems.set(slot.getIndex(), stack), stack);
                break;
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            MinecraftServer server = this.getServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (player != null) {
                    Entity camera = player.getCamera();

                    player.teleportTo((ServerLevel) this.level(), this.getX(),
                            this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                    player.teleportTo((ServerLevel) this.level(), this.getX(),
                            this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                    player.setGameMode(GameType.SURVIVAL);

                    boolean success = player.hurt(source, amount);

                    if (player.isDeadOrDying()) {
                        ((PersistentData) player).bodycam$getPersistentData().putBoolean("bodycam_active", false);
                        this.discard();
                        return true;
                    } else {
                        super.hurt(source, 0.0F);
                        player.setGameMode(GameType.SPECTATOR);
                        if (player.getCamera() != camera) {
                            player.setCamera(camera);
                        }
                        this.setHealth(player.getHealth());
                        return success;
                    }
                }
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void heal(float amount) {
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            MinecraftServer server = this.getServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (player != null) {
                    Entity camera = player.getCamera();
                    ServerLevel camLevel = (ServerLevel) camera.level();
                    ServerLevel dummyLevel = (ServerLevel) this.level();
                    player.setGameMode(GameType.SURVIVAL);
                    player.teleportTo(dummyLevel, this.getX(), this.getY(),
                            this.getZ(), this.getYRot(), this.getXRot());
                    player.teleportTo(dummyLevel, this.getX(), this.getY(),
                            this.getZ(), this.getYRot(), this.getXRot());
                    player.heal(amount);
                    player.setGameMode(GameType.SPECTATOR);
                    player.teleportTo(camLevel, camera.getX(),
                            camera.getY(),
                            camera.getZ(), camera.getYRot(), camera.getXRot());
                    player.teleportTo(camLevel, camera.getX(),
                            camera.getY(),
                            camera.getZ(), camera.getYRot(), camera.getXRot());
                    player.setCamera(camera);
                    super.heal(amount);
                    this.setHealth(player.getHealth());
                    return;
                }
            }
        }
        super.heal(amount);
    }

    @Override
    public boolean addEffect(MobEffectInstance effectInstance, @Nullable Entity entity) {
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            MinecraftServer server = this.getServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (player != null) {
                    Entity camera = player.getCamera();
                    ServerLevel camLevel = (ServerLevel) camera.level();
                    ServerLevel dummyLevel = (ServerLevel) this.level();

                    player.setGameMode(GameType.SURVIVAL);
                    player.teleportTo(dummyLevel, this.getX(), this.getY(),
                            this.getZ(), this.getYRot(), this.getXRot());
                    player.teleportTo(dummyLevel, this.getX(), this.getY(),
                            this.getZ(), this.getYRot(), this.getXRot());
                    boolean success = player.addEffect(effectInstance, entity);
                    player.setGameMode(GameType.SPECTATOR);
                    player.teleportTo(camLevel, camera.getX(),
                            camera.getY(),
                            camera.getZ(), camera.getYRot(), camera.getXRot());
                    player.teleportTo(camLevel, camera.getX(),
                            camera.getY(),
                            camera.getZ(), camera.getYRot(), camera.getXRot());
                    player.setCamera(camera);
                    return success;
                }
            }
        }
        return super.addEffect(effectInstance, entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.getOwnerUUID() == null) {
                this.discard();
                return;
            }
            ServerPlayer owner = this.level().getServer().getPlayerList()
                    .getPlayer(this.getOwnerUUID());
            if (owner == null || !((PersistentData) owner).bodycam$getPersistentData().getBoolean("bodycam_active")
                    || !this.getUUID().equals(
                            ((PersistentData) owner).bodycam$getPersistentData().getUUID("bodycam_dummy_uuid"))) {
                this.discard();
                return;
            }

            DUMMY_POS.put(this.getOwnerUUID(), this.position());
            DUMMY_FALL.put(this.getOwnerUUID(), this.fallDistance);
            DUMMY_MOTION.put(this.getOwnerUUID(), this.getDeltaMovement());

            this.setHealth(owner.getHealth());
            this.setAbsorptionAmount(owner.getAbsorptionAmount());

            List<Mob> mobs = this.level().getEntitiesOfClass(
                    Mob.class,
                    this.getBoundingBox().inflate(16.0D),
                    e -> e instanceof Enemy);
            for (Mob mob : mobs) {
                if (mob.getTarget() == null) {
                    mob.setTarget(this);
                }
            }
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance pPotions) {
        return true;
    }
}
