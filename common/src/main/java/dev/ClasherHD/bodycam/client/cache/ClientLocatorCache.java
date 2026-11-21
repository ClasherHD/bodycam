package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class ClientLocatorCache {
    public static volatile BlockPos structureTarget = null;
    public static volatile ResourceKey<Level> structureTargetDimension = null;
    public static volatile GlobalPos structureGlobalPos = null;

    public static void updateStructure(BlockPos pos, String dim) {
        if (pos == null) {
            clear();
            return;
        }
        if (dim == null) {
            structureTarget = pos;
            structureTargetDimension = null;
            structureGlobalPos = null;
            return;
        }
        ResourceLocation rl = ResourceLocation.tryParse(dim);
        if (rl == null) {
            clear();
            return;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, rl);
        structureTargetDimension = key;
        structureTarget = pos;
        structureGlobalPos = GlobalPos.of(key, pos);
    }

    public static void clear() {
        structureTarget = null;
        structureTargetDimension = null;
        structureGlobalPos = null;
    }
}
