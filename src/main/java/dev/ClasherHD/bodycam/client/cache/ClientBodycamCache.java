package dev.ClasherHD.bodycam.client.cache;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClientBodycamCache {
    public static final Map<UUID, Integer> jammers = new HashMap<>();
    public static final Map<UUID, UUID> targets = new HashMap<>();
    public static final Map<UUID, String> dimensions = new HashMap<>();
    public static final Map<UUID, BlockPos> positions = new HashMap<>();
    public static final Map<UUID, Boolean> anonymizers = new HashMap<>();
    public static final Set<UUID> hidden = new HashSet<>();
    public static final Map<UUID, String> watchPoints = new LinkedHashMap<>();

    public static void update(Map<UUID, Integer> jammersIn, Map<UUID, UUID> targetsIn, Map<UUID, String> dimensionsIn,
            Map<UUID, BlockPos> positionsIn, Map<UUID, Boolean> anonymizersIn, Set<UUID> hiddenIn, Map<UUID, String> watchPointsIn) {
        jammers.clear();
        targets.clear();
        dimensions.clear();
        positions.clear();
        anonymizers.clear();
        hidden.clear();
        watchPoints.clear();
        jammers.putAll(jammersIn);
        targets.putAll(targetsIn);
        dimensions.putAll(dimensionsIn);
        positions.putAll(positionsIn);
        anonymizers.putAll(anonymizersIn);
        hidden.addAll(hiddenIn);
        watchPoints.putAll(watchPointsIn);
    }
}
