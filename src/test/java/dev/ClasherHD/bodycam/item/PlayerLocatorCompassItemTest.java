package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class PlayerLocatorCompassItemTest {

    private static PlayerLocatorCompassItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (PlayerLocatorCompassItem) ModItems.PLAYER_LOCATOR_COMPASS.get();
    }

    @Test
    @DisplayName("PlayerLocatorCompassItem should have expected durability and properties")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.getMaxDamage(stack)).isEqualTo(ItemDurabilityHelper.PLAYER_LOCATOR_DURABILITY);
        assertThat(item.isEnchantable(stack)).isTrue();
        assertThat(item.getEnchantmentValue()).isEqualTo(1);
        assertThat(item.isRepairable(stack)).isFalse();
    }

    @Test
    @DisplayName("isBarVisible should be true only when damaged and not empty")
    void isBarVisible_evaluatesCorrectly() {
        ItemStack stack = new ItemStack(item);
        assertThat(item.isBarVisible(stack)).isFalse();

        stack.setDamageValue(15);
        assertThat(item.isBarVisible(stack)).isTrue();

        ItemDurabilityHelper.setEmpty(stack);
        assertThat(item.isBarVisible(stack)).isFalse();
    }

    @Test
    @DisplayName("resetToIdle on fully populated CompoundTag should clean all locator and routing fields")
    void resetToIdle_withPopulatedTag_cleansAllFields() {
        CompoundTag tag = new CompoundTag();
        UUID targetUUID = UUID.randomUUID();
        tag.putUUID("LocatorTargetUUID", targetUUID);
        tag.putLong("LocatorTargetUUIDMost", targetUUID.getMostSignificantBits());
        tag.putLong("LocatorTargetUUIDLeast", targetUUID.getLeastSignificantBits());
        tag.putInt("LocatorState", 3);
        tag.putLong("LocatorStateTimestamp", 12345L);
        tag.putInt("LocatorCachedStructX", 100);
        tag.putInt("LocatorCachedStructZ", -200);
        tag.putInt("LocatorCachedPortalY", 64);
        tag.putBoolean("LocatorCachedIsPortal", true);
        tag.putString("LocatorLastTargetDim", "minecraft:the_nether");
        tag.putString("LocatorLastMyDim", "minecraft:overworld");
        tag.putLong("LocatorLastCheckTick", 500L);
        tag.putLong("LocatorStructSearchFailedTick", 600L);

        item.resetToIdle(tag);

        assertThat(tag.getInt("LocatorState")).isEqualTo(0);
        assertThat(tag.contains("LocatorTargetUUID")).isFalse();
        assertThat(tag.contains("LocatorTargetUUIDMost")).isFalse();
        assertThat(tag.contains("LocatorTargetUUIDLeast")).isFalse();
        assertThat(tag.contains("LocatorStateTimestamp")).isFalse();
        assertThat(tag.contains("LocatorCachedStructX")).isFalse();
        assertThat(tag.contains("LocatorCachedStructZ")).isFalse();
        assertThat(tag.contains("LocatorCachedPortalY")).isFalse();
        assertThat(tag.contains("LocatorCachedIsPortal")).isFalse();
        assertThat(tag.contains("LocatorLastTargetDim")).isFalse();
        assertThat(tag.contains("LocatorLastMyDim")).isFalse();
        assertThat(tag.contains("LocatorLastCheckTick")).isFalse();
        assertThat(tag.contains("LocatorStructSearchFailedTick")).isFalse();
    }

    @Test
    @DisplayName("resetToIdle on empty CompoundTag should initialize state to 0 safely without exceptions")
    void resetToIdle_withEmptyTag_executesSafely() {
        CompoundTag emptyTag = new CompoundTag();

        item.resetToIdle(emptyTag);

        assertThat(emptyTag.getInt("LocatorState")).isEqualTo(0);
    }

    @Test
    @DisplayName("Enchantment rules should allow Unbreaking and reject Mending")
    void enchantmentRules_adhereToPolicy() {
        ItemStack stack = new ItemStack(item);

        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.UNBREAKING)).isTrue();
        assertThat(item.canApplyAtEnchantingTable(stack, Enchantments.MENDING)).isFalse();
    }

    @Test
    @DisplayName("shouldCauseReequipAnimation should reequip on slot change or different item")
    void reequipAnimation_evaluatesCorrectly() {
        ItemStack stackA = new ItemStack(item);
        ItemStack stackB = new ItemStack(item);
        ItemStack other = new ItemStack(ModItems.ANONYMIZER.get());

        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, false)).isFalse();
        assertThat(item.shouldCauseReequipAnimation(stackA, stackB, true)).isTrue();
        assertThat(item.shouldCauseReequipAnimation(stackA, other, false)).isTrue();
    }

    @Test
    @DisplayName("inventoryTick recalculates state before sending 20-tick target coordinate updates")
    void inventoryTick_recalculatesBeforeSendingCoordinates() {
        ModServerConfig.SPEC.acceptConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
        PodestBlockEntity.LOADED_PODESTS.clear();
        UUID targetId = UUID.randomUUID();
        ServerLevel overworld = mock(ServerLevel.class);
        when(overworld.isClientSide()).thenReturn(false);
        when(overworld.dimension()).thenReturn(Level.OVERWORLD);
        when(overworld.getGameTime()).thenReturn(100L);

        ServerPlayer owner = mock(ServerPlayer.class);
        ServerGamePacketListenerImpl connection = mock(ServerGamePacketListenerImpl.class);
        owner.connection = connection;
        ServerPlayer target = mock(ServerPlayer.class);
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList playerList = mock(PlayerList.class);

        MinecraftTestEnvironment.setField(owner, "server", server);
        when(server.getPlayerList()).thenReturn(playerList);
        when(playerList.getPlayer(targetId)).thenReturn(target);

        when(owner.level()).thenReturn(overworld);
        when(owner.isCreative()).thenReturn(true);
        when(owner.blockPosition()).thenReturn(BlockPos.ZERO);
        when(owner.getPersistentData()).thenReturn(new CompoundTag());

        Inventory targetInv = mock(Inventory.class);
        when(targetInv.getContainerSize()).thenReturn(0);
        when(target.getInventory()).thenReturn(targetInv);
        when(target.getUUID()).thenReturn(targetId);
        when(target.level()).thenReturn(overworld);
        when(target.blockPosition()).thenReturn(new BlockPos(50, 64, 50));
        when(target.position()).thenReturn(new Vec3(50, 64, 50));
        when(target.getArmorSlots()).thenReturn(java.util.Collections.emptyList());

        CompoundTag targetPersistent = new CompoundTag();
        targetPersistent.putUUID("bodycam_active_jammer_id", UUID.randomUUID());
        targetPersistent.putLong("bodycam_jammer_heartbeat", 100L);
        targetPersistent.putInt("bodycam_jammer_mode", 1);
        when(target.getPersistentData()).thenReturn(targetPersistent);

        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID("LocatorTargetUUID", targetId);
        tag.putInt("LocatorState", 1);
        tag.putLong("LocatorLastCheckTick", 0L);
        tag.putString("LocatorLastTargetDim", "minecraft:overworld");
        tag.putString("LocatorLastMyDim", "minecraft:overworld");

        try (MockedStatic<ModList> mockedModList = mockStatic(ModList.class)) {
            ModList modList = mock(ModList.class);
            when(modList.isLoaded("curios")).thenReturn(false);
            mockedModList.when(ModList::get).thenReturn(modList);

            item.inventoryTick(stack, overworld, owner, 0, true);
        }

        assertThat(tag.getInt("LocatorState")).isEqualTo(4);
        verify(connection, never()).send(any());
    }
}
