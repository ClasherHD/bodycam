package dev.ClasherHD.bodycam.event;

import com.mojang.brigadier.CommandDispatcher;
import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.item.LogBookItem;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
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
        dev.ClasherHD.bodycam.entity.CreepingDeathEntity.ACTIVE_HUNTS_BY_TARGET.put(id, UUID.randomUUID());
        synchronized (dev.ClasherHD.bodycam.entity.CreepingDeathEntity.PENDING_SPAWNS) {
            dev.ClasherHD.bodycam.entity.CreepingDeathEntity.PENDING_SPAWNS.add(
                    new dev.ClasherHD.bodycam.entity.CreepingDeathEntity.PendingSpawn(id, null, "test", 100));
        }
        synchronized (dev.ClasherHD.bodycam.entity.CreepingDeathEntity.PENDING_OSC_NUKES) {
            dev.ClasherHD.bodycam.entity.CreepingDeathEntity.PENDING_OSC_NUKES.add(
                    new dev.ClasherHD.bodycam.entity.CreepingDeathEntity.PendingOscNuke(Level.OVERWORLD, Vec3.ZERO, 50));
        }

        ServerStoppedEvent event = MinecraftTestEnvironment.allocateInstance(ServerStoppedEvent.class);
        ServerEvents.onServerStopped(event);

        assertThat(ServerEvents.POSITION_LOCKS).isEmpty();
        assertThat(BodycamSetCameraPacket.ORIGINAL_POS).isEmpty();
        assertThat(BodycamDummyEntity.DUMMY_POS).isEmpty();
        assertThat(PodestBlockEntity.LOADED_PODESTS).isEmpty();
        assertThat(dev.ClasherHD.bodycam.entity.CreepingDeathEntity.ACTIVE_HUNTS_BY_TARGET).isEmpty();
        assertThat(dev.ClasherHD.bodycam.entity.CreepingDeathEntity.PENDING_SPAWNS).isEmpty();
        assertThat(dev.ClasherHD.bodycam.entity.CreepingDeathEntity.PENDING_OSC_NUKES).isEmpty();
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
        LogBookItem.clearStaticState();
        assertThat(LogBookItem.isOnCooldown(player)).isFalse();
        assertThat(LogBookItem.isOnCooldown(player)).isTrue();

        PlayerEvent.PlayerLoggedOutEvent event = new PlayerEvent.PlayerLoggedOutEvent(player);
        ServerEvents.onPlayerLogout(event);

        assertThat(ServerEvents.POSITION_LOCKS).doesNotContainKey(playerId);
        assertThat(LogBookItem.isOnCooldown(player)).isFalse();
        LogBookItem.clearStaticState();
    }

    @Test
    @DisplayName("onPlayerLogin safely handles invalid dimension string and non-finite recovery coordinates")
    void onPlayerLogin_handlesInvalidDimensionAndNonFiniteCoords() {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        net.minecraft.server.MinecraftServer server = mock(net.minecraft.server.MinecraftServer.class);
        when(level.isClientSide()).thenReturn(false);
        when(player.level()).thenReturn(level);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        MinecraftTestEnvironment.setField(player, "server", server);

        CompoundTag persistent = new CompoundTag();
        persistent.putBoolean("bodycam_active", true);
        persistent.putString("bodycam_orig_dim", "Invalid Dimension String");
        persistent.putDouble("bodycam_orig_x", Double.NaN);
        persistent.putDouble("bodycam_orig_y", 64.0D);
        persistent.putDouble("bodycam_orig_z", 0.0D);
        when(player.getPersistentData()).thenReturn(persistent);

        PlayerEvent.PlayerLoggedInEvent event = new PlayerEvent.PlayerLoggedInEvent(player);
        assertThatCode(() -> ServerEvents.onPlayerLogin(event)).doesNotThrowAnyException();
        verify(player, never()).teleportTo(any(), anyDouble(), anyDouble(), anyDouble(), anyFloat(), anyFloat());

        CompoundTag nonFiniteTag = new CompoundTag();
        nonFiniteTag.putBoolean("bodycam_active", true);
        nonFiniteTag.putString("bodycam_orig_dim", "minecraft:overworld");
        nonFiniteTag.putDouble("bodycam_orig_x", Double.POSITIVE_INFINITY);
        nonFiniteTag.putDouble("bodycam_orig_y", 64.0D);
        nonFiniteTag.putDouble("bodycam_orig_z", 0.0D);
        when(player.getPersistentData()).thenReturn(nonFiniteTag);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);

        assertThatCode(() -> ServerEvents.onPlayerLogin(event)).doesNotThrowAnyException();
        verify(player, never()).teleportTo(any(), anyDouble(), anyDouble(), anyDouble(), anyFloat(), anyFloat());
    }

    @Test
    @DisplayName("onPlayerTick skips fog particles when coordinates are non-finite or chunk is unloaded")
    void onPlayerTick_guardsFogParticlesWithFiniteAndIsLoaded() {
        ServerPlayer observer = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        AbstractContainerMenu menu = mock(AbstractContainerMenu.class);
        when(menu.getCarried()).thenReturn(ItemStack.EMPTY);
        observer.containerMenu = menu;
        when(level.isClientSide()).thenReturn(false);
        when(observer.level()).thenReturn(level);
        when(observer.serverLevel()).thenReturn(level);
        when(observer.getUUID()).thenReturn(UUID.randomUUID());
        observer.tickCount = 1;

        CompoundTag persistent = new CompoundTag();
        persistent.putInt("bodycam_all_seeing_fog_ticks", 2);
        persistent.putDouble("bodycam_fog_x", Double.NaN);
        persistent.putDouble("bodycam_fog_y", 64.0D);
        persistent.putDouble("bodycam_fog_z", 0.0D);
        when(observer.getPersistentData()).thenReturn(persistent);

        try (MockedStatic<ModList> mockedModList = mockStatic(ModList.class)) {
            ModList modList = mock(ModList.class);
            when(modList.isLoaded("curios")).thenReturn(false);
            mockedModList.when(ModList::get).thenReturn(modList);

            TickEvent.PlayerTickEvent event = new TickEvent.PlayerTickEvent(TickEvent.Phase.END, observer);
            ServerEvents.onPlayerTick(event);
            verify(level, never()).sendParticles(any(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble());

            persistent.putInt("bodycam_all_seeing_fog_ticks", 1);
            persistent.putDouble("bodycam_fog_x", 10.0D);
            when(level.isLoaded(BlockPos.containing(10.0D, 64.0D, 0.0D))).thenReturn(false);
            ServerEvents.onPlayerTick(event);
            verify(level, never()).sendParticles(any(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble());

            persistent.putInt("bodycam_all_seeing_fog_ticks", 1);
            when(level.isLoaded(BlockPos.containing(10.0D, 64.0D, 0.0D))).thenReturn(true);
            ServerEvents.onPlayerTick(event);
            verify(level).sendParticles(ParticleTypes.END_ROD, 10.0D, 65.0D, 0.0D, 25, 0.85D, 0.85D, 0.85D, 0.03D);
        }
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
