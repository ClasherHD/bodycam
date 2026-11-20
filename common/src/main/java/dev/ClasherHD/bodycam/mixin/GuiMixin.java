package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void bodycam$hideHudDuringMonitoring(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (BodycamViewScreen.isMonitoring) {
            ci.cancel();
        }
    }
}
