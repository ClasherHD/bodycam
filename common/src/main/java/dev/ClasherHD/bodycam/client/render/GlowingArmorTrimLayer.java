package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

public class GlowingArmorTrimLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private HumanoidModel<T> innerModel;
    private HumanoidModel<T> outerModel;

    public GlowingArmorTrimLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    private void ensureModels() {
        if (this.innerModel == null || this.outerModel == null) {
            EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
            this.innerModel = new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
            this.outerModel = new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        }
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            T livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        TextureAtlas armorTrimAtlas = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = livingEntity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armorItem) || armorItem.getEquipmentSlot() != slot) {
                continue;
            }
            ArmorTrim trim = stack.get(DataComponents.TRIM);
            if (trim == null || !"observation_crystal".equals(trim.material().value().assetName())) {
                continue;
            }
            if (armorTrimAtlas == null) {
                ensureModels();
                armorTrimAtlas = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET);
            }
            boolean useInner = slot == EquipmentSlot.LEGS;
            HumanoidModel<T> armorModel = useInner ? this.innerModel : this.outerModel;
            this.getParentModel().copyPropertiesTo(armorModel);
            setPartVisibility(armorModel, slot);
            TextureAtlasSprite sprite = armorTrimAtlas.getSprite(
                    useInner ? trim.innerTexture(armorItem.getMaterial()) : trim.outerTexture(armorItem.getMaterial())
            );
            VertexConsumer vertexConsumer = sprite.wrap(buffer.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())));
            armorModel.renderToBuffer(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }

    private static void setPartVisibility(HumanoidModel<?> model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }
}
