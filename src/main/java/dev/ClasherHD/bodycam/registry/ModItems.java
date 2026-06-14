package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.DimensionLocatorItem;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, bodycam.MODID);

    public static final RegistryObject<Item> BODYCAM_MONITOR = ITEMS.register("bodycam_monitor",
            () -> new BodycamMonitorItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> OBSERVATION_CRYSTAL = ITEMS.register("observation_crystal",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> JAMMER = ITEMS.register("jammer",
            () -> new JammerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DIMENSION_LOCATOR = ITEMS.register("dimension_locator",
            () -> new DimensionLocatorItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ANONYMIZER = ITEMS.register("anonymizer",
            () -> new AnonymizerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HOLOGRAM_BLOCK_ITEM = ITEMS.register("hologram_block",
            () -> new BlockItem(ModBlocks.HOLOGRAM_BLOCK.get(), new Item.Properties()));
}
