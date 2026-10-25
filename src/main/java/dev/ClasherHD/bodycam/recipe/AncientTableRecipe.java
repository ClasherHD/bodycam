package dev.ClasherHD.bodycam.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("null")
public class AncientTableRecipe implements Recipe<SimpleContainer> {

    private static final int[][] SLOT_PERMUTATIONS = {
            {0, 1, 2},
            {0, 2, 1},
            {1, 0, 2},
            {1, 2, 0},
            {2, 0, 1},
            {2, 1, 0}
    };

    public record CountedIngredient(Ingredient ingredient, int count) {
        public static final CountedIngredient EMPTY = new CountedIngredient(Ingredient.EMPTY, 0);

        public boolean matches(ItemStack stack) {
            if (ingredient.isEmpty() || count <= 0) {
                return stack.isEmpty();
            }
            return !stack.isEmpty() && ingredient.test(stack) && stack.getCount() >= count;
        }
    }

    private final ResourceLocation id;
    private final CountedIngredient req0;
    private final CountedIngredient req1;
    private final CountedIngredient req2;
    private final ItemStack outputTemplate;
    private final boolean startEmpty;
    private final int crystalsRequired;
    private final int customDurationTicks;

    public AncientTableRecipe(
            ResourceLocation id,
            CountedIngredient req0,
            CountedIngredient req1,
            CountedIngredient req2,
            ItemStack outputTemplate,
            boolean startEmpty,
            int crystalsRequired,
            int customDurationTicks
    ) {
        this.id = id;
        this.req0 = req0;
        this.req1 = req1;
        this.req2 = req2;
        this.outputTemplate = outputTemplate;
        this.startEmpty = startEmpty;
        this.crystalsRequired = Math.max(1, crystalsRequired);
        this.customDurationTicks = customDurationTicks;
    }

    public CountedIngredient getReq0() {
        return req0;
    }

    public CountedIngredient getReq1() {
        return req1;
    }

    public CountedIngredient getReq2() {
        return req2;
    }

    public int getCrystalsRequired() {
        return crystalsRequired;
    }

    public int getDurationTicks() {
        if (customDurationTicks > 0) {
            return customDurationTicks;
        }
        int totalItems = Math.max(1, req0.count() + req1.count() + req2.count());
        int secondsPerItem = ModServerConfig.SPEC.isLoaded()
                ? ModServerConfig.ANCIENT_TABLE_CRAFT_SECONDS_PER_ITEM.get()
                : 10;
        return totalItems * secondsPerItem * 20;
    }

    public int[] matchSlots(ItemStackHandler handler) {
        for (int[] perm : SLOT_PERMUTATIONS) {
            if (req0.matches(handler.getStackInSlot(perm[0]))
                    && req1.matches(handler.getStackInSlot(perm[1]))
                    && req2.matches(handler.getStackInSlot(perm[2]))) {
                return perm;
            }
        }
        return null;
    }

    public ItemStack createOutputStack() {
        ItemStack result = outputTemplate.copy();
        if (startEmpty) {
            ItemDurabilityHelper.setEmpty(result);
        }
        return result;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (container.getContainerSize() < 3) {
            return false;
        }
        for (int[] perm : SLOT_PERMUTATIONS) {
            if (req0.matches(container.getItem(perm[0]))
                    && req1.matches(container.getItem(perm[1]))
                    && req2.matches(container.getItem(perm[2]))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return createOutputStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return createOutputStack();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ANCIENT_TABLE_CRAFTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ANCIENT_TABLE_CRAFTING_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<AncientTableRecipe> {

        private static CountedIngredient parseCountedIngredient(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return CountedIngredient.EMPTY;
            }
            JsonObject obj = GsonHelper.convertToJsonObject(element, "ingredient");
            if (obj.size() == 0) {
                return CountedIngredient.EMPTY;
            }
            int count = GsonHelper.getAsInt(obj, "count", 1);
            Ingredient ing = Ingredient.fromJson(obj);
            return new CountedIngredient(ing, Math.max(1, count));
        }

        @Override
        public AncientTableRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            JsonArray ingredientsArray = GsonHelper.getAsJsonArray(json, "ingredients");
            if (ingredientsArray.isEmpty() || ingredientsArray.size() > 3) {
                throw new JsonSyntaxException("Ancient Table recipe must have between 1 and 3 ingredients");
            }

            CountedIngredient r0 = ingredientsArray.size() > 0 ? parseCountedIngredient(ingredientsArray.get(0)) : CountedIngredient.EMPTY;
            CountedIngredient r1 = ingredientsArray.size() > 1 ? parseCountedIngredient(ingredientsArray.get(1)) : CountedIngredient.EMPTY;
            CountedIngredient r2 = ingredientsArray.size() > 2 ? parseCountedIngredient(ingredientsArray.get(2)) : CountedIngredient.EMPTY;

            JsonObject resultObj = GsonHelper.getAsJsonObject(json, "result");
            ItemStack output;
            if (resultObj.has("nbt")) {
                output = ShapedRecipe.itemStackFromJson(resultObj);
            } else {
                String itemName = GsonHelper.getAsString(resultObj, "item");
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
                if (item == null) {
                    throw new JsonSyntaxException("Unknown result item '" + itemName + "'");
                }
                int count = GsonHelper.getAsInt(resultObj, "count", 1);
                output = new ItemStack(item, count);
            }

            boolean defaultStartEmpty = ItemDurabilityHelper.isRepairableModItem(output);
            boolean startEmpty = GsonHelper.getAsBoolean(resultObj, "start_empty",
                    GsonHelper.getAsBoolean(json, "start_empty", defaultStartEmpty));
            int crystals = GsonHelper.getAsInt(json, "crystals", 1);
            int customDurationTicks = GsonHelper.getAsInt(json, "duration_ticks", 0);

            return new AncientTableRecipe(recipeId, r0, r1, r2, output, startEmpty, crystals, customDurationTicks);
        }

        @Override
        public AncientTableRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            CountedIngredient r0 = readCountedIngredient(buf);
            CountedIngredient r1 = readCountedIngredient(buf);
            CountedIngredient r2 = readCountedIngredient(buf);
            ItemStack output = buf.readItem();
            boolean startEmpty = buf.readBoolean();
            int crystals = buf.readVarInt();
            int customDurationTicks = buf.readVarInt();
            return new AncientTableRecipe(recipeId, r0, r1, r2, output, startEmpty, crystals, customDurationTicks);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AncientTableRecipe recipe) {
            writeCountedIngredient(buf, recipe.req0);
            writeCountedIngredient(buf, recipe.req1);
            writeCountedIngredient(buf, recipe.req2);
            buf.writeItem(recipe.outputTemplate);
            buf.writeBoolean(recipe.startEmpty);
            buf.writeVarInt(recipe.crystalsRequired);
            buf.writeVarInt(recipe.customDurationTicks);
        }

        private static CountedIngredient readCountedIngredient(FriendlyByteBuf buf) {
            int count = buf.readVarInt();
            if (count <= 0) {
                return CountedIngredient.EMPTY;
            }
            Ingredient ing = Ingredient.fromNetwork(buf);
            return new CountedIngredient(ing, count);
        }

        private static void writeCountedIngredient(FriendlyByteBuf buf, CountedIngredient ci) {
            if (ci == null || ci.count() <= 0 || ci.ingredient().isEmpty()) {
                buf.writeVarInt(0);
                return;
            }
            buf.writeVarInt(ci.count());
            ci.ingredient().toNetwork(buf);
        }
    }
}
