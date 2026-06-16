package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class ClientLocatorCache {
    public static BlockPos structureTarget = null;
    public static ResourceKey<Level> structureTargetDimension = null;

    public static void updateStructure(BlockPos pos, String dim) {
        structureTarget = pos;
        if (dim != null) {
            structureTargetDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dim));
        } else {
            structureTargetDimension = null;
        }
    }

    public static void clear() {
        structureTarget = null;
        structureTargetDimension = null;
    }
}
