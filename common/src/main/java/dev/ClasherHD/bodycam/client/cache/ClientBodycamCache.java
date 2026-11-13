package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientBodycamCache {
    public static final Map<UUID, Integer> jammers = new ConcurrentHashMap<>();
    public static final Map<UUID, UUID> targets = new ConcurrentHashMap<>();
    public static final Map<UUID, String> dimensions = new ConcurrentHashMap<>();
    public static final Map<UUID, BlockPos> positions = new ConcurrentHashMap<>();
    public static final Map<UUID, Boolean> anonymizers = new ConcurrentHashMap<>();
    public static final Set<UUID> hidden = ConcurrentHashMap.newKeySet();
    public static final Map<UUID, String> watchPoints = Collections.synchronizedMap(new LinkedHashMap<>());

    public static volatile int REACH_LEVEL = 0;
    public static volatile boolean IS_ON_HOLOGRAM = false;
    public static volatile boolean IS_ALL_SEEING_EYE = false;

    private ClientBodycamCache() {
    }

    public static void update(
            Map<UUID, Integer> jammersIn,
            Map<UUID, UUID> targetsIn,
            Map<UUID, String> dimensionsIn,
            Map<UUID, BlockPos> positionsIn,
            Map<UUID, Boolean> anonymizersIn,
            Set<UUID> hiddenIn,
            Map<UUID, String> watchPointsIn
    ) {
        jammers.clear();
        targets.clear();
        dimensions.clear();
        positions.clear();
        anonymizers.clear();
        hidden.clear();
        watchPoints.clear();
        if (jammersIn != null) jammers.putAll(jammersIn);
        if (targetsIn != null) targets.putAll(targetsIn);
        if (dimensionsIn != null) dimensions.putAll(dimensionsIn);
        if (positionsIn != null) positions.putAll(positionsIn);
        if (anonymizersIn != null) anonymizers.putAll(anonymizersIn);
        if (hiddenIn != null) hidden.addAll(hiddenIn);
        if (watchPointsIn != null) watchPoints.putAll(watchPointsIn);
    }

    public static void clear() {
        jammers.clear();
        targets.clear();
        dimensions.clear();
        positions.clear();
        anonymizers.clear();
        hidden.clear();
        watchPoints.clear();
        REACH_LEVEL = 0;
        IS_ON_HOLOGRAM = false;
        IS_ALL_SEEING_EYE = false;
    }
}
