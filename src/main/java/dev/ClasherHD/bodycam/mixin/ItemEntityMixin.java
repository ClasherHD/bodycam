package dev.ClasherHD.bodycam.mixin;

import net.minecraft.world.entity.item.ItemEntity;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow public abstract ItemStack getItem();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!((net.minecraft.world.entity.Entity)(Object)this).level().isClientSide && ((net.minecraft.world.entity.item.ItemEntity)(Object)this).tickCount == 1) {
            ItemStack stack = this.getItem();
            if (stack.getItem() instanceof dev.ClasherHD.bodycam.item.JammerItem) {
                if (stack.hasTag()) {
                    stack.getTag().putInt("JammerMode", 0);
                    stack.getTag().remove("active_id");
                }
            } else if (stack.getItem() instanceof dev.ClasherHD.bodycam.item.AnonymizerItem) {
                if (stack.hasTag()) {
                    stack.getTag().putBoolean("AnonymizerActive", false);
                    stack.getTag().remove("active_id");
                }
            }
        }
    }
}
