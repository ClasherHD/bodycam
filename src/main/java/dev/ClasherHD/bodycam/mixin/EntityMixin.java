package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.util.PersistentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements PersistentData {

    @Unique
    private CompoundTag bodycamPersistentData;

    @Override
    public CompoundTag bodycam$getPersistentData() {
        if (this.bodycamPersistentData == null) {
            this.bodycamPersistentData = new CompoundTag();
        }
        return this.bodycamPersistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void saveBodycamData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.bodycamPersistentData != null && !this.bodycamPersistentData.isEmpty()) {
            tag.put("ForgeData", this.bodycamPersistentData);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void loadBodycamData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("ForgeData", 10)) {
            this.bodycamPersistentData = tag.getCompound("ForgeData");
        }
    }
}
