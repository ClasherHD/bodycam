package dev.ClasherHD.bodycam.recipe;

import com.mojang.serialization.MapCodec;
import dev.ClasherHD.bodycam.item.LoggerItem;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class LoggerRefillRecipe extends CustomRecipe {
    public static final LoggerRefillRecipe INSTANCE = new LoggerRefillRecipe();
    public static final MapCodec<LoggerRefillRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, LoggerRefillRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<LoggerRefillRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public LoggerRefillRecipe() {
        super();
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
    public ItemStack assemble(CraftingInput input) {
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

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<LoggerRefillRecipe> getSerializer() {
        return ModRecipes.LOGGER_REFILL_SERIALIZER.get();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LoggerRefillRecipe;
    }

    @Override
    public int hashCode() {
        return LoggerRefillRecipe.class.hashCode();
    }
}
