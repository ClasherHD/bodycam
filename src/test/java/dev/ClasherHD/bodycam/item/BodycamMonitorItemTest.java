package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class BodycamMonitorItemTest {

    private static BodycamMonitorItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (BodycamMonitorItem) ModItems.BODYCAM_MONITOR.get();
    }

    @Test
    @DisplayName("BodycamMonitorItem should have spyglass animation and 72000 tick use duration")
    void animationAndDuration_haveExpectedValues() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.getUseAnimation(stack)).isEqualTo(UseAnim.SPYGLASS);
        assertThat(item.getUseDuration(stack)).isEqualTo(72000);
    }

    @Test
    @DisplayName("Durability and enchantment properties should match spyglass configuration")
    void durabilityAndEnchantment_haveExpectedProperties() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.getMaxDamage(stack)).isEqualTo(ItemDurabilityHelper.SPYGLASS_DURABILITY);
        assertThat(item.isEnchantable(stack)).isTrue();
        assertThat(item.getEnchantmentValue()).isEqualTo(1);
        assertThat(item.isRepairable(stack)).isFalse();
    }

    @Test
    @DisplayName("canApplyAtEnchantingTable should reject Mending and reject Unbreaking since spyglass is not per-time")
    void enchantmentTableRules_evaluatesExpectedEnchantments() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.MENDING)).isFalse();
        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.UNBREAKING)).isFalse();
        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.SHARPNESS)).isFalse();
    }

    @Test
    @DisplayName("isBarVisible should be true only when damaged and not empty")
    void isBarVisible_evaluatesCorrectly() {
        ItemStack stack = new ItemStack(item);
        assertThat(item.isBarVisible(stack)).isFalse();

        stack.setDamageValue(5);
        assertThat(item.isBarVisible(stack)).isTrue();

        ItemDurabilityHelper.setEmpty(stack);
        assertThat(item.isBarVisible(stack)).isFalse();
    }

    @Test
    @DisplayName("use fails when stack is empty and consumes when stack is charged")
    void use_checksEmptyState() {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);

        ItemStack emptyStack = new ItemStack(item);
        ItemDurabilityHelper.setEmpty(emptyStack);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(emptyStack);

        InteractionResultHolder<ItemStack> failResult = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(failResult.getResult()).isEqualTo(InteractionResult.FAIL);

        ItemStack validStack = new ItemStack(item);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(validStack);

        InteractionResultHolder<ItemStack> consumeResult = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(consumeResult.getResult()).isEqualTo(InteractionResult.CONSUME);
        verify(player).startUsingItem(InteractionHand.MAIN_HAND);
    }

    @Test
    @DisplayName("finishUsingItem and releaseUsing play stop using sound")
    void stopUsing_playsSound() {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        when(player.level()).thenReturn(level);

        ItemStack stack = new ItemStack(item);
        ItemStack finished = item.finishUsingItem(stack, level, player);
        assertThat(finished).isEqualTo(stack);

        item.releaseUsing(stack, level, player, 100);
        verify(level, org.mockito.Mockito.times(2)).playSound(any(), anyDouble(), anyDouble(), anyDouble(), any(), any(), eq(1.0F), eq(1.5F));
    }

    @Test
    @DisplayName("shouldCauseReequipAnimation triggers on slot change or different items")
    void shouldCauseReequipAnimation_evaluatesChanges() {
        ItemStack stackA = new ItemStack(item);
        ItemStack stackB = new ItemStack(item);
        ItemStack other = new ItemStack(Items.STICK);

        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, true)).isTrue();
        assertThat(item.shouldCauseReequipAnimation(stackA, other, false)).isTrue();
        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, false)).isFalse();
    }
}
