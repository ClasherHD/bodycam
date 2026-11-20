package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.block.entity.AncientTableBlockEntity;
import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

@SuppressWarnings("null")
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(bodycam.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<AncientTableBlockEntity>> ANCIENT_TABLE =
            BLOCK_ENTITIES.register("ancient_table",
                    () -> new BlockEntityType<>(AncientTableBlockEntity::new, Set.of(ModBlocks.ANCIENT_TABLE.get())));

    public static final RegistrySupplier<BlockEntityType<WatchPointBlockEntity>> WATCH_POINT =
            BLOCK_ENTITIES.register("watch_point",
                    () -> new BlockEntityType<>(WatchPointBlockEntity::new, Set.of(ModBlocks.WATCH_POINT.get())));

    public static final RegistrySupplier<BlockEntityType<PodestBlockEntity>> PODEST =
            BLOCK_ENTITIES.register("podest",
                    () -> new BlockEntityType<>(PodestBlockEntity::new, Set.of(ModBlocks.PODEST.get())));

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
