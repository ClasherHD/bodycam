package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.block.entity.AncientTableBlockEntity;
import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, bodycam.MODID);

    public static final RegistryObject<BlockEntityType<AncientTableBlockEntity>> ANCIENT_TABLE = BLOCK_ENTITIES.register(
            "ancient_table",
            () -> BlockEntityType.Builder.of(AncientTableBlockEntity::new, ModBlocks.ANCIENT_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<WatchPointBlockEntity>> WATCH_POINT = BLOCK_ENTITIES.register(
            "watch_point",
            () -> BlockEntityType.Builder.of(WatchPointBlockEntity::new, ModBlocks.WATCH_POINT.get()).build(null));

    public static final RegistryObject<BlockEntityType<PodestBlockEntity>> PODEST = BLOCK_ENTITIES.register(
            "podest",
            () -> BlockEntityType.Builder.of(PodestBlockEntity::new, ModBlocks.PODEST.get()).build(null));
}
