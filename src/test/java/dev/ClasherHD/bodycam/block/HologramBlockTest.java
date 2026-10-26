package dev.ClasherHD.bodycam.block;

import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HologramBlockTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("HologramBlock should instantiate properly and match registered block")
    void instantiation_matchesRegisteredBlock() {
        HologramBlock registered = (HologramBlock) ModBlocks.HOLOGRAM_BLOCK.get();
        assertThat(registered).isNotNull();

        HologramBlock custom = new HologramBlock(BlockBehaviour.Properties.of());
        assertThat(custom).isNotNull();
        assertThat(custom.defaultBlockState()).isNotNull();
    }
}
