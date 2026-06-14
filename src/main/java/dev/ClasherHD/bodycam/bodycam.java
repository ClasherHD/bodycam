package dev.ClasherHD.bodycam;

import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModCreativeTabs;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(bodycam.MODID)
public class bodycam {
    public static final String MODID = "bodycam";

    public bodycam() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                dev.ClasherHD.bodycam.config.ModClientConfig.SPEC, "bodycam-client.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                dev.ClasherHD.bodycam.config.ModServerConfig.SPEC, "bodycam-server.toml");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
    }
}
