package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DimensionLocatorItemTest {

    private static DimensionLocatorItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (DimensionLocatorItem) ModItems.DIMENSION_LOCATOR.get();
        com.electronwill.nightconfig.core.CommentedConfig serverConfig = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        ModServerConfig.SPEC.acceptConfig(serverConfig);
    }

    @Test
    @DisplayName("DimensionLocatorItem should have expected durability and unenchantable properties")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.getMaxDamage(stack)).isEqualTo(ItemDurabilityHelper.DIMENSION_LOCATOR_DURABILITY);
        assertThat(item.isEnchantable(stack)).isFalse();
        assertThat(item.isRepairable(stack)).isFalse();
        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.UNBREAKING)).isFalse();
        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.MENDING)).isFalse();
        assertThat(item.isBookEnchantable(stack, ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("onCraftedBy should mark stack as empty (uncharged)")
    void onCraftedBy_marksStackAsEmpty() {
        ItemStack stack = new ItemStack(item);
        assertThat(ItemDurabilityHelper.isEmpty(stack)).isFalse();

        item.onCraftedBy(stack, null, null);
        assertThat(ItemDurabilityHelper.isEmpty(stack)).isTrue();
    }

    @Test
    @DisplayName("isBarVisible should be true only when damaged and not empty")
    void isBarVisible_evaluatesCorrectly() {
        ItemStack stack = new ItemStack(item);
        assertThat(item.isBarVisible(stack)).isFalse();

        stack.setDamageValue(2);
        assertThat(item.isBarVisible(stack)).isTrue();

        ItemDurabilityHelper.setEmpty(stack);
        assertThat(item.isBarVisible(stack)).isFalse();
    }

    @Test
    @DisplayName("shouldCauseReequipAnimation should handle same item vs other item and slot changes")
    void reequipAnimation_evaluatesCorrectly() {
        ItemStack stackA = new ItemStack(item);
        ItemStack stackB = new ItemStack(item);
        ItemStack other = new ItemStack(ModItems.ANONYMIZER.get());

        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, false)).isFalse();
        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, true)).isTrue();
        assertThat(item.shouldCauseReequipAnimation(stackA, other, false)).isTrue();
    }

    @Test
    @DisplayName("use fails when item is empty")
    void use_failsWhenEmpty() {
        Player player = mock(Player.class);
        Level level = mock(Level.class);

        ItemStack emptyStack = new ItemStack(item);
        ItemDurabilityHelper.setEmpty(emptyStack);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(emptyStack);

        InteractionResultHolder<ItemStack> result = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(result.getResult()).isEqualTo(InteractionResult.FAIL);
    }

    @Test
    @DisplayName("use on client side returns success for charged item")
    void use_clientSideReturnsSuccess() {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(true);

        ItemStack stack = new ItemStack(item);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);

        InteractionResultHolder<ItemStack> result = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(result.getResult()).isEqualTo(InteractionResult.SUCCESS);
    }

    @Test
    @DisplayName("use on server side fails when OP_ONLY_MODE is enabled and player is not OP")
    void use_opOnlyCheck() {
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        when(player.hasPermissions(2)).thenReturn(false);

        ItemStack stack = new ItemStack(item);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);

        ModServerConfig.OP_ONLY_MODE.set(true);
        InteractionResultHolder<ItemStack> result = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(result.getResult()).isEqualTo(InteractionResult.FAIL);
        ModServerConfig.OP_ONLY_MODE.set(false);
    }
}
