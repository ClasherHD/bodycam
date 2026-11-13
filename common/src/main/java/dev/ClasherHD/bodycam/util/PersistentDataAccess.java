package dev.ClasherHD.bodycam.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

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
}
