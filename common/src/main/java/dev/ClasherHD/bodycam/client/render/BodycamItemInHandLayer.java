package dev.ClasherHD.bodycam.client.render;

import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

@SuppressWarnings("null")
public class BodycamItemInHandLayer<S extends AvatarRenderState, M extends EntityModel<S> & ArmedModel<S> & HeadedModel>
        extends PlayerItemInHandLayer<S, M> {

    public BodycamItemInHandLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }
}
