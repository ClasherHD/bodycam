package dev.ClasherHD.bodycam.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class BodycamDummyEntity extends LivingEntity {
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData
            .defineId(BodycamDummyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(BodycamDummyEntity.class,
            EntityDataSerializers.STRING);

    public static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> DUMMY_POS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<java.util.UUID, Float> DUMMY_FALL = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> DUMMY_MOTION = new java.util.concurrent.ConcurrentHashMap<>();

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
    }

    private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);

    public BodycamDummyEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
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
            net.minecraft.server.MinecraftServer server = this.getServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (player != null) {
                    net.minecraft.world.entity.Entity camera = player.getCamera();

                    player.teleportTo((net.minecraft.server.level.ServerLevel) this.level(), this.getX(),
                            this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                    player.teleportTo((net.minecraft.server.level.ServerLevel) this.level(), this.getX(),
                            this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

                    boolean success = player.hurt(source, amount);

                    if (player.isDeadOrDying()) {
                        player.getPersistentData().putBoolean("bodycam_active", false);
                        this.discard();
                        return true;
                    } else {
                        super.hurt(source, 0.0F);
                        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
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
            net.minecraft.server.MinecraftServer server = this.getServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (player != null) {
                    net.minecraft.world.entity.Entity camera = player.getCamera();
                    net.minecraft.server.level.ServerLevel camLevel = (net.minecraft.server.level.ServerLevel) camera
                            .level();
                    net.minecraft.server.level.ServerLevel dummyLevel = (net.minecraft.server.level.ServerLevel) this
                            .level();
                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    player.teleportTo(dummyLevel, this.getX(), this.getY(),
                            this.getZ(), this.getYRot(), this.getXRot());
                    player.teleportTo(dummyLevel, this.getX(), this.getY(),
                            this.getZ(), this.getYRot(), this.getXRot());
                    player.heal(amount);
                    player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
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
    public boolean addEffect(net.minecraft.world.effect.MobEffectInstance effectInstance,
            @javax.annotation.Nullable net.minecraft.world.entity.Entity entity) {
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            net.minecraft.server.MinecraftServer server = this.getServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (player != null) {
                    net.minecraft.world.entity.Entity camera = player.getCamera();
                    net.minecraft.server.level.ServerLevel camLevel = (net.minecraft.server.level.ServerLevel) camera
                            .level();
                    net.minecraft.server.level.ServerLevel dummyLevel = (net.minecraft.server.level.ServerLevel) this
                            .level();

                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    player.teleportTo(dummyLevel, this.getX(), this.getY(),
                            this.getZ(), this.getYRot(), this.getXRot());
                    player.teleportTo(dummyLevel, this.getX(), this.getY(),
                            this.getZ(), this.getYRot(), this.getXRot());
                    boolean success = player.addEffect(effectInstance, entity);
                    player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
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
            net.minecraft.server.level.ServerPlayer owner = this.level().getServer().getPlayerList()
                    .getPlayer(this.getOwnerUUID());
            if (owner == null || !owner.getPersistentData().getBoolean("bodycam_active")
                    || !this.getUUID().equals(owner.getPersistentData().getUUID("bodycam_dummy_uuid"))) {
                this.discard();
                return;
            }

            DUMMY_POS.put(this.getOwnerUUID(), this.position());
            DUMMY_FALL.put(this.getOwnerUUID(), this.fallDistance);
            DUMMY_MOTION.put(this.getOwnerUUID(), this.getDeltaMovement());

            this.setHealth(owner.getHealth());
            this.setAbsorptionAmount(owner.getAbsorptionAmount());

            java.util.List<net.minecraft.world.entity.Mob> mobs = this.level().getEntitiesOfClass(
                    net.minecraft.world.entity.Mob.class,
                    this.getBoundingBox().inflate(16.0D),
                    e -> e instanceof net.minecraft.world.entity.monster.Enemy);
            for (net.minecraft.world.entity.Mob mob : mobs) {
                if (mob.getTarget() == null) {
                    mob.setTarget(this);
                }
            }
        }
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance pPotions) {
        return true;
    }
}
