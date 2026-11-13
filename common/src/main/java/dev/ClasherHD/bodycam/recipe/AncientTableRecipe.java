package dev.ClasherHD.bodycam.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AncientTableRecipe implements Recipe<RecipeInput> {
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

        public static final Codec<CountedIngredient> CODEC = Codec.PASSTHROUGH.flatXmap(
                CountedIngredient::decodeDynamic,
                CountedIngredient::encodeDynamic
        );

        public boolean matches(ItemStack stack) {
            if (ingredient.isEmpty() || count <= 0) {
                return stack.isEmpty();
            }
            if (stack.isEmpty() || !ingredient.test(stack) || stack.getCount() < count) {
                return false;
            }
            if (stack.getItem() instanceof AllSeeingEyeItem && !AllSeeingEyeItem.isActivated(stack)) {
                return false;
            }
            return true;
        }

        private static <T> DataResult<CountedIngredient> decodeDynamic(Dynamic<T> dyn) {
            var mapResult = dyn.asMapOpt();
            if (mapResult.isSuccess() && mapResult.getOrThrow().findAny().isEmpty()) {
                return DataResult.success(EMPTY);
            }
            int count = dyn.get("count").asInt(1);
            if (count <= 0) {
                return DataResult.success(EMPTY);
            }
            Optional<Dynamic<T>> nested = dyn.get("ingredient").result();
            Dynamic<T> ingredientDyn = nested.orElseGet(() -> dyn.remove("count"));
            return Ingredient.CODEC_NONEMPTY.parse(ingredientDyn).map(ing -> new CountedIngredient(ing, count));
        }

        private static <T> DataResult<Dynamic<T>> encodeDynamic(CountedIngredient ci) {
            throw new UnsupportedOperationException("Encoding requires ops");
        }
    }

    private record ResultSpec(ItemStack stack, Boolean explicitStartEmpty) {
        private static final Codec<ResultSpec> CODEC = Codec.PASSTHROUGH.flatXmap(
                ResultSpec::decodeDynamic,
                spec -> DataResult.error(() -> "Encoding ResultSpec is unsupported")
        );

        private static <T> DataResult<ResultSpec> decodeDynamic(Dynamic<T> dyn) {
            Boolean explicitStartEmpty = dyn.get("start_empty").asBoolean().result().orElse(null);
            Dynamic<T> stackDyn = dyn.remove("start_empty");
            Optional<Dynamic<T>> itemVal = stackDyn.get("item").result();
            if (itemVal.isPresent() && stackDyn.get("id").result().isEmpty()) {
                stackDyn = stackDyn.set("id", itemVal.get()).remove("item");
            }
            return ItemStack.STRICT_CODEC.parse(stackDyn).map(stack -> new ResultSpec(stack, explicitStartEmpty));
        }
    }

    private final CountedIngredient req0;
    private final CountedIngredient req1;
    private final CountedIngredient req2;
    private final ItemStack outputTemplate;
    private final boolean startEmpty;
    private final int crystalsRequired;
    private final int customDurationTicks;

    public AncientTableRecipe(
            CountedIngredient req0,
            CountedIngredient req1,
            CountedIngredient req2,
            ItemStack outputTemplate,
            boolean startEmpty,
            int crystalsRequired,
            int customDurationTicks
    ) {
        this.req0 = req0 != null ? req0 : CountedIngredient.EMPTY;
        this.req1 = req1 != null ? req1 : CountedIngredient.EMPTY;
        this.req2 = req2 != null ? req2 : CountedIngredient.EMPTY;
        this.outputTemplate = outputTemplate != null ? outputTemplate : ItemStack.EMPTY;
        this.startEmpty = startEmpty;
        this.crystalsRequired = Math.max(1, crystalsRequired);
        this.customDurationTicks = Math.max(0, customDurationTicks);
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

    public ItemStack getOutputTemplate() {
        return outputTemplate;
    }

    public boolean isStartEmpty() {
        return startEmpty;
    }

    public int getCrystalsRequired() {
        return crystalsRequired;
    }

    public int getCustomDurationTicks() {
        return customDurationTicks;
    }

    public int getDurationTicks() {
        if (customDurationTicks > 0) {
            return customDurationTicks;
        }
        int totalItems = Math.max(1, req0.count() + req1.count() + req2.count());
        int secondsPerItem = ModServerConfig.ANCIENT_TABLE_CRAFT_SECONDS_PER_ITEM.get();
        return totalItems * secondsPerItem * 20;
    }

    public ItemStack createOutputStack() {
        ItemStack result = outputTemplate.copy();
        if (startEmpty) {
            ItemDurabilityHelper.setEmpty(result);
        }
        return result;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (input == null || input.size() < 3) {
            return false;
        }
        for (int[] perm : SLOT_PERMUTATIONS) {
            if (req0.matches(input.getItem(perm[0]))
                    && req1.matches(input.getItem(perm[1]))
                    && req2.matches(input.getItem(perm[2]))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return createOutputStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return createOutputStack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        if (req0.count() > 0 && !req0.ingredient().isEmpty()) {
            list.add(req0.ingredient());
        }
        if (req1.count() > 0 && !req1.ingredient().isEmpty()) {
            list.add(req1.ingredient());
        }
        if (req2.count() > 0 && !req2.ingredient().isEmpty()) {
            list.add(req2.ingredient());
        }
        return list;
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
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<AncientTableRecipe> CODEC = RecordCodecBuilder.<AncientTableRecipe>mapCodec(instance -> instance.group(
                CountedIngredient.CODEC.listOf(1, 3).fieldOf("ingredients").forGetter(Serializer::collectCountedIngredients),
                ResultSpec.CODEC.optionalFieldOf("result").forGetter(r -> Optional.of(new ResultSpec(r.outputTemplate, r.startEmpty))),
                ResultSpec.CODEC.optionalFieldOf("output").forGetter(r -> Optional.empty()),
                Codec.BOOL.optionalFieldOf("start_empty").forGetter(r -> Optional.of(r.startEmpty)),
                Codec.INT.optionalFieldOf("crystals").forGetter(r -> Optional.of(r.crystalsRequired)),
                Codec.INT.optionalFieldOf("fuel_cost").forGetter(r -> Optional.empty()),
                Codec.INT.optionalFieldOf("duration_ticks").forGetter(r -> Optional.of(r.customDurationTicks)),
                Codec.INT.optionalFieldOf("craft_time_ticks").forGetter(r -> Optional.empty())
        ).apply(instance, (ingredients, resultOpt, outputOpt, topStartEmpty, crystalsOpt, fuelCostOpt, durationOpt, craftTimeOpt) -> {
            CountedIngredient r0 = !ingredients.isEmpty() ? ingredients.get(0) : CountedIngredient.EMPTY;
            CountedIngredient r1 = ingredients.size() > 1 ? ingredients.get(1) : CountedIngredient.EMPTY;
            CountedIngredient r2 = ingredients.size() > 2 ? ingredients.get(2) : CountedIngredient.EMPTY;
            ResultSpec res = resultOpt.or(() -> outputOpt).orElse(new ResultSpec(ItemStack.EMPTY, null));
            boolean defaultStartEmpty = ItemDurabilityHelper.isRepairableModItem(res.stack());
            boolean startEmpty = res.explicitStartEmpty() != null
                    ? res.explicitStartEmpty()
                    : topStartEmpty.orElse(defaultStartEmpty);
            int crystals = crystalsOpt.or(() -> fuelCostOpt).orElse(1);
            int duration = durationOpt.or(() -> craftTimeOpt).orElse(0);
            return new AncientTableRecipe(r0, r1, r2, res.stack(), startEmpty, crystals, duration);
        }));

        private static final StreamCodec<RegistryFriendlyByteBuf, AncientTableRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        private static List<CountedIngredient> collectCountedIngredients(AncientTableRecipe recipe) {
            List<CountedIngredient> list = new ArrayList<>();
            if (recipe.req0.count() > 0 && !recipe.req0.ingredient().isEmpty()) {
                list.add(recipe.req0);
            }
            if (recipe.req1.count() > 0 && !recipe.req1.ingredient().isEmpty()) {
                list.add(recipe.req1);
            }
            if (recipe.req2.count() > 0 && !recipe.req2.ingredient().isEmpty()) {
                list.add(recipe.req2);
            }
            if (list.isEmpty()) {
                list.add(CountedIngredient.EMPTY);
            }
            return list;
        }

        private static void encode(RegistryFriendlyByteBuf buf, AncientTableRecipe recipe) {
            writeCountedIngredient(buf, recipe.req0);
            writeCountedIngredient(buf, recipe.req1);
            writeCountedIngredient(buf, recipe.req2);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.outputTemplate);
            buf.writeBoolean(recipe.startEmpty);
            buf.writeVarInt(recipe.crystalsRequired);
            buf.writeVarInt(recipe.customDurationTicks);
        }

        private static AncientTableRecipe decode(RegistryFriendlyByteBuf buf) {
            CountedIngredient r0 = readCountedIngredient(buf);
            CountedIngredient r1 = readCountedIngredient(buf);
            CountedIngredient r2 = readCountedIngredient(buf);
            ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            boolean startEmpty = buf.readBoolean();
            int crystals = buf.readVarInt();
            int customDurationTicks = buf.readVarInt();
            return new AncientTableRecipe(r0, r1, r2, output, startEmpty, crystals, customDurationTicks);
        }

        private static void writeCountedIngredient(RegistryFriendlyByteBuf buf, CountedIngredient ci) {
            if (ci == null || ci.count() <= 0 || ci.ingredient().isEmpty()) {
                buf.writeVarInt(0);
                return;
            }
            buf.writeVarInt(ci.count());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ci.ingredient());
        }

        private static CountedIngredient readCountedIngredient(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            if (count <= 0) {
                return CountedIngredient.EMPTY;
            }
            Ingredient ing = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            return new CountedIngredient(ing, count);
        }

        @Override
        public MapCodec<AncientTableRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AncientTableRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
