package dev.ClasherHD.bodycam.recipe;

import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModRecipes;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class LoggerRefillRecipeTest {

    private static LoggerRefillRecipe recipe;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        recipe = new LoggerRefillRecipe(
                new ResourceLocation("bodycam", "test_logger_refill"),
                CraftingBookCategory.MISC
        );
    }

    @Test
    @DisplayName("canCraftInDimensions should return true if area is at least 2")
    void canCraftInDimensions_evaluatesDimensions() {
        assertThat(recipe.canCraftInDimensions(1, 1)).isFalse();
        assertThat(recipe.canCraftInDimensions(2, 1)).isTrue();
        assertThat(recipe.canCraftInDimensions(3, 3)).isTrue();
    }

    @Test
    @DisplayName("getSerializer should return LOGGER_REFILL_SERIALIZER")
    void getSerializer_returnsCorrectSerializer() {
        assertThat(recipe.getSerializer()).isEqualTo(ModRecipes.LOGGER_REFILL_SERIALIZER.get());
    }

    @Test
    @DisplayName("matches and assemble should refill damaged logger with paper")
    void matchesAndAssemble_refillsDamagedLogger() {
        ItemStack damagedLogger = new ItemStack(ModItems.LOGGER.get());
        damagedLogger.setDamageValue(4);

        CraftingContainer container = mock(CraftingContainer.class);
        when(container.getContainerSize()).thenReturn(9);
        when(container.getItem(0)).thenReturn(damagedLogger);
        when(container.getItem(1)).thenReturn(new ItemStack(Items.PAPER));
        when(container.getItem(2)).thenReturn(new ItemStack(Items.PAPER));
        for (int i = 3; i < 9; i++) {
            when(container.getItem(i)).thenReturn(ItemStack.EMPTY);
        }

        assertThat(recipe.matches(container, null)).isTrue();

        ItemStack result = recipe.assemble(container, RegistryAccess.EMPTY);
        assertThat(result.getItem()).isEqualTo(ModItems.LOGGER.get());
        assertThat(result.getDamageValue()).isEqualTo(2); // 4 - 2 paper = 2
    }

    @Test
    @DisplayName("matches should reject undamaged logger or unbreakable logger")
    void matches_rejectsUndamagedOrUnbreakable() {
        ItemStack undamaged = new ItemStack(ModItems.LOGGER.get());
        undamaged.setDamageValue(0);

        CraftingContainer container = mock(CraftingContainer.class);
        when(container.getContainerSize()).thenReturn(2);
        when(container.getItem(0)).thenReturn(undamaged);
        when(container.getItem(1)).thenReturn(new ItemStack(Items.PAPER));

        assertThat(recipe.matches(container, null)).isFalse();

        ItemStack unbreakable = new ItemStack(ModItems.LOGGER.get());
        unbreakable.setDamageValue(4);
        unbreakable.getOrCreateTag().putBoolean("Unbreakable", true);
        when(container.getItem(0)).thenReturn(unbreakable);

        assertThat(recipe.matches(container, null)).isFalse();
    }

    @Test
    @DisplayName("matches should reject crafting if paper count exceeds damage")
    void matches_rejectsPaperCountExceedingDamage() {
        ItemStack damagedLogger = new ItemStack(ModItems.LOGGER.get());
        damagedLogger.setDamageValue(1);

        CraftingContainer container = mock(CraftingContainer.class);
        when(container.getContainerSize()).thenReturn(3);
        when(container.getItem(0)).thenReturn(damagedLogger);
        when(container.getItem(1)).thenReturn(new ItemStack(Items.PAPER));
        when(container.getItem(2)).thenReturn(new ItemStack(Items.PAPER));

        assertThat(recipe.matches(container, null)).isFalse();
    }
}
