package dev.ClasherHD.bodycam.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public class BodycamHelper {
    @ExpectPlatform
    public static CompoundTag getPersistentData(Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isServer() {
        throw new AssertionError();
    }
}
