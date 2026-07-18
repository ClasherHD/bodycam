package dev.ClasherHD.bodycam.util.fabric;

import dev.ClasherHD.bodycam.util.IEntityPersistentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public class BodycamHelperImpl {
    public static CompoundTag getPersistentData(Entity entity) {
        return ((IEntityPersistentData) entity).getPersistentData();
    }

    public static boolean isServer() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER;
    }
}
