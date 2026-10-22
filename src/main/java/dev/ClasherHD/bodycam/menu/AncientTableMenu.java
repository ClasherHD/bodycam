package dev.ClasherHD.bodycam.menu;

import dev.ClasherHD.bodycam.block.entity.AncientTableBlockEntity;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

@SuppressWarnings("null")
public class AncientTableMenu extends AbstractContainerMenu {

    public static final int BUTTON_CRAFT = 0;

    private static final int TABLE_SLOTS = AncientTableBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_START = TABLE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final AncientTableBlockEntity table;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public AncientTableMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(AncientTableBlockEntity.DATA_COUNT));
    }

    public AncientTableMenu(int containerId, Inventory inventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.ANCIENT_TABLE.get(), containerId);
        this.table = blockEntity instanceof AncientTableBlockEntity t ? t : null;
        this.access = table != null ? ContainerLevelAccess.create(table.getLevel(), table.getBlockPos()) : ContainerLevelAccess.NULL;
        this.data = data;

        IItemHandler handler = table != null ? table.getItems() : new ItemStackHandler(TABLE_SLOTS);
        addTableSlots(handler);
        addPlayerSlots(inventory);
        addDataSlots(data);
    }

    private void addTableSlots(IItemHandler handler) {
        addSlot(new SlotItemHandler(handler, 0, 18, 12));
        addSlot(new SlotItemHandler(handler, 1, 11, 35));
        addSlot(new SlotItemHandler(handler, 2, 18, 58));
        addSlot(new SlotItemHandler(handler, AncientTableBlockEntity.OUTPUT_SLOT, 52, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new SlotItemHandler(handler, AncientTableBlockEntity.FUEL_SLOT, 151, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AncientTableBlockEntity.isFuel(stack);
            }
        });
    }

    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CRAFT && table != null) {
            table.onCraftButtonPressed();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < TABLE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (AncientTableBlockEntity.isFuel(stack)
                && moveItemStackTo(stack, AncientTableBlockEntity.FUEL_SLOT, AncientTableBlockEntity.FUEL_SLOT + 1, false)) {
            slot.setChanged();
        } else if (!moveItemStackTo(stack, 0, AncientTableBlockEntity.INPUT_SLOT_COUNT, false)) {
            if (index < PLAYER_INV_END) {
                if (!moveItemStackTo(stack, PLAYER_INV_END, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ANCIENT_TABLE.get());
    }

    public int getFuel() {
        return data.get(AncientTableBlockEntity.DATA_FUEL);
    }

    public float getProgressRatio() {
        int max = data.get(AncientTableBlockEntity.DATA_MAX_PROGRESS);
        return max <= 0 ? 0.0F : Math.min(1.0F, data.get(AncientTableBlockEntity.DATA_PROGRESS) / (float) max);
    }

    public boolean isShowingError() {
        return data.get(AncientTableBlockEntity.DATA_ERROR_TICKS) > 0;
    }
}
