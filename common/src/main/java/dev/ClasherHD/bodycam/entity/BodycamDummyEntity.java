package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.util.BodycamHelper;
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
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumMap;
import java.util.Arrays;
import java.util.List;

public class BodycamDummyEntity extends LivingEntity {
    public static final EntityDataAccessor<String> OWNER_UUID_STR = SynchedEntityData
            .defineId(BodycamDummyEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(BodycamDummyEntity.class,
            EntityDataSerializers.STRING);

    public static final Map<UUID, Vec3> DUMMY_POS = new ConcurrentHashMap<>();
    public static final Map<UUID, Double> DUMMY_FALL = new ConcurrentHashMap<>();
    public static final Map<UUID, Vec3> DUMMY_MOTION = new ConcurrentHashMap<>();

    private int currentLoadedChunkX = Integer.MAX_VALUE;
    private int currentLoadedChunkZ = Integer.MAX_VALUE;
    private boolean isChunkForced = false;

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH)
                .add(Attributes.ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE)
                .add(Attributes.ATTACK_DAMAGE);
    }

    private final EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

    public BodycamDummyEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.equipment.put(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.isChunkForced) {
                serverLevel.setChunkForced(this.currentLoadedChunkX, this.currentLoadedChunkZ, false);
                this.isChunkForced = false;
            }
            List<Mob> mobs = serverLevel.getEntitiesOfClass(
                    Mob.class,
                    this.getBoundingBox().inflate(32.0D),
                    m -> m.getTarget() == this);
            for (Mob m : mobs) {
                m.setTarget(null);
            }
            if (this.getOwnerUUID() != null) {
                DUMMY_POS.remove(this.getOwnerUUID());
                DUMMY_FALL.remove(this.getOwnerUUID());
                DUMMY_MOTION.remove(this.getOwnerUUID());
            }
        }
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            ServerPlayer owner = ((net.minecraft.server.level.ServerLevel) this.level()).getServer().getPlayerList()
                    .getPlayer(this.getOwnerUUID());
            if (owner != null && BodycamHelper.getPersistentData(owner).getBooleanOr("bodycam_active", false)) {
                int gmId = BodycamHelper.getPersistentData(owner).getIntOr("bodycam_original_gamemode", 0);
                owner.setGameMode(GameType.byId(gmId));
                dev.ClasherHD.bodycam.network.ServerNetworking.executeReset(owner);
                dev.architectury.networking.NetworkManager.sendToPlayer(owner,
                        new dev.ClasherHD.bodycam.network.BodycamForceClosePacket());
            }
        }
        super.die(source);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID_STR, "");
        builder.define(OWNER_NAME, "");
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);
        valueInput.read("OwnerUUID", net.minecraft.core.UUIDUtil.CODEC).ifPresent(uuid -> {
            this.entityData.set(OWNER_UUID_STR, uuid.toString());
        });
        valueInput.getString("OwnerName").ifPresent(name -> {
            this.entityData.set(OWNER_NAME, name);
        });
        valueInput.read("DummyEquipment", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(equipmentTag -> {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (equipmentTag.contains(slot.getName())) {
                    net.minecraft.nbt.Tag stackTag = equipmentTag.get(slot.getName());
                    ItemStack stack = ItemStack.CODEC.parse(
                            net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,
                                    this.registryAccess()),
                            stackTag).result().orElse(ItemStack.EMPTY);
                    this.setItemSlot(slot, stack);
                }
            }
        });
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);
        valueOutput.storeNullable("OwnerUUID", net.minecraft.core.UUIDUtil.CODEC, this.getOwnerUUID());
        valueOutput.store("OwnerName", com.mojang.serialization.Codec.STRING, this.entityData.get(OWNER_NAME));
        net.minecraft.nbt.CompoundTag equipmentTag = new net.minecraft.nbt.CompoundTag();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (stack != null && !stack.isEmpty()) {
                net.minecraft.nbt.Tag stackTag = ItemStack.CODEC.encodeStart(
                        net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,
                                this.registryAccess()),
                        stack).result().orElse(null);
                if (stackTag != null) {
                    equipmentTag.put(slot.getName(), stackTag);
                }
            }
        }
        valueOutput.store("DummyEquipment", net.minecraft.nbt.CompoundTag.CODEC, equipmentTag);
    }

    public UUID getOwnerUUID() {
        String uuidStr = this.entityData.get(OWNER_UUID_STR);
        if (uuidStr != null && !uuidStr.isEmpty()) {
            try {
                return UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
            }
        }
        return null;
    }

    public Iterable<ItemStack> getArmorSlots() {
        return Arrays.asList(
                this.equipment.get(EquipmentSlot.FEET),
                this.equipment.get(EquipmentSlot.LEGS),
                this.equipment.get(EquipmentSlot.CHEST),
                this.equipment.get(EquipmentSlot.HEAD));
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return this.equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        ItemStack old = this.equipment.put(slot, stack);
        this.onEquipItem(slot, old == null ? ItemStack.EMPTY : old, stack);
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void hurtArmor(DamageSource source, float damage) {
        if (damage <= 0.0F)
            return;
        damage /= 4.0F;
        if (damage < 1.0F)
            damage = 1.0F;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor()) {
                ItemStack stack = this.getItemBySlot(slot);
                if (stack != null && !stack.isEmpty()) {
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.getOwnerUUID() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.getOwnerUUID());
            if (player != null) {
                boolean result = super.hurtServer(level, source, amount);
                if (!this.isDeadOrDying()) {
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        if (slot.isArmor()) {
                            ItemStack dummyPiece = this.getItemBySlot(slot);
                            ItemStack playerPiece = player.getItemBySlot(slot);
                            if (!playerPiece.isEmpty() && !dummyPiece.isEmpty()) {
                                playerPiece.setDamageValue(dummyPiece.getDamageValue());
                                player.setItemSlot(slot, playerPiece);
                            } else if (!playerPiece.isEmpty() && dummyPiece.isEmpty()) {
                                player.setItemSlot(slot, ItemStack.EMPTY);
                            }
                        }
                    }
                    player.setHealth(this.getHealth());
                    player.setAbsorptionAmount(this.getAbsorptionAmount());
                } else {
                    player.getCombatTracker().recordDamage(source, amount);
                    int gmId = BodycamHelper.getPersistentData(player).getIntOr("bodycam_original_gamemode", 0);
                    player.setGameMode(GameType.byId(gmId));
                    BodycamHelper.getPersistentData(player).putBoolean("bodycam_allow_teleport", true);
                    player.teleportTo(level, this.getX(), this.getY(), this.getZ(), java.util.Collections.emptySet(),
                            this.getYRot(), this.getXRot(), true);
                    BodycamHelper.getPersistentData(player).remove("bodycam_allow_teleport");
                    player.setHealth(0);
                    player.die(source);
                }
                return result;
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void heal(float amount) {
        super.heal(amount);
        if (!this.level().isClientSide() && this.getOwnerUUID() != null) {
            ServerPlayer player = ((net.minecraft.server.level.ServerLevel) this.level()).getServer().getPlayerList()
                    .getPlayer(this.getOwnerUUID());
            if (player != null) {
                player.setHealth(this.getHealth());
                player.setAbsorptionAmount(this.getAbsorptionAmount());
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
            ServerPlayer owner = ((net.minecraft.server.level.ServerLevel) this.level()).getServer().getPlayerList()
                    .getPlayer(this.getOwnerUUID());
            if (owner == null || !BodycamHelper.getPersistentData(owner).getBooleanOr("bodycam_active", false)
                    || !this.getUUID().equals(BodycamHelper.getPersistentData(owner)
                            .read("bodycam_dummy_uuid", net.minecraft.core.UUIDUtil.CODEC).orElse(null))) {
                this.discard();
                return;
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                int currentChunkX = this.blockPosition().getX() >> 4;
                int currentChunkZ = this.blockPosition().getZ() >> 4;

                if (currentChunkX != this.currentLoadedChunkX || currentChunkZ != this.currentLoadedChunkZ) {
                    if (this.isChunkForced) {
                        serverLevel.setChunkForced(this.currentLoadedChunkX, this.currentLoadedChunkZ, false);
                    }
                    serverLevel.setChunkForced(currentChunkX, currentChunkZ, true);
                    this.currentLoadedChunkX = currentChunkX;
                    this.currentLoadedChunkZ = currentChunkZ;
                    this.isChunkForced = true;
                }
            }

            DUMMY_POS.put(this.getOwnerUUID(), this.position());
            DUMMY_FALL.put(this.getOwnerUUID(), this.fallDistance);
            DUMMY_MOTION.put(this.getOwnerUUID(), this.getDeltaMovement());

            this.setHealth(owner.getHealth());
            this.setAbsorptionAmount(owner.getAbsorptionAmount());

            for (net.minecraft.world.effect.MobEffectInstance effect : owner.getActiveEffects()) {
                if (!this.hasEffect(effect.getEffect())
                        || this.getEffect(effect.getEffect()).getDuration() < effect.getDuration()) {
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
                }
            }

            if (this.tickCount % 10 == 0) {
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
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance pPotions) {
        return true;
    }
}
