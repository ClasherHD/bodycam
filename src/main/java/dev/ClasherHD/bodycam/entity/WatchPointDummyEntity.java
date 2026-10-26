package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class WatchPointDummyEntity extends BodycamDummyEntity {

    public static final EntityDataAccessor<String> WATCH_POINT_NAME = SynchedEntityData.defineId(
            WatchPointDummyEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Optional<BlockPos>> WATCH_POINT_POS = SynchedEntityData.defineId(
            WatchPointDummyEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    public static final Set<WatchPointDummyEntity> ACTIVE_WATCH_POINTS = ConcurrentHashMap.newKeySet();

    public WatchPointDummyEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected boolean isLinkedToOwnerHealth() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WATCH_POINT_NAME, "");
        this.entityData.define(WATCH_POINT_POS, Optional.empty());
    }

    public void setWatchPointName(String name) {
        String safe = name == null ? "" : name.trim();
        this.entityData.set(WATCH_POINT_NAME, safe);
        String ownerName = this.entityData.get(OWNER_NAME);
        if (!safe.isEmpty()) {
            this.setCustomName(Component.literal(ownerName + " [" + safe + "]"));
        } else {
            this.setCustomName(Component.literal(ownerName));
        }
        this.setCustomNameVisible(true);
    }

    public String getWatchPointName() {
        return this.entityData.get(WATCH_POINT_NAME);
    }

    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    public String getSelectionDisplayName() {
        String custom = getWatchPointName();
        if (custom != null && !custom.isEmpty()) {
            return custom;
        }
        BlockPos pos = getWatchPointPos();
        if (pos != null) {
            return "Watch Point (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
        }
        return "Watch Point";
    }

    public void setWatchPointPos(BlockPos pos) {
        this.entityData.set(WATCH_POINT_POS, Optional.ofNullable(pos));
    }

    public BlockPos getWatchPointPos() {
        return this.entityData.get(WATCH_POINT_POS).orElse(null);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide()) {
            ACTIVE_WATCH_POINTS.add(this);
        }
    }

    public static List<WatchPointDummyEntity> getAllActive(MinecraftServer server) {
        ACTIVE_WATCH_POINTS.removeIf(e -> e == null || e.isRemoved() || !e.isAlive());
        List<WatchPointDummyEntity> list = new ArrayList<>(ACTIVE_WATCH_POINTS);
        list.sort(java.util.Comparator.comparing(Entity::getUUID));
        return list;
    }

    public static List<WatchPointDummyEntity> getActiveForOwner(MinecraftServer server, UUID ownerId) {
        List<WatchPointDummyEntity> list = new ArrayList<>();
        for (WatchPointDummyEntity dummy : getAllActive(server)) {
            if (ownerId.equals(dummy.getOwnerUUID())) {
                list.add(dummy);
            }
        }
        return list;
    }

    public static int getTotalActiveCount() {
        ACTIVE_WATCH_POINTS.removeIf(e -> e == null || e.isRemoved() || !e.isAlive());
        return ACTIVE_WATCH_POINTS.size();
    }

    public static WatchPointDummyEntity findByUUID(MinecraftServer server, UUID dummyId) {
        if (dummyId == null || server == null) {
            return null;
        }
        for (WatchPointDummyEntity dummy : ACTIVE_WATCH_POINTS) {
            if (!dummy.isRemoved() && dummy.isAlive() && dummyId.equals(dummy.getUUID())) {
                return dummy;
            }
        }
        for (ServerLevel lvl : server.getAllLevels()) {
            Entity e = lvl.getEntity(dummyId);
            if (e instanceof WatchPointDummyEntity wp && wp.isAlive() && !wp.isRemoved()) {
                ACTIVE_WATCH_POINTS.add(wp);
                return wp;
            }
        }
        return null;
    }

    private boolean kickedObservingOwner = false;

    @Override
    protected void updateChunkForcing() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (!this.isAlive() || this.isRemoved()) {
                ACTIVE_WATCH_POINTS.remove(this);
                return;
            }
            ACTIVE_WATCH_POINTS.add(this);

            BlockPos wpPos = getWatchPointPos();
            if (wpPos == null) {
                wpPos = this.blockPosition().below();
                setWatchPointPos(wpPos);
            }

            if (!this.level().getBlockState(wpPos).is(ModBlocks.WATCH_POINT.get())) {
                kickObservingOwner(false);
                this.discard();
                return;
            }

            double targetX = wpPos.getX() + 0.5D;
            double targetY = wpPos.getY() + 1.0D;
            double targetZ = wpPos.getZ() + 0.5D;
            if (this.distanceToSqr(targetX, targetY, targetZ) > 0.001D) {
                this.setPos(targetX, targetY, targetZ);
            }
            this.setDeltaMovement(Vec3.ZERO);
            this.fallDistance = 0.0F;
        }
    }

    private void kickObservingOwner(boolean wasKilled) {
        if (this.kickedObservingOwner || this.level().isClientSide() || this.getOwnerUUID() == null) {
            return;
        }
        this.kickedObservingOwner = true;
        MinecraftServer server = this.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(this.getOwnerUUID());
        if (owner != null) {
            if (owner.getPersistentData().getBoolean("bodycam_active")
                    && this.getUUID().equals(owner.getPersistentData().getUUID("bodycam_target_uuid"))) {
                BodycamResetCameraPacket.executeReset(owner);
                PacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> owner),
                        new BodycamForceClosePacket()
                );
            }
            if (wasKilled) {
                owner.sendSystemMessage(Component.translatable("message.bodycam.watch_point.destroyed", getSelectionDisplayName())
                        .withStyle(ChatFormatting.RED));
            }
        }
        BlockPos wpPos = getWatchPointPos();
        if (wpPos != null && this.level().getBlockEntity(wpPos) instanceof WatchPointBlockEntity be) {
            be.clearActiveDummyIfMatches(this.getUUID());
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            ACTIVE_WATCH_POINTS.remove(this);
            kickObservingOwner(true);
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            ACTIVE_WATCH_POINTS.remove(this);
            kickObservingOwner(false);
        }
        super.remove(reason);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("WatchPointName", getWatchPointName());
        BlockPos wpPos = getWatchPointPos();
        if (wpPos != null) {
            tag.put("WatchPointPos", NbtUtils.writeBlockPos(wpPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("WatchPointPos")) {
            setWatchPointPos(NbtUtils.readBlockPos(tag.getCompound("WatchPointPos")));
        }
        if (tag.contains("WatchPointName")) {
            setWatchPointName(tag.getString("WatchPointName"));
        }
    }
}
