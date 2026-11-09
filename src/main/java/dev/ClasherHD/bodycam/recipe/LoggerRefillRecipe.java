package dev.ClasherHD.bodycam.recipe;

import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.LoggerItem;
import dev.ClasherHD.bodycam.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class LoggerRefillRecipe extends CustomRecipe {

    public LoggerRefillRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private static int countPaper(CraftingContainer container) {
        int paper = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(Items.PAPER)) {
                paper++;
            }
        }
        return paper;
    }

    private static ItemStack findLogger(CraftingContainer container) {
        ItemStack logger = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || stack.is(Items.PAPER)) {
                continue;
            }
            if (!(stack.getItem() instanceof LoggerItem) || !logger.isEmpty()) {
                return ItemStack.EMPTY;
            }
            logger = stack;
        }
        return logger;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack logger = findLogger(container);
        if (logger.isEmpty() || ItemDurabilityHelper.isUnbreakable(logger)) {
            return false;
        }
        int paper = countPaper(container);
        return paper >= 1 && paper <= logger.getDamageValue();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack logger = findLogger(container);
        if (logger.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = logger.copy();
        result.setDamageValue(Math.max(0, result.getDamageValue() - countPaper(container)));
        return result;
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
