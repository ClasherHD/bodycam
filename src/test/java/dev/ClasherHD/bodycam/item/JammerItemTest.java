package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JammerItemTest {

    private static JammerItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (JammerItem) ModItems.JAMMER.get();
        com.electronwill.nightconfig.core.CommentedConfig serverConfig = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        ModServerConfig.SPEC.acceptConfig(serverConfig);
    }

    @Test
    @DisplayName("JammerItem should have expected default durability and properties")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.getMaxDamage(stack)).isEqualTo(ItemDurabilityHelper.JAMMER_DURABILITY);
        assertThat(item.isEnchantable(stack)).isTrue();
        assertThat(item.getEnchantmentValue()).isEqualTo(1);
        assertThat(item.isRepairable(stack)).isFalse();
    }

    @Test
    @DisplayName("isBarVisible should be true only when damaged and not empty")
    void isBarVisible_evaluatesCorrectly() {
        ItemStack stack = new ItemStack(item);
        assertThat(item.isBarVisible(stack)).isFalse();

        stack.setDamageValue(20);
        assertThat(item.isBarVisible(stack)).isTrue();

        ItemDurabilityHelper.setEmpty(stack);
        assertThat(item.isBarVisible(stack)).isFalse();
    }

    @Test
    @DisplayName("Enchantment rules should allow Unbreaking and reject Mending")
    void enchantmentRules_adhereToPolicy() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.UNBREAKING)).isTrue();
        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.MENDING)).isFalse();
        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.FIRE_ASPECT)).isFalse();
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

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    @DisplayName("Mode cycling via use should advance 0 -> 1 -> 2 -> 0")
    void modeCycling_advancesProperly(int currentMode) {
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(player.hasPermissions(2)).thenReturn(true);
        CompoundTag persistentData = new CompoundTag();
        when(player.getPersistentData()).thenReturn(persistentData);

        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt("JammerMode", currentMode);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);

        Inventory inventory = mock(Inventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getContainerSize()).thenReturn(1);
        when(inventory.getItem(0)).thenReturn(stack);

        InteractionResultHolder<ItemStack> result = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(result.getResult()).isEqualTo(InteractionResult.CONSUME);

        int expectedNext = (currentMode + 1) % 3;
        assertThat(stack.getTag().getInt("JammerMode")).isEqualTo(expectedNext);
    }

    @Test
    @DisplayName("use fails when stack is empty and resets JammerMode")
    void use_failsWhenEmpty() {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);

        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt("JammerMode", 1);
        ItemDurabilityHelper.setEmpty(stack);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);

        InteractionResultHolder<ItemStack> result = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(result.getResult()).isEqualTo(InteractionResult.FAIL);
        assertThat(stack.getTag().getInt("JammerMode")).isEqualTo(0);
    }

    @Test
    @DisplayName("inventoryTick resets JammerMode to 0 if stack is empty")
    void inventoryTick_resetsEmptyJammer() {
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);

        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt("JammerMode", 2);
        ItemDurabilityHelper.setEmpty(stack);

        item.inventoryTick(stack, level, player, 0, false);
        assertThat(stack.getTag().getInt("JammerMode")).isEqualTo(0);
    }
}
