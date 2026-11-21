package dev.ClasherHD.bodycam.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@SuppressWarnings("null")
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @SuppressWarnings("unchecked")
    @Inject(
            method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <S extends EntityRenderState> void bodycam$preferRegisteredEntityRenderer(
            S state,
            CallbackInfoReturnable<EntityRenderer<?, ? super S>> cir
    ) {
        if (state instanceof AvatarRenderState
                && state.entityType != null
                && state.entityType != EntityTypes.PLAYER
                && state.entityType != EntityTypes.MANNEQUIN) {
            EntityRenderer<?, ?> registered = this.renderers.get(state.entityType);
            if (registered != null) {
                cir.setReturnValue((EntityRenderer<?, ? super S>) registered);
            }
        }
    }
}
