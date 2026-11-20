package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.client.render.CherryHouseFrameStateAccess;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrameRenderState.class)
public class ItemFrameRenderStateMixin implements CherryHouseFrameStateAccess {
    @Unique
    private ItemStack bodycam$framedItem = ItemStack.EMPTY;

    @Override
    public ItemStack bodycam$getFramedItem() {
        return this.bodycam$framedItem;
    }

    @Override
    public void bodycam$setFramedItem(ItemStack stack) {
        this.bodycam$framedItem = stack == null ? ItemStack.EMPTY : stack;
    }
}
