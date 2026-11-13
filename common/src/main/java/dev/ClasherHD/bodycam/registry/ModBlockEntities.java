package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.block.entity.AncientTableBlockEntity;
import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(bodycam.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<AncientTableBlockEntity>> ANCIENT_TABLE =
            BLOCK_ENTITIES.register("ancient_table",
                    () -> BlockEntityType.Builder.of(AncientTableBlockEntity::new, ModBlocks.ANCIENT_TABLE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<WatchPointBlockEntity>> WATCH_POINT =
            BLOCK_ENTITIES.register("watch_point",
                    () -> BlockEntityType.Builder.of(WatchPointBlockEntity::new, ModBlocks.WATCH_POINT.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<PodestBlockEntity>> PODEST =
            BLOCK_ENTITIES.register("podest",
                    () -> BlockEntityType.Builder.of(PodestBlockEntity::new, ModBlocks.PODEST.get()).build(null));

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
