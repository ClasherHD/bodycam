package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortableNetherPortalItemTest {

    private static PortableNetherPortalItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (PortableNetherPortalItem) ModItems.PORTABLE_NETHER_PORTAL.get();
        com.electronwill.nightconfig.core.CommentedConfig serverConfig = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        ModServerConfig.SPEC.acceptConfig(serverConfig);
    }

    @Test
    @DisplayName("Constants should have expected configuration values")
    void constants_haveExpectedValues() {
        assertThat(PortableNetherPortalItem.MAX_CHARGES).isEqualTo(2);
        assertThat(PortableNetherPortalItem.CHANNEL_TICKS).isEqualTo(80);
        assertThat(PortableNetherPortalItem.TAG_CHARGES).isEqualTo("PortalCharges");
    }

    @Test
    @DisplayName("getCharges on freshly created stack without tag should return 0")
    void getCharges_withoutTag_returnsZero() {
        ItemStack stack = new ItemStack(item);
        assertThat(PortableNetherPortalItem.getCharges(stack)).isEqualTo(0);
    }

    @ParameterizedTest(name = "setCharges({0}) -> getCharges() = {1}")
    @CsvSource({
            "-10, 0",
            "-1, 0",
            "0, 0",
            "1, 1",
            "2, 2",
            "3, 2",
            "100, 2"
    })
    @DisplayName("setCharges should clamp values within range [0, MAX_CHARGES]")
    void setCharges_clampsWithinValidRange(int inputCharges, int expectedCharges) {
        ItemStack stack = new ItemStack(item);
        PortableNetherPortalItem.setCharges(stack, inputCharges);

        assertThat(PortableNetherPortalItem.getCharges(stack)).isEqualTo(expectedCharges);
    }

    @Test
    @DisplayName("Bar visibility, width, and color should reflect charge levels accurately")
    void barProperties_reflectChargeLevels() {
        ItemStack stack = new ItemStack(item);

        PortableNetherPortalItem.setCharges(stack, 0);
        assertThat(item.isBarVisible(stack)).isFalse();
        assertThat(item.getBarWidth(stack)).isEqualTo(0);

        PortableNetherPortalItem.setCharges(stack, 1);
        assertThat(item.isBarVisible(stack)).isTrue();
        assertThat(item.getBarWidth(stack)).isEqualTo(7);

        PortableNetherPortalItem.setCharges(stack, 2);
        assertThat(item.isBarVisible(stack)).isTrue();
        assertThat(item.getBarWidth(stack)).isEqualTo(13);

        assertThat(item.getBarColor(stack)).isEqualTo(0xAA00FF);
    }

    @Test
    @DisplayName("Use animation and duration should match bow channel semantics")
    void animationAndDuration_matchBowSemantics() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.getUseAnimation(stack)).isEqualTo(UseAnim.BOW);
        assertThat(item.getUseDuration(stack)).isEqualTo(80);
    }

    @Test
    @DisplayName("appendHoverText should add appropriate tooltip depending on charge status")
    void appendHoverText_addsExpectedTooltip() {
        ItemStack unchargedStack = new ItemStack(item);
        List<Component> unchargedTooltip = new ArrayList<>();
        item.appendHoverText(unchargedStack, null, unchargedTooltip, TooltipFlag.Default.NORMAL);

        assertThat(unchargedTooltip).hasSize(1);
        assertThat(unchargedTooltip.get(0).getString()).contains("tooltip.bodycam.portable_portal.uncharged");

        ItemStack chargedStack = new ItemStack(item);
        PortableNetherPortalItem.setCharges(chargedStack, 1);
        List<Component> chargedTooltip = new ArrayList<>();
        item.appendHoverText(chargedStack, null, chargedTooltip, TooltipFlag.Default.NORMAL);

        assertThat(chargedTooltip).hasSize(1);
        assertThat(chargedTooltip.get(0).getString()).contains("tooltip.bodycam.portable_portal.charges");
    }

    @Test
    @DisplayName("shouldCauseReequipAnimation should reequip on slot change or different item")
    void reequipAnimation_evaluatesCorrectly() {
        ItemStack stackA = new ItemStack(item);
        ItemStack stackB = new ItemStack(item);
        ItemStack other = new ItemStack(ModItems.ANONYMIZER.get());

        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, false)).isFalse();
        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, true)).isTrue();
        assertThat(item.shouldCauseReequipAnimation(stackA, other, false)).isTrue();
    }

    @Test
    @DisplayName("use fails when item has no charges")
    void use_failsWhenUncharged() {
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(player.hasPermissions(2)).thenReturn(true);
        when(player.getEyePosition()).thenReturn(Vec3.ZERO);
        when(level.clip(any())).thenReturn(BlockHitResult.miss(Vec3.ZERO, net.minecraft.core.Direction.UP, BlockPos.ZERO));
        when(player.getPersistentData()).thenReturn(new CompoundTag());

        ItemStack stack = new ItemStack(item);
        PortableNetherPortalItem.setCharges(stack, 0);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);

        InteractionResultHolder<ItemStack> result = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(result.getResult()).isEqualTo(InteractionResult.FAIL);
    }

    @Test
    @DisplayName("use fails when in invalid dimension such as the End")
    void use_failsInTheEnd() {
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        when(level.dimension()).thenReturn(Level.END);
        when(player.hasPermissions(2)).thenReturn(true);
        when(player.getEyePosition()).thenReturn(Vec3.ZERO);
        when(level.clip(any())).thenReturn(BlockHitResult.miss(Vec3.ZERO, net.minecraft.core.Direction.UP, BlockPos.ZERO));
        when(player.getPersistentData()).thenReturn(new CompoundTag());

        ItemStack stack = new ItemStack(item);
        PortableNetherPortalItem.setCharges(stack, 2);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);

        InteractionResultHolder<ItemStack> result = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(result.getResult()).isEqualTo(InteractionResult.FAIL);
    }

    @Test
    @DisplayName("useOn passes when clicked position is not near a portal")
    void useOn_passesWhenNotNearPortal() {
        Level level = mock(Level.class);
        BlockPos pos = new BlockPos(10, 64, 10);
        when(level.getBlockState(any())).thenReturn(Blocks.DIRT.defaultBlockState());

        ServerPlayer player = mock(ServerPlayer.class);
        ItemStack stack = new ItemStack(item);
        BlockHitResult hit = new BlockHitResult(Vec3.ZERO, net.minecraft.core.Direction.UP, pos, false);
        UseOnContext context = new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit);

        InteractionResult result = item.useOn(context);
        assertThat(result).isEqualTo(InteractionResult.PASS);
    }
}
