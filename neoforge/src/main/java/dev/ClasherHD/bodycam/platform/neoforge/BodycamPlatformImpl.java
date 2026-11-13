package dev.ClasherHD.bodycam.platform.neoforge;

import dev.ClasherHD.bodycam.platform.PlatformCommonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BodycamPlatformImpl {
    private static volatile boolean curiosChecked = false;
    private static volatile Method getCuriosInventoryMethod = null;
    private static volatile Method findCuriosMethod = null;
    private static volatile Method slotResultStackMethod = null;

    private BodycamPlatformImpl() {
    }

    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static void forceChunk(ServerLevel level, UUID ownerUuid, int chunkX, int chunkZ, boolean add) {
        PlatformCommonHelper.forceEntityChunk(level, ownerUuid, chunkX, chunkZ, add);
    }

    public static void forceBlockChunk(ServerLevel level, BlockPos ownerPos, int chunkX, int chunkZ, boolean add) {
        PlatformCommonHelper.forceBlockChunk(level, ownerPos, chunkX, chunkZ, add);
    }

    public static List<ItemStack> getEquippedAccessoryItems(ServerPlayer player, Item item) {
        if (player == null || item == null || !ModList.get().isLoaded("curios")) {
            return Collections.emptyList();
        }
        if (!curiosChecked) {
            synchronized (BodycamPlatformImpl.class) {
                if (!curiosChecked) {
                    try {
                        Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                        Class<?> handlerClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
                        Class<?> slotResultClass = Class.forName("top.theillusivec4.curios.api.SlotResult");
                        getCuriosInventoryMethod = apiClass.getMethod("getCuriosInventory", LivingEntity.class);
                        findCuriosMethod = handlerClass.getMethod("findCurios", Item.class);
                        slotResultStackMethod = slotResultClass.getMethod("stack");
                    } catch (Throwable ignored) {
                        getCuriosInventoryMethod = null;
                        findCuriosMethod = null;
                        slotResultStackMethod = null;
                    }
                    curiosChecked = true;
                }
            }
        }
        if (getCuriosInventoryMethod == null || findCuriosMethod == null || slotResultStackMethod == null) {
            return Collections.emptyList();
        }
        try {
            Object invObj = getCuriosInventoryMethod.invoke(null, player);
            Object handler = null;
            if (invObj instanceof Optional<?> opt) {
                if (opt.isEmpty()) {
                    return Collections.emptyList();
                }
                handler = opt.get();
            } else if (invObj != null) {
                Method resolve = invObj.getClass().getMethod("resolve");
                Object resolved = resolve.invoke(invObj);
                if (resolved instanceof Optional<?> opt && opt.isPresent()) {
                    handler = opt.get();
                }
            }
            if (handler == null) {
                return Collections.emptyList();
            }
            Object listObj = findCuriosMethod.invoke(handler, item);
            if (!(listObj instanceof List<?> slotResults) || slotResults.isEmpty()) {
                return Collections.emptyList();
            }
            List<ItemStack> result = null;
            for (Object sr : slotResults) {
                if (sr != null) {
                    Object stackObj = slotResultStackMethod.invoke(sr);
                    if (stackObj instanceof ItemStack stack && !stack.isEmpty()) {
                        if (result == null) {
                            result = new ArrayList<>(2);
                        }
                        result.add(stack);
                    }
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
