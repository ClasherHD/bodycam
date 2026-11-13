package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.util.BodycamPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements BodycamPersistentDataHolder {
    @Unique
    private CompoundTag bodycam$persistentData;

    @Override
    public CompoundTag bodycam$getPersistentData() {
        if (this.bodycam$persistentData == null) {
            this.bodycam$persistentData = new CompoundTag();
        }
        return this.bodycam$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void bodycam$savePersistentData(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.bodycam$persistentData != null && !this.bodycam$persistentData.isEmpty()) {
            compound.put("BodycamPersistentData", this.bodycam$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void bodycam$loadPersistentData(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("BodycamPersistentData", 10)) {
            this.bodycam$persistentData = compound.getCompound("BodycamPersistentData").copy();
        }
    }
}
