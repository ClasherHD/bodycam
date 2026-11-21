package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

@SuppressWarnings("null")
public class GlowingArmorTrimLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private static final String OBSERVATION_CRYSTAL_SUFFIX = "observation_crystal";

    private final ArmorModelSet<HumanoidModel<S>> armorModels;

    public GlowingArmorTrimLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.armorModels = ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, modelSet, HumanoidModel::new);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, S state,
            float yRot, float xRot) {
        renderTrimPiece(poseStack, submitNodeCollector, state, state.chestEquipment, EquipmentSlot.CHEST);
        renderTrimPiece(poseStack, submitNodeCollector, state, state.legsEquipment, EquipmentSlot.LEGS);
        renderTrimPiece(poseStack, submitNodeCollector, state, state.feetEquipment, EquipmentSlot.FEET);
        renderTrimPiece(poseStack, submitNodeCollector, state, state.headEquipment, EquipmentSlot.HEAD);
    }

    private void renderTrimPiece(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, S state,
            ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) {
            return;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot() != slot || equippable.assetId().isEmpty()) {
            return;
        }
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim == null || !OBSERVATION_CRYSTAL_SUFFIX.equals(trim.material().value().assets().base().suffix())) {
            return;
        }

        EquipmentClientInfo.LayerType layerType = slot == EquipmentSlot.LEGS
                ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
                : EquipmentClientInfo.LayerType.HUMANOID;
        Identifier spriteId = trim.layerAssetId(layerType.trimAssetPrefix(), equippable.assetId().get());
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.ARMOR_TRIMS)
                .getSprite(spriteId);
        HumanoidModel<S> armorModel = this.armorModels.get(slot);
        submitNodeCollector.order(4).submitModel(
                armorModel,
                state,
                poseStack,
                Sheets.armorTrimsSheet(trim.pattern().value().decal()),
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                -1,
                sprite,
                state.outlineColor,
                null
        );
    }
}
