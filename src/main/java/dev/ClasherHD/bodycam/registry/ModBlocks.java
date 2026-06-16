package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.block.HologramBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.util.ForgeSoundType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModBlocks {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
                        bodycam.MODID);

        public static final SoundType OBSERVATION_CRYSTAL_SOUNDS = new ForgeSoundType(
                        1.0F, 1.8F,
                        () -> net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK,
                        () -> net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_STEP,
                        () -> net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_PLACE,
                        () -> net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_HIT,
                        () -> net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_FALL);

        public static final RegistryObject<Block> HOLOGRAM_BLOCK = BLOCKS.register("hologram_block",
                        () -> new HologramBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)
                                        .noOcclusion().strength(0.3F).sound(SoundType.GLASS)));

        public static final RegistryObject<Block> OBSERVATION_CRYSTAL_BLOCK = BLOCKS.register(
                        "observation_crystal_block",
                        () -> new Block(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                                        .sound(OBSERVATION_CRYSTAL_SOUNDS)
                                        .lightLevel(state -> 5)));
}
