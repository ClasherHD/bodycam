package dev.ClasherHD.bodycam.block.entity;

import dev.ClasherHD.bodycam.block.AncientTableBlock;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.menu.AncientTableMenu;
import dev.ClasherHD.bodycam.recipe.AncientTableRecipe;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModRecipes;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
public class AncientTableBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOT_COUNT = 3;
    public static final int OUTPUT_SLOT = 3;
    public static final int FUEL_SLOT = 4;
    public static final int SLOT_COUNT = 5;

    public static final int MAX_FUEL = 5;
    public static final int DEFAULT_FUEL_PER_CRYSTAL = 112;
    public static final int ERROR_DISPLAY_TICKS = 40;

    public static final int DATA_FUEL = 0;
    public static final int DATA_PROGRESS = 1;
    public static final int DATA_MAX_PROGRESS = 2;
    public static final int DATA_ERROR_TICKS = 3;
    public static final int DATA_COUNT = 4;

    private static final int PROCESS_NONE = 0;
    private static final int PROCESS_REPAIR = 1;
    private static final int PROCESS_CRAFT = 2;

    private final SimpleContainer items = new SimpleContainer(SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            inputDirty = true;
            AncientTableBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == FUEL_SLOT) {
                return isFuel(stack);
            }
            return slot != OUTPUT_SLOT;
        }
    };

    private int fuelUnits;
    private int progress;
    private int maxProgress;
    private int errorTicks;

    private int processType = PROCESS_NONE;
    private int activeRepairSlot = -1;
    private int activeRepairAmount;
    private ResourceLocation activeRecipeId;
    private int activeFuelCost;
    private RecipeHolder<AncientTableRecipe> cachedRecipeHolder;
    private int[] cachedPermutation;
    private boolean inputDirty = true;

    public static int getFuelPerCrystal() {
        return ModServerConfig.isLoaded()
                ? Math.max(1, ModServerConfig.ANCIENT_TABLE_FUEL_PER_CRYSTAL.get())
                : DEFAULT_FUEL_PER_CRYSTAL;
    }

    public static int getMaxFuelUnits() {
        return MAX_FUEL * getFuelPerCrystal();
    }

    public static int getTicksPerCrystalRepair() {
        int seconds = ModServerConfig.isLoaded()
                ? Math.max(1, ModServerConfig.ANCIENT_TABLE_REPAIR_SECONDS_PER_CRYSTAL.get())
                : 28;
        return seconds * 20;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FUEL -> getDisplayFuelStep();
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> maxProgress;
                case DATA_ERROR_TICKS -> errorTicks;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FUEL -> fuelUnits = value * getFuelPerCrystal();
                case DATA_PROGRESS -> progress = value;
                case DATA_MAX_PROGRESS -> maxProgress = value;
                case DATA_ERROR_TICKS -> errorTicks = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AncientTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCIENT_TABLE.get(), pos, state);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(ModItems.OBSERVATION_CRYSTAL.get());
    }

    public int getDisplayFuelStep() {
        if (fuelUnits <= 0) {
            return 0;
        }
        int perCrystal = getFuelPerCrystal();
        return Math.min(MAX_FUEL, (fuelUnits + perCrystal - 1) / perCrystal);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AncientTableBlockEntity table) {
        if (table.errorTicks > 0) {
            table.errorTicks--;
        }
        table.tryRefuel();
        table.tickActiveProcess();
        table.syncFuelState(level, pos, state);
    }

    private void tryRefuel() {
        ItemStack fuelStack = items.getItem(FUEL_SLOT);
        int perCrystal = getFuelPerCrystal();
        int maxUnits = getMaxFuelUnits();
        if (fuelUnits <= maxUnits - perCrystal && isFuel(fuelStack)) {
            items.removeItem(FUEL_SLOT, 1);
            fuelUnits = Math.min(maxUnits, fuelUnits + perCrystal);
            if (this.level != null && !this.level.isClientSide()) {
                this.level.playSound(null, this.worldPosition, ModSounds.ANCIENT_TABLE_REFUEL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            setChanged();
        }
    }

    private void syncFuelState(Level level, BlockPos pos, BlockState state) {
        int displayFuel = getDisplayFuelStep();
        if (state.getValue(AncientTableBlock.FUEL_LEVEL) != displayFuel) {
            level.setBlock(pos, state.setValue(AncientTableBlock.FUEL_LEVEL, displayFuel), 3);
        }
    }

    public void onCraftButtonPressed() {
        if (processType != PROCESS_NONE) {
            return;
        }
        if (hasNoInput()) {
            return;
        }
        if (tryStartRepair() || tryStartCraft()) {
            errorTicks = 0;
            if (this.level != null && !this.level.isClientSide()) {
                this.level.playSound(null, this.worldPosition, ModSounds.ANCIENT_TABLE_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            setChanged();
        } else {
            errorTicks = ERROR_DISPLAY_TICKS;
        }
    }

    private boolean tryStartRepair() {
        if (!items.getItem(OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        int foundSlot = -1;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            if (!items.getItem(i).isEmpty()) {
                if (foundSlot != -1) {
                    return false;
                }
                foundSlot = i;
            }
        }
        if (foundSlot == -1) {
            return false;
        }

        ItemStack stack = items.getItem(foundSlot);
        if (stack.getItem() instanceof AllSeeingEyeItem) {
            if (AllSeeingEyeItem.isActivated(stack) || fuelUnits < getMaxFuelUnits()) {
                return false;
            }
            this.processType = PROCESS_REPAIR;
            this.activeRepairSlot = foundSlot;
            this.activeRepairAmount = 1;
            this.activeRecipeId = null;
            this.activeFuelCost = getMaxFuelUnits();
            this.progress = 0;
            this.maxProgress = getTicksPerCrystalRepair();
            return true;
        }
        if (ItemDurabilityHelper.isUnbreakable(stack)
                || !ItemDurabilityHelper.isRepairableModItem(stack)
                || stack.getDamageValue() <= 0) {
            return false;
        }

        int missing = stack.getDamageValue();
        int perCrystal = getFuelPerCrystal();
        int ticksPerCrystal = getTicksPerCrystalRepair();
        int repairAmount;
        int fuelCost;
        int durationTicks;

        if (ItemDurabilityHelper.isPerTimeItem(stack)) {
            repairAmount = Math.min(missing, fuelUnits);
            if (repairAmount <= 0) {
                return false;
            }
            fuelCost = repairAmount;
            durationTicks = Math.max(1, Math.round((float) repairAmount * ticksPerCrystal / (float) perCrystal));
        } else {
            int availableWholeCrystals = fuelUnits / perCrystal;
            repairAmount = Math.min(missing, availableWholeCrystals);
            if (repairAmount <= 0) {
                return false;
            }
            fuelCost = repairAmount * perCrystal;
            durationTicks = repairAmount * ticksPerCrystal;
        }

        this.processType = PROCESS_REPAIR;
        this.activeRepairSlot = foundSlot;
        this.activeRepairAmount = repairAmount;
        this.activeRecipeId = null;
        this.activeFuelCost = fuelCost;
        this.progress = 0;
        this.maxProgress = durationTicks;
        return true;
    }

    private boolean tryStartCraft() {
        if (this.level == null || !items.getItem(OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        int perCrystal = getFuelPerCrystal();
        List<RecipeHolder<AncientTableRecipe>> recipes = this.level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.ANCIENT_TABLE_CRAFTING_TYPE.get());
        for (RecipeHolder<AncientTableRecipe> holder : recipes) {
            AncientTableRecipe recipe = holder.value();
            int requiredFuel = recipe.getCrystalsRequired() * perCrystal;
            int[] match = findMatchingSlots(recipe, items);
            if (fuelUnits >= requiredFuel && match != null) {
                this.processType = PROCESS_CRAFT;
                this.activeRecipeId = holder.id();
                this.activeRepairSlot = -1;
                this.activeRepairAmount = 0;
                this.activeFuelCost = requiredFuel;
                this.progress = 0;
                this.maxProgress = recipe.getDurationTicks();
                this.cachedRecipeHolder = holder;
                this.cachedPermutation = match;
                this.inputDirty = false;
                return true;
            }
        }
        return false;
    }

    private static final int[][] SLOT_PERMUTATIONS = {
            {0, 1, 2},
            {0, 2, 1},
            {1, 0, 2},
            {1, 2, 0},
            {2, 0, 1},
            {2, 1, 0}
    };

    private static int[] findMatchingSlots(AncientTableRecipe recipe, SimpleContainer container) {
        if (recipe == null || container == null || container.getContainerSize() < 3) {
            return null;
        }
        for (int[] perm : SLOT_PERMUTATIONS) {
            if (recipe.getReq0().matches(container.getItem(perm[0]))
                    && recipe.getReq1().matches(container.getItem(perm[1]))
                    && recipe.getReq2().matches(container.getItem(perm[2]))) {
                return perm;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private RecipeHolder<AncientTableRecipe> getActiveCraftRecipe() {
        if (this.level == null || this.activeRecipeId == null) {
            return null;
        }
        Optional<RecipeHolder<?>> found = this.level.getRecipeManager().byKey(this.activeRecipeId);
        if (found.isPresent() && found.get().value() instanceof AncientTableRecipe) {
            return (RecipeHolder<AncientTableRecipe>) found.get();
        }
        return null;
    }

    private void tickActiveProcess() {
        if (processType == PROCESS_NONE || maxProgress <= 0) {
            return;
        }
        if (processType == PROCESS_REPAIR) {
            if (!isRepairStillValid()) {
                cancelProcess();
                setChanged();
                return;
            }
            progress++;
            if (progress >= maxProgress) {
                ItemStack input = items.getItem(activeRepairSlot);
                ItemStack repaired = input.copy();
                if (repaired.getItem() instanceof AllSeeingEyeItem) {
                    AllSeeingEyeItem.setActivated(repaired, true);
                } else {
                    repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - activeRepairAmount));
                }
                items.setItem(activeRepairSlot, ItemStack.EMPTY);
                items.setItem(OUTPUT_SLOT, repaired);
                fuelUnits = Math.max(0, fuelUnits - activeFuelCost);
                if (this.level != null && !this.level.isClientSide()) {
                    this.level.playSound(null, this.worldPosition, ModSounds.ANCIENT_TABLE_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                cancelProcess();
                setChanged();
            } else if (progress % 20 == 0) {
                setChanged();
            }
        } else if (processType == PROCESS_CRAFT) {
            if (cachedRecipeHolder == null || (activeRecipeId != null && !activeRecipeId.equals(cachedRecipeHolder.id()))) {
                cachedRecipeHolder = getActiveCraftRecipe();
                inputDirty = true;
            }
            if (cachedRecipeHolder == null) {
                cancelProcess();
                setChanged();
                return;
            }
            AncientTableRecipe cachedRecipe = cachedRecipeHolder.value();
            if (inputDirty) {
                cachedPermutation = findMatchingSlots(cachedRecipe, items);
                inputDirty = false;
            }
            if (cachedPermutation == null || !items.getItem(OUTPUT_SLOT).isEmpty() || fuelUnits < activeFuelCost) {
                cancelProcess();
                setChanged();
                return;
            }
            progress++;
            if (progress >= maxProgress) {
                if (cachedRecipe.getReq0().count() > 0) {
                    items.removeItem(cachedPermutation[0], cachedRecipe.getReq0().count());
                }
                if (cachedRecipe.getReq1().count() > 0) {
                    items.removeItem(cachedPermutation[1], cachedRecipe.getReq1().count());
                }
                if (cachedRecipe.getReq2().count() > 0) {
                    items.removeItem(cachedPermutation[2], cachedRecipe.getReq2().count());
                }
                items.setItem(OUTPUT_SLOT, cachedRecipe.createOutputStack());
                fuelUnits = Math.max(0, fuelUnits - activeFuelCost);
                if (this.level != null && !this.level.isClientSide()) {
                    this.level.playSound(null, this.worldPosition, ModSounds.ANCIENT_TABLE_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                cancelProcess();
                setChanged();
            } else if (progress % 20 == 0) {
                setChanged();
            }
        }
    }

    private boolean isRepairStillValid() {
        if (activeRepairSlot < 0 || activeRepairSlot >= INPUT_SLOT_COUNT || activeRepairAmount <= 0) {
            return false;
        }
        if (!items.getItem(OUTPUT_SLOT).isEmpty() || fuelUnits < activeFuelCost) {
            return false;
        }
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            if (i != activeRepairSlot && !items.getItem(i).isEmpty()) {
                return false;
            }
        }
        ItemStack stack = items.getItem(activeRepairSlot);
        if (stack.getItem() instanceof AllSeeingEyeItem) {
            return !AllSeeingEyeItem.isActivated(stack);
        }
        return !ItemDurabilityHelper.isUnbreakable(stack)
                && ItemDurabilityHelper.isRepairableModItem(stack)
                && stack.getDamageValue() >= activeRepairAmount;
    }

    private void cancelProcess() {
        this.processType = PROCESS_NONE;
        this.progress = 0;
        this.maxProgress = 0;
        this.activeRepairSlot = -1;
        this.activeRepairAmount = 0;
        this.activeRecipeId = null;
        this.activeFuelCost = 0;
        this.cachedRecipeHolder = null;
        this.cachedPermutation = null;
        this.inputDirty = true;
    }

    private boolean hasNoInput() {
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            if (!items.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public SimpleContainer getItems() {
        return items;
    }

    public SimpleContainer getInventory() {
        return items;
    }

    public ContainerData getData() {
        return data;
    }

    public void dropContents(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, items);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bodycam.ancient_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AncientTableMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            list.set(i, items.getItem(i));
        }
        CompoundTag invTag = new CompoundTag();
        ContainerHelper.saveAllItems(invTag, list, registries);
        tag.put("Inventory", invTag);
        tag.putInt("FuelUnits", fuelUnits);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("ProcessType", processType);
        tag.putInt("ActiveRepairSlot", activeRepairSlot);
        tag.putInt("ActiveRepairAmount", activeRepairAmount);
        if (activeRecipeId != null) {
            tag.putString("ActiveRecipeId", activeRecipeId.toString());
        }
        tag.putInt("ActiveFuelCost", activeFuelCost);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        if (tag.contains("Inventory")) {
            ContainerHelper.loadAllItems(tag.getCompound("Inventory"), list, registries);
        } else {
            ContainerHelper.loadAllItems(tag, list, registries);
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.setItem(i, list.get(i));
        }
        int maxUnits = getMaxFuelUnits();
        if (tag.contains("FuelUnits")) {
            fuelUnits = Math.min(tag.getInt("FuelUnits"), maxUnits);
        } else {
            fuelUnits = Math.min(tag.getInt("Fuel") * getFuelPerCrystal(), maxUnits);
        }
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        processType = tag.getInt("ProcessType");
        activeRepairSlot = tag.getInt("ActiveRepairSlot");
        activeRepairAmount = tag.getInt("ActiveRepairAmount");
        if (tag.contains("ActiveRecipeId")) {
            activeRecipeId = ResourceLocation.tryParse(tag.getString("ActiveRecipeId"));
        } else {
            activeRecipeId = null;
        }
        activeFuelCost = tag.getInt("ActiveFuelCost");
    }
}
