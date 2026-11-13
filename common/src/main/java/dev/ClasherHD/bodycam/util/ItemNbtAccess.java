package dev.ClasherHD.bodycam.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;
import java.util.function.Consumer;

public final class ItemNbtAccess {
    private ItemNbtAccess() {
    }

    public static boolean hasTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    public static CompoundTag getTag(ItemStack stack) {
        if (!hasTag(stack)) {
            return null;
        }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @SuppressWarnings("deprecation")
    public static CompoundTag getReadOnlyTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty() ? data.getUnsafe() : null;
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }
        return tag;
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
        }
    }

    public static void updateTag(ItemStack stack, Consumer<CompoundTag> mutator) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag tag = getOrCreateTag(stack);
        mutator.accept(tag);
        setTag(stack, tag);
    }

    public static boolean contains(ItemStack stack, String key) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.contains(key);
    }

    public static boolean hasUUID(ItemStack stack, String key) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.hasUUID(key);
    }

    public static boolean getBoolean(ItemStack stack, String key) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.getBoolean(key);
    }

    public static int getInt(ItemStack stack, String key) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null ? tag.getInt(key) : 0;
    }

    public static long getLong(ItemStack stack, String key) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null ? tag.getLong(key) : 0L;
    }

    public static double getDouble(ItemStack stack, String key) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null ? tag.getDouble(key) : 0.0D;
    }

    public static String getString(ItemStack stack, String key) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null ? tag.getString(key) : "";
    }

    public static UUID getUUID(ItemStack stack, String key) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.hasUUID(key) ? tag.getUUID(key) : null;
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        updateTag(stack, tag -> tag.putBoolean(key, value));
    }

    public static void putInt(ItemStack stack, String key, int value) {
        updateTag(stack, tag -> tag.putInt(key, value));
    }

    public static void putLong(ItemStack stack, String key, long value) {
        updateTag(stack, tag -> tag.putLong(key, value));
    }

    public static void putDouble(ItemStack stack, String key, double value) {
        updateTag(stack, tag -> tag.putDouble(key, value));
    }

    public static void putString(ItemStack stack, String key, String value) {
        updateTag(stack, tag -> tag.putString(key, value));
    }

    public static void putUUID(ItemStack stack, String key, UUID value) {
        if (value == null) {
            remove(stack, key);
            return;
        }
        updateTag(stack, tag -> tag.putUUID(key, value));
    }

    public static void remove(ItemStack stack, String key) {
        if (!hasTag(stack)) {
            return;
        }
        updateTag(stack, tag -> tag.remove(key));
    }
}
