package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.block.AncientTableBlock;
import dev.ClasherHD.bodycam.block.HologramBlock;
import dev.ClasherHD.bodycam.block.PodestBlock;
import dev.ClasherHD.bodycam.block.WatchPointBlock;
import dev.ClasherHD.bodycam.bodycam;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(bodycam.MODID, Registries.BLOCK);

    public static final SoundType OBSERVATION_CRYSTAL_SOUNDS = new SoundType(
            1.0F,
            1.8F,
            SoundEvents.AMETHYST_BLOCK_BREAK,
            SoundEvents.AMETHYST_BLOCK_STEP,
            SoundEvents.AMETHYST_BLOCK_PLACE,
            SoundEvents.AMETHYST_BLOCK_HIT,
            SoundEvents.AMETHYST_BLOCK_FALL
    );

    public static final RegistrySupplier<Block> HOLOGRAM_BLOCK = BLOCKS.register("hologram_block",
            () -> new HologramBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .noOcclusion()
                    .strength(0.3F)
                    .sound(SoundType.GLASS)));

    public static final RegistrySupplier<Block> OBSERVATION_CRYSTAL_BLOCK = BLOCKS.register("observation_crystal_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)
                    .sound(OBSERVATION_CRYSTAL_SOUNDS)
                    .lightLevel(state -> 5)));

    public static final RegistrySupplier<Block> ANCIENT_TABLE = BLOCKS.register("ancient_table",
            () -> new AncientTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .lightLevel(ModBlocks::getAncientTableLightLevel)));

    public static final RegistrySupplier<Block> WATCH_POINT = BLOCKS.register("watch_point",
            () -> new WatchPointBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .noOcclusion()
                    .strength(1.5F)
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 5)));

    public static final RegistrySupplier<Block> PODEST = BLOCKS.register("podest",
            () -> new PodestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LECTERN)
                    .noOcclusion()
                    .strength(2.5F)
                    .sound(SoundType.STONE)
                    .lightLevel(ModBlocks::getPodestLightLevel)));

    private static int getAncientTableLightLevel(BlockState state) {
        for (var entry : state.getValues().entrySet()) {
            if ("fuel_level".equals(entry.getKey().getName()) && entry.getValue() instanceof Integer level) {
                return level * 2;
            }
        }
        return 0;
    }

    private static int getPodestLightLevel(BlockState state) {
        boolean powered = false;
        int charges = 0;
        for (var entry : state.getValues().entrySet()) {
            if ("powered".equals(entry.getKey().getName()) && entry.getValue() instanceof Boolean b) {
                powered = b;
            } else if ("charges".equals(entry.getKey().getName()) && entry.getValue() instanceof Integer c) {
                charges = c;
            }
        }
        return powered && charges > 0 ? 7 : 0;
    }

    public static void register() {
        BLOCKS.register();
    }
}
