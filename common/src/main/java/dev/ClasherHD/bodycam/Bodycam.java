package dev.ClasherHD.bodycam;

import dev.ClasherHD.bodycam.block.HologramBlock;
import dev.ClasherHD.bodycam.entity.CompassDummyEntity;
import dev.ClasherHD.bodycam.entity.HologramDummyEntity;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.item.DimensionLocatorItem;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class Bodycam {
        public static final String MOD_ID = "bodycam";

        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MOD_ID, Registries.BLOCK);
        public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(MOD_ID,
                        Registries.ENTITY_TYPE);
        public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(MOD_ID,
                        Registries.CREATIVE_MODE_TAB);

        public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("bodycam_tab",
                        () -> CreativeTabRegistry.create(Component.translatable("creativetab.bodycam_tab"),
                                        () -> new ItemStack(Bodycam.MONITOR.get())));

        public static final RegistrySupplier<Block> HOLOGRAM_BLOCK = BLOCKS.register("hologram_block",
                        () -> new HologramBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                                        .setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,
                                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID,
                                                                        "hologram_block")))
                                        .noOcclusion().strength(0.3F).sound(SoundType.GLASS)));

        public static final RegistrySupplier<Item> HOLOGRAM_BLOCK_ITEM = ITEMS.register("hologram_block",
                        () -> new BlockItem(HOLOGRAM_BLOCK.get(),
                                        new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(
                                                        Registries.ITEM,
                                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID,
                                                                        "hologram_block")))
                                                        .arch$tab(TAB)));

        public static final RegistrySupplier<Item> MONITOR = ITEMS.register("bodycam_monitor",
                        () -> new BodycamMonitorItem(new Item.Properties()
                                        .setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM,
                                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID,
                                                                        "bodycam_monitor")))
                                        .stacksTo(1).arch$tab(TAB)));

        public static final RegistrySupplier<Item> CRYSTAL = ITEMS.register("observation_crystal",
                        () -> new Item(new Item.Properties().setId(net.minecraft.resources.ResourceKey
                                        .create(Registries.ITEM, net.minecraft.resources.Identifier
                                                        .fromNamespaceAndPath(MOD_ID, "observation_crystal")))
                                        .arch$tab(TAB)));

        public static final RegistrySupplier<Item> JAMMER = ITEMS.register("jammer",
                        () -> new JammerItem(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(
                                        Registries.ITEM,
                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "jammer")))
                                        .stacksTo(1).arch$tab(TAB)));

        public static final RegistrySupplier<Item> DIMENSION_LOCATOR = ITEMS.register("dimension_locator",
                        () -> new DimensionLocatorItem(new Item.Properties()
                                        .setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM,
                                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID,
                                                                        "dimension_locator")))
                                        .stacksTo(1).arch$tab(TAB)));

        public static final RegistrySupplier<Item> ANONYMIZER = ITEMS.register("anonymizer",
                        () -> new AnonymizerItem(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(
                                        Registries.ITEM,
                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "anonymizer")))
                                        .stacksTo(1).arch$tab(TAB)));

        public static final RegistrySupplier<EntityType<CompassDummyEntity>> COMPASS_DUMMY = ENTITIES.register(
                        "compass_dummy",
                        () -> EntityType.Builder.of(CompassDummyEntity::new, MobCategory.MISC).sized(0.6F, 1.8F)
                                        .build(net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE,
                                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID,
                                                                        "compass_dummy"))));

        public static final RegistrySupplier<EntityType<HologramDummyEntity>> HOLOGRAM_DUMMY = ENTITIES.register(
                        "hologram_dummy",
                        () -> EntityType.Builder.of(HologramDummyEntity::new, MobCategory.MISC).sized(0.6F, 1.8F)
                                        .build(net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE,
                                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID,
                                                                        "hologram_dummy"))));

        public static void init() {
                try {
                        System.err.println("--- DUMPING AbstractSelectionList ---");
                        for (java.lang.reflect.Method m : Class
                                        .forName("net.minecraft.client.gui.components.AbstractSelectionList")
                                        .getDeclaredMethods()) {
                                System.err.println("METHOD: " + m.getName() + " | return: "
                                                + m.getReturnType().getName() + " | params: "
                                                + java.util.Arrays.toString(m.getParameterTypes()));
                        }
                        System.err.println("--- DUMPING ObjectSelectionList ---");
                        for (java.lang.reflect.Method m : Class
                                        .forName("net.minecraft.client.gui.components.ObjectSelectionList")
                                        .getDeclaredMethods()) {
                                System.err.println("METHOD: " + m.getName() + " | return: "
                                                + m.getReturnType().getName() + " | params: "
                                                + java.util.Arrays.toString(m.getParameterTypes()));
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }

                BLOCKS.register();
                ITEMS.register();
                ENTITIES.register();
                TABS.register();

                dev.architectury.registry.level.entity.EntityAttributeRegistry.register(COMPASS_DUMMY,
                                dev.ClasherHD.bodycam.entity.BodycamDummyEntity::createAttributes);
                dev.architectury.registry.level.entity.EntityAttributeRegistry.register(HOLOGRAM_DUMMY,
                                dev.ClasherHD.bodycam.entity.BodycamDummyEntity::createAttributes);

                dev.ClasherHD.bodycam.config.ModServerConfig.load();
                dev.ClasherHD.bodycam.component.ModDataComponents.register();
                dev.ClasherHD.bodycam.network.ModNetworking.init();
                dev.ClasherHD.bodycam.util.BodycamLifecycle.init();

                dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register(
                                (dispatcher, registryAccess, environment) -> dev.ClasherHD.bodycam.command.CamTpCommand
                                                .register(dispatcher));
        }
}
