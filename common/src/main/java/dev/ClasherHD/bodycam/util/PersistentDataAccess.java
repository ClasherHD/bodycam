package dev.ClasherHD.bodycam.util;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("null")
public final class PersistentDataAccess {
    private PersistentDataAccess() {
    }

    public static CompoundTag get(Entity entity) {
        if (entity instanceof BodycamPersistentDataHolder holder) {
            return holder.bodycam$getPersistentData();
        }
        return new CompoundTag();
    }

    public static CompoundTag get(BlockEntity blockEntity) {
        if (blockEntity instanceof BodycamPersistentDataHolder holder) {
            return holder.bodycam$getPersistentData();
        }
        return new CompoundTag();
    }

    public static boolean hasUUID(CompoundTag tag, String key) {
        return tag != null && key != null && tag.read(key, UUIDUtil.CODEC).isPresent();
    }

    public static UUID getUUID(CompoundTag tag, String key) {
        if (tag == null || key == null) {
            return null;
        }
        return tag.read(key, UUIDUtil.CODEC).orElse(null);
    }

    public static void putUUID(CompoundTag tag, String key, UUID uuid) {
        if (tag == null || key == null) {
            return;
        }
        if (uuid == null) {
            tag.remove(key);
            return;
        }
        tag.store(key, UUIDUtil.CODEC, uuid);
    }
}
