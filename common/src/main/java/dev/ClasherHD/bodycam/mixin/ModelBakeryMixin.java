package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.bodycam;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {
    @Unique
    private static final ModelResourceLocation BODYCAM_IN_HAND =
            ModelResourceLocation.inventory(bodycam.id("observation_spyglass/bodycam_monitor_in_hand"));
    @Unique
    private static final ModelResourceLocation BODYCAM_IN_HAND_EMPTY =
            ModelResourceLocation.inventory(bodycam.id("observation_spyglass/bodycam_monitor_in_hand_empty"));

    @Shadow
    private void loadSpecialItemModelAndDependencies(ModelResourceLocation modelLocation) {
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/ModelBakery;loadSpecialItemModelAndDependencies(Lnet/minecraft/client/resources/model/ModelResourceLocation;)V",
                    ordinal = 0
            )
    )
    private void bodycam$loadInHandModels(CallbackInfo ci) {
        this.loadSpecialItemModelAndDependencies(BODYCAM_IN_HAND);
        this.loadSpecialItemModelAndDependencies(BODYCAM_IN_HAND_EMPTY);
    }
}
