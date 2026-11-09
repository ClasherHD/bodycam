package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.recipe.AncientTableRecipe;
import dev.ClasherHD.bodycam.recipe.LoggerRefillRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, bodycam.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, bodycam.MODID);

    public static final RegistryObject<RecipeType<AncientTableRecipe>> ANCIENT_TABLE_CRAFTING_TYPE =
            RECIPE_TYPES.register("ancient_table_crafting", () -> new RecipeType<AncientTableRecipe>() {
                @Override
                public String toString() {
                    return bodycam.MODID + ":ancient_table_crafting";
                }
            });

    public static final RegistryObject<RecipeSerializer<AncientTableRecipe>> ANCIENT_TABLE_CRAFTING_SERIALIZER =
            SERIALIZERS.register("ancient_table_crafting", AncientTableRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<LoggerRefillRecipe>> LOGGER_REFILL_SERIALIZER =
            SERIALIZERS.register("logger_refill", () -> new SimpleCraftingRecipeSerializer<>(LoggerRefillRecipe::new));
}
