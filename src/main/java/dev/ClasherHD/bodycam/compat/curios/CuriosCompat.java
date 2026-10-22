package dev.ClasherHD.bodycam.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.Collections;
import java.util.List;

public final class CuriosCompat {

    private static final String CURIOS_MOD_ID = "curios";

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }

    public static boolean hasItem(LivingEntity entity, Item item) {
        if (!isLoaded()) {
            return false;
        }
        return CuriosInternal.hasItem(entity, item);
    }

    public static List<ItemStack> getEquippedItems(LivingEntity entity, Item item) {
        if (!isLoaded()) {
            return Collections.emptyList();
        }
        return CuriosInternal.getEquippedItems(entity, item);
    }
}
