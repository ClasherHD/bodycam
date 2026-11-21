package dev.ClasherHD.bodycam.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class CreepingDeathRendererTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @BeforeEach
    @AfterEach
    void clearCache() {
        CreepingDeathRenderer.clearProfileCache();
    }

    @Test
    @DisplayName("getSkinForPlayer caches fallback profile on HTTP exception and clears cleanly")
    void getSkinForPlayer_cachesFallbackOnErrorAndCapsSize() {
        UUID uuid = UUID.randomUUID();
        ResourceLocation fallbackSkin = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

        Minecraft mc = mock(Minecraft.class);
        SkinManager skinManager = mock(SkinManager.class);
        MinecraftSessionService sessionService = mock(MinecraftSessionService.class);
        when(mc.getConnection()).thenReturn(null);
        when(mc.getSkinManager()).thenReturn(skinManager);
        when(mc.getMinecraftSessionService()).thenReturn(sessionService);
        when(skinManager.getInsecureSkinLocation(any(GameProfile.class))).thenReturn(fallbackSkin);
        when(sessionService.fillProfileProperties(any(GameProfile.class), anyBoolean()))
                .thenThrow(new RuntimeException("HTTP 429"));

        ExecutorService directBg = mock(ExecutorService.class);
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(directBg).execute(any(Runnable.class));

        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class);
             MockedStatic<Util> mockedUtil = mockStatic(Util.class)) {
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);
            mockedUtil.when(Util::backgroundExecutor).thenReturn(directBg);

            CreepingDeathRenderer.getSkinForPlayer(uuid, "OfflinePlayer");
            ResourceLocation second = CreepingDeathRenderer.getSkinForPlayer(uuid, "OfflinePlayer");

            assertThat(second).isEqualTo(fallbackSkin);
            verify(sessionService, times(1)).fillProfileProperties(any(GameProfile.class), anyBoolean());

            Map<UUID, GameProfile> resolved = MinecraftTestEnvironment.getField(CreepingDeathRenderer.class, "RESOLVED_PROFILES");
            assertThat(resolved).containsKey(uuid);

            for (int i = 0; i < 260; i++) {
                resolved.put(UUID.randomUUID(), new GameProfile(UUID.randomUUID(), "P" + i));
            }
            CreepingDeathRenderer.getSkinForPlayer(UUID.randomUUID(), "TriggerCap");
            assertThat(resolved.size()).isLessThanOrEqualTo(1);

            CreepingDeathRenderer.clearProfileCache();
            assertThat(resolved).isEmpty();
        }
    }
}
