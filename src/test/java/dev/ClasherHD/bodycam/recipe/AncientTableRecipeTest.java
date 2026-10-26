package dev.ClasherHD.bodycam.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class AncientTableRecipeTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("CountedIngredient.EMPTY should match empty ItemStack and report 0 count")
    void countedIngredient_empty_matchesEmptyStack() {
        AncientTableRecipe.CountedIngredient emptyCi = AncientTableRecipe.CountedIngredient.EMPTY;

        assertThat(emptyCi.count()).isEqualTo(0);
        assertThat(emptyCi.ingredient().isEmpty()).isTrue();
        assertThat(emptyCi.matches(ItemStack.EMPTY)).isTrue();
    }

    @Test
    @DisplayName("AncientTableRecipe constructor should clamp crystalsRequired to a minimum of 1")
    void constructor_clampsCrystalsRequiredToMinimumOne() {
        ResourceLocation id = new ResourceLocation("bodycam", "test_recipe");
        AncientTableRecipe.CountedIngredient req = AncientTableRecipe.CountedIngredient.EMPTY;

        AncientTableRecipe recipeZero = new AncientTableRecipe(id, req, req, req, ItemStack.EMPTY, false, 0, 0);
        assertThat(recipeZero.getCrystalsRequired()).isEqualTo(1);

        AncientTableRecipe recipeNegative = new AncientTableRecipe(id, req, req, req, ItemStack.EMPTY, false, -5, 0);
        assertThat(recipeNegative.getCrystalsRequired()).isEqualTo(1);

        AncientTableRecipe recipePositive = new AncientTableRecipe(id, req, req, req, ItemStack.EMPTY, false, 4, 0);
        assertThat(recipePositive.getCrystalsRequired()).isEqualTo(4);
    }

    @Test
    @DisplayName("getDurationTicks should return customDurationTicks if positive")
    void getDurationTicks_withCustomDuration_returnsCustomDuration() {
        ResourceLocation id = new ResourceLocation("bodycam", "test_recipe_custom");
        AncientTableRecipe.CountedIngredient req = AncientTableRecipe.CountedIngredient.EMPTY;

        AncientTableRecipe recipe = new AncientTableRecipe(id, req, req, req, ItemStack.EMPTY, false, 1, 600);
        assertThat(recipe.getDurationTicks()).isEqualTo(600);
    }

    @Test
    @DisplayName("getDurationTicks should compute duration based on item counts when custom duration is 0")
    void getDurationTicks_withoutCustomDuration_computesDurationFromIngredients() {
        ResourceLocation id = new ResourceLocation("bodycam", "test_recipe_computed");
        AncientTableRecipe.CountedIngredient req0 = new AncientTableRecipe.CountedIngredient(Ingredient.EMPTY, 2);
        AncientTableRecipe.CountedIngredient req1 = new AncientTableRecipe.CountedIngredient(Ingredient.EMPTY, 3);
        AncientTableRecipe.CountedIngredient req2 = AncientTableRecipe.CountedIngredient.EMPTY;

        AncientTableRecipe recipe = new AncientTableRecipe(id, req0, req1, req2, ItemStack.EMPTY, false, 1, 0);
        int expectedTicks = (2 + 3 + 0) * 10 * 20;
        assertThat(recipe.getDurationTicks()).isEqualTo(expectedTicks);
    }

    @Test
    @DisplayName("matches should return false if container size is less than 3")
    void matches_withContainerSmallerThanThreeSlots_returnsFalse() {
        ResourceLocation id = new ResourceLocation("bodycam", "test_recipe_container");
        AncientTableRecipe.CountedIngredient req = AncientTableRecipe.CountedIngredient.EMPTY;
        AncientTableRecipe recipe = new AncientTableRecipe(id, req, req, req, ItemStack.EMPTY, false, 1, 0);

        SimpleContainer containerWith2Slots = new SimpleContainer(2);
        assertThat(recipe.matches(containerWith2Slots, null)).isFalse();
    }

    @Test
    @DisplayName("canCraftInDimensions and isSpecial should return true")
    void recipeProperties_returnExpectedDefaults() {
        ResourceLocation id = new ResourceLocation("bodycam", "test_recipe_props");
        AncientTableRecipe.CountedIngredient req = AncientTableRecipe.CountedIngredient.EMPTY;
        AncientTableRecipe recipe = new AncientTableRecipe(id, req, req, req, ItemStack.EMPTY, false, 1, 0);

        assertThat(recipe.canCraftInDimensions(1, 1)).isTrue();
        assertThat(recipe.canCraftInDimensions(3, 3)).isTrue();
        assertThat(recipe.isSpecial()).isTrue();
        assertThat(recipe.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("getDurationTicks should clamp total items to 1 when all ingredients have count 0")
    void getDurationTicks_whenAllIngredientsHaveZeroCount_clampsTotalItemsToOne() {
        ResourceLocation id = new ResourceLocation("bodycam", "test_zero_count");
        AncientTableRecipe.CountedIngredient req = AncientTableRecipe.CountedIngredient.EMPTY;
        AncientTableRecipe recipe = new AncientTableRecipe(id, req, req, req, ItemStack.EMPTY, false, 1, 0);

        int expectedTicks = 1 * 10 * 20;
        assertThat(recipe.getDurationTicks()).isEqualTo(expectedTicks);
    }

    @Test
    @DisplayName("getReq0, getReq1, getReq2 should return assigned CountedIngredients")
    void getters_returnConfiguredCountedIngredients() {
        ResourceLocation id = new ResourceLocation("bodycam", "test_getters");
        AncientTableRecipe.CountedIngredient req0 = new AncientTableRecipe.CountedIngredient(Ingredient.EMPTY, 1);
        AncientTableRecipe.CountedIngredient req1 = new AncientTableRecipe.CountedIngredient(Ingredient.EMPTY, 2);
        AncientTableRecipe.CountedIngredient req2 = new AncientTableRecipe.CountedIngredient(Ingredient.EMPTY, 3);
        AncientTableRecipe recipe = new AncientTableRecipe(id, req0, req1, req2, ItemStack.EMPTY, false, 1, 0);

        assertThat(recipe.getReq0()).isEqualTo(req0);
        assertThat(recipe.getReq1()).isEqualTo(req1);
        assertThat(recipe.getReq2()).isEqualTo(req2);
    }

    @Test
    @DisplayName("createOutputStack, assemble and getResultItem should return copies of output template")
    void outputStackMethods_returnTemplateCopies() {
        ResourceLocation id = new ResourceLocation("bodycam", "test_output");
        AncientTableRecipe recipe = new AncientTableRecipe(
                id,
                AncientTableRecipe.CountedIngredient.EMPTY,
                AncientTableRecipe.CountedIngredient.EMPTY,
                AncientTableRecipe.CountedIngredient.EMPTY,
                ItemStack.EMPTY,
                false,
                1,
                0
        );

        assertThat(recipe.createOutputStack().isEmpty()).isTrue();
        assertThat(recipe.assemble(null, null).isEmpty()).isTrue();
        assertThat(recipe.getResultItem(null).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("matches and matchSlots with empty requirements should match 3 empty slots")
    void matches_withEmptyRequirementsAnd3EmptySlots_returnsTrue() {
        ResourceLocation id = new ResourceLocation("bodycam", "test_empty_match");
        AncientTableRecipe recipe = new AncientTableRecipe(
                id,
                AncientTableRecipe.CountedIngredient.EMPTY,
                AncientTableRecipe.CountedIngredient.EMPTY,
                AncientTableRecipe.CountedIngredient.EMPTY,
                ItemStack.EMPTY,
                false,
                1,
                0
        );

        SimpleContainer container = new SimpleContainer(3);
        assertThat(recipe.matches(container, null)).isTrue();

        net.minecraftforge.items.ItemStackHandler handler = new net.minecraftforge.items.ItemStackHandler(3);
        int[] slots = recipe.matchSlots(handler);
        assertThat(slots).isNotNull().containsExactly(0, 1, 2);
    }
}
