package dev.ClasherHD.bodycam;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import dev.ClasherHD.bodycam.util.IEntityPersistentData;

public final class BodycamHelper {
    private BodycamHelper() {}

    public static CompoundTag getPersistentData(Entity entity) {
        return ((IEntityPersistentData) entity).getPersistentData();
    }
}
