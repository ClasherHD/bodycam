package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void bodycam$extractMonitorHeldOnHead(AvatarlikeEntity entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (entity.isUsingItem()) {
            ItemStack useItem = entity.getUseItem();
            if (useItem.is(ModItems.BODYCAM_MONITOR.get())) {
                ((LivingEntityRendererAccessor) this).bodycam$getItemModelResolver()
                        .updateForLiving(state.heldOnHead, useItem, ItemDisplayContext.HEAD, entity);
            }
        }
    }
}
