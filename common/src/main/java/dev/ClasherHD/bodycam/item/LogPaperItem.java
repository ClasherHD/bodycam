package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

@SuppressWarnings("null")
public class LogPaperItem extends Item {

    public static final String TAG_AUTHOR = "Author";
    public static final String TAG_AUTHOR_UUID = "AuthorUUID";
    public static final String TAG_ENTRIES = "Entries";
    public static final String ENTRY_NAME = "Name";
    public static final String ENTRY_UUID = "UUID";
    public static final String ENTRY_TIME = "Time";
    public static final String ENTRY_FAR = "Far";
    public static final String ENTRY_SAME_DIM = "SameDim";

    public LogPaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag == null) {
            return;
        }
        String author = tag.getString(TAG_AUTHOR);
        if (!author.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.bodycam.log_paper.author", author).withStyle(ChatFormatting.GRAY));
        }
        ListTag entries = tag.getList(TAG_ENTRIES, 10);
        tooltip.add(Component.translatable("tooltip.bodycam.log_paper.entries", entries.size()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
