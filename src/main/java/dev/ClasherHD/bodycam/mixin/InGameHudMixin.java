package dev.ClasherHD.bodycam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;

@Mixin(Gui.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRenderHud(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (BodycamViewScreen.isMonitoring) {
            ci.cancel();
        }
    }
}
