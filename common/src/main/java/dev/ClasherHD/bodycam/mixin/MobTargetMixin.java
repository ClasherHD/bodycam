package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.entity.ScholarEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public class MobTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void bodycam$preventTargetingScholar(LivingEntity target, CallbackInfo ci) {
        if (target instanceof ScholarEntity) {
            ci.cancel();
        }
    }
}
