package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ClasherHD.bodycam.block.PodestBlock;
import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PodestBlockEntityRendererTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("OUTLINE_OFFSETS contains 4 directional offset coordinates")
    void outlineOffsets_hasCorrectStructure() throws Exception {
        Field offsetsField = PodestBlockEntityRenderer.class.getDeclaredField("OUTLINE_OFFSETS");
        offsetsField.setAccessible(true);
        float[][] offsets = (float[][]) offsetsField.get(null);

        assertThat(offsets).hasDimensions(4, 2);
        assertThat(offsets[0]).containsExactly(-0.035F, 0.0F);
        assertThat(offsets[1]).containsExactly(0.035F, 0.0F);
        assertThat(offsets[2]).containsExactly(0.0F, -0.035F);
        assertThat(offsets[3]).containsExactly(0.0F, 0.035F);
    }

    @Test
    @DisplayName("render with empty held item returns early without touching ItemRenderer")
    void render_emptyItem_returnsImmediately() {
        BlockEntityRendererProvider.Context context = mock(BlockEntityRendererProvider.Context.class);
        ItemRenderer itemRenderer = mock(ItemRenderer.class);
        when(context.getItemRenderer()).thenReturn(itemRenderer);

        PodestBlockEntityRenderer renderer = new PodestBlockEntityRenderer(context);

        PodestBlockEntity podest = new PodestBlockEntity(BlockPos.ZERO, ModBlocks.PODEST.get().defaultBlockState());
        podest.setHeldItem(ItemStack.EMPTY);

        PoseStack poseStack = new PoseStack();
        MultiBufferSource bufferSource = mock(MultiBufferSource.class);

        renderer.render(podest, 0.0F, poseStack, bufferSource, 15, 0);

        verifyNoInteractions(itemRenderer);
    }

    @Test
    @DisplayName("render with active Jammer calls itemRenderer outline and static render")
    void render_activeJammer_rendersOutlineAndMain() {
        BlockEntityRendererProvider.Context context = mock(BlockEntityRendererProvider.Context.class);
        ItemRenderer itemRenderer = mock(ItemRenderer.class);
        when(context.getItemRenderer()).thenReturn(itemRenderer);

        BakedModel bakedModel = mock(BakedModel.class);
        when(itemRenderer.getModel(any(ItemStack.class), any(), any(), anyInt())).thenReturn(bakedModel);

        PodestBlockEntityRenderer renderer = new PodestBlockEntityRenderer(context);

        BlockState activeState = ModBlocks.PODEST.get().defaultBlockState()
                .setValue(PodestBlock.FACING, Direction.NORTH)
                .setValue(PodestBlock.POWERED, true)
                .setValue(PodestBlock.CHARGES, 5);

        PodestBlockEntity podest = new PodestBlockEntity(BlockPos.ZERO, activeState);
        ItemStack jammerStack = new ItemStack(ModItems.JAMMER.get());
        jammerStack.getOrCreateTag().putInt("JammerMode", 2);
        podest.setHeldItem(jammerStack);

        PoseStack poseStack = new PoseStack();
        MultiBufferSource bufferSource = mock(MultiBufferSource.class);
        VertexConsumer vertexConsumer = mock(VertexConsumer.class);
        when(bufferSource.getBuffer(any(RenderType.class))).thenReturn(vertexConsumer);

        renderer.render(podest, 0.0F, poseStack, bufferSource, 15, 0);

        verify(itemRenderer, times(4)).render(
                any(ItemStack.class),
                eq(ItemDisplayContext.FIXED),
                eq(false),
                any(PoseStack.class),
                any(MultiBufferSource.class),
                anyInt(),
                anyInt(),
                eq(bakedModel)
        );

        ArgumentCaptor<ItemStack> captor = ArgumentCaptor.forClass(ItemStack.class);
        verify(itemRenderer).renderStatic(
                captor.capture(),
                eq(ItemDisplayContext.FIXED),
                anyInt(),
                anyInt(),
                any(PoseStack.class),
                eq(bufferSource),
                any(),
                anyInt()
        );

        assertThat(captor.getValue().getItem()).isEqualTo(ModItems.JAMMER.get());
        assertThat(captor.getValue().getTag().getInt("JammerMode")).isEqualTo(2);
    }

    @Test
    @DisplayName("render with active Anonymizer sets AnonymizerActive tag on rendered stack")
    void render_activeAnonymizer_setsActiveTag() {
        BlockEntityRendererProvider.Context context = mock(BlockEntityRendererProvider.Context.class);
        ItemRenderer itemRenderer = mock(ItemRenderer.class);
        when(context.getItemRenderer()).thenReturn(itemRenderer);

        BakedModel bakedModel = mock(BakedModel.class);
        when(itemRenderer.getModel(any(ItemStack.class), any(), any(), anyInt())).thenReturn(bakedModel);

        PodestBlockEntityRenderer renderer = new PodestBlockEntityRenderer(context);

        BlockState activeState = ModBlocks.PODEST.get().defaultBlockState()
                .setValue(PodestBlock.FACING, Direction.SOUTH)
                .setValue(PodestBlock.POWERED, true)
                .setValue(PodestBlock.CHARGES, 3);

        PodestBlockEntity podest = new PodestBlockEntity(BlockPos.ZERO, activeState);
        ItemStack anonymizerStack = new ItemStack(ModItems.ANONYMIZER.get());
        podest.setHeldItem(anonymizerStack);

        PoseStack poseStack = new PoseStack();
        MultiBufferSource bufferSource = mock(MultiBufferSource.class);
        VertexConsumer vertexConsumer = mock(VertexConsumer.class);
        when(bufferSource.getBuffer(any(RenderType.class))).thenReturn(vertexConsumer);

        renderer.render(podest, 0.0F, poseStack, bufferSource, 15, 0);

        ArgumentCaptor<ItemStack> captor = ArgumentCaptor.forClass(ItemStack.class);
        verify(itemRenderer).renderStatic(
                captor.capture(),
                eq(ItemDisplayContext.FIXED),
                anyInt(),
                anyInt(),
                any(PoseStack.class),
                eq(bufferSource),
                any(),
                anyInt()
        );

        assertThat(captor.getValue().getItem()).isEqualTo(ModItems.ANONYMIZER.get());
        assertThat(captor.getValue().getTag().getBoolean("AnonymizerActive")).isTrue();
    }
}
