package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class LogPaperItemTest {

    private static LogPaperItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (LogPaperItem) ModItems.LOG_PAPER.get();
    }

    @Test
    @DisplayName("LogPaperItem should stack up to 16")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);
        assertThat(stack.getMaxStackSize()).isEqualTo(16);
    }

    @Test
    @DisplayName("appendHoverText should display author and entry count")
    void appendHoverText_displaysAuthorAndEntries() {
        ItemStack stack = new ItemStack(item);
        List<Component> tooltip = new ArrayList<>();
        item.appendHoverText(stack, null, tooltip, TooltipFlag.NORMAL);
        assertThat(tooltip).isEmpty();

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(LogPaperItem.TAG_AUTHOR, "Inspector");
        ListTag entries = new ListTag();
        entries.add(new CompoundTag());
        entries.add(new CompoundTag());
        tag.put(LogPaperItem.TAG_ENTRIES, entries);

        List<Component> taggedTooltip = new ArrayList<>();
        item.appendHoverText(stack, null, taggedTooltip, TooltipFlag.NORMAL);
        assertThat(taggedTooltip).hasSize(2);
        assertThat(taggedTooltip.get(0).getString()).contains("tooltip.bodycam.log_paper.author");
        assertThat(taggedTooltip.get(1).getString()).contains("tooltip.bodycam.log_paper.entries");
    }
}
