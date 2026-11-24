package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.event.ServerEvents;
import dev.ClasherHD.bodycam.util.BodycamPersistentDataHolder;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class CursorMovePreservationTest {

    private static JammerItem testJammerItem;
    private static AnonymizerItem testAnonymizerItem;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        testJammerItem = Mockito.mock(JammerItem.class, Mockito.CALLS_REAL_METHODS);
        testAnonymizerItem = Mockito.mock(AnonymizerItem.class, Mockito.CALLS_REAL_METHODS);
    }

    private static ServerPlayer mockPlayer(ServerLevel level, CompoundTag pData, boolean creative,
                                           InventoryMenu invMenu, AbstractContainerMenu openMenu, Inventory inventory) {
        ServerPlayer player = mock(ServerPlayer.class, Mockito.withSettings().extraInterfaces(BodycamPersistentDataHolder.class));
        when(((BodycamPersistentDataHolder) player).bodycam$getPersistentData()).thenReturn(pData);
        when(player.level()).thenReturn(level);
        when(player.isCreative()).thenReturn(creative);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
        try {
            Field invMenuField = Player.class.getField("inventoryMenu");
            invMenuField.setAccessible(true);
            invMenuField.set(player, invMenu);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        player.containerMenu = openMenu;
        player.tickCount = 1;
        return player;
    }

    @Test
    void creativeCursorMove_preservesActiveJammerWhenPlacedBackInInventory() {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(100L);
        CompoundTag pData = new CompoundTag();
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContainerSize()).thenReturn(0);
        InventoryMenu invMenu = mock(InventoryMenu.class);
        when(invMenu.getCarried()).thenReturn(ItemStack.EMPTY);

        ServerPlayer player = mockPlayer(level, pData, true, invMenu, invMenu, inventory);

        UUID activeId = UUID.randomUUID();
        ItemStack jammer = new ItemStack(Holder.direct(testJammerItem));
        ItemNbtAccess.updateTag(jammer, tag -> {
            tag.putInt("JammerMode", 2);
            PersistentDataAccess.putUUID(tag, "active_id", activeId);
        });
        PersistentDataAccess.putUUID(pData, "bodycam_active_jammer_id", activeId);
        pData.putInt("bodycam_jammer_mode", 2);
        pData.putLong("bodycam_jammer_heartbeat", 100L);

        // Tick while Creative cursor holds the item (slot cleared on server, carried is EMPTY).
        ServerEvents.syncPersonalDeviceState(player, pData, ItemStack.EMPTY, Collections.emptyList(), Collections.emptyList());

        assertThat(PersistentDataAccess.hasUUID(pData, "bodycam_active_jammer_id")).isFalse();
        assertThat(PersistentDataAccess.getUUID(pData, "bodycam_creative_carried_jammer_id")).isEqualTo(activeId);

        // Player places the jammer into another slot 40 ticks later -> inventoryTick runs.
        when(level.getGameTime()).thenReturn(140L);
        testJammerItem.inventoryTick(jammer, level, player, null);

        assertThat(JammerItem.getMode(jammer)).isEqualTo(2);
        assertThat(PersistentDataAccess.getUUID(pData, "bodycam_active_jammer_id")).isEqualTo(activeId);
        assertThat(pData.getIntOr("bodycam_jammer_mode", 0)).isEqualTo(2);
        assertThat(PersistentDataAccess.hasUUID(pData, "bodycam_creative_carried_jammer_id")).isFalse();
    }

    @Test
    void creativeCursorMove_preservesActiveAnonymizerWhenPlacedBackInInventory() {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(200L);
        CompoundTag pData = new CompoundTag();
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContainerSize()).thenReturn(0);
        InventoryMenu invMenu = mock(InventoryMenu.class);
        when(invMenu.getCarried()).thenReturn(ItemStack.EMPTY);

        ServerPlayer player = mockPlayer(level, pData, true, invMenu, invMenu, inventory);

        UUID activeId = UUID.randomUUID();
        ItemStack anonymizer = new ItemStack(Holder.direct(testAnonymizerItem));
        ItemNbtAccess.updateTag(anonymizer, tag -> {
            tag.putBoolean("AnonymizerActive", true);
            PersistentDataAccess.putUUID(tag, "active_id", activeId);
        });
        PersistentDataAccess.putUUID(pData, "bodycam_active_anonymizer_id", activeId);
        pData.putLong("bodycam_anonymizer_heartbeat", 200L);

        ServerEvents.syncPersonalDeviceState(player, pData, ItemStack.EMPTY, Collections.emptyList(), Collections.emptyList());

        assertThat(PersistentDataAccess.hasUUID(pData, "bodycam_active_anonymizer_id")).isFalse();
        assertThat(PersistentDataAccess.getUUID(pData, "bodycam_creative_carried_anonymizer_id")).isEqualTo(activeId);

        when(level.getGameTime()).thenReturn(260L);
        testAnonymizerItem.inventoryTick(anonymizer, level, player, null);

        assertThat(ItemNbtAccess.getBoolean(anonymizer, "AnonymizerActive")).isTrue();
        assertThat(PersistentDataAccess.getUUID(pData, "bodycam_active_anonymizer_id")).isEqualTo(activeId);
        assertThat(PersistentDataAccess.hasUUID(pData, "bodycam_creative_carried_anonymizer_id")).isFalse();
    }

    @Test
    void creativeDropOrChestMove_stillTurnsOffJammer() {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(300L);
        CompoundTag pData = new CompoundTag();
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContainerSize()).thenReturn(0);
        InventoryMenu invMenu = mock(InventoryMenu.class);
        AbstractContainerMenu chestMenu = mock(AbstractContainerMenu.class);
        when(chestMenu.getCarried()).thenReturn(ItemStack.EMPTY);

        ServerPlayer player = mockPlayer(level, pData, true, invMenu, chestMenu, inventory);

        UUID activeId = UUID.randomUUID();
        ItemStack jammer = new ItemStack(Holder.direct(testJammerItem));
        ItemNbtAccess.updateTag(jammer, tag -> {
            tag.putInt("JammerMode", 1);
            PersistentDataAccess.putUUID(tag, "active_id", activeId);
        });
        PersistentDataAccess.putUUID(pData, "bodycam_active_jammer_id", activeId);
        pData.putInt("bodycam_jammer_mode", 1);
        pData.putLong("bodycam_jammer_heartbeat", 300L);

        // Placed into a chest (containerMenu != inventoryMenu) -> must NOT save creative carried id.
        ServerEvents.syncPersonalDeviceState(player, pData, ItemStack.EMPTY, Collections.emptyList(), Collections.emptyList());
        assertThat(PersistentDataAccess.hasUUID(pData, "bodycam_creative_carried_jammer_id")).isFalse();

        // Taking it back out later turns it off.
        testJammerItem.inventoryTick(jammer, level, player, null);
        assertThat(JammerItem.getMode(jammer)).isZero();
    }

    @Test
    void survivalCarriedJammer_staysActiveWhileHeldOnCursor() {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(400L);
        CompoundTag pData = new CompoundTag();
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContainerSize()).thenReturn(0);

        UUID activeId = UUID.randomUUID();
        ItemStack jammer = new ItemStack(Holder.direct(testJammerItem));
        ItemNbtAccess.updateTag(jammer, tag -> {
            tag.putInt("JammerMode", 1);
            PersistentDataAccess.putUUID(tag, "active_id", activeId);
        });
        InventoryMenu invMenu = mock(InventoryMenu.class);
        when(invMenu.getCarried()).thenReturn(jammer);

        ServerPlayer player = mockPlayer(level, pData, false, invMenu, invMenu, inventory);
        PersistentDataAccess.putUUID(pData, "bodycam_active_jammer_id", activeId);
        pData.putInt("bodycam_jammer_mode", 1);
        pData.putLong("bodycam_jammer_heartbeat", 398L);

        ServerEvents.syncPersonalDeviceState(player, pData, jammer, Collections.emptyList(), Collections.emptyList());

        assertThat(JammerItem.getMode(jammer)).isEqualTo(1);
        assertThat(PersistentDataAccess.getUUID(pData, "bodycam_active_jammer_id")).isEqualTo(activeId);
    }
}
