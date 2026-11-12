package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class CreepingDeathItemTest {

    private static CreepingDeathItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (CreepingDeathItem) ModItems.CREEPING_DEATH.get();
    }

    @Test
    @DisplayName("CreepingDeathItem should have max stack size of 1")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);
        assertThat(stack.getMaxStackSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Skin manipulation helpers should set, retrieve, and clear skin data")
    void skinHelpers_manipulateTagsCorrectly() {
        ItemStack stack = new ItemStack(item);
        assertThat(CreepingDeathItem.hasCustomSkin(stack)).isFalse();
        assertThat(CreepingDeathItem.getSkinUUID(stack)).isNull();
        assertThat(CreepingDeathItem.getSkinName(stack)).isEmpty();

        UUID skinId = UUID.randomUUID();
        String skinName = "CustomHunter";

        CreepingDeathItem.setSkin(stack, skinId, skinName);
        assertThat(CreepingDeathItem.hasCustomSkin(stack)).isTrue();
        assertThat(CreepingDeathItem.getSkinUUID(stack)).isEqualTo(skinId);
        assertThat(CreepingDeathItem.getSkinName(stack)).isEqualTo(skinName);

        CreepingDeathItem.clearSkin(stack);
        assertThat(CreepingDeathItem.hasCustomSkin(stack)).isFalse();
        assertThat(CreepingDeathItem.getSkinUUID(stack)).isNull();
        assertThat(CreepingDeathItem.getSkinName(stack)).isEmpty();

        // Setting null or empty name should clear skin
        CreepingDeathItem.setSkin(stack, skinId, "");
        assertThat(CreepingDeathItem.hasCustomSkin(stack)).isFalse();
    }

    @Test
    @DisplayName("appendHoverText should display default skin or custom skin name")
    void appendHoverText_displaysSkinInfo() {
        ItemStack stack = new ItemStack(item);
        List<Component> defaultTooltip = new ArrayList<>();
        item.appendHoverText(stack, null, defaultTooltip, TooltipFlag.NORMAL);
        assertThat(defaultTooltip).isNotEmpty();
        assertThat(defaultTooltip).anyMatch(c -> c.getString().contains("tooltip.bodycam.creeping_death.skin_default"));

        UUID skinId = UUID.randomUUID();
        CreepingDeathItem.setSkin(stack, skinId, "PhantomSkin");
        List<Component> customTooltip = new ArrayList<>();
        item.appendHoverText(stack, null, customTooltip, TooltipFlag.NORMAL);
        assertThat(customTooltip).anyMatch(c -> c.getString().contains("tooltip.bodycam.creeping_death.skin"));
    }

    @Test
    @DisplayName("invalidateKnownPlayersCache should execute without throwing exception")
    void invalidateKnownPlayersCache_executesCleanly() {
        CreepingDeathItem.invalidateKnownPlayersCache();
    }
}
