package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.lang.reflect.Field;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class GlowingArmorTrimLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private final A innerModel;
    private final A outerModel;
    private final TextureAtlas armorTrimAtlas;

    @SuppressWarnings("unchecked")
    public GlowingArmorTrimLayer(RenderLayerParent<T, M> parent, HumanoidArmorLayer<T, M, A> parentArmorLayer) {
        super(parent);
        
        A inner = null;
        A outer = null;
        TextureAtlas atlas = null;
        try {
            for (Field field : HumanoidArmorLayer.class.getDeclaredFields()) {
                field.setAccessible(true);
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                if (field.getType() == TextureAtlas.class) {
                    atlas = (TextureAtlas) field.get(parentArmorLayer);
                } else if (HumanoidModel.class.isAssignableFrom(field.getType())) {
                    if (inner == null) {
                        inner = (A) field.get(parentArmorLayer);
                    } else if (outer == null) {
                        outer = (A) field.get(parentArmorLayer);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        this.innerModel = inner;
        this.outerModel = outer;
        this.armorTrimAtlas = atlas;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (this.innerModel == null || this.outerModel == null || this.armorTrimAtlas == null) {
            return;
        }
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == slot) {
                    Optional<ArmorTrim> trimOpt = ArmorTrim.getTrim(entity.level().registryAccess(), stack);
                    if (trimOpt.isPresent()) {
                        ArmorTrim trim = trimOpt.get();
                        String materialName = trim.material().value().assetName();
                        if ("observation_crystal".equals(materialName)) {
                            boolean useInner = (slot == EquipmentSlot.LEGS);
                            A armorModel = useInner ? this.innerModel : this.outerModel;
                            
                            this.getParentModel().copyPropertiesTo(armorModel);
                            this.setPartVisibility(armorModel, slot);
                            
                            ArmorMaterial material = armorItem.getMaterial();
                            TextureAtlasSprite sprite = this.armorTrimAtlas.getSprite(useInner ? trim.innerTexture(material) : trim.outerTexture(material));
                            VertexConsumer vertexConsumer = sprite.wrap(bufferSource.getBuffer(Sheets.armorTrimsSheet()));
                            
                            // 15728880 is LightTexture.pack(15, 15) which is full bright
                            armorModel.renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                        }
                    }
                }
            }
        }
    }

    protected void setPartVisibility(A model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD:
                model.head.visible = true;
                model.hat.visible = true;
                break;
            case CHEST:
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
                break;
            case LEGS:
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
                break;
            case FEET:
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
                break;
            default:
                break;
        }
    }
}
