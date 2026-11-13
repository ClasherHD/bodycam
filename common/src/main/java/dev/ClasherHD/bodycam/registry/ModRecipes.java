package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.recipe.AncientTableRecipe;
import dev.ClasherHD.bodycam.recipe.LoggerRefillRecipe;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(bodycam.MODID, Registries.RECIPE_SERIALIZER);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(bodycam.MODID, Registries.RECIPE_TYPE);
    public static final DeferredRegister<RecipeType<?>> TYPES = RECIPE_TYPES;

    public static final RegistrySupplier<RecipeType<AncientTableRecipe>> ANCIENT_TABLE_CRAFTING_TYPE =
            RECIPE_TYPES.register("ancient_table_crafting", () -> new RecipeType<AncientTableRecipe>() {
                @Override
                public String toString() {
                    return bodycam.MODID + ":ancient_table_crafting";
                }
            });

    public static final RegistrySupplier<RecipeSerializer<AncientTableRecipe>> ANCIENT_TABLE_CRAFTING_SERIALIZER =
            SERIALIZERS.register("ancient_table_crafting", () -> AncientTableRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<LoggerRefillRecipe>> LOGGER_REFILL_SERIALIZER =
            SERIALIZERS.register("logger_refill", () -> new SimpleCraftingRecipeSerializer<>(LoggerRefillRecipe::new));

    public static void register() {
        SERIALIZERS.register();
        RECIPE_TYPES.register();
    }
}
