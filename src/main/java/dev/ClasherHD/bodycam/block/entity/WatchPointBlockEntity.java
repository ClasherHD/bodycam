package dev.ClasherHD.bodycam.block.entity;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.watchpoint.OpenWatchPointScreenS2CPacket;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("null")
public class WatchPointBlockEntity extends BlockEntity {

    private UUID ownerUUID;
    private String ownerName = "";
    private String customName = "";
    private UUID activeDummyUUID;

    public WatchPointBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATCH_POINT.get(), pos, state);
    }

    public void setOwner(UUID ownerUUID, String ownerName) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName == null ? "" : ownerName;
        setChanged();
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String name) {
        String safe = name == null ? "" : name.trim();
        if (safe.length() > 32) {
            safe = safe.substring(0, 32);
        }
        this.customName = safe;
        if (this.level instanceof ServerLevel serverLevel && this.activeDummyUUID != null) {
            Entity e = serverLevel.getEntity(this.activeDummyUUID);
            if (e instanceof WatchPointDummyEntity dummy && dummy.isAlive()) {
                dummy.setWatchPointName(this.customName);
            }
        }
        setChanged();
    }

    public void clearActiveDummyIfMatches(UUID dummyUUID) {
        if (this.activeDummyUUID != null && this.activeDummyUUID.equals(dummyUUID)) {
            this.activeDummyUUID = null;
            setChanged();
        }
    }

    public void onPlayerInteract(ServerPlayer player) {
        if (ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
            return;
        }
        if (!ModServerConfig.ENABLE_WATCH_POINT.get()) {
            player.sendSystemMessage(Component.translatable("message.bodycam.watch_point_disabled").withStyle(ChatFormatting.RED));
            return;
        }

        if (this.ownerUUID == null) {
            setOwner(player.getUUID(), player.getName().getString());
        } else if (!this.ownerUUID.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.bodycam.watch_point.not_owner").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (player.isShiftKeyDown()) {
            PacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenWatchPointScreenS2CPacket(this.worldPosition, this.customName)
            );
            return;
        }

        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        WatchPointDummyEntity existing = findActiveDummy(serverLevel);
        if (existing != null) {
            existing.discard();
            this.activeDummyUUID = null;
            setChanged();
            serverLevel.playSound(null, this.worldPosition, ModSounds.WATCH_POINT_DEACTIVATE.get(), SoundSource.BLOCKS, 0.5F, 1.4F);
            player.displayClientMessage(Component.translatable("message.bodycam.watch_point.deactivated").withStyle(ChatFormatting.RED), true);
            return;
        }

        int totalCount = WatchPointDummyEntity.getTotalActiveCount();
        int maxTotal = ModServerConfig.MAX_WATCH_POINTS_TOTAL.get();
        if (totalCount >= maxTotal) {
            player.displayClientMessage(Component.translatable("message.bodycam.watch_point.server_limit", maxTotal).withStyle(ChatFormatting.RED), true);
            return;
        }

        List<WatchPointDummyEntity> playerDummies = WatchPointDummyEntity.getActiveForOwner(player.server, player.getUUID());
        int maxPlayer = ModServerConfig.MAX_WATCH_POINTS_PER_PLAYER.get();
        if (playerDummies.size() >= maxPlayer) {
            player.displayClientMessage(Component.translatable("message.bodycam.watch_point.player_limit", maxPlayer).withStyle(ChatFormatting.RED), true);
            return;
        }

        WatchPointDummyEntity dummy = new WatchPointDummyEntity(ModEntityTypes.WATCH_POINT_DUMMY.get(), serverLevel);
        dummy.setPos(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D);
        float yaw = Mth.wrapDegrees(player.getYRot() + 180.0F);
        dummy.setYRot(yaw);
        dummy.setXRot(0.0F);
        dummy.setYHeadRot(yaw);
        dummy.setYBodyRot(yaw);

        dummy.getEntityData().set(BodycamDummyEntity.OWNER_UUID, Optional.of(player.getUUID()));
        dummy.getEntityData().set(BodycamDummyEntity.OWNER_NAME, player.getName().getString());
        dummy.setWatchPointPos(this.worldPosition);
        dummy.setWatchPointName(this.customName);

        if (player.getAttribute(Attributes.ARMOR) != null && dummy.getAttribute(Attributes.ARMOR) != null) {
            dummy.getAttribute(Attributes.ARMOR).setBaseValue(player.getAttributeValue(Attributes.ARMOR));
        }
        if (player.getAttribute(Attributes.ARMOR_TOUGHNESS) != null && dummy.getAttribute(Attributes.ARMOR_TOUGHNESS) != null) {
            dummy.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        }
        if (player.getAttribute(Attributes.MAX_HEALTH) != null && dummy.getAttribute(Attributes.MAX_HEALTH) != null) {
            dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getAttributeValue(Attributes.MAX_HEALTH));
        }
        dummy.setHealth(dummy.getMaxHealth());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                dummy.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }
        }

        serverLevel.addFreshEntity(dummy);
        WatchPointDummyEntity.ACTIVE_WATCH_POINTS.add(dummy);
        int chunkX = Mth.floor(dummy.getX()) >> 4;
        int chunkZ = Mth.floor(dummy.getZ()) >> 4;
        ForgeChunkManager.forceChunk(serverLevel, "bodycam", dummy, chunkX, chunkZ, true, true);

        this.activeDummyUUID = dummy.getUUID();
        setChanged();

        serverLevel.playSound(null, this.worldPosition, ModSounds.WATCH_POINT_ACTIVATE.get(), SoundSource.BLOCKS, 0.5F, 1.4F);
        player.displayClientMessage(Component.translatable("message.bodycam.watch_point.activated").withStyle(ChatFormatting.GREEN), true);
    }

    private WatchPointDummyEntity findActiveDummy(ServerLevel serverLevel) {
        if (this.activeDummyUUID != null) {
            Entity e = serverLevel.getEntity(this.activeDummyUUID);
            if (e instanceof WatchPointDummyEntity wp && wp.isAlive() && !wp.isRemoved()) {
                return wp;
            }
        }
        List<WatchPointDummyEntity> nearby = serverLevel.getEntitiesOfClass(
                WatchPointDummyEntity.class,
                new net.minecraft.world.phys.AABB(this.worldPosition.above()).inflate(0.5D),
                e -> e.isAlive() && !e.isRemoved()
        );
        if (!nearby.isEmpty()) {
            WatchPointDummyEntity found = nearby.get(0);
            this.activeDummyUUID = found.getUUID();
            return found;
        }
        return null;
    }

    public void despawnDummy(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            WatchPointDummyEntity existing = findActiveDummy(serverLevel);
            if (existing != null) {
                existing.discard();
            }
            this.activeDummyUUID = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putString("OwnerName", this.ownerName);
        tag.putString("CustomName", this.customName);
        if (this.activeDummyUUID != null) {
            tag.putUUID("ActiveDummyUUID", this.activeDummyUUID);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        this.ownerName = tag.getString("OwnerName");
        this.customName = tag.getString("CustomName");
        if (tag.hasUUID("ActiveDummyUUID")) {
            this.activeDummyUUID = tag.getUUID("ActiveDummyUUID");
        }
    }
}
