package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.util.BodycamPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void bodycam$savePersistentData(ValueOutput output, CallbackInfo ci) {
        if (this.bodycam$persistentData != null && !this.bodycam$persistentData.isEmpty()) {
            output.store("BodycamPersistentData", CompoundTag.CODEC, this.bodycam$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void bodycam$loadPersistentData(ValueInput input, CallbackInfo ci) {
        input.read("BodycamPersistentData", CompoundTag.CODEC).ifPresent(tag -> this.bodycam$persistentData = tag.copy());
    }
}
