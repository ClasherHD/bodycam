package dev.ClasherHD.bodycam.item;

import com.mojang.authlib.GameProfile;
import dev.ClasherHD.bodycam.network.creepingdeath.OpenCreepingDeathScreenS2CPacket;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.storage.LevelResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("collectAllKnownPlayers runs offline disk scan via Util.ioPool and respects cache invalidation")
    void collectAllKnownPlayers_scansOffMainThreadAndRespectsInvalidation(@TempDir Path tempDir) throws Exception {
        CreepingDeathItem.invalidateKnownPlayersCache();
        try {
            UUID onlineId = UUID.randomUUID();
            UUID offlineUsercacheId = UUID.randomUUID();
            UUID offlineDatId = UUID.randomUUID();

            Files.writeString(
                    tempDir.resolve("usercache.json"),
                    "[{\"uuid\":\"" + offlineUsercacheId + "\",\"name\":\"OfflineUser\"}]",
                    StandardCharsets.UTF_8
            );
            Path playerDataDir = tempDir.resolve("playerdata");
            Files.createDirectories(playerDataDir);
            Files.writeString(playerDataDir.resolve(offlineDatId + ".dat"), "", StandardCharsets.UTF_8);

            ServerPlayer onlinePlayer = mock(ServerPlayer.class);
            when(onlinePlayer.getUUID()).thenReturn(onlineId);
            when(onlinePlayer.getGameProfile()).thenReturn(new GameProfile(onlineId, "OnlineUser"));

            PlayerList playerList = mock(PlayerList.class);
            when(playerList.getPlayers()).thenReturn(List.of(onlinePlayer));

            GameProfileCache profileCache = mock(GameProfileCache.class);
            when(profileCache.get(offlineDatId)).thenReturn(Optional.of(new GameProfile(offlineDatId, "DatUser")));

            MinecraftServer server = mock(MinecraftServer.class);
            when(server.getPlayerList()).thenReturn(playerList);
            when(server.getServerDirectory()).thenReturn(tempDir.toFile());
            when(server.getWorldPath(LevelResource.PLAYER_DATA_DIR)).thenReturn(playerDataDir);
            when(server.getProfileCache()).thenReturn(profileCache);
            when(server.isRunning()).thenReturn(true);

            ExecutorService directIoPool = mock(ExecutorService.class);
            doAnswer(inv -> {
                ((Runnable) inv.getArgument(0)).run();
                return null;
            }).when(directIoPool).execute(any(Runnable.class));

            try (MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
                mockedUtil.when(Util::ioPool).thenReturn(directIoPool);

                List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> first = CreepingDeathItem.collectAllKnownPlayers(server);
                assertThat(first).extracting(OpenCreepingDeathScreenS2CPacket.PlayerEntryData::name)
                        .containsExactlyInAnyOrder("OnlineUser", "OfflineUser", "DatUser");

                CreepingDeathItem.invalidateKnownPlayersCache();
                when(server.isRunning()).thenReturn(false);

                List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> afterStop = CreepingDeathItem.collectAllKnownPlayers(server);
                assertThat(afterStop).extracting(OpenCreepingDeathScreenS2CPacket.PlayerEntryData::name)
                        .containsExactly("OnlineUser");
            }
        } finally {
            CreepingDeathItem.invalidateKnownPlayersCache();
        }
    }
}
