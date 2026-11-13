package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow
    @Final
    private DataSlot cost;

    protected AnvilMenuMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(type, containerId, inventory, access);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void bodycam$restrictAnvil(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);
        if (left.isEmpty() || right.isEmpty()) {
            return;
        }
        if (ItemDurabilityHelper.isRepairableModItem(left) || left.getItem() instanceof PortableNetherPortalItem) {
            boolean block = false;
            if (right.getItem() instanceof EnchantedBookItem || right.isEnchanted()) {
                if (!ItemDurabilityHelper.isBookEnchantable(left, right)) {
                    block = true;
                }
            } else if (ItemStack.isSameItem(left, right)) {
                if (!right.isEnchanted() || !ItemDurabilityHelper.isBookEnchantable(left, right)) {
                    block = true;
                }
            }
            if (block) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                this.cost.set(0);
                this.broadcastChanges();
                ci.cancel();
            }
        }
    }
}
