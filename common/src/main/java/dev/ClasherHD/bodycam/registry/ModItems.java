package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.item.CherryHouseExplorerMapItem;
import dev.ClasherHD.bodycam.item.CreepingDeathItem;
import dev.ClasherHD.bodycam.item.DimensionLocatorItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.LogBookItem;
import dev.ClasherHD.bodycam.item.LogPaperItem;
import dev.ClasherHD.bodycam.item.LoggerItem;
import dev.ClasherHD.bodycam.item.PlayerLocatorCompassItem;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import dev.ClasherHD.bodycam.item.WatchPointBlockItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

@SuppressWarnings("null")
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(bodycam.MODID, Registries.ITEM);

    public static final RegistrySupplier<Item> BODYCAM_MONITOR = ITEMS.register("bodycam_monitor",
            () -> new BodycamMonitorItem(itemProperties("bodycam_monitor").durability(ItemDurabilityHelper.SPYGLASS_DURABILITY).enchantable(1)));

    public static final RegistrySupplier<Item> OBSERVATION_CRYSTAL = ITEMS.register("observation_crystal",
            () -> new Item(itemProperties("observation_crystal")
                    .trimMaterial(ResourceKey.create(Registries.TRIM_MATERIAL, bodycam.id("observation_crystal")))));

    public static final RegistrySupplier<Item> JAMMER = ITEMS.register("jammer",
            () -> new JammerItem(itemProperties("jammer").durability(ItemDurabilityHelper.JAMMER_DURABILITY).enchantable(1)));

    public static final RegistrySupplier<Item> DIMENSION_LOCATOR = ITEMS.register("dimension_locator",
            () -> new DimensionLocatorItem(itemProperties("dimension_locator").durability(ItemDurabilityHelper.DIMENSION_LOCATOR_DURABILITY)));

    public static final RegistrySupplier<Item> ANONYMIZER = ITEMS.register("anonymizer",
            () -> new AnonymizerItem(itemProperties("anonymizer").durability(ItemDurabilityHelper.ANONYMIZER_DURABILITY).enchantable(1)));

    public static final RegistrySupplier<Item> PLAYER_LOCATOR_COMPASS = ITEMS.register("player_locator_compass",
            () -> new PlayerLocatorCompassItem(itemProperties("player_locator_compass").durability(ItemDurabilityHelper.PLAYER_LOCATOR_DURABILITY).enchantable(1)));

    public static final RegistrySupplier<Item> PORTABLE_NETHER_PORTAL = ITEMS.register("portable_nether_portal",
            () -> new PortableNetherPortalItem(itemProperties("portable_nether_portal").stacksTo(1)));

    public static final RegistrySupplier<Item> LOGGER = ITEMS.register("logger",
            () -> new LoggerItem(itemProperties("logger").durability(ItemDurabilityHelper.LOGGER_DURABILITY)));

    public static final RegistrySupplier<Item> LOG_PAPER = ITEMS.register("log_paper",
            () -> new LogPaperItem(itemProperties("log_paper").stacksTo(16)));

    public static final RegistrySupplier<Item> LOG_BOOK = ITEMS.register("log_book",
            () -> new LogBookItem(itemProperties("log_book")));

    public static final RegistrySupplier<Item> ALL_SEEING_EYE = ITEMS.register("all_seeing_eye",
            () -> new AllSeeingEyeItem(itemProperties("all_seeing_eye")));

    public static final RegistrySupplier<Item> CREEPING_DEATH = ITEMS.register("creeping_death",
            () -> new CreepingDeathItem(itemProperties("creeping_death")));

    public static final RegistrySupplier<Item> HOLOGRAM_BLOCK = ITEMS.register("hologram_block",
            () -> new BlockItem(ModBlocks.HOLOGRAM_BLOCK.get(), blockItemProperties("hologram_block")));

    public static final RegistrySupplier<Item> OBSERVATION_CRYSTAL_BLOCK = ITEMS.register("observation_crystal_block",
            () -> new BlockItem(ModBlocks.OBSERVATION_CRYSTAL_BLOCK.get(), blockItemProperties("observation_crystal_block")));

    public static final RegistrySupplier<Item> ANCIENT_TABLE = ITEMS.register("ancient_table",
            () -> new BlockItem(ModBlocks.ANCIENT_TABLE.get(), blockItemProperties("ancient_table")));

    public static final RegistrySupplier<Item> WATCH_POINT = ITEMS.register("watch_point",
            () -> new WatchPointBlockItem(ModBlocks.WATCH_POINT.get(), blockItemProperties("watch_point")));

    public static final RegistrySupplier<Item> PODEST = ITEMS.register("podest",
            () -> new BlockItem(ModBlocks.PODEST.get(), blockItemProperties("podest")));

    public static final RegistrySupplier<Item> SCHOLAR_SPAWN_EGG = ITEMS.register("scholar_spawn_egg",
            () -> new SpawnEggItem(itemProperties("scholar_spawn_egg").spawnEgg(ModEntityTypes.SCHOLAR.get())));

    public static final RegistrySupplier<Item> CHERRY_HOUSE_EXPLORER_MAP = ITEMS.register("cherry_house_explorer_map",
            () -> new CherryHouseExplorerMapItem(itemProperties("cherry_house_explorer_map")));

    private static Item.Properties itemProperties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, bodycam.id(name)));
    }

    private static Item.Properties blockItemProperties(String name) {
        return itemProperties(name).useBlockDescriptionPrefix();
    }

    public static void register() {
        ITEMS.register();
    }
}
