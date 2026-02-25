package dev.ClasherHD.bodycam;

import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.client.player.AbstractClientPlayer;

@Mod(bodycam.MODID)
public class bodycam {
        public static final String MODID = "bodycam";

        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
        public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister
                        .create(ForgeRegistries.ENCHANTMENTS, MODID);

        public static final RegistryObject<Item> BODYCAM_MONITOR = ITEMS.register("bodycam_monitor",
                        () -> new BodycamMonitorItem(new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Enchantment> REACH_ENCHANTMENT = ENCHANTMENTS.register("reach",
                        () -> new dev.ClasherHD.bodycam.enchantment.BodycamReachEnchantment());

        public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPES = DeferredRegister
                        .create(ForgeRegistries.ENTITY_TYPES, MODID);

        public static final RegistryObject<net.minecraft.world.entity.EntityType<dev.ClasherHD.bodycam.entity.BodycamDummyEntity>> BODYCAM_DUMMY = ENTITY_TYPES
                        .register(
                                        "bodycam_dummy",
                                        () -> net.minecraft.world.entity.EntityType.Builder.<dev.ClasherHD.bodycam.entity.BodycamDummyEntity>of(
                                                        dev.ClasherHD.bodycam.entity.BodycamDummyEntity::new,
                                                        net.minecraft.world.entity.MobCategory.MISC)
                                                        .sized(0.6F, 1.8F).clientTrackingRange(10)
                                                        .build("bodycam_dummy"));

        public bodycam() {
                IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

                ITEMS.register(modEventBus);
                ENCHANTMENTS.register(modEventBus);
                ENTITY_TYPES.register(modEventBus);

                modEventBus.addListener(this::commonSetup);
                modEventBus.addListener(this::clientSetup);
                modEventBus.addListener(this::addCreative);

                MinecraftForge.EVENT_BUS.register(this);
        }

        private void addCreative(BuildCreativeModeTabContentsEvent event) {
                if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                        net.minecraft.world.item.ItemStack compassStack = new net.minecraft.world.item.ItemStack(
                                        Items.RECOVERY_COMPASS);
                        net.minecraft.world.item.ItemStack monitorStack = new net.minecraft.world.item.ItemStack(
                                        BODYCAM_MONITOR.get());

                        event.getEntries().putAfter(compassStack, monitorStack,
                                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
        }

        private void commonSetup(final FMLCommonSetupEvent event) {
                PacketHandler.init();
        }

        private void clientSetup(final FMLClientSetupEvent event) {
        }

        @SubscribeEvent
        public void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
                if (!event.getEntity().level().isClientSide()
                                && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer) {
                        net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) event
                                        .getEntity();
                        player.getPersistentData().putBoolean("bodycam_active", false);
                        player.setCamera(player);
                        player.getPersistentData().remove("bodycam_last_x");
                        player.getPersistentData().remove("bodycam_last_y");
                        player.getPersistentData().remove("bodycam_last_z");
                        player.getPersistentData().remove("bodycam_last_fall");
                }
        }

        @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
        public static class CommonModEvents {
                @SubscribeEvent
                public static void entityAttributes(EntityAttributeCreationEvent event) {
                        event.put(BODYCAM_DUMMY.get(),
                                        dev.ClasherHD.bodycam.entity.BodycamDummyEntity.createAttributes().build());
                }
        }

        @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
        public static class ClientModEvents {
                @SubscribeEvent
                public static void registerRenderers(
                                net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
                        event.registerEntityRenderer(BODYCAM_DUMMY.get(),
                                        dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);
                }
        }
}
