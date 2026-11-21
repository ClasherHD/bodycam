package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void bodycam$hideHudDuringMonitoring(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (BodycamViewScreen.isMonitoring) {
            ci.cancel();
        }
    }
}
