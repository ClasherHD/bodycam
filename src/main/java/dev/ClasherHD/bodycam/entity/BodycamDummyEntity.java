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

@SuppressWarnings("null")
public class BodycamDummyEntity extends LivingEntity {
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData
            .defineId(BodycamDummyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(BodycamDummyEntity.class,
            EntityDataSerializers.STRING);

    public static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> DUMMY_POS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<java.util.UUID, Float> DUMMY_FALL = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> DUMMY_MOTION = new java.util.concurrent.ConcurrentHashMap<>();

    private int currentLoadedChunkX = Integer.MAX_VALUE;
    private int currentLoadedChunkZ = Integer.MAX_VALUE;
    private boolean isChunkForced = false;

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
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (!this.level().isClientSide()) {
            if (this.isChunkForced && this.level() instanceof net.minecraft.server.level.ServerLevel) {
                net.minecraftforge.common.world.ForgeChunkManager.forceChunk((net.minecraft.server.level.ServerLevel) this.level(), "bodycam", this, this.currentLoadedChunkX, this.currentLoadedChunkZ, false, false);
                this.isChunkForced = false;
            }
            java.util.List<net.minecraft.world.entity.Mob> mobs = this.level().getEntitiesOfClass(
                    net.minecraft.world.entity.Mob.class,
                    this.getBoundingBox().inflate(32.0D),
                    m -> m.getTarget() == this);
            for (net.minecraft.world.entity.Mob mob : mobs) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            net.minecraft.server.MinecraftServer server = this.getServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer owner = server.getPlayerList().getPlayer(this.getOwnerUUID());
                if (owner != null && owner.getPersistentData().getBoolean("bodycam_active")) {
                    dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket.executeReset(owner);
                    dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> owner),
                            new dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket()
                    );
                }
            }
        }
        super.die(source);
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
    protected void hurtArmor(DamageSource source, float damage) {
        if (damage <= 0.0F) return;
        damage /= 4.0F;
        if (damage < 1.0F) damage = 1.0F;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                net.minecraft.world.item.ItemStack stack = this.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && stack.getItem().isFireResistant()) continue;
                    if (stack.isDamageableItem()) {
                        int newDamage = stack.getDamageValue() + (int) damage;
                        if (newDamage >= stack.getMaxDamage()) {
                            this.setItemSlot(slot, ItemStack.EMPTY);
                        } else {
                            stack.setDamageValue(newDamage);
                            this.setItemSlot(slot, stack);
                        }
                    }
                }
            }
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
                    for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                        if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                            net.minecraft.world.item.ItemStack dummyPiece = this.getItemBySlot(slot);
                            net.minecraft.world.item.ItemStack playerPiece = player.getItemBySlot(slot);
                            if (!playerPiece.isEmpty() && !dummyPiece.isEmpty()) {
                                playerPiece.setDamageValue(dummyPiece.getDamageValue());
                                player.setItemSlot(slot, playerPiece);
                            } else if (!playerPiece.isEmpty() && dummyPiece.isEmpty()) {
                                player.setItemSlot(slot, ItemStack.EMPTY);
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
    public boolean addEffect(net.minecraft.world.effect.MobEffectInstance effectInstance,
            @javax.annotation.Nullable net.minecraft.world.entity.Entity entity) {
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



            int currentChunkX = this.blockPosition().getX() >> 4;
            int currentChunkZ = this.blockPosition().getZ() >> 4;

            if (currentChunkX != this.currentLoadedChunkX || currentChunkZ != this.currentLoadedChunkZ) {
                if (this.isChunkForced && this.currentLoadedChunkX != Integer.MAX_VALUE && this.currentLoadedChunkZ != Integer.MAX_VALUE) {
                    net.minecraftforge.common.world.ForgeChunkManager.forceChunk((net.minecraft.server.level.ServerLevel) this.level(), "bodycam", this, this.currentLoadedChunkX, this.currentLoadedChunkZ, false, false);
                }
                net.minecraftforge.common.world.ForgeChunkManager.forceChunk((net.minecraft.server.level.ServerLevel) this.level(), "bodycam", this, currentChunkX, currentChunkZ, true, true);
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwnerUUID() != null) {
            tag.putUUID("OwnerUUID", this.getOwnerUUID());
        }
        tag.putString("OwnerName", this.entityData.get(OWNER_NAME));

        net.minecraft.nbt.ListTag armorList = new net.minecraft.nbt.ListTag();
        for (ItemStack stack : this.armorItems) {
            CompoundTag itemTag = new CompoundTag();
            stack.save(itemTag);
            armorList.add(itemTag);
        }
        tag.put("ArmorItems", armorList);

        net.minecraft.nbt.ListTag handList = new net.minecraft.nbt.ListTag();
        for (ItemStack stack : this.handItems) {
            CompoundTag itemTag = new CompoundTag();
            stack.save(itemTag);
            handList.add(itemTag);
        }
        tag.put("HandItems", handList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
        if (tag.contains("OwnerName")) {
            this.entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        }

        if (tag.contains("ArmorItems", 9)) {
            net.minecraft.nbt.ListTag armorList = tag.getList("ArmorItems", 10);
            for (int i = 0; i < this.armorItems.size() && i < armorList.size(); i++) {
                this.armorItems.set(i, ItemStack.of(armorList.getCompound(i)));
            }
        }

        if (tag.contains("HandItems", 9)) {
            net.minecraft.nbt.ListTag handList = tag.getList("HandItems", 10);
            for (int i = 0; i < this.handItems.size() && i < handList.size(); i++) {
                this.handItems.set(i, ItemStack.of(handList.getCompound(i)));
            }
        }
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance pPotions) {
        return true;
    }
}
