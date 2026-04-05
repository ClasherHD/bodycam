package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.util.IEntityPersistentData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityPersistentDataMixin implements IEntityPersistentData {

    @Unique
    private CompoundTag bodycam$persistentData = new CompoundTag();

    @Override
    public CompoundTag getPersistentData() {
        return this.bodycam$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void bodycam$saveData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (!this.bodycam$persistentData.isEmpty()) {
            tag.put("bodycam:PersistentData", this.bodycam$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void bodycam$loadData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("bodycam:PersistentData", 10)) {
            this.bodycam$persistentData = tag.getCompound("bodycam:PersistentData");
        }
    }
}
