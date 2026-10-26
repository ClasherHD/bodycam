package dev.ClasherHD.bodycam.block.entity;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class WatchPointBlockEntityTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        com.electronwill.nightconfig.core.CommentedConfig serverConfig = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        ModServerConfig.SPEC.acceptConfig(serverConfig);
    }

    private WatchPointBlockEntity createEntity() {
        return new WatchPointBlockEntity(BlockPos.ZERO, ModBlocks.WATCH_POINT.get().defaultBlockState());
    }

    @Test
    @DisplayName("setOwner should set ownerUUID and sanitize null ownerName to empty string")
    void setOwner_sanitizesInput() {
        WatchPointBlockEntity entity = createEntity();
        UUID ownerId = UUID.randomUUID();

        entity.setOwner(ownerId, null);
        assertThat(entity.getOwnerUUID()).isEqualTo(ownerId);
        String name = MinecraftTestEnvironment.getField(entity, "ownerName");
        assertThat(name).isEmpty();

        entity.setOwner(ownerId, "Alex");
        name = MinecraftTestEnvironment.getField(entity, "ownerName");
        assertThat(name).isEqualTo("Alex");
    }

    @ParameterizedTest(name = "input=''{0}'' -> expected=''{1}''")
    @CsvSource({
            "'', ''",
            "'   trimmed   ', 'trimmed'",
            "'1234567890123456789012345678901234567890', '12345678901234567890123456789012'"
    })
    @DisplayName("setCustomName should trim and truncate names exceeding 32 characters")
    void setCustomName_trimsAndTruncates(String inputName, String expectedName) {
        WatchPointBlockEntity entity = createEntity();
        entity.setCustomName(inputName);

        assertThat(entity.getCustomName()).isEqualTo(expectedName);
    }

    @Test
    @DisplayName("setCustomName with null should set empty string")
    void setCustomName_withNull_setsEmptyString() {
        WatchPointBlockEntity entity = createEntity();
        entity.setCustomName(null);

        assertThat(entity.getCustomName()).isEmpty();
    }

    @Test
    @DisplayName("clearActiveDummyIfMatches should clear dummy only when UUIDs match")
    void clearActiveDummyIfMatches_evaluatesCorrectly() {
        WatchPointBlockEntity entity = createEntity();
        UUID dummyA = UUID.randomUUID();
        UUID dummyB = UUID.randomUUID();

        MinecraftTestEnvironment.setField(entity, "activeDummyUUID", dummyA);

        entity.clearActiveDummyIfMatches(dummyB);
        UUID current = MinecraftTestEnvironment.getField(entity, "activeDummyUUID");
        assertThat(current).isEqualTo(dummyA);

        entity.clearActiveDummyIfMatches(dummyA);
        current = MinecraftTestEnvironment.getField(entity, "activeDummyUUID");
        assertThat(current).isNull();
    }

    @Test
    @DisplayName("onPlayerInteract rejects non-owner player")
    void onPlayerInteract_rejectsNonOwner() {
        WatchPointBlockEntity entity = createEntity();
        UUID ownerId = UUID.randomUUID();
        entity.setOwner(ownerId, "OwnerPlayer");

        ServerPlayer intruder = mock(ServerPlayer.class);
        when(intruder.getUUID()).thenReturn(UUID.randomUUID());
        when(intruder.hasPermissions(2)).thenReturn(true);

        entity.onPlayerInteract(intruder);
        verify(intruder).displayClientMessage(any(Component.class), eq(true));
    }

    @Test
    @DisplayName("onPlayerInteract handles shift key press")
    void onPlayerInteract_shiftOpensScreen() {
        WatchPointBlockEntity entity = createEntity();
        UUID ownerId = UUID.randomUUID();
        entity.setOwner(ownerId, "OwnerPlayer");

        ServerPlayer player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(ownerId);
        when(player.hasPermissions(2)).thenReturn(true);
        when(player.isShiftKeyDown()).thenReturn(true);
        net.minecraft.server.network.ServerGamePacketListenerImpl connection = mock(net.minecraft.server.network.ServerGamePacketListenerImpl.class);
        net.minecraft.network.Connection netConnection = mock(net.minecraft.network.Connection.class);
        MinecraftTestEnvironment.setField(connection, "connection", netConnection);
        player.connection = connection;

        entity.onPlayerInteract(player);
        verify(netConnection).send(any());
    }

    @Test
    @DisplayName("despawnDummy executes safely on non-server levels")
    void despawnDummy_safeOnClientLevel() {
        WatchPointBlockEntity entity = createEntity();
        Level clientLevel = mock(Level.class);

        assertThatCode(() -> entity.despawnDummy(clientLevel)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("NBT serialization and deserialization should preserve owner, custom name, and active dummy UUID")
    void nbtRoundtrip_preservesState() {
        WatchPointBlockEntity entity = createEntity();
        UUID ownerId = UUID.randomUUID();
        UUID dummyId = UUID.randomUUID();

        entity.setOwner(ownerId, "Operator");
        entity.setCustomName("Perimeter Watch");
        MinecraftTestEnvironment.setField(entity, "activeDummyUUID", dummyId);

        CompoundTag tag = new CompoundTag();
        entity.saveAdditional(tag);

        WatchPointBlockEntity restored = createEntity();
        restored.load(tag);

        assertThat(restored.getOwnerUUID()).isEqualTo(ownerId);
        String restoredOwnerName = MinecraftTestEnvironment.getField(restored, "ownerName");
        assertThat(restoredOwnerName).isEqualTo("Operator");
        assertThat(restored.getCustomName()).isEqualTo("Perimeter Watch");
        UUID restoredDummyId = MinecraftTestEnvironment.getField(restored, "activeDummyUUID");
        assertThat(restoredDummyId).isEqualTo(dummyId);
    }
}
