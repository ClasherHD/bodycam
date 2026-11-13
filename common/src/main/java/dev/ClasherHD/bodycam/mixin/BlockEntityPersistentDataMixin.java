package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.util.BodycamPersistentDataHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityPersistentDataMixin implements BodycamPersistentDataHolder {
    @Unique
    private CompoundTag bodycam$persistentData;

    @Override
    public CompoundTag bodycam$getPersistentData() {
        if (this.bodycam$persistentData == null) {
            this.bodycam$persistentData = new CompoundTag();
        }
        return this.bodycam$persistentData;
    }

    @Inject(method = "saveAdditional", at = @At("HEAD"))
    private void bodycam$savePersistentData(CompoundTag compound, HolderLookup.Provider registries, CallbackInfo ci) {
        if (this.bodycam$persistentData != null && !this.bodycam$persistentData.isEmpty()) {
            compound.put("BodycamPersistentData", this.bodycam$persistentData.copy());
        }
    }

    @Inject(method = "loadAdditional", at = @At("HEAD"))
    private void bodycam$loadPersistentData(CompoundTag compound, HolderLookup.Provider registries, CallbackInfo ci) {
        if (compound.contains("BodycamPersistentData", 10)) {
            this.bodycam$persistentData = compound.getCompound("BodycamPersistentData").copy();
        }
    }
}
