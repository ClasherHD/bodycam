package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.client.camera.CameraSnapState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "setup", at = @At("HEAD"))
    private void bodycam$onCameraSetupHead(
            BlockGetter level,
            Entity focusedEntity,
            boolean detached,
            boolean thirdPersonReverse,
            float partialTick,
            CallbackInfo ci
    ) {
        if (focusedEntity != null) {
            CameraSnapState.enforceRenderFrameGuard(focusedEntity);
        }
    }
}
