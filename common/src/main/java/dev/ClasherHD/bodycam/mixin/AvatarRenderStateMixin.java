package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.client.render.BodycamHologramState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements BodycamHologramState {
    @Unique
    private boolean bodycam$isHologram;
    @Unique
    private float bodycam$partialTick;
    @Unique
    private long bodycam$gameTime;

    @Override
    public boolean bodycam$isHologram() {
        return this.bodycam$isHologram;
    }

    @Override
    public void bodycam$setHologram(boolean hologram) {
        this.bodycam$isHologram = hologram;
    }

    @Override
    public float bodycam$getPartialTick() {
        return this.bodycam$partialTick;
    }

    @Override
    public void bodycam$setPartialTick(float partialTick) {
        this.bodycam$partialTick = partialTick;
    }

    @Override
    public long bodycam$getGameTime() {
        return this.bodycam$gameTime;
    }

    @Override
    public void bodycam$setGameTime(long gameTime) {
        this.bodycam$gameTime = gameTime;
    }
}
