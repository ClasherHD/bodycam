package dev.ClasherHD.bodycam.event;

import com.mojang.brigadier.CommandDispatcher;
import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerEventsTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("LockData constructor should initialize all fields with default lockTicks of 10")
    void lockData_initializesFieldsProperly() {
        ServerEvents.LockData data = new ServerEvents.LockData(Level.OVERWORLD, 10.5, 64.0, -20.5, 90.0F, 0.0F);

        assertThat(data.originalDim).isEqualTo(Level.OVERWORLD);
        assertThat(data.x).isEqualTo(10.5);
        assertThat(data.y).isEqualTo(64.0);
        assertThat(data.z).isEqualTo(-20.5);
        assertThat(data.yaw).isEqualTo(90.0F);
        assertThat(data.pitch).isEqualTo(0.0F);
        assertThat(data.lockTicks).isEqualTo(10);
    }

    @Test
    @DisplayName("onServerStopped should clear all position locks, camera tracking, and loaded podest sets")
    void onServerStopped_clearsAllState() {
        UUID id = UUID.randomUUID();
        ServerEvents.POSITION_LOCKS.put(id, new ServerEvents.LockData(Level.OVERWORLD, 0, 0, 0, 0, 0));
        BodycamSetCameraPacket.ORIGINAL_POS.put(id, Vec3.ZERO);
        BodycamDummyEntity.DUMMY_POS.put(id, Vec3.ZERO);
        PodestBlockEntity podest = new PodestBlockEntity(BlockPos.ZERO, ModBlocks.PODEST.get().defaultBlockState());
        PodestBlockEntity.LOADED_PODESTS.add(podest);

        ServerStoppedEvent event = MinecraftTestEnvironment.allocateInstance(ServerStoppedEvent.class);
        ServerEvents.onServerStopped(event);

        assertThat(ServerEvents.POSITION_LOCKS).isEmpty();
        assertThat(BodycamSetCameraPacket.ORIGINAL_POS).isEmpty();
        assertThat(BodycamDummyEntity.DUMMY_POS).isEmpty();
        assertThat(PodestBlockEntity.LOADED_PODESTS).isEmpty();
    }

    @Test
    @DisplayName("onAnvilUpdate should cancel combining mod repairable item with invalid enchanted book or unenchanted same item")
    void onAnvilUpdate_blocksInvalidCombinations() {
        ItemStack monitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);

        AnvilUpdateEvent bookEvent = new AnvilUpdateEvent(monitor, enchantedBook, "Monitor", 1, null);
        ServerEvents.onAnvilUpdate(bookEvent);
        assertThat(bookEvent.isCanceled()).isTrue();

        ItemStack secondMonitor = new ItemStack(ModItems.BODYCAM_MONITOR.get());
        AnvilUpdateEvent sameItemEvent = new AnvilUpdateEvent(monitor, secondMonitor, "Monitor", 1, null);
        ServerEvents.onAnvilUpdate(sameItemEvent);
        assertThat(sameItemEvent.isCanceled()).isTrue();
    }

    @Test
    @DisplayName("onAnvilUpdate should ignore non-mod items")
    void onAnvilUpdate_ignoresVanillaItems() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack diamond = new ItemStack(Items.DIAMOND);

        AnvilUpdateEvent event = new AnvilUpdateEvent(sword, diamond, "Sword", 1, null);
        ServerEvents.onAnvilUpdate(event);

        assertThat(event.isCanceled()).isFalse();
    }

    @Test
    @DisplayName("onRegisterCommands registers bodycam command root")
    void onRegisterCommands_registersCommand() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        RegisterCommandsEvent event = new RegisterCommandsEvent(dispatcher, net.minecraft.commands.Commands.CommandSelection.ALL, null);
        ServerEvents.onRegisterCommands(event);

        assertThat(dispatcher.getRoot().getChild("bodycam")).isNotNull();
    }

    @Test
    @DisplayName("onPlayerLogin does nothing when player has no active bodycam session")
    void onPlayerLogin_inactiveSession_doesNothing() {
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        when(player.level()).thenReturn(level);
        CompoundTag persistentData = new CompoundTag();
        when(player.getPersistentData()).thenReturn(persistentData);

        PlayerEvent.PlayerLoggedInEvent event = new PlayerEvent.PlayerLoggedInEvent(player);
        assertThatCode(() -> ServerEvents.onPlayerLogin(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("onPlayerLogout removes player from position locks and cooldowns")
    void onPlayerLogout_clearsPlayerLocks() {
        ServerPlayer player = mock(ServerPlayer.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        when(player.level()).thenReturn(level);
        UUID playerId = UUID.randomUUID();
        when(player.getUUID()).thenReturn(playerId);
        CompoundTag persistentData = new CompoundTag();
        when(player.getPersistentData()).thenReturn(persistentData);

        ServerEvents.POSITION_LOCKS.put(playerId, new ServerEvents.LockData(Level.OVERWORLD, 0, 0, 0, 0, 0));

        PlayerEvent.PlayerLoggedOutEvent event = new PlayerEvent.PlayerLoggedOutEvent(player);
        ServerEvents.onPlayerLogout(event);

        assertThat(ServerEvents.POSITION_LOCKS).doesNotContainKey(playerId);
    }

    @Test
    @DisplayName("onPlayerTick ignores START phase and client side levels")
    void onPlayerTick_ignoresStartAndClient() {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(true);
        when(player.level()).thenReturn(level);

        TickEvent.PlayerTickEvent startEvent = new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player);
        assertThatCode(() -> ServerEvents.onPlayerTick(startEvent)).doesNotThrowAnyException();

        TickEvent.PlayerTickEvent endClientEvent = new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player);
        assertThatCode(() -> ServerEvents.onPlayerTick(endClientEvent)).doesNotThrowAnyException();
    }
}
