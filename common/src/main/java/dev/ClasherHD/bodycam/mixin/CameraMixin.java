package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.client.camera.CameraSnapState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private Entity entity;

    @Inject(method = "update", at = @At("HEAD"))
    private void bodycam$onCameraUpdateHead(DeltaTracker deltaTracker, CallbackInfo ci) {
        Entity focusedEntity = this.entity != null ? this.entity : this.minecraft.player;
        if (focusedEntity != null) {
            CameraSnapState.enforceRenderFrameGuard(focusedEntity);
        }
    }
}
