package dev.ClasherHD.bodycam.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class BodycamPlatform {
    private BodycamPlatform() {
    }

    @ExpectPlatform
    public static Path getConfigDirectory() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void forceChunk(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void forceBlockChunk(ServerLevel level, BlockPos ownerPos, int chunkX, int chunkZ, boolean add) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<ItemStack> getEquippedAccessoryItems(ServerPlayer player, Item item) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void openExtendedMenu(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void stopRidingAndSetCamera(ServerPlayer observer, Entity target) {
        throw new AssertionError();
    }
}
