package dev.ClasherHD.bodycam.enchantment;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("null")
public final class NightspyEffectHandler {

    private static final String LEVEL_KEY = "bodycam_nightspy_level";
    private static final String APPLIED_KEY = "bodycam_nightspy_applied";
    private static final String GRACE_KEY = "bodycam_nightspy_grace";
    private static final int GRACE_TICKS = 100;
    private static final int EFFECT_DURATION = 260;
    private static final int REFRESH_BELOW = 220;
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;

    private NightspyEffectHandler() {
    }

    public static void begin(ServerPlayer observer, int level) {
        if (level <= 0) {
            clear(observer);
            return;
        }
        observer.getPersistentData().putInt(LEVEL_KEY, level);
    }

    public static void tick(ServerPlayer observer, Entity target, Vec3 bodyPos, ResourceKey<Level> bodyDimension) {
        CompoundTag data = observer.getPersistentData();
        int level = data.getInt(LEVEL_KEY);
        if (level <= 0) {
            return;
        }
        if (isActive(level, target, bodyPos, bodyDimension)) {
            data.putInt(GRACE_KEY, GRACE_TICKS);
            refresh(observer, data);
            return;
        }
        if (!data.getBoolean(APPLIED_KEY)) {
            return;
        }
        int grace = data.getInt(GRACE_KEY) - 1;
        if (grace <= 0) {
            removeEffect(observer, data);
            return;
        }
        data.putInt(GRACE_KEY, grace);
        refresh(observer, data);
    }

    public static void clear(ServerPlayer observer) {
        CompoundTag data = observer.getPersistentData();
        removeEffect(observer, data);
        data.remove(LEVEL_KEY);
    }

    private static boolean isActive(int level, Entity target, Vec3 bodyPos, ResourceKey<Level> bodyDimension) {
        if (level >= 4) {
            return true;
        }
        if (!(target.level() instanceof ServerLevel targetLevel)) {
            return false;
        }
        if (bodyDimension != targetLevel.dimension()) {
            return false;
        }
        if (level == 1 && bodyPos != null) {
            double maxDistance = (double) ModServerConfig.MAX_MONITOR_DISTANCE.get();
            if (bodyPos.distanceToSqr(target.position()) > maxDistance * maxDistance) {
                return false;
            }
        }
        return level >= 3 || isNight(targetLevel);
    }

    private static boolean isNight(ServerLevel level) {
        if (level.dimensionType().hasFixedTime() || !level.dimensionType().hasSkyLight()) {
            return true;
        }
        long time = level.getDayTime() % 24000L;
        return time >= NIGHT_START && time < NIGHT_END;
    }

    private static void refresh(ServerPlayer observer, CompoundTag data) {
        MobEffectInstance current = observer.getEffect(MobEffects.NIGHT_VISION);
        if (current != null && !data.getBoolean(APPLIED_KEY)) {
            return;
        }
        if (current == null || current.getDuration() < REFRESH_BELOW) {
            observer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, false, false, false));
        }
        data.putBoolean(APPLIED_KEY, true);
    }

    private static void removeEffect(ServerPlayer observer, CompoundTag data) {
        if (data.getBoolean(APPLIED_KEY)) {
            observer.removeEffect(MobEffects.NIGHT_VISION);
        }
        data.remove(APPLIED_KEY);
        data.remove(GRACE_KEY);
    }
}
