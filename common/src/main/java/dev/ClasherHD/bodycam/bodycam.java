package dev.ClasherHD.bodycam;

import com.mojang.logging.LogUtils;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.event.ModEvents;
import dev.ClasherHD.bodycam.event.ServerEvents;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModCreativeTabs;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModMenus;
import dev.ClasherHD.bodycam.registry.ModRecipes;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.registry.ModStructures;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

@SuppressWarnings("null")
public class bodycam {
    public static final String MODID = "bodycam";
    public static final String MOD_ID = MODID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static void init() {
        ModServerConfig.load();
        ModClientConfig.load();
        ModSounds.register();
        ModBlocks.register();
        ModEntityTypes.register();
        ModBlockEntities.register();
        ModItems.register();
        ModEnchantments.register();
        ModMenus.register();
        ModRecipes.register();
        ModStructures.register();
        ModCreativeTabs.register();
        PacketHandler.init();
        ModEvents.init();
        ServerEvents.init();
    }
}
