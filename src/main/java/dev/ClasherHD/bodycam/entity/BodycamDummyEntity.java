package dev.ClasherHD.bodycam.entity;

import net.minecraft.core.NonNullList;
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

    public static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> DUMMY_POS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<java.util.UUID, Float> DUMMY_FALL = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> DUMMY_MOTION = new java.util.concurrent.ConcurrentHashMap<>();

    private int currentLoadedChunkX = Integer.MAX_VALUE;
    private int currentLoadedChunkZ = Integer.MAX_VALUE;
    private boolean isChunkForced = false;

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.LivingEntity.createLivingAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 20.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, 0.0D);
    }

    private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);

    public BodycamDummyEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (!this.level().isClientSide() && this.isChunkForced && this.level() instanceof net.minecraft.server.level.ServerLevel) {
            this.isChunkForced = false;
        }
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            net.minecraft.server.MinecraftServer server = this.getServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer owner = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (owner != null && dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(owner).getBoolean("bodycam_active")) {
                    dev.ClasherHD.bodycam.network.BodycamResetCameraPacket.executeReset(owner);
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(owner, dev.ClasherHD.bodycam.network.BodycamPacketIDs.RESET_CAMERA_PACKET_ID, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
                }
            }
        }
        super.die(source);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
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

    private void damageArmorItems(DamageSource source, float damage) {
        if (damage <= 0.0F) return;
        damage /= 4.0F;
        if (damage < 1.0F) damage = 1.0F;
        for (int i = 0; i < this.armorItems.size(); i++) {
            net.minecraft.world.item.ItemStack stack = this.armorItems.get(i);
            if (stack.isEmpty()) continue;
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && stack.getItem().isFireResistant()) continue;
            int finalI = i;
            stack.hurtAndBreak((int) damage, this, (entity) -> {
                entity.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.byTypeAndIndex(
                        net.minecraft.world.entity.EquipmentSlot.Type.ARMOR, finalI));
            });
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            net.minecraft.server.MinecraftServer server = this.getServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (player != null) {
                    boolean result = super.hurt(source, amount);
                    damageArmorItems(source, amount);
                    for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                        if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                            net.minecraft.world.item.ItemStack dummyPiece = this.getItemBySlot(slot);
                            net.minecraft.world.item.ItemStack playerPiece = player.getItemBySlot(slot);
                            if (!playerPiece.isEmpty() && !dummyPiece.isEmpty()) {
                                playerPiece.setDamageValue(dummyPiece.getDamageValue());
                            } else if (!playerPiece.isEmpty() && dummyPiece.isEmpty()) {
                                playerPiece.setDamageValue(playerPiece.getMaxDamage());
                            }
                        }
                    }
                    if (this.isDeadOrDying()) {
                        player.getCombatTracker().recordDamage(source, amount);
                        player.setHealth(0);
                        player.die(source);
                        return result;
                    }
                    player.setHealth(this.getHealth());
                    player.setAbsorptionAmount(this.getAbsorptionAmount());
                    return result;
                }
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void heal(float amount) {
        super.heal(amount);
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            net.minecraft.server.MinecraftServer server = this.getServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (player != null) {
                    player.setHealth(this.getHealth());
                    player.setAbsorptionAmount(this.getAbsorptionAmount());
                }
            }
        }
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
            if (owner == null || !dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(owner).getBoolean("bodycam_active")
                    || !this.getUUID().equals(dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(owner).getUUID("bodycam_dummy_uuid"))) {
                this.discard();
                return;
            }

            int currentChunkX = this.blockPosition().getX() >> 4;
            int currentChunkZ = this.blockPosition().getZ() >> 4;

            if (currentChunkX != this.currentLoadedChunkX || currentChunkZ != this.currentLoadedChunkZ) {
                this.currentLoadedChunkX = currentChunkX;
                this.currentLoadedChunkZ = currentChunkZ;
                this.isChunkForced = true;
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
