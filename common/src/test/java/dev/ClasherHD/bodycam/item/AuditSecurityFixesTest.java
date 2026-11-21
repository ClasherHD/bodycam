package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.network.creepingdeath.CreepingDeathActionC2SPacket;
import dev.ClasherHD.bodycam.network.creepingdeath.OpenCreepingDeathScreenS2CPacket;
import dev.ClasherHD.bodycam.util.BodycamPersistentDataHolder;
import dev.architectury.networking.NetworkManager;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("null")
class AuditSecurityFixesTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void tearDown() {
        LogBookItem.clearStaticState();
        CreepingDeathEntity.clearStaticState();
        ModServerConfig.BLACKLISTED_DIMENSIONS.set(List.of());
        ModServerConfig.ENABLE_PORTABLE_NETHER_PORTAL.set(true);
        ModServerConfig.ENABLE_CREEPING_DEATH.set(true);
        ModServerConfig.OP_ONLY_MODE.set(false);
    }

    private static ServerPlayer mockPlayerWithGameTime(UUID id, long gameTime) {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(gameTime);
        when(level.dimension()).thenReturn(Level.OVERWORLD);

        CompoundTag persistent = new CompoundTag();
        ServerPlayer player = mock(ServerPlayer.class, withSettings().extraInterfaces(BodycamPersistentDataHolder.class));
        when(((BodycamPersistentDataHolder) player).bodycam$getPersistentData()).thenReturn(persistent);
        when(player.getUUID()).thenReturn(id);
        when(player.level()).thenReturn(level);
        return player;
    }

    private static ItemStack mockCustomDataStack(Object item, boolean emptyDurability) {
        ItemStack stack = mock(ItemStack.class);
        AtomicReference<CustomData> customDataRef = new AtomicReference<>(null);
        when(stack.getItem()).thenReturn((net.minecraft.world.item.Item) item);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.isDamageableItem()).thenReturn(true);
        when(stack.getMaxDamage()).thenReturn(8);
        when(stack.getDamageValue()).thenReturn(emptyDurability ? 8 : 0);
        when(stack.has(DataComponents.UNBREAKABLE)).thenReturn(false);
        when(stack.get(DataComponents.CUSTOM_DATA)).thenAnswer(inv -> customDataRef.get());
        when(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
                .thenAnswer(inv -> customDataRef.get() != null ? customDataRef.get() : CustomData.EMPTY);
        doAnswer(inv -> {
            customDataRef.set(inv.getArgument(1));
            return null;
        }).when(stack).set(org.mockito.ArgumentMatchers.eq(DataComponents.CUSTOM_DATA), any(CustomData.class));
        return stack;
    }

    @Test
    void logBook_lockoutEscalatesResetsOnSuccessAndSurvivesLogoutCooldownClear() {
        UUID playerId = UUID.randomUUID();
        ServerPlayer player = mockPlayerWithGameTime(playerId, 100L);
        ServerLevel level = player.level();

        for (int i = 0; i < 4; i++) {
            LogBookItem.recordFailedAttempt(player);
            assertThat(LogBookItem.isLockedOut(player)).isFalse();
        }

        // 5th failure triggers 200-tick lockout
        LogBookItem.recordFailedAttempt(player);
        assertThat(LogBookItem.isLockedOut(player)).isTrue();

        // Logout clears attempt cooldown only, not security lockout
        assertThat(LogBookItem.isOnCooldown(player)).isFalse();
        assertThat(LogBookItem.isOnCooldown(player)).isTrue();
        LogBookItem.clearPlayerCooldown(playerId);
        assertThat(LogBookItem.isOnCooldown(player)).isFalse();
        assertThat(LogBookItem.isLockedOut(player)).isTrue();

        // Once lockout expires and unlock succeeds, clearLockout resets failed attempt count
        when(level.getGameTime()).thenReturn(400L);
        assertThat(LogBookItem.isLockedOut(player)).isFalse();
        LogBookItem.clearLockout(playerId);

        LogBookItem.recordFailedAttempt(player);
        assertThat(LogBookItem.isLockedOut(player)).isFalse();
    }

    @Test
    void dimensionLocator_rejectsEmptyOrInvalidDimensionAndAcceptsValidServerDimension() {
        UUID id = UUID.randomUUID();
        ServerPlayer player = mockPlayerWithGameTime(id, 100L);
        MinecraftServer server = mock(MinecraftServer.class);
        when(player.level().getServer()).thenReturn(server);
        when(server.levelKeys()).thenReturn(Set.of(Level.OVERWORLD, Level.NETHER));

        DimensionLocatorItem locatorItem = mock(DimensionLocatorItem.class);
        ItemStack emptyStack = mockCustomDataStack(locatorItem, true);
        when(player.getMainHandItem()).thenReturn(emptyStack);
        when(player.getOffhandItem()).thenReturn(ItemStack.EMPTY);

        // Depleted item must be rejected
        DimensionLocatorItem.handleSelectDimension(player, "minecraft:the_nether");
        assertThat(emptyStack.get(DataComponents.CUSTOM_DATA)).isNull();

        // Valid item rejects malformed or unknown dimensions
        ItemStack chargedStack = mockCustomDataStack(locatorItem, false);
        when(player.getMainHandItem()).thenReturn(chargedStack);
        DimensionLocatorItem.handleSelectDimension(player, "Invalid Dimension!");
        DimensionLocatorItem.handleSelectDimension(player, "custom:not_registered");
        assertThat(chargedStack.get(DataComponents.CUSTOM_DATA)).isNull();

        // Valid server dimension is stored
        DimensionLocatorItem.handleSelectDimension(player, "minecraft:the_nether");
        assertThat(chargedStack.get(DataComponents.CUSTOM_DATA)).isNotNull();
        assertThat(chargedStack.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("SelectedDimension", ""))
                .isEqualTo("minecraft:the_nether");
    }

    @Test
    void playerLocator_handleSelectionIgnoresEmptyDurabilityItem() {
        ServerPlayer player = mockPlayerWithGameTime(UUID.randomUUID(), 100L);
        PlayerLocatorCompassItem compassItem = mock(PlayerLocatorCompassItem.class);
        ItemStack emptyCompass = mockCustomDataStack(compassItem, true);
        when(player.getMainHandItem()).thenReturn(emptyCompass);
        when(player.getOffhandItem()).thenReturn(ItemStack.EMPTY);

        PlayerLocatorCompassItem.handleSelection(player, UUID.randomUUID(), false);

        verify(emptyCompass, never()).set(any(), any());
    }

    @Test
    void portablePortal_executeTeleportRequiresActiveChannelingAndRespectsCooldownAndBlacklist() {
        ServerPlayer player = mockPlayerWithGameTime(UUID.randomUUID(), 100L);
        PortableNetherPortalItem portalItem = mock(PortableNetherPortalItem.class);
        ItemStack portalStack = mockCustomDataStack(portalItem, false);
        ItemCooldowns cooldowns = mock(ItemCooldowns.class);

        when(player.getMainHandItem()).thenReturn(portalStack);
        when(player.getCooldowns()).thenReturn(cooldowns);

        // Not channeling -> no teleport, no message about missing charges
        when(cooldowns.isOnCooldown(portalStack)).thenReturn(false);
        when(player.isUsingItem()).thenReturn(false);
        PortableNetherPortalItem.executeTeleport(player);
        verify(player, never()).displayClientMessage(any(), org.mockito.ArgumentMatchers.anyBoolean());

        // Channeling with too few ticks -> blocked
        when(player.isUsingItem()).thenReturn(true);
        when(player.getUseItem()).thenReturn(portalStack);
        when(player.getTicksUsingItem()).thenReturn(PortableNetherPortalItem.CHANNEL_TICKS - 1);
        PortableNetherPortalItem.executeTeleport(player);
        verify(player, never()).displayClientMessage(any(), org.mockito.ArgumentMatchers.anyBoolean());

        // On cooldown -> blocked even with full ticks
        when(player.getTicksUsingItem()).thenReturn(PortableNetherPortalItem.CHANNEL_TICKS);
        when(cooldowns.isOnCooldown(portalStack)).thenReturn(true);
        PortableNetherPortalItem.executeTeleport(player);
        verify(player, never()).displayClientMessage(any(), org.mockito.ArgumentMatchers.anyBoolean());

        // Blacklisted dimension -> blocked with message before teleport
        when(cooldowns.isOnCooldown(portalStack)).thenReturn(false);
        ModServerConfig.BLACKLISTED_DIMENSIONS.set(List.of("minecraft:overworld"));
        PortableNetherPortalItem.executeTeleport(player);
        verify(player).displayClientMessage(any(Component.class), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    void creepingDeathPacket_checksAlreadyHuntedBeforeResettingTargetBodycam() {
        UUID senderId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        ServerPlayer sender = mockPlayerWithGameTime(senderId, 100L);
        ServerPlayer target = mockPlayerWithGameTime(targetId, 100L);
        ((BodycamPersistentDataHolder) target).bodycam$getPersistentData().putBoolean("bodycam_active", true);

        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList playerList = mock(PlayerList.class);
        when(sender.level().getServer()).thenReturn(server);
        when(server.getPlayerList()).thenReturn(playerList);
        when(playerList.getPlayer(targetId)).thenReturn(target);

        when(target.isAlive()).thenReturn(true);
        when(target.isCreative()).thenReturn(false);
        when(target.isSpectator()).thenReturn(false);

        CreepingDeathItem cdItem = mock(CreepingDeathItem.class);
        ItemStack cdStack = mockCustomDataStack(cdItem, false);
        when(sender.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(cdStack);

        synchronized (CreepingDeathEntity.PENDING_SPAWNS) {
            CreepingDeathEntity.PENDING_SPAWNS.add(new CreepingDeathEntity.PendingSpawn(targetId, targetId, "Victim", 200));
        }

        NetworkManager.PacketContext ctx = mock(NetworkManager.PacketContext.class);
        when(ctx.getPlayer()).thenReturn(sender);
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(ctx).queue(any(Runnable.class));

        CreepingDeathActionC2SPacket packet = new CreepingDeathActionC2SPacket(
                OpenCreepingDeathScreenS2CPacket.MODE_TARGET, false, targetId, "Victim");
        CreepingDeathActionC2SPacket.handle(packet, ctx);

        // Must notify sender that target is already hunted WITHOUT resetting target's active bodycam or shrinking item
        assertThat(((BodycamPersistentDataHolder) target).bodycam$getPersistentData().getBooleanOr("bodycam_active", false))
                .isTrue();
        verify(cdStack, never()).shrink(1);

        ArgumentCaptor<Component> msgCaptor = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendSystemMessage(msgCaptor.capture());
        assertThat(msgCaptor.getValue().getString()).contains("already_hunted");
    }

    @Test
    void creepingDeath_scanUserCacheFileParsesValidEntriesAndHandlesMalformedOrMissing(@TempDir Path tempDir) throws Exception {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        Path validFile = tempDir.resolve("usercache.json");
        Files.writeString(validFile,
                "[{\"uuid\":\"" + u1 + "\",\"name\":\"Alice\"},{\"uuid\":\"" + u2 + "\",\"name\":\"\"},{\"name\":\"MissingUuid\"}]",
                StandardCharsets.UTF_8);

        Map<UUID, String> parsed = CreepingDeathItem.scanUserCacheFile(validFile);
        assertThat(parsed).containsExactly(Map.entry(u1, "Alice"));

        Path malformed = tempDir.resolve("bad.json");
        Files.writeString(malformed, "{not valid json", StandardCharsets.UTF_8);
        assertThat(CreepingDeathItem.scanUserCacheFile(malformed)).isEmpty();
        assertThat(CreepingDeathItem.scanUserCacheFile(tempDir.resolve("missing.json"))).isEmpty();
    }
}
