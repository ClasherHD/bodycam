package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class BodycamItemInHandLayerTest {

    interface TestModelInterface extends ArmedModel, HeadedModel {}
    static abstract class TestEntityModel<T extends LivingEntity> extends EntityModel<T> implements TestModelInterface {}

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("renderArmWithItem renders at eye using spyglass pose when player is using BodycamMonitorItem without swing")
    void renderArmWithItem_playerUsingMonitor() {
        @SuppressWarnings("unchecked")
        RenderLayerParent<LivingEntity, TestEntityModel<LivingEntity>> parent = mock(RenderLayerParent.class);
        @SuppressWarnings("unchecked")
        TestEntityModel<LivingEntity> model = mock(TestEntityModel.class);
        ModelPart headPart = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        when(model.getHead()).thenReturn(headPart);
        when(parent.getModel()).thenReturn(model);

        ItemInHandRenderer itemInHandRenderer = mock(ItemInHandRenderer.class);
        BodycamItemInHandLayer<LivingEntity, TestEntityModel<LivingEntity>> layer =
                new BodycamItemInHandLayer<>(parent, itemInHandRenderer);

        ServerPlayer player = MinecraftTestEnvironment.allocateInstance(ServerPlayer.class);
        ItemStack monitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());
        MinecraftTestEnvironment.setField(player, "useItem", monitor);
        player.swingTime = 0;

        PoseStack poseStack = new PoseStack();
        MultiBufferSource buffer = mock(MultiBufferSource.class);

        layer.renderArmWithItem(player, monitor, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                HumanoidArm.RIGHT, poseStack, buffer, 15);

        verify(itemInHandRenderer).renderItem(eq(player), eq(monitor), eq(ItemDisplayContext.HEAD),
                eq(false), any(PoseStack.class), eq(buffer), eq(15));
    }

    @Test
    @DisplayName("renderArmWithItem renders at eye for dummy holding BodycamMonitorItem without swing")
    void renderArmWithItem_dummyHoldingMonitor() {
        @SuppressWarnings("unchecked")
        RenderLayerParent<LivingEntity, TestEntityModel<LivingEntity>> parent = mock(RenderLayerParent.class);
        @SuppressWarnings("unchecked")
        TestEntityModel<LivingEntity> model = mock(TestEntityModel.class);
        ModelPart headPart = new ModelPart(Collections.emptyList(), Collections.emptyMap());
        when(model.getHead()).thenReturn(headPart);
        when(parent.getModel()).thenReturn(model);

        ItemInHandRenderer itemInHandRenderer = mock(ItemInHandRenderer.class);
        BodycamItemInHandLayer<LivingEntity, TestEntityModel<LivingEntity>> layer =
                new BodycamItemInHandLayer<>(parent, itemInHandRenderer);

        BodycamDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(BodycamDummyEntity.class);
        dummy.swingTime = 0;
        ItemStack monitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());

        PoseStack poseStack = new PoseStack();
        MultiBufferSource buffer = mock(MultiBufferSource.class);

        layer.renderArmWithItem(dummy, monitor, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                HumanoidArm.RIGHT, poseStack, buffer, 15);

        verify(itemInHandRenderer).renderItem(eq(dummy), eq(monitor), eq(ItemDisplayContext.HEAD),
                eq(false), any(PoseStack.class), eq(buffer), eq(15));
    }

    @Test
    @DisplayName("renderArmWithItem falls back to standard rendering when swinging")
    void renderArmWithItem_swingingFallsBack() {
        @SuppressWarnings("unchecked")
        RenderLayerParent<LivingEntity, TestEntityModel<LivingEntity>> parent = mock(RenderLayerParent.class);
        @SuppressWarnings("unchecked")
        TestEntityModel<LivingEntity> model = mock(TestEntityModel.class);
        when(parent.getModel()).thenReturn(model);

        ItemInHandRenderer itemInHandRenderer = mock(ItemInHandRenderer.class);
        BodycamItemInHandLayer<LivingEntity, TestEntityModel<LivingEntity>> layer =
                new BodycamItemInHandLayer<>(parent, itemInHandRenderer);

        ServerPlayer player = MinecraftTestEnvironment.allocateInstance(ServerPlayer.class);
        ItemStack monitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());
        MinecraftTestEnvironment.setField(player, "useItem", monitor);
        player.swingTime = 5;

        PoseStack poseStack = new PoseStack();
        MultiBufferSource buffer = mock(MultiBufferSource.class);

        layer.renderArmWithItem(player, monitor, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                HumanoidArm.RIGHT, poseStack, buffer, 15);

        verify(model).translateToHand(eq(HumanoidArm.RIGHT), any(PoseStack.class));
    }
}
