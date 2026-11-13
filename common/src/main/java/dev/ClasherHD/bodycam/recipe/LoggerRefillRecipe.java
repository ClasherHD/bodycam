package dev.ClasherHD.bodycam.recipe;

import dev.ClasherHD.bodycam.item.LoggerItem;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class LoggerRefillRecipe extends CustomRecipe {
    public LoggerRefillRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack logger = ItemStack.EMPTY;
        int paperCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.LOGGER.get())) {
                if (!logger.isEmpty()) {
                    return false;
                }
                logger = stack;
            } else if (stack.is(Items.PAPER) || stack.is(ModItems.LOG_PAPER.get())) {
                paperCount++;
            } else {
                return false;
            }
        }
        return !logger.isEmpty() && paperCount > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack logger = ItemStack.EMPTY;
        int paperCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModItems.LOGGER.get())) {
                logger = stack.copy();
            } else if (stack.is(Items.PAPER) || stack.is(ModItems.LOG_PAPER.get())) {
                paperCount++;
            }
        }
        if (logger.isEmpty()) {
            return ItemStack.EMPTY;
        }
        LoggerItem.refuel(logger, paperCount);
        return logger;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.LOGGER_REFILL_SERIALIZER.get();
    }
}
