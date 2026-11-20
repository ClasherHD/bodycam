package dev.ClasherHD.bodycam.client.render;

import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("null")
public class WatchPointDummyRenderer extends BodycamDummyRenderer<WatchPointDummyEntity> {
    public WatchPointDummyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
