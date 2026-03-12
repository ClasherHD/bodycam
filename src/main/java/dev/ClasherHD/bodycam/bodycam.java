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

        public static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, LockData> POSITION_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();

        public static class LockData {
                public final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> originalDim;
                public final double x, y, z;
                public final float yaw, pitch;
                public int lockTicks = 10;

                public LockData(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> originalDim, double x, double y, double z, float yaw, float pitch) {
                        this.originalDim = originalDim;
                        this.x = x;
                        this.y = y;
                        this.z = z;
                        this.yaw = yaw;
                        this.pitch = pitch;
                }
        }

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
        public void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
                if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer) {
                        net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) event.getEntity();
                        if (player.getPersistentData().getBoolean("bodycam_active")) {
                                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                                player.setCamera(player);
                                player.getPersistentData().putBoolean("bodycam_active", false);
                                player.getPersistentData().remove("bodycam_target_uuid");
                                player.getPersistentData().remove("bodycam_disconnect_ticks");
                                player.getPersistentData().remove("bodycam_dummy_uuid");
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(player.getUUID());
                                POSITION_LOCKS.remove(player.getUUID());
                        }
                }
        }

        @SubscribeEvent
        public void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
                if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && !event.player.level().isClientSide() && event.player instanceof net.minecraft.server.level.ServerPlayer) {
                        net.minecraft.server.level.ServerPlayer observer = (net.minecraft.server.level.ServerPlayer) event.player;

                        if (POSITION_LOCKS.containsKey(observer.getUUID())) {
                                LockData data = POSITION_LOCKS.get(observer.getUUID());
                                if (observer.level().dimension() == data.originalDim) {
                                        observer.teleportTo(observer.serverLevel(), data.x, data.y, data.z, data.yaw, data.pitch);
                                        observer.hurtMarked = true;
                                        data.lockTicks--;
                                        if (data.lockTicks <= 0) {
                                                POSITION_LOCKS.remove(observer.getUUID());
                                        }
                                }
                        }

                        if (observer.getPersistentData().getBoolean("bodycam_active") && observer.getPersistentData().contains("bodycam_target_uuid")) {
                                java.util.UUID targetId = observer.getPersistentData().getUUID("bodycam_target_uuid");
                                net.minecraft.server.level.ServerPlayer target = observer.server.getPlayerList().getPlayer(targetId);
                                if (target != null && target.isAlive() && !target.isRemoved()) {
                                        observer.getPersistentData().putInt("bodycam_disconnect_ticks", 0);
                                        if (observer.level().dimension() != target.level().dimension()) {
                                                observer.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
                                                observer.setCamera(target);
                                        }
                                } else {
                                        int ticks = observer.getPersistentData().getInt("bodycam_disconnect_ticks");
                                        ticks++;
                                        observer.getPersistentData().putInt("bodycam_disconnect_ticks", ticks);
                                        if (ticks >= 40) {
                                                dev.ClasherHD.bodycam.network.BodycamResetCameraPacket.executeReset(observer);
                                        }
                                }
                        }
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
