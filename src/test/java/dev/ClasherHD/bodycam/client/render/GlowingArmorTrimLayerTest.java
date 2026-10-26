package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlowingArmorTrimLayerTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("render terminates safely when models or atlas are null")
    void render_withNullFields_returnsEarlyWithoutException() {
        @SuppressWarnings("unchecked")
        GlowingArmorTrimLayer<?, ?, ?> layer =
                MinecraftTestEnvironment.allocateInstance(GlowingArmorTrimLayer.class);

        assertThatCode(() -> layer.render(null, null, 0, null, 0, 0, 0, 0, 0, 0))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("render iterates over armor slots safely when entity has no armor")
    void render_withEntityHavingNoArmor_completesNormally() {
        @SuppressWarnings("unchecked")
        RenderLayerParent<LivingEntity, HumanoidModel<LivingEntity>> parent = mock(RenderLayerParent.class);
        @SuppressWarnings("unchecked")
        HumanoidArmorLayer<LivingEntity, HumanoidModel<LivingEntity>, HumanoidModel<LivingEntity>> parentArmor =
                MinecraftTestEnvironment.allocateInstance(HumanoidArmorLayer.class);

        @SuppressWarnings("unchecked")
        HumanoidModel<LivingEntity> inner = MinecraftTestEnvironment.allocateInstance(HumanoidModel.class);
        @SuppressWarnings("unchecked")
        HumanoidModel<LivingEntity> outer = MinecraftTestEnvironment.allocateInstance(HumanoidModel.class);
        TextureAtlas atlas = mock(TextureAtlas.class);

        MinecraftTestEnvironment.setField(parentArmor, "innerModel", inner);
        MinecraftTestEnvironment.setField(parentArmor, "outerModel", outer);
        MinecraftTestEnvironment.setField(parentArmor, "armorTrimAtlas", atlas);

        GlowingArmorTrimLayer<LivingEntity, HumanoidModel<LivingEntity>, HumanoidModel<LivingEntity>> layer =
                new GlowingArmorTrimLayer<>(parent, parentArmor);

        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getItemBySlot(EquipmentSlot.HEAD)).thenReturn(ItemStack.EMPTY);
        when(entity.getItemBySlot(EquipmentSlot.CHEST)).thenReturn(ItemStack.EMPTY);
        when(entity.getItemBySlot(EquipmentSlot.LEGS)).thenReturn(ItemStack.EMPTY);
        when(entity.getItemBySlot(EquipmentSlot.FEET)).thenReturn(ItemStack.EMPTY);

        PoseStack poseStack = new PoseStack();
        MultiBufferSource bufferSource = mock(MultiBufferSource.class);

        assertThatCode(() -> layer.render(poseStack, bufferSource, 0, entity, 0, 0, 0, 0, 0, 0))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("setPartVisibility configures visibility for all armor slots")
    void setPartVisibility_configuresSlotsCorrectly() throws Exception {
        @SuppressWarnings("unchecked")
        GlowingArmorTrimLayer<?, ?, HumanoidModel<?>> layer =
                MinecraftTestEnvironment.allocateInstance(GlowingArmorTrimLayer.class);

        @SuppressWarnings("unchecked")
        HumanoidModel<?> model = MinecraftTestEnvironment.allocateInstance(HumanoidModel.class);

        MinecraftTestEnvironment.setField(model, "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()));
        MinecraftTestEnvironment.setField(model, "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()));
        MinecraftTestEnvironment.setField(model, "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()));
        MinecraftTestEnvironment.setField(model, "rightArm", new ModelPart(Collections.emptyList(), Collections.emptyMap()));
        MinecraftTestEnvironment.setField(model, "leftArm", new ModelPart(Collections.emptyList(), Collections.emptyMap()));
        MinecraftTestEnvironment.setField(model, "rightLeg", new ModelPart(Collections.emptyList(), Collections.emptyMap()));
        MinecraftTestEnvironment.setField(model, "leftLeg", new ModelPart(Collections.emptyList(), Collections.emptyMap()));

        Method setPartVisibility = GlowingArmorTrimLayer.class.getDeclaredMethod("setPartVisibility",
                HumanoidModel.class, EquipmentSlot.class);
        setPartVisibility.setAccessible(true);

        setPartVisibility.invoke(layer, model, EquipmentSlot.HEAD);
        assertThat(model.head.visible).isTrue();
        assertThat(model.hat.visible).isTrue();
        assertThat(model.body.visible).isFalse();
        assertThat(model.rightArm.visible).isFalse();
        assertThat(model.leftArm.visible).isFalse();
        assertThat(model.rightLeg.visible).isFalse();
        assertThat(model.leftLeg.visible).isFalse();

        setPartVisibility.invoke(layer, model, EquipmentSlot.CHEST);
        assertThat(model.head.visible).isFalse();
        assertThat(model.body.visible).isTrue();
        assertThat(model.rightArm.visible).isTrue();
        assertThat(model.leftArm.visible).isTrue();
        assertThat(model.rightLeg.visible).isFalse();
        assertThat(model.leftLeg.visible).isFalse();

        setPartVisibility.invoke(layer, model, EquipmentSlot.LEGS);
        assertThat(model.head.visible).isFalse();
        assertThat(model.body.visible).isTrue();
        assertThat(model.rightArm.visible).isFalse();
        assertThat(model.leftArm.visible).isFalse();
        assertThat(model.rightLeg.visible).isTrue();
        assertThat(model.leftLeg.visible).isTrue();

        setPartVisibility.invoke(layer, model, EquipmentSlot.FEET);
        assertThat(model.head.visible).isFalse();
        assertThat(model.body.visible).isFalse();
        assertThat(model.rightArm.visible).isFalse();
        assertThat(model.leftArm.visible).isFalse();
        assertThat(model.rightLeg.visible).isTrue();
        assertThat(model.leftLeg.visible).isTrue();
    }
}
