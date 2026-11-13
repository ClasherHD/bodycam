package dev.ClasherHD.bodycam.platform.fabric;

import dev.ClasherHD.bodycam.platform.PlatformCommonHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BodycamPlatformImpl {
    private static volatile boolean trinketsChecked = false;
    private static volatile Method getTrinketComponentMethod = null;
    private static volatile Method getAllEquippedMethod = null;

    private BodycamPlatformImpl() {
    }

    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static void forceChunk(ServerLevel level, UUID ownerUuid, int chunkX, int chunkZ, boolean add) {
        PlatformCommonHelper.forceEntityChunk(level, ownerUuid, chunkX, chunkZ, add);
    }

    public static void forceBlockChunk(ServerLevel level, BlockPos ownerPos, int chunkX, int chunkZ, boolean add) {
        PlatformCommonHelper.forceBlockChunk(level, ownerPos, chunkX, chunkZ, add);
    }

    public static List<ItemStack> getEquippedAccessoryItems(ServerPlayer player, Item item) {
        if (player == null || item == null || !FabricLoader.getInstance().isModLoaded("trinkets")) {
            return Collections.emptyList();
        }
        if (!trinketsChecked) {
            synchronized (BodycamPlatformImpl.class) {
                if (!trinketsChecked) {
                    try {
                        Class<?> apiClass = Class.forName("dev.emi.trinkets.api.TrinketsApi");
                        Class<?> componentClass = Class.forName("dev.emi.trinkets.api.TrinketComponent");
                        getTrinketComponentMethod = apiClass.getMethod("getTrinketComponent", LivingEntity.class);
                        getAllEquippedMethod = componentClass.getMethod("getAllEquipped");
                    } catch (Throwable ignored) {
                        getTrinketComponentMethod = null;
                        getAllEquippedMethod = null;
                    }
                    trinketsChecked = true;
                }
            }
        }
        if (getTrinketComponentMethod == null || getAllEquippedMethod == null) {
            return Collections.emptyList();
        }
        try {
            Object compObj = getTrinketComponentMethod.invoke(null, player);
            if (!(compObj instanceof Optional<?> opt) || opt.isEmpty()) {
                return Collections.emptyList();
            }
            Object equippedObj = getAllEquippedMethod.invoke(opt.get());
            if (!(equippedObj instanceof List<?> list) || list.isEmpty()) {
                return Collections.emptyList();
            }
            List<ItemStack> result = null;
            for (Object entry : list) {
                if (entry instanceof Tuple<?, ?> tuple && tuple.getB() instanceof ItemStack stack && !stack.isEmpty() && stack.is(item)) {
                    if (result == null) {
                        result = new ArrayList<>(2);
                    }
                    result.add(stack);
                }
            }
            return result != null ? result : Collections.emptyList();
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    public static void openExtendedMenu(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        PlatformCommonHelper.openExtendedMenu(player, provider, pos);
    }

    public static void stopRidingAndSetCamera(ServerPlayer observer, Entity target) {
        PlatformCommonHelper.stopRidingAndSetCamera(observer, target);
    }
}
