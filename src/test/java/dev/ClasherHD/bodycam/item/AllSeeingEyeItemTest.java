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

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class AllSeeingEyeItemTest {

    private static AllSeeingEyeItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (AllSeeingEyeItem) ModItems.ALL_SEEING_EYE.get();
    }

    @Test
    @DisplayName("AllSeeingEyeItem properties should have max stack size of 1 and not be enchantable")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);
        assertThat(stack.getMaxStackSize()).isEqualTo(1);
        assertThat(item.isEnchantable(stack)).isFalse();
        assertThat(item.canApplyAtEnchantingTable(stack, null)).isFalse();
        assertThat(item.isBookEnchantable(stack, ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("isActivated and setActivated should manage activation state correctly")
    void activation_state_togglesCorrectly() {
        ItemStack stack = new ItemStack(item);
        assertThat(AllSeeingEyeItem.isActivated(stack)).isTrue();

        AllSeeingEyeItem.setActivated(stack, false);
        assertThat(AllSeeingEyeItem.isActivated(stack)).isFalse();
        assertThat(stack.hasTag()).isTrue();
        assertThat(stack.getTag().getBoolean(AllSeeingEyeItem.TAG_DEACTIVATED)).isTrue();

        AllSeeingEyeItem.setActivated(stack, true);
        assertThat(AllSeeingEyeItem.isActivated(stack)).isTrue();
    }

    @Test
    @DisplayName("createActivatedStack and createDeactivatedStack should produce correct states")
    void factoryMethods_createCorrectStacks() {
        ItemStack active = AllSeeingEyeItem.createActivatedStack();
        assertThat(AllSeeingEyeItem.isActivated(active)).isTrue();

        ItemStack deactive = AllSeeingEyeItem.createDeactivatedStack();
        assertThat(AllSeeingEyeItem.isActivated(deactive)).isFalse();
    }

    @Test
    @DisplayName("getName should format differently for activated vs deactivated")
    void getName_formatsAccordingToState() {
        ItemStack active = AllSeeingEyeItem.createActivatedStack();
        Component activeName = item.getName(active);

        ItemStack deactive = AllSeeingEyeItem.createDeactivatedStack();
        Component deactiveName = item.getName(deactive);

        assertThat(activeName).isNotEqualTo(deactiveName);
    }

    @Test
    @DisplayName("appendHoverText should display activation state tooltip")
    void appendHoverText_displaysState() {
        ItemStack active = AllSeeingEyeItem.createActivatedStack();
        List<Component> activeTooltip = new ArrayList<>();
        item.appendHoverText(active, null, activeTooltip, TooltipFlag.NORMAL);
        assertThat(activeTooltip).isNotEmpty();

        ItemStack deactive = AllSeeingEyeItem.createDeactivatedStack();
        List<Component> deactiveTooltip = new ArrayList<>();
        item.appendHoverText(deactive, null, deactiveTooltip, TooltipFlag.NORMAL);
        assertThat(deactiveTooltip).isNotEmpty();
    }
}
