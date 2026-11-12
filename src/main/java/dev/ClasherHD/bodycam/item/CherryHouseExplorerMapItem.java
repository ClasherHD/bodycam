package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@SuppressWarnings("null")
public class CherryHouseExplorerMapItem extends MapItem {

    public static final TagKey<Structure> CHERRY_HOUSE_TAG = TagKey.create(
            Registries.STRUCTURE, new ResourceLocation(bodycam.MODID, "cherry_house"));
    public static final String TAG_TARGET_X = "CherryHouseX";
    public static final String TAG_TARGET_Z = "CherryHouseZ";
    public static final String TAG_CENTER_X = "CherryHouseCenterX";
    public static final String TAG_CENTER_Z = "CherryHouseCenterZ";
    public static final String TAG_SEARCHED = "CherryHouseSearched";

    public CherryHouseExplorerMapItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createExplorerMap(ServerLevel level, BlockPos origin) {
        ItemStack stack = new ItemStack(ModItems.CHERRY_HOUSE_EXPLORER_MAP.get());
        if (bindToNearestCherryHouse(stack, level, origin)) {
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public static boolean bindToNearestCherryHouse(ItemStack stack, ServerLevel level, BlockPos origin) {
        BlockPos found = level.findNearestMapStructure(CHERRY_HOUSE_TAG, origin, 200, false);
        if (found == null) {
            stack.getOrCreateTag().putBoolean(TAG_SEARCHED, true);
            return false;
        }

        int targetX = (found.getX() & ~15) + 8;
        int targetZ = (found.getZ() & ~15) + 8;

        ItemStack vanillaMap = MapItem.create(level, targetX, targetZ, (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, vanillaMap);
        Integer mapId = MapItem.getMapId(vanillaMap);
        if (mapId == null) {
            return false;
        }

        MapItemSavedData savedData = MapItem.getSavedData(mapId, level);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("map", mapId);
        tag.putInt(TAG_TARGET_X, targetX);
        tag.putInt(TAG_TARGET_Z, targetZ);
        if (savedData != null) {
            tag.putInt(TAG_CENTER_X, savedData.centerX);
            tag.putInt(TAG_CENTER_Z, savedData.centerZ);
        }
        tag.putBoolean(TAG_SEARCHED, true);
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && level instanceof ServerLevel sLevel) {
            CompoundTag tag = stack.getTag();
            if (tag == null || (!tag.contains("map", 99) && !tag.getBoolean(TAG_SEARCHED))) {
                bindToNearestCherryHouse(stack, sLevel, entity.blockPosition());
            } else if (tag.contains("map", 99) && !tag.contains(TAG_CENTER_X, 99)) {
                bindToNearestCherryHouse(stack, sLevel, entity.blockPosition());
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && level instanceof ServerLevel sLevel && player instanceof ServerPlayer sp) {
            if (!stack.hasTag() || !stack.getTag().contains("map", 99)) {
                if (!bindToNearestCherryHouse(stack, sLevel, sp.blockPosition())) {
                    sp.displayClientMessage(
                            Component.translatable("message.bodycam.cherry_house_explorer_map.not_found")
                                    .withStyle(ChatFormatting.RED),
                            true);
                    return InteractionResultHolder.fail(stack);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
