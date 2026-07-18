package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.component.ModDataComponents;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
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
        ItemEntity entity = (ItemEntity) (Object) this;
        if (!entity.level().isClientSide() && entity.tickCount == 1) {
            ItemStack stack = this.getItem();
            if (stack.getItem() instanceof JammerItem) {
                stack.set(ModDataComponents.JAMMER_MODE.get(), 0);
                stack.set(ModDataComponents.IS_MASTER.get(), false);
            } else if (stack.getItem() instanceof AnonymizerItem) {
                stack.set(ModDataComponents.ANONYMIZER_ACTIVE.get(), false);
                stack.set(ModDataComponents.IS_MASTER.get(), false);
            }
        }
    }
}
