package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.block.HologramBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, bodycam.MODID);

    public static final RegistryObject<Block> HOLOGRAM_BLOCK = BLOCKS.register("hologram_block",
            () -> new HologramBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)
                    .noOcclusion().strength(0.3F).sound(SoundType.GLASS)));
}
