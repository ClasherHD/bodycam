package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.item.CreepingDeathItem;
import dev.ClasherHD.bodycam.item.DimensionLocatorItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.LogBookItem;
import dev.ClasherHD.bodycam.item.LogPaperItem;
import dev.ClasherHD.bodycam.item.LoggerItem;
import dev.ClasherHD.bodycam.item.PlayerLocatorCompassItem;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, bodycam.MODID);

    public static final RegistryObject<Item> BODYCAM_MONITOR = ITEMS.register("bodycam_monitor",
            () -> new BodycamMonitorItem(new Item.Properties().durability(ItemDurabilityHelper.SPYGLASS_DURABILITY)));
    public static final RegistryObject<Item> OBSERVATION_CRYSTAL = ITEMS.register("observation_crystal",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> JAMMER = ITEMS.register("jammer",
            () -> new JammerItem(new Item.Properties().durability(ItemDurabilityHelper.JAMMER_DURABILITY)));
    public static final RegistryObject<Item> DIMENSION_LOCATOR = ITEMS.register("dimension_locator",
            () -> new DimensionLocatorItem(new Item.Properties().durability(ItemDurabilityHelper.DIMENSION_LOCATOR_DURABILITY)));
    public static final RegistryObject<Item> ANONYMIZER = ITEMS.register("anonymizer",
            () -> new AnonymizerItem(new Item.Properties().durability(ItemDurabilityHelper.ANONYMIZER_DURABILITY)));
    public static final RegistryObject<Item> PLAYER_LOCATOR_COMPASS = ITEMS.register("player_locator_compass",
            () -> new PlayerLocatorCompassItem(new Item.Properties().durability(ItemDurabilityHelper.PLAYER_LOCATOR_DURABILITY)));
    public static final RegistryObject<Item> PORTABLE_NETHER_PORTAL = ITEMS.register("portable_nether_portal",
            () -> new PortableNetherPortalItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LOGGER = ITEMS.register("logger",
            () -> new LoggerItem(new Item.Properties().durability(ItemDurabilityHelper.LOGGER_DURABILITY)));
    public static final RegistryObject<Item> LOG_PAPER = ITEMS.register("log_paper",
            () -> new LogPaperItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> LOG_BOOK = ITEMS.register("log_book",
            () -> new LogBookItem(new Item.Properties()));
    public static final RegistryObject<Item> ALL_SEEING_EYE = ITEMS.register("all_seeing_eye",
            () -> new AllSeeingEyeItem(new Item.Properties()));
    public static final RegistryObject<Item> CREEPING_DEATH = ITEMS.register("creeping_death",
            () -> new CreepingDeathItem(new Item.Properties()));
    public static final RegistryObject<Item> HOLOGRAM_BLOCK_ITEM = ITEMS.register("hologram_block",
            () -> new BlockItem(ModBlocks.HOLOGRAM_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> OBSERVATION_CRYSTAL_BLOCK_ITEM = ITEMS.register("observation_crystal_block",
            () -> new BlockItem(ModBlocks.OBSERVATION_CRYSTAL_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANCIENT_TABLE_ITEM = ITEMS.register("ancient_table",
            () -> new BlockItem(ModBlocks.ANCIENT_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WATCH_POINT_ITEM = ITEMS.register("watch_point",
            () -> new BlockItem(ModBlocks.WATCH_POINT.get(), new Item.Properties()));
    public static final RegistryObject<Item> PODEST_ITEM = ITEMS.register("podest",
            () -> new BlockItem(ModBlocks.PODEST.get(), new Item.Properties()));
    public static final RegistryObject<Item> SCHOLAR_SPAWN_EGG = ITEMS.register("scholar_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.SCHOLAR, 0x3B2B28, 0xE6A8C2, new Item.Properties()));
}
