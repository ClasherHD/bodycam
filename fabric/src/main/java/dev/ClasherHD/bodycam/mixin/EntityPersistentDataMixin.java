package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.util.IEntityPersistentData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityPersistentDataMixin implements IEntityPersistentData {

    @Unique
    private CompoundTag bodycam$persistentData = new CompoundTag();

    @Override
    public CompoundTag getPersistentData() {
        return this.bodycam$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void bodycam$saveData(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        if (!this.bodycam$persistentData.isEmpty()) {
            output.store("bodycam:PersistentData", CompoundTag.CODEC, this.bodycam$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void bodycam$loadData(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        input.read("bodycam:PersistentData", CompoundTag.CODEC).ifPresent(tag -> {
            this.bodycam$persistentData = tag.copy();
        });
    }
}
