package dev.ClasherHD.bodycam.block.entity;

import dev.ClasherHD.bodycam.block.AncientTableBlock;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.menu.AncientTableMenu;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class AncientTableBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOT_COUNT = 3;
    public static final int OUTPUT_SLOT = 3;
    public static final int FUEL_SLOT = 4;
    public static final int SLOT_COUNT = 5;

    public static final int MAX_FUEL = 5;
    public static final int FUEL_PER_CRYSTAL = 112;
    public static final int MAX_FUEL_UNITS = MAX_FUEL * FUEL_PER_CRYSTAL;

    public static final int TICKS_PER_CRYSTAL_REPAIR = 28 * 20;
    public static final int TICKS_PER_FUEL_UNIT_REPAIR = TICKS_PER_CRYSTAL_REPAIR / FUEL_PER_CRYSTAL;
    public static final int TICKS_PER_CRAFT_ITEM = 10 * 20;
    public static final int ERROR_DISPLAY_TICKS = 40;

    public static final int DATA_FUEL = 0;
    public static final int DATA_PROGRESS = 1;
    public static final int DATA_MAX_PROGRESS = 2;
    public static final int DATA_ERROR_TICKS = 3;
    public static final int DATA_COUNT = 4;

    private static final int PROCESS_NONE = 0;
    private static final int PROCESS_REPAIR = 1;
    private static final int PROCESS_CRAFT = 2;

    private static final int[][] SLOT_PERMUTATIONS = {
            {0, 1, 2},
            {0, 2, 1},
            {1, 0, 2},
            {1, 2, 0},
            {2, 0, 1},
            {2, 1, 0}
    };

    private record IngredientReq(Supplier<Item> item, int count) {
        boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.is(item.get()) && stack.getCount() >= count;
        }
    }

    private record TableRecipe(
            IngredientReq req0,
            IngredientReq req1,
            IngredientReq req2,
            Supplier<ItemStack> outputFactory
    ) {
        int getDurationTicks() {
            return (req0.count() + req1.count() + req2.count()) * TICKS_PER_CRAFT_ITEM;
        }

        int[] matchSlots(ItemStackHandler handler) {
            for (int[] perm : SLOT_PERMUTATIONS) {
                if (req0.matches(handler.getStackInSlot(perm[0]))
                        && req1.matches(handler.getStackInSlot(perm[1]))
                        && req2.matches(handler.getStackInSlot(perm[2]))) {
                    return perm;
                }
            }
            return null;
        }
    }

    private static final List<TableRecipe> RECIPES = List.of(
            new TableRecipe(
                    new IngredientReq(() -> Items.GREEN_DYE, 1),
                    new IngredientReq(() -> Items.SPYGLASS, 1),
                    new IngredientReq(() -> Items.QUARTZ, 1),
                    () -> ItemDurabilityHelper.createEmptyStack(ModItems.BODYCAM_MONITOR.get())
            ),
            new TableRecipe(
                    new IngredientReq(() -> Items.QUARTZ, 1),
                    new IngredientReq(() -> Items.COMPASS, 1),
                    new IngredientReq(() -> Items.QUARTZ, 1),
                    () -> ItemDurabilityHelper.createEmptyStack(ModItems.PLAYER_LOCATOR_COMPASS.get())
            ),
            new TableRecipe(
                    new IngredientReq(() -> Items.REPEATER, 1),
                    new IngredientReq(() -> Items.ITEM_FRAME, 1),
                    new IngredientReq(() -> Items.COPPER_INGOT, 1),
                    () -> ItemDurabilityHelper.createEmptyStack(ModItems.JAMMER.get())
            ),
            new TableRecipe(
                    new IngredientReq(() -> Items.NETHERITE_INGOT, 1),
                    new IngredientReq(() -> Items.ITEM_FRAME, 1),
                    new IngredientReq(() -> Items.OBSIDIAN, 1),
                    () -> ItemDurabilityHelper.createEmptyStack(ModItems.ANONYMIZER.get())
            ),
            new TableRecipe(
                    new IngredientReq(() -> Items.FIRE_CHARGE, 2),
                    new IngredientReq(() -> Items.OBSIDIAN, 14),
                    new IngredientReq(ModItems.OBSERVATION_CRYSTAL::get, 1),
                    () -> new ItemStack(ModItems.PORTABLE_NETHER_PORTAL.get())
            )
    );

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == FUEL_SLOT) {
                return isFuel(stack);
            }
            return slot != OUTPUT_SLOT;
        }
    };

    private final LazyOptional<ItemStackHandler> itemCapability = LazyOptional.of(() -> items);

    private int fuelUnits;
    private int progress;
    private int maxProgress;
    private int errorTicks;

    private int processType = PROCESS_NONE;
    private int activeRepairSlot = -1;
    private int activeRepairAmount;
    private int activeRecipeIndex = -1;
    private int activeFuelCost;

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
                case DATA_FUEL -> fuelUnits = value * FUEL_PER_CRYSTAL;
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
        return Math.min(MAX_FUEL, (fuelUnits + FUEL_PER_CRYSTAL - 1) / FUEL_PER_CRYSTAL);
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
        ItemStack fuelStack = items.getStackInSlot(FUEL_SLOT);
        if (fuelUnits <= MAX_FUEL_UNITS - FUEL_PER_CRYSTAL && isFuel(fuelStack)) {
            items.extractItem(FUEL_SLOT, 1, false);
            fuelUnits = Math.min(MAX_FUEL_UNITS, fuelUnits + FUEL_PER_CRYSTAL);
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
        if (!items.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        int foundSlot = -1;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                if (foundSlot != -1) {
                    return false;
                }
                foundSlot = i;
            }
        }
        if (foundSlot == -1) {
            return false;
        }

        ItemStack stack = items.getStackInSlot(foundSlot);
        if (!ItemDurabilityHelper.isRepairableModItem(stack) || stack.getDamageValue() <= 0) {
            return false;
        }

        int missing = stack.getDamageValue();
        int repairAmount;
        int fuelCost;
        int durationTicks;

        if (ItemDurabilityHelper.isPerTimeItem(stack)) {
            repairAmount = Math.min(missing, fuelUnits);
            if (repairAmount <= 0) {
                return false;
            }
            fuelCost = repairAmount;
            durationTicks = repairAmount * TICKS_PER_FUEL_UNIT_REPAIR;
        } else {
            int availableWholeCrystals = fuelUnits / FUEL_PER_CRYSTAL;
            repairAmount = Math.min(missing, availableWholeCrystals);
            if (repairAmount <= 0) {
                return false;
            }
            fuelCost = repairAmount * FUEL_PER_CRYSTAL;
            durationTicks = repairAmount * TICKS_PER_CRYSTAL_REPAIR;
        }

        this.processType = PROCESS_REPAIR;
        this.activeRepairSlot = foundSlot;
        this.activeRepairAmount = repairAmount;
        this.activeRecipeIndex = -1;
        this.activeFuelCost = fuelCost;
        this.progress = 0;
        this.maxProgress = durationTicks;
        return true;
    }

    private boolean tryStartCraft() {
        if (!items.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        if (fuelUnits < FUEL_PER_CRYSTAL) {
            return false;
        }
        for (int i = 0; i < RECIPES.size(); i++) {
            TableRecipe recipe = RECIPES.get(i);
            if (recipe.matchSlots(items) != null) {
                this.processType = PROCESS_CRAFT;
                this.activeRecipeIndex = i;
                this.activeRepairSlot = -1;
                this.activeRepairAmount = 0;
                this.activeFuelCost = FUEL_PER_CRYSTAL;
                this.progress = 0;
                this.maxProgress = recipe.getDurationTicks();
                return true;
            }
        }
        return false;
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
                ItemStack input = items.getStackInSlot(activeRepairSlot);
                ItemStack repaired = input.copy();
                repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - activeRepairAmount));
                items.setStackInSlot(activeRepairSlot, ItemStack.EMPTY);
                items.setStackInSlot(OUTPUT_SLOT, repaired);
                fuelUnits = Math.max(0, fuelUnits - activeFuelCost);
                if (this.level != null && !this.level.isClientSide()) {
                    this.level.playSound(null, this.worldPosition, ModSounds.ANCIENT_TABLE_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                cancelProcess();
            }
            setChanged();
        } else if (processType == PROCESS_CRAFT) {
            if (activeRecipeIndex < 0 || activeRecipeIndex >= RECIPES.size()) {
                cancelProcess();
                setChanged();
                return;
            }
            TableRecipe recipe = RECIPES.get(activeRecipeIndex);
            int[] perm = recipe.matchSlots(items);
            if (perm == null || !items.getStackInSlot(OUTPUT_SLOT).isEmpty() || fuelUnits < activeFuelCost) {
                cancelProcess();
                setChanged();
                return;
            }
            progress++;
            if (progress >= maxProgress) {
                items.extractItem(perm[0], recipe.req0().count(), false);
                items.extractItem(perm[1], recipe.req1().count(), false);
                items.extractItem(perm[2], recipe.req2().count(), false);
                items.setStackInSlot(OUTPUT_SLOT, recipe.outputFactory().get());
                fuelUnits = Math.max(0, fuelUnits - activeFuelCost);
                if (this.level != null && !this.level.isClientSide()) {
                    this.level.playSound(null, this.worldPosition, ModSounds.ANCIENT_TABLE_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                cancelProcess();
            }
            setChanged();
        }
    }

    private boolean isRepairStillValid() {
        if (activeRepairSlot < 0 || activeRepairSlot >= INPUT_SLOT_COUNT || activeRepairAmount <= 0) {
            return false;
        }
        if (!items.getStackInSlot(OUTPUT_SLOT).isEmpty() || fuelUnits < activeFuelCost) {
            return false;
        }
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            if (i != activeRepairSlot && !items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        ItemStack stack = items.getStackInSlot(activeRepairSlot);
        return ItemDurabilityHelper.isRepairableModItem(stack) && stack.getDamageValue() >= activeRepairAmount;
    }

    private void cancelProcess() {
        this.processType = PROCESS_NONE;
        this.progress = 0;
        this.maxProgress = 0;
        this.activeRepairSlot = -1;
        this.activeRepairAmount = 0;
        this.activeRecipeIndex = -1;
        this.activeFuelCost = 0;
    }

    private boolean hasNoInput() {
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(items.getSlots());
        for (int i = 0; i < items.getSlots(); i++) {
            container.setItem(i, items.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, container);
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
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("FuelUnits", fuelUnits);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("ProcessType", processType);
        tag.putInt("ActiveRepairSlot", activeRepairSlot);
        tag.putInt("ActiveRepairAmount", activeRepairAmount);
        tag.putInt("ActiveRecipeIndex", activeRecipeIndex);
        tag.putInt("ActiveFuelCost", activeFuelCost);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("FuelUnits")) {
            fuelUnits = Math.min(tag.getInt("FuelUnits"), MAX_FUEL_UNITS);
        } else {
            fuelUnits = Math.min(tag.getInt("Fuel") * FUEL_PER_CRYSTAL, MAX_FUEL_UNITS);
        }
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        processType = tag.getInt("ProcessType");
        activeRepairSlot = tag.getInt("ActiveRepairSlot");
        activeRepairAmount = tag.getInt("ActiveRepairAmount");
        activeRecipeIndex = tag.getInt("ActiveRecipeIndex");
        activeFuelCost = tag.getInt("ActiveFuelCost");
    }
}
