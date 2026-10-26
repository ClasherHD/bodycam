package dev.ClasherHD.bodycam.enchantment;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NightspyEffectHandlerTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        com.electronwill.nightconfig.core.CommentedConfig serverConfig = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        ModServerConfig.SPEC.acceptConfig(serverConfig);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    @DisplayName("begin should record nightspy level in persistent data")
    void begin_withPositiveLevel_storesLevel(int level) {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        when(player.getPersistentData()).thenReturn(tag);

        NightspyEffectHandler.begin(player, level);

        assertThat(tag.getInt("bodycam_nightspy_level")).isEqualTo(level);
    }

    @Test
    @DisplayName("begin with zero or negative level should invoke clear and remove level tag")
    void begin_withZeroOrNegative_clearsLevel() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        when(player.getPersistentData()).thenReturn(tag);

        tag.putInt("bodycam_nightspy_level", 2);
        NightspyEffectHandler.begin(player, 0);
        assertThat(tag.contains("bodycam_nightspy_level")).isFalse();

        tag.putInt("bodycam_nightspy_level", 3);
        NightspyEffectHandler.begin(player, -1);
        assertThat(tag.contains("bodycam_nightspy_level")).isFalse();
    }

    @Test
    @DisplayName("clear should remove nightspy level and reset tags")
    void clear_removesNightspyTags() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 4);
        tag.putInt("bodycam_nightspy_grace", 50);
        tag.putBoolean("bodycam_nightspy_applied", true);
        when(player.getPersistentData()).thenReturn(tag);

        NightspyEffectHandler.clear(player);

        assertThat(tag.contains("bodycam_nightspy_level")).isFalse();
        assertThat(tag.contains("bodycam_nightspy_grace")).isFalse();
        assertThat(tag.contains("bodycam_nightspy_applied")).isFalse();
        verify(player).removeEffect(MobEffects.NIGHT_VISION);
    }

    @Test
    @DisplayName("clear without applied effect should not call removeEffect on player")
    void clear_withoutAppliedEffect_doesNotRemoveFromPlayer() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 2);
        when(player.getPersistentData()).thenReturn(tag);

        NightspyEffectHandler.clear(player);

        assertThat(tag.contains("bodycam_nightspy_level")).isFalse();
        verify(player, never()).removeEffect(any());
    }

    @Test
    @DisplayName("tick with level 0 should return immediately without doing anything")
    void tick_withZeroLevel_doesNothing() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        when(player.getPersistentData()).thenReturn(tag);

        NightspyEffectHandler.tick(player, null, null, null);

        verify(player, never()).addEffect(any());
    }

    @Test
    @DisplayName("tick with level 4 should always activate effect regardless of context")
    void tick_withLevelFour_activatesEffect() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 4);
        when(player.getPersistentData()).thenReturn(tag);

        NightspyEffectHandler.tick(player, null, null, null);

        assertThat(tag.getInt("bodycam_nightspy_grace")).isEqualTo(100);
        assertThat(tag.getBoolean("bodycam_nightspy_applied")).isTrue();
        verify(player).addEffect(any(MobEffectInstance.class));
    }

    @Test
    @DisplayName("tick with level 1 should check distance, dimension, and nighttime")
    void tick_withLevelOne_evaluatesConditions() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 1);
        when(player.getPersistentData()).thenReturn(tag);

        Entity target = mock(Entity.class);
        ServerLevel level = mock(ServerLevel.class);
        DimensionType dimType = mock(DimensionType.class);
        when(target.level()).thenReturn(level);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.dimensionType()).thenReturn(dimType);
        when(dimType.hasFixedTime()).thenReturn(false);
        when(dimType.hasSkyLight()).thenReturn(true);
        when(level.getDayTime()).thenReturn(15000L);

        Vec3 bodyPos = new Vec3(0, 64, 0);
        when(target.position()).thenReturn(new Vec3(10, 64, 10));

        NightspyEffectHandler.tick(player, target, bodyPos, Level.OVERWORLD);

        assertThat(tag.getInt("bodycam_nightspy_grace")).isEqualTo(100);
        assertThat(tag.getBoolean("bodycam_nightspy_applied")).isTrue();
        verify(player).addEffect(any(MobEffectInstance.class));
    }

    @Test
    @DisplayName("tick with level 1 outside max distance should not activate")
    void tick_withLevelOne_outsideDistance_doesNotActivate() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 1);
        when(player.getPersistentData()).thenReturn(tag);

        Entity target = mock(Entity.class);
        ServerLevel level = mock(ServerLevel.class);
        when(target.level()).thenReturn(level);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(target.position()).thenReturn(new Vec3(100000, 64, 100000));

        Vec3 bodyPos = new Vec3(0, 64, 0);

        NightspyEffectHandler.tick(player, target, bodyPos, Level.OVERWORLD);

        assertThat(tag.getBoolean("bodycam_nightspy_applied")).isFalse();
        verify(player, never()).addEffect(any());
    }

    @Test
    @DisplayName("tick with different dimension should not activate")
    void tick_withDifferentDimension_doesNotActivate() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 2);
        when(player.getPersistentData()).thenReturn(tag);

        Entity target = mock(Entity.class);
        ServerLevel level = mock(ServerLevel.class);
        when(target.level()).thenReturn(level);
        when(level.dimension()).thenReturn(Level.NETHER);

        NightspyEffectHandler.tick(player, target, Vec3.ZERO, Level.OVERWORLD);

        assertThat(tag.getBoolean("bodycam_nightspy_applied")).isFalse();
        verify(player, never()).addEffect(any());
    }

    @Test
    @DisplayName("tick with daytime on level 2 should not activate")
    void tick_withLevelTwoDaytime_doesNotActivate() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 2);
        when(player.getPersistentData()).thenReturn(tag);

        Entity target = mock(Entity.class);
        ServerLevel level = mock(ServerLevel.class);
        DimensionType dimType = mock(DimensionType.class);
        when(target.level()).thenReturn(level);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.dimensionType()).thenReturn(dimType);
        when(dimType.hasFixedTime()).thenReturn(false);
        when(dimType.hasSkyLight()).thenReturn(true);
        when(level.getDayTime()).thenReturn(6000L);

        NightspyEffectHandler.tick(player, target, Vec3.ZERO, Level.OVERWORLD);

        assertThat(tag.getBoolean("bodycam_nightspy_applied")).isFalse();
        verify(player, never()).addEffect(any());
    }

    @Test
    @DisplayName("tick with level 3 should activate even during daytime")
    void tick_withLevelThree_activatesInDaytime() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 3);
        when(player.getPersistentData()).thenReturn(tag);

        Entity target = mock(Entity.class);
        ServerLevel level = mock(ServerLevel.class);
        DimensionType dimType = mock(DimensionType.class);
        when(target.level()).thenReturn(level);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.dimensionType()).thenReturn(dimType);
        when(dimType.hasFixedTime()).thenReturn(false);
        when(dimType.hasSkyLight()).thenReturn(true);
        when(level.getDayTime()).thenReturn(6000L);

        NightspyEffectHandler.tick(player, target, Vec3.ZERO, Level.OVERWORLD);

        assertThat(tag.getBoolean("bodycam_nightspy_applied")).isTrue();
        verify(player).addEffect(any());
    }

    @Test
    @DisplayName("tick should handle grace period decrement and eventual removal")
    void tick_gracePeriodCountdown_removesEffectWhenZero() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 1);
        tag.putBoolean("bodycam_nightspy_applied", true);
        tag.putInt("bodycam_nightspy_grace", 2);
        when(player.getPersistentData()).thenReturn(tag);

        Entity target = mock(Entity.class);
        when(target.level()).thenReturn(mock(Level.class));

        NightspyEffectHandler.tick(player, target, Vec3.ZERO, Level.OVERWORLD);
        assertThat(tag.getInt("bodycam_nightspy_grace")).isEqualTo(1);
        assertThat(tag.getBoolean("bodycam_nightspy_applied")).isTrue();

        NightspyEffectHandler.tick(player, target, Vec3.ZERO, Level.OVERWORLD);
        assertThat(tag.contains("bodycam_nightspy_grace")).isFalse();
        assertThat(tag.contains("bodycam_nightspy_applied")).isFalse();
        verify(player).removeEffect(MobEffects.NIGHT_VISION);
    }

    @Test
    @DisplayName("refresh should not override external night vision effect")
    void refresh_doesNotOverrideExternalNightVision() {
        ServerPlayer player = mock(ServerPlayer.class);
        CompoundTag tag = new CompoundTag();
        tag.putInt("bodycam_nightspy_level", 4);
        when(player.getPersistentData()).thenReturn(tag);
        MobEffectInstance externalEffect = new MobEffectInstance(MobEffects.NIGHT_VISION, 1000);
        when(player.getEffect(MobEffects.NIGHT_VISION)).thenReturn(externalEffect);

        NightspyEffectHandler.tick(player, null, null, null);

        verify(player, never()).addEffect(any());
        assertThat(tag.getBoolean("bodycam_nightspy_applied")).isFalse();
    }
}
