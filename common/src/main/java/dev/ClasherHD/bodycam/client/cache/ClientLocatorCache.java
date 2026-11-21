package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class ClientLocatorCache {
    public static volatile BlockPos structureTarget = null;
    public static volatile ResourceKey<Level> structureTargetDimension = null;

    public static void updateStructure(BlockPos pos, String dim) {
        if (pos == null) {
            clear();
            return;
        }
        Identifier id = dim == null ? null : Identifier.tryParse(dim);
        structureTargetDimension = id != null ? ResourceKey.create(Registries.DIMENSION, id) : null;
        structureTarget = pos;
    }

    public static void clear() {
        structureTarget = null;
        structureTargetDimension = null;
    }
}
