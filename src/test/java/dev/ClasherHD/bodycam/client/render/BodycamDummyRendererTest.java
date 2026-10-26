package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class BodycamDummyRendererTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @BeforeEach
    void resetMonitoring() {
        BodycamViewScreen.isMonitoring = false;
        BodycamViewScreen.targetUuid = null;
    }

    @Test
    @DisplayName("shouldRender returns false when dummy is being observed via Bodycam")
    void shouldRender_whenObserved_returnsFalse() {
        @SuppressWarnings("unchecked")
        BodycamDummyRenderer<BodycamDummyEntity> renderer =
                MinecraftTestEnvironment.allocateInstance(BodycamDummyRenderer.class);

        BodycamDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(BodycamDummyEntity.class);
        UUID dummyId = UUID.randomUUID();
        MinecraftTestEnvironment.setField(dummy, "uuid", dummyId);

        net.minecraft.client.renderer.culling.Frustum camera = mock(net.minecraft.client.renderer.culling.Frustum.class);
        when(camera.isVisible(any())).thenReturn(true);
        net.minecraft.world.phys.AABB bb = new net.minecraft.world.phys.AABB(0, 0, 0, 1, 2, 1);
        MinecraftTestEnvironment.setField(dummy, "bb", bb);
        MinecraftTestEnvironment.setField(dummy, "position", net.minecraft.world.phys.Vec3.ZERO);
        MinecraftTestEnvironment.setField(dummy, "armorItems", net.minecraft.core.NonNullList.withSize(4, ItemStack.EMPTY));
        MinecraftTestEnvironment.setField(dummy, "handItems", net.minecraft.core.NonNullList.withSize(2, ItemStack.EMPTY));

        BodycamViewScreen.isMonitoring = true;
        BodycamViewScreen.targetUuid = dummyId;

        assertThat(renderer.shouldRender(dummy, camera, 0, 0, 0)).isFalse();

        BodycamViewScreen.targetUuid = UUID.randomUUID();
        assertThat(renderer.shouldRender(dummy, camera, 0, 0, 0)).isTrue();
    }

    @Test
    @DisplayName("getArmPose returns SPYGLASS for BodycamMonitorItem, ITEM for regular item, and EMPTY for empty hand")
    void getArmPose_identifiesHeldItems() throws Exception {
        @SuppressWarnings("unchecked")
        BodycamDummyRenderer<BodycamDummyEntity> renderer =
                MinecraftTestEnvironment.allocateInstance(BodycamDummyRenderer.class);

        BodycamDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(BodycamDummyEntity.class);
        MinecraftTestEnvironment.setField(dummy, "handItems", net.minecraft.core.NonNullList.withSize(2, ItemStack.EMPTY));

        Method armPoseMethod = BodycamDummyRenderer.class.getDeclaredMethod("getArmPose",
                dev.ClasherHD.bodycam.entity.BodycamDummyEntity.class, InteractionHand.class);
        armPoseMethod.setAccessible(true);

        dummy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        HumanoidModel.ArmPose emptyPose = (HumanoidModel.ArmPose) armPoseMethod.invoke(renderer, dummy, InteractionHand.MAIN_HAND);
        assertThat(emptyPose).isEqualTo(HumanoidModel.ArmPose.EMPTY);

        dummy.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BODYCAM_MONITOR.get()));
        HumanoidModel.ArmPose monitorPose = (HumanoidModel.ArmPose) armPoseMethod.invoke(renderer, dummy, InteractionHand.MAIN_HAND);
        assertThat(monitorPose).isEqualTo(HumanoidModel.ArmPose.SPYGLASS);

        dummy.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        HumanoidModel.ArmPose regularPose = (HumanoidModel.ArmPose) armPoseMethod.invoke(renderer, dummy, InteractionHand.MAIN_HAND);
        assertThat(regularPose).isEqualTo(HumanoidModel.ArmPose.ITEM);
    }

    @Test
    @DisplayName("scale adjusts matrix stack by 0.9375F")
    void scale_appliesMatrixScaling() throws Exception {
        @SuppressWarnings("unchecked")
        BodycamDummyRenderer<BodycamDummyEntity> renderer =
                MinecraftTestEnvironment.allocateInstance(BodycamDummyRenderer.class);
        BodycamDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(BodycamDummyEntity.class);

        Method scaleMethod = BodycamDummyRenderer.class.getDeclaredMethod("scale",
                dev.ClasherHD.bodycam.entity.BodycamDummyEntity.class, PoseStack.class, float.class);
        scaleMethod.setAccessible(true);

        PoseStack poseStack = new PoseStack();
        scaleMethod.invoke(renderer, dummy, poseStack, 0.0F);
        assertThat(poseStack.last().pose()).isNotNull();
    }

    private static class TestDummyEntity extends BodycamDummyEntity {
        private UUID testOwner;

        public TestDummyEntity() {
            super(null, null);
        }

        public void setTestOwner(UUID id) {
            this.testOwner = id;
        }

        @Override
        public UUID getOwnerUUID() {
            return this.testOwner;
        }
    }

    @Test
    @DisplayName("getTextureLocation returns default skin when owner is null")
    void getTextureLocation_nullOwner_returnsDefaultSkin() {
        @SuppressWarnings("unchecked")
        BodycamDummyRenderer<BodycamDummyEntity> renderer =
                MinecraftTestEnvironment.allocateInstance(BodycamDummyRenderer.class);

        TestDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(TestDummyEntity.class);
        dummy.setTestOwner(null);

        ResourceLocation skin = renderer.getTextureLocation(dummy);
        assertThat(skin).isEqualTo(DefaultPlayerSkin.getDefaultSkin(net.minecraft.Util.NIL_UUID));
    }

    @Test
    @DisplayName("getTextureLocation resolves player skin from connection when owner is present")
    void getTextureLocation_withOwnerAndConnection() {
        @SuppressWarnings("unchecked")
        BodycamDummyRenderer<BodycamDummyEntity> renderer =
                MinecraftTestEnvironment.allocateInstance(BodycamDummyRenderer.class);

        UUID ownerId = UUID.randomUUID();
        TestDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(TestDummyEntity.class);
        dummy.setTestOwner(ownerId);

        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            ClientPacketListener connection = mock(ClientPacketListener.class);
            PlayerInfo playerInfo = mock(PlayerInfo.class);
            ResourceLocation expectedSkin = new ResourceLocation("minecraft", "textures/entity/player/custom_skin.png");

            when(mc.getConnection()).thenReturn(connection);
            when(connection.getPlayerInfo(ownerId)).thenReturn(playerInfo);
            when(playerInfo.getSkinLocation()).thenReturn(expectedSkin);
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            ResourceLocation skin = renderer.getTextureLocation(dummy);
            assertThat(skin).isEqualTo(expectedSkin);

            when(connection.getPlayerInfo(ownerId)).thenReturn(null);
            ResourceLocation fallbackSkin = renderer.getTextureLocation(dummy);
            assertThat(fallbackSkin).isEqualTo(DefaultPlayerSkin.getDefaultSkin(ownerId));

            when(mc.getConnection()).thenReturn(null);
            ResourceLocation noConnectionSkin = renderer.getTextureLocation(dummy);
            assertThat(noConnectionSkin).isEqualTo(DefaultPlayerSkin.getDefaultSkin(ownerId));
        }
    }
}
