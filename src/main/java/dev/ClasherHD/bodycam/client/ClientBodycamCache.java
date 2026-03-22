package dev.ClasherHD.bodycam.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public class ClientBodycamCache {
    public static final Map<UUID, Integer> jammers = new HashMap<>();
    public static final Map<UUID, UUID> targets = new HashMap<>();
    public static final Map<UUID, String> dimensions = new HashMap<>();
    public static final Map<UUID, BlockPos> positions = new HashMap<>();
    public static final Map<UUID, Boolean> anonymizers = new HashMap<>();

    public static void update(Map<UUID, Integer> jammersIn, Map<UUID, UUID> targetsIn, Map<UUID, String> dimensionsIn, Map<UUID, BlockPos> positionsIn, Map<UUID, Boolean> anonymizersIn) {
        jammers.clear();
        targets.clear();
        dimensions.clear();
        positions.clear();
        anonymizers.clear();
        jammers.putAll(jammersIn);
        targets.putAll(targetsIn);
        dimensions.putAll(dimensionsIn);
        positions.putAll(positionsIn);
        anonymizers.putAll(anonymizersIn);
    }
}
