package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
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

@SuppressWarnings("null")
public class BodycamDummyEntity extends LivingEntity {
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData
            .defineId(BodycamDummyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData
            .defineId(BodycamDummyEntity.class, EntityDataSerializers.STRING);

    public static final Map<UUID, Vec3> DUMMY_POS = new ConcurrentHashMap<>();
    public static final Map<UUID, Float> DUMMY_FALL = new ConcurrentHashMap<>();
    public static final Map<UUID, Vec3> DUMMY_MOTION = new ConcurrentHashMap<>();

    private int currentLoadedChunkX = Integer.MAX_VALUE;
    private int currentLoadedChunkZ = Integer.MAX_VALUE;
    private boolean isChunkForced = false;

    private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);

    public BodycamDummyEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH)
                .add(Attributes.ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE)
                .add(Attributes.ATTACK_DAMAGE);
    }

    protected boolean isLinkedToOwnerHealth() {
        return true;
    }

    public void markChunkForced(int chunkX, int chunkZ) {
        this.currentLoadedChunkX = chunkX;
        this.currentLoadedChunkZ = chunkZ;
        this.isChunkForced = true;
    }

    public void markChunkUnforced() {
        this.isChunkForced = false;
    }

    protected void updateChunkForcing() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int currentChunkX = this.blockPosition().getX() >> 4;
        int currentChunkZ = this.blockPosition().getZ() >> 4;
        if (!this.isChunkForced || currentChunkX != this.currentLoadedChunkX || currentChunkZ != this.currentLoadedChunkZ) {
            if (this.isChunkForced && this.currentLoadedChunkX != Integer.MAX_VALUE && this.currentLoadedChunkZ != Integer.MAX_VALUE) {
                BodycamPlatform.forceChunk(serverLevel, this.getUUID(), this.currentLoadedChunkX, this.currentLoadedChunkZ, false);
            }
            BodycamPlatform.forceChunk(serverLevel, this.getUUID(), currentChunkX, currentChunkZ, true);
            this.currentLoadedChunkX = currentChunkX;
            this.currentLoadedChunkZ = currentChunkZ;
            this.isChunkForced = true;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            if (this.isChunkForced && this.level() instanceof ServerLevel serverLevel) {
                BodycamPlatform.forceChunk(serverLevel, this.getUUID(), this.currentLoadedChunkX, this.currentLoadedChunkZ, false);
                this.isChunkForced = false;
            }
            List<Mob> mobs = this.level().getEntitiesOfClass(
                    Mob.class,
                    this.getBoundingBox().inflate(32.0D),
                    m -> m.getTarget() == this
            );
            for (Mob mob : mobs) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        UUID ownerId = getOwnerUuidOrNull();
        if (!this.level().isClientSide() && this.isLinkedToOwnerHealth() && ownerId != null) {
            MinecraftServer server = this.getServer();
            if (server != null) {
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
                if (owner != null && PersistentDataAccess.get(owner).getBoolean("bodycam_active")) {
                    BodycamResetCameraPacket.executeReset(owner);
                    PacketHandler.sendToPlayer(owner, new BodycamForceClosePacket());
                }
            }
        }
        super.die(source);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(OWNER_NAME, "");
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    @Nullable
    public UUID getOwnerUuidOrNull() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    public void setOwnerName(@Nullable String name) {
        this.entityData.set(OWNER_NAME, name == null ? "" : name);
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
            case HUMANOID_ARMOR:
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
            case HUMANOID_ARMOR:
                this.onEquipItem(slot, this.armorItems.set(slot.getIndex(), stack), stack);
                break;
            default:
                break;
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    protected void hurtArmor(DamageSource source, float damage) {
        if (!this.isLinkedToOwnerHealth()) {
            return;
        }
        if (damage <= 0.0F) {
            return;
        }
        damage /= 4.0F;
        if (damage < 1.0F) {
            damage = 1.0F;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = this.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    if (source.is(DamageTypeTags.IS_FIRE) && stack.has(DataComponents.FIRE_RESISTANT)) {
                        continue;
                    }
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
        if (!this.isLinkedToOwnerHealth()) {
            return super.hurt(source, amount);
        }
        UUID ownerId = getOwnerUuidOrNull();
        if (!this.level().isClientSide() && ownerId != null) {
            MinecraftServer server = this.getServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
                if (player != null) {
                    boolean result = super.hurt(source, amount);
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
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
        if (!this.isLinkedToOwnerHealth()) {
            return;
        }
        UUID ownerId = getOwnerUuidOrNull();
        if (!this.level().isClientSide() && ownerId != null) {
            MinecraftServer server = this.getServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
                if (player != null) {
                    player.setHealth(this.getHealth());
                    player.setAbsorptionAmount(this.getAbsorptionAmount());
                }
            }
        }
    }

    @Override
    public boolean addEffect(MobEffectInstance effectInstance, @Nullable Entity entity) {
        return super.addEffect(effectInstance, entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.isLinkedToOwnerHealth()) {
            UUID ownerId = getOwnerUuidOrNull();
            if (ownerId == null || this.level().getServer() == null) {
                this.discard();
                return;
            }
            ServerPlayer owner = this.level().getServer().getPlayerList().getPlayer(ownerId);
            if (owner == null) {
                this.discard();
                return;
            }
            CompoundTag ownerData = PersistentDataAccess.get(owner);
            if (!ownerData.getBoolean("bodycam_active")
                    || !ownerData.hasUUID("bodycam_dummy_uuid")
                    || !this.getUUID().equals(ownerData.getUUID("bodycam_dummy_uuid"))) {
                this.discard();
                return;
            }

            this.updateChunkForcing();

            DUMMY_POS.put(ownerId, this.position());
            DUMMY_FALL.put(ownerId, this.fallDistance);
            DUMMY_MOTION.put(ownerId, this.getDeltaMovement());

            this.setHealth(owner.getHealth());
            this.setAbsorptionAmount(owner.getAbsorptionAmount());

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack ownerItem = owner.getItemBySlot(slot);
                ItemStack dummyItem = this.getItemBySlot(slot);
                if (!ItemStack.matches(ownerItem, dummyItem)) {
                    this.setItemSlot(slot, ownerItem.copy());
                }
            }

            if (this.tickCount % 20 == 0 && !this.isInvulnerable() && this.isLinkedToOwnerHealth()) {
                int origGm = BodycamSetCameraPacket.ORIGINAL_GAMEMODE
                        .getOrDefault(ownerId, ownerData.getInt("bodycam_original_gamemode"));
                if (origGm == GameType.SURVIVAL.getId()
                        || origGm == GameType.ADVENTURE.getId()) {
                    List<Mob> mobs = this.level().getEntitiesOfClass(
                            Mob.class,
                            this.getBoundingBox().inflate(16.0D),
                            e -> e instanceof Enemy
                    );
                    for (Mob mob : mobs) {
                        if (mob.getTarget() == null && mob.getSensing().hasLineOfSight(this)) {
                            mob.setTarget(this);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID ownerId = getOwnerUuidOrNull();
        if (ownerId != null) {
            tag.putUUID("OwnerUUID", ownerId);
        }
        tag.putString("OwnerName", getOwnerName());

        ListTag armorList = new ListTag();
        for (ItemStack stack : this.armorItems) {
            Tag saved = stack.saveOptional(this.registryAccess());
            armorList.add(saved instanceof CompoundTag compound ? compound : new CompoundTag());
        }
        tag.put("ArmorItems", armorList);

        ListTag handList = new ListTag();
        for (ItemStack stack : this.handItems) {
            Tag saved = stack.saveOptional(this.registryAccess());
            handList.add(saved instanceof CompoundTag compound ? compound : new CompoundTag());
        }
        tag.put("HandItems", handList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            setOwnerUUID(tag.getUUID("OwnerUUID"));
        }
        if (tag.contains("OwnerName")) {
            setOwnerName(tag.getString("OwnerName"));
        }

        if (tag.contains("ArmorItems", Tag.TAG_LIST)) {
            ListTag armorList = tag.getList("ArmorItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < this.armorItems.size() && i < armorList.size(); i++) {
                this.armorItems.set(i, ItemStack.parseOptional(this.registryAccess(), armorList.getCompound(i)));
            }
        }

        if (tag.contains("HandItems", Tag.TAG_LIST)) {
            ListTag handList = tag.getList("HandItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < this.handItems.size() && i < handList.size(); i++) {
                this.handItems.set(i, ItemStack.parseOptional(this.registryAccess(), handList.getCompound(i)));
            }
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance pPotions) {
        return true;
    }
}
