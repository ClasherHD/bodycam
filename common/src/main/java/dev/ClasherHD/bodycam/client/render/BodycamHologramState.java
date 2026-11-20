package dev.ClasherHD.bodycam.client.render;

public interface BodycamHologramState {
    boolean bodycam$isHologram();

    void bodycam$setHologram(boolean hologram);

    float bodycam$getPartialTick();

    void bodycam$setPartialTick(float partialTick);

    long bodycam$getGameTime();

    void bodycam$setGameTime(long gameTime);
}
