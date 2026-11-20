package dev.ClasherHD.bodycam.client.render;

import net.minecraft.world.item.ItemStack;

public interface CherryHouseFrameStateAccess {
    ItemStack bodycam$getFramedItem();

    void bodycam$setFramedItem(ItemStack stack);
}
