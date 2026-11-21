package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class DimensionLocatorItemTest {

    @Test
    void handleSelectDimension_rejectsEmptyItemAndUnknownOrInvalidDimensions() {
        MinecraftServer server = mock(MinecraftServer.class);
        ServerLevel overworld = mock(ServerLevel.class);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(overworld);

        ServerPlayer player = mock(ServerPlayer.class);
        try {
            var serverField = ServerPlayer.class.getField("server");
            serverField.setAccessible(true);
            serverField.set(player, server);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        ItemStack stack = mock(ItemStack.class);
        DimensionLocatorItem item = mock(DimensionLocatorItem.class);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.getItem()).thenReturn(item);
        when(player.getMainHandItem()).thenReturn(stack);

        try (MockedStatic<ItemDurabilityHelper> durabilityMock = Mockito.mockStatic(ItemDurabilityHelper.class);
             MockedStatic<ItemNbtAccess> nbtMock = Mockito.mockStatic(ItemNbtAccess.class)) {

            // Empty durability -> reject even valid dimension
            durabilityMock.when(() -> ItemDurabilityHelper.isEmpty(stack)).thenReturn(true);
            DimensionLocatorItem.handleSelectDimension(player, "minecraft:overworld");
            nbtMock.verify(() -> ItemNbtAccess.putString(any(), anyString(), anyString()), never());

            // Non-empty item, invalid dimension string -> reject
            durabilityMock.when(() -> ItemDurabilityHelper.isEmpty(stack)).thenReturn(false);
            DimensionLocatorItem.handleSelectDimension(player, "Not A Dimension!");
            nbtMock.verify(() -> ItemNbtAccess.putString(any(), anyString(), anyString()), never());

            // Non-empty item, unknown dimension on server -> reject
            DimensionLocatorItem.handleSelectDimension(player, "minecraft:the_end");
            nbtMock.verify(() -> ItemNbtAccess.putString(any(), anyString(), anyString()), never());

            // Non-empty item, valid server dimension -> allow
            DimensionLocatorItem.handleSelectDimension(player, "minecraft:overworld");
            nbtMock.verify(() -> ItemNbtAccess.putString(stack, "SelectedDimension", "minecraft:overworld"));
        }
    }
}
