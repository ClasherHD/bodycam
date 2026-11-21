package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class ClientLocatorCache {
    public static volatile BlockPos structureTarget = null;
    public static volatile ResourceKey<Level> structureTargetDimension = null;

    public static void updateStructure(BlockPos pos, String dim) {
        structureTarget = pos;
        ResourceLocation rl = dim != null ? ResourceLocation.tryParse(dim) : null;
        structureTargetDimension = rl != null ? ResourceKey.create(Registries.DIMENSION, rl) : null;
    }

    public static void clear() {
        structureTarget = null;
        structureTargetDimension = null;
    }
}
