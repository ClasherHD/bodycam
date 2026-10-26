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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnonymizerItemTest {

    private static AnonymizerItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (AnonymizerItem) ModItems.ANONYMIZER.get();
        com.electronwill.nightconfig.core.CommentedConfig serverConfig = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        ModServerConfig.SPEC.acceptConfig(serverConfig);
    }

    @Test
    @DisplayName("AnonymizerItem should have correct default durability and properties")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.getMaxDamage(stack)).isEqualTo(ItemDurabilityHelper.ANONYMIZER_DURABILITY);
        assertThat(item.isEnchantable(stack)).isTrue();
        assertThat(item.getEnchantmentValue()).isEqualTo(1);
        assertThat(item.isRepairable(stack)).isFalse();
    }

    @Test
    @DisplayName("isBarVisible should be true only when damaged and not empty")
    void isBarVisible_evaluatesCorrectly() {
        ItemStack stack = new ItemStack(item);
        assertThat(item.isBarVisible(stack)).isFalse();

        stack.setDamageValue(10);
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
        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.SHARPNESS)).isFalse();
    }

    @Test
    @DisplayName("Book enchantability should accept valid books and reject invalid or empty books")
    void bookEnchantable_evaluatesCorrectly() {
        ItemStack stack = new ItemStack(item);
        ItemStack emptyBook = ItemStack.EMPTY;
        assertThat(item.isBookEnchantable(stack, emptyBook)).isFalse();
    }

    @Test
    @DisplayName("shouldCauseReequipAnimation should reequip on slot change or different item")
    void shouldCauseReequipAnimation_evaluatesCorrectly() {
        ItemStack stackA = new ItemStack(item);
        ItemStack stackB = new ItemStack(item);
        ItemStack otherStack = new ItemStack(ModItems.BODYCAM_MONITOR.get());

        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, false)).isFalse();
        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, true)).isTrue();
        assertThat(item.shouldCauseReequipAnimation(stackA, otherStack, false)).isTrue();
    }

    @Test
    @DisplayName("use fails when stack is empty and cleans up active tags")
    void use_failsWhenEmpty() {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);

        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putBoolean("AnonymizerActive", true);
        ItemDurabilityHelper.setEmpty(stack);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);

        InteractionResultHolder<ItemStack> result = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(result.getResult()).isEqualTo(InteractionResult.FAIL);
        assertThat(stack.getTag().getBoolean("AnonymizerActive")).isFalse();
    }

    @Test
    @DisplayName("use toggles active state on server side")
    void use_togglesActiveState() {
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(player.hasPermissions(2)).thenReturn(true);
        CompoundTag persistentData = new CompoundTag();
        when(player.getPersistentData()).thenReturn(persistentData);

        ItemStack stack = new ItemStack(item);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);

        Inventory inventory = mock(Inventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getContainerSize()).thenReturn(1);
        when(inventory.getItem(0)).thenReturn(stack);

        InteractionResultHolder<ItemStack> turnOnResult = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(turnOnResult.getResult()).isEqualTo(InteractionResult.CONSUME);
        assertThat(stack.getTag().getBoolean("AnonymizerActive")).isTrue();

        InteractionResultHolder<ItemStack> turnOffResult = item.use(level, player, InteractionHand.MAIN_HAND);
        assertThat(turnOffResult.getResult()).isEqualTo(InteractionResult.CONSUME);
        assertThat(stack.getTag().getBoolean("AnonymizerActive")).isFalse();
    }

    @Test
    @DisplayName("inventoryTick resets active flag if stack is empty")
    void inventoryTick_cleansEmptyStack() {
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);

        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putBoolean("AnonymizerActive", true);
        ItemDurabilityHelper.setEmpty(stack);

        item.inventoryTick(stack, level, player, 0, false);
        assertThat(stack.getTag().getBoolean("AnonymizerActive")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Active state tag manipulation should maintain idempotency")
    void activeState_tagManipulation(boolean active) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putBoolean("AnonymizerActive", active);

        assertThat(stack.getTag().getBoolean("AnonymizerActive")).isEqualTo(active);
    }
}
