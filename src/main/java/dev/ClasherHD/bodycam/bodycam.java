package dev.ClasherHD.bodycam;

import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(bodycam.MODID)
public class bodycam {
        public static final String MODID = "bodycam";

        public static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, LockData> POSITION_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();

        public static class LockData {
                public final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> originalDim;
                public final double x, y, z;
                public final float yaw, pitch;
                public int lockTicks = 10;

                public LockData(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> originalDim,
                                double x, double y, double z, float yaw, float pitch) {
                        this.originalDim = originalDim;
                        this.x = x;
                        this.y = y;
                        this.z = z;
                        this.yaw = yaw;
                        this.pitch = pitch;
                }
        }

        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
        public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
                        .create(Registries.CREATIVE_MODE_TAB, MODID);
        public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister
                        .create(ForgeRegistries.ENCHANTMENTS, MODID);

        public static final RegistryObject<Item> BODYCAM_MONITOR = ITEMS.register("bodycam_monitor",
                        () -> new BodycamMonitorItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> OBSERVATION_CRYSTAL = ITEMS.register("observation_crystal",
                        () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> JAMMER = ITEMS.register("jammer",
                        () -> new dev.ClasherHD.bodycam.item.JammerItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> DIMENSION_LOCATOR = ITEMS.register("dimension_locator",
                        () -> new dev.ClasherHD.bodycam.item.DimensionLocatorItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> ANONYMIZER = ITEMS.register("anonymizer",
                        () -> new dev.ClasherHD.bodycam.item.AnonymizerItem(new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Block> HOLOGRAM_BLOCK = BLOCKS.register("hologram_block",
                        () -> new dev.ClasherHD.bodycam.block.HologramBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)
                                        .noOcclusion().strength(0.3F).sound(SoundType.GLASS)));
        public static final RegistryObject<Item> HOLOGRAM_BLOCK_ITEM = ITEMS.register("hologram_block",
                        () -> new BlockItem(HOLOGRAM_BLOCK.get(), new Item.Properties()));

        public static final RegistryObject<CreativeModeTab> BODYCAM_TAB = CREATIVE_MODE_TABS.register("bodycam_tab",
                        () -> CreativeModeTab.builder()
                                        .icon(() -> new net.minecraft.world.item.ItemStack(BODYCAM_MONITOR.get()))
                                        .title(Component.translatable("creativetab.bodycam_tab"))
                                        .displayItems((parameters, output) -> {
                                                output.accept(BODYCAM_MONITOR.get());
                                                output.accept(OBSERVATION_CRYSTAL.get());
                                                output.accept(JAMMER.get());
                                                output.accept(DIMENSION_LOCATOR.get());
                                                output.accept(ANONYMIZER.get());
                                                output.accept(HOLOGRAM_BLOCK_ITEM.get());
                                        })
                                        .build());

        public static final RegistryObject<Enchantment> REACH_ENCHANTMENT = ENCHANTMENTS.register("reach",
                        () -> new dev.ClasherHD.bodycam.enchantment.BodycamReachEnchantment());

        public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPES = DeferredRegister
                        .create(ForgeRegistries.ENTITY_TYPES, MODID);

        public static final RegistryObject<net.minecraft.world.entity.EntityType<dev.ClasherHD.bodycam.entity.CompassDummyEntity>> COMPASS_DUMMY = ENTITY_TYPES
                        .register(
                                        "compass_dummy",
                                        () -> net.minecraft.world.entity.EntityType.Builder.<dev.ClasherHD.bodycam.entity.CompassDummyEntity>of(
                                                        dev.ClasherHD.bodycam.entity.CompassDummyEntity::new,
                                                        net.minecraft.world.entity.MobCategory.MISC)
                                                        .sized(0.6F, 1.8F).clientTrackingRange(10)
                                                        .build("compass_dummy"));

        public static final RegistryObject<net.minecraft.world.entity.EntityType<dev.ClasherHD.bodycam.entity.HologramDummyEntity>> HOLOGRAM_DUMMY = ENTITY_TYPES
                        .register(
                                        "hologram_dummy",
                                        () -> net.minecraft.world.entity.EntityType.Builder.<dev.ClasherHD.bodycam.entity.HologramDummyEntity>of(
                                                        dev.ClasherHD.bodycam.entity.HologramDummyEntity::new,
                                                        net.minecraft.world.entity.MobCategory.MISC)
                                                        .sized(0.6F, 1.8F).clientTrackingRange(10)
                                                        .build("hologram_dummy"));

        public bodycam() {
                net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                                dev.ClasherHD.bodycam.config.ModClientConfig.SPEC, "bodycam-client.toml");
                net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                                net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                                dev.ClasherHD.bodycam.config.ModServerConfig.SPEC, "bodycam-server.toml");
                net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft,
                                                screen) -> new dev.ClasherHD.bodycam.client.gui.ClientConfigScreen(
                                                                screen)));

                IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

                ITEMS.register(modEventBus);
                BLOCKS.register(modEventBus);
                CREATIVE_MODE_TABS.register(modEventBus);
                ENCHANTMENTS.register(modEventBus);
                ENTITY_TYPES.register(modEventBus);

                modEventBus.addListener(this::commonSetup);
                modEventBus.addListener(this::clientSetup);

                MinecraftForge.EVENT_BUS.register(this);
        }

        private void commonSetup(final FMLCommonSetupEvent event) {
                PacketHandler.init();
                event.enqueueWork(() -> {
                        net.minecraftforge.common.world.ForgeChunkManager.setForcedChunkLoadingCallback(MODID,
                                        (level, ticketHelper) -> {
                                                ticketHelper.getEntityTickets().forEach((uuid, chunks) -> {
                                                        net.minecraft.world.entity.Entity entity = level
                                                                        .getEntity(uuid);
                                                        if (entity instanceof dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy) {
                                                                java.util.UUID ownerUUID = dummy.getOwnerUUID();
                                                                if (ownerUUID != null) {
                                                                        net.minecraft.server.level.ServerPlayer owner = level
                                                                                        .getServer().getPlayerList()
                                                                                        .getPlayer(ownerUUID);
                                                                        if (owner != null && owner.getPersistentData()
                                                                                        .getBoolean("bodycam_active")) {
                                                                                return;
                                                                        }
                                                                }
                                                        }
                                                        ticketHelper.removeAllTickets(uuid);
                                                });
                                        });
                });
        }

        private void clientSetup(final FMLClientSetupEvent event) {
                event.enqueueWork(() -> {
                        net.minecraft.client.renderer.item.ItemProperties.register(JAMMER.get(),
                                        new net.minecraft.resources.ResourceLocation("bodycam", "mode"),
                                        (stack, level, entity, seed) -> {
                                                if (stack.hasTag() && stack.getTag().contains("JammerMode")) {
                                                        return stack.getTag().getInt("JammerMode");
                                                }
                                                return 0.0F;
                                        });
                        net.minecraft.client.renderer.item.ItemProperties.register(ANONYMIZER.get(),
                                        new net.minecraft.resources.ResourceLocation("bodycam", "active"),
                                        (stack, level, entity, seed) -> {
                                                if (stack.hasTag() && stack.getTag().contains("AnonymizerActive")
                                                                && stack.getTag().getBoolean("AnonymizerActive")) {
                                                        return 1.0F;
                                                }
                                                return 0.0F;
                                        });
                });
        }

        @SubscribeEvent
        public void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
                if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer) {
                        net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) event.getEntity();
                        if (player.getPersistentData().contains("bodycam_target_uuid")) {
                                player.getPersistentData().remove("bodycam_target_uuid");
                                player.getPersistentData().remove("bodycam_dummy_uuid");
                                player.getPersistentData().remove("bodycam_disconnect_ticks");
                                player.getPersistentData().remove("bodycam_active");
                                player.getPersistentData().remove("bodycam_has_reach");
                                player.setInvisible(false);
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(player.getUUID());
                                POSITION_LOCKS.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                        new dev.ClasherHD.bodycam.network.BodycamResetCameraPacket()
                                );
                        }
                }
        }

        @SubscribeEvent
        public void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
                if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer) {
                        net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) event.getEntity();
                        if (player.getPersistentData().contains("bodycam_target_uuid") || player.getPersistentData().contains("bodycam_dummy_uuid")) {
                                if (player.getPersistentData().contains("bodycam_dummy_uuid")) {
                                        java.util.UUID dummyId = player.getPersistentData().getUUID("bodycam_dummy_uuid");
                                        for (net.minecraft.server.level.ServerLevel lvl : player.server.getAllLevels()) {
                                                net.minecraft.world.entity.Entity e = lvl.getEntity(dummyId);
                                                if (e != null) {
                                                        player.teleportTo(lvl, e.getX(), e.getY(), e.getZ(), e.getYRot(), e.getXRot());
                                                        e.discard();
                                                        break;
                                                }
                                        }
                                }
                                player.getPersistentData().remove("bodycam_target_uuid");
                                player.getPersistentData().remove("bodycam_dummy_uuid");
                                player.getPersistentData().remove("bodycam_disconnect_ticks");
                                player.getPersistentData().remove("bodycam_active");
                                player.getPersistentData().remove("bodycam_has_reach");
                                player.setInvisible(false);
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_ROT.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.remove(player.getUUID());
                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_GAMEMODE.remove(player.getUUID());
                                POSITION_LOCKS.remove(player.getUUID());
                        }
                }
        }

        @SubscribeEvent
        public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
                event.getDispatcher().register(
                        net.minecraft.commands.Commands.literal("camtp")
                                .requires(source -> source.hasPermission(2))
                                .then(net.minecraft.commands.Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                                        .executes(context -> {
                                                net.minecraft.server.level.ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
                                                net.minecraft.server.level.ServerPlayer targetPlayer = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");

                                                if (targetPlayer.getPersistentData().contains("bodycam_dummy_uuid") && targetPlayer.getPersistentData().contains("bodycam_original_dimension")) {
                                                        java.util.UUID dummyId = targetPlayer.getPersistentData().getUUID("bodycam_dummy_uuid");
                                                        String dimStr = targetPlayer.getPersistentData().getString("bodycam_original_dimension");
                                                        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = net.minecraft.resources.ResourceKey.create(
                                                                net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(dimStr));
                                                        net.minecraft.server.level.ServerLevel targetLevel = context.getSource().getServer().getLevel(dimKey);
                                                        
                                                        if (targetLevel != null) {
                                                                net.minecraft.world.entity.Entity dummy = targetLevel.getEntity(dummyId);
                                                                if (dummy != null) {
                                                                        sourcePlayer.teleportTo(targetLevel, dummy.getX(), dummy.getY(), dummy.getZ(), dummy.getYRot(), dummy.getXRot());
                                                                        return 1;
                                                                }
                                                        }
                                                }
                                                sourcePlayer.teleportTo(targetPlayer.serverLevel(), targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(), targetPlayer.getYRot(), targetPlayer.getXRot());
                                                return 1;
                                        })
                                )
                );
        }

        @SubscribeEvent
        public void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
                if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && !event.player.level().isClientSide()
                                && event.player instanceof net.minecraft.server.level.ServerPlayer) {
                        net.minecraft.server.level.ServerPlayer observer = (net.minecraft.server.level.ServerPlayer) event.player;

                        net.minecraft.world.item.ItemStack carried = observer.containerMenu.getCarried();
                        if (!carried.isEmpty()) {
                                if (carried.getItem() instanceof dev.ClasherHD.bodycam.item.JammerItem) {
                                        if (carried.hasTag() && carried.getTag().contains("JammerMode") && carried.getTag().getInt("JammerMode") > 0) {
                                                if (carried.getTag().hasUUID("active_id") && observer.getPersistentData().hasUUID("bodycam_active_jammer_id")) {
                                                        if (carried.getTag().getUUID("active_id").equals(observer.getPersistentData().getUUID("bodycam_active_jammer_id"))) {
                                                                observer.getPersistentData().putLong("bodycam_jammer_heartbeat", observer.level().getGameTime());
                                                        }
                                                }
                                        }
                                } else if (carried.getItem() instanceof dev.ClasherHD.bodycam.item.AnonymizerItem) {
                                        if (carried.hasTag() && carried.getTag().contains("AnonymizerActive") && carried.getTag().getBoolean("AnonymizerActive")) {
                                                if (carried.getTag().hasUUID("active_id") && observer.getPersistentData().hasUUID("bodycam_active_anonymizer_id")) {
                                                        if (carried.getTag().getUUID("active_id").equals(observer.getPersistentData().getUUID("bodycam_active_anonymizer_id"))) {
                                                                observer.getPersistentData().putLong("bodycam_anonymizer_heartbeat", observer.level().getGameTime());
                                                        }
                                                }
                                        }
                                }
                        }

                        if (POSITION_LOCKS.containsKey(observer.getUUID())) {
                                LockData data = POSITION_LOCKS.get(observer.getUUID());
                                if (observer.level().dimension() == data.originalDim) {
                                        observer.teleportTo(observer.serverLevel(), data.x, data.y, data.z, data.yaw,
                                                        data.pitch);
                                        observer.hurtMarked = true;
                                        data.lockTicks--;
                                        if (data.lockTicks <= 0) {
                                                POSITION_LOCKS.remove(observer.getUUID());
                                        }
                                }
                        }

                        if (observer.getPersistentData().getBoolean("bodycam_active")
                                        && observer.getPersistentData().contains("bodycam_target_uuid")) {
                                java.util.UUID targetId = observer.getPersistentData().getUUID("bodycam_target_uuid");
                                net.minecraft.server.level.ServerPlayer target = observer.server.getPlayerList()
                                                .getPlayer(targetId);
                                if (target != null && target.isAlive() && !target.isRemoved()) {
                                        observer.getPersistentData().putInt("bodycam_disconnect_ticks", 0);

                                        long lastJammer = target.getPersistentData().getLong("bodycam_jammer_heartbeat");
                                        int currentJammerMode = target.getPersistentData().getInt("bodycam_jammer_mode");
                                        boolean isJammerActive = (target.level().getGameTime() - lastJammer) <= 10;
                                        if (!isJammerActive) {
                                                currentJammerMode = 0;
                                        }

                                        if (currentJammerMode == 2) {
                                                net.minecraft.world.phys.Vec3 dummyPos = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS
                                                                .getOrDefault(observer.getUUID(),
                                                                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS
                                                                                                .get(observer.getUUID()));
                                                boolean outOfRange = observer.level().dimension() != target.level()
                                                                .dimension();
                                                if (!outOfRange && dummyPos != null) {
                                                        double maxDist = (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE
                                                                        .get();
                                                        outOfRange = dummyPos.distanceToSqr(target.position()) > maxDist
                                                                        * maxDist;
                                                }
                                                if (outOfRange) {
                                                        dev.ClasherHD.bodycam.network.BodycamResetCameraPacket
                                                                        .executeReset(observer);
                                                        dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                                                        net.minecraftforge.network.PacketDistributor.PLAYER
                                                                                        .with(() -> observer),
                                                                        new dev.ClasherHD.bodycam.network.BodycamForceClosePacket());
                                                        observer.sendSystemMessage(net.minecraft.network.chat.Component
                                                                        .translatable(
                                                                                        "message.bodycam.jammer_blocked")
                                                                        .withStyle(net.minecraft.ChatFormatting.RED));
                                                        return;
                                                }
                                        } else if (currentJammerMode == 1) {
                                                dev.ClasherHD.bodycam.network.BodycamResetCameraPacket
                                                                .executeReset(observer);
                                                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                                                net.minecraftforge.network.PacketDistributor.PLAYER
                                                                                .with(() -> observer),
                                                                new dev.ClasherHD.bodycam.network.BodycamForceClosePacket());
                                                observer.sendSystemMessage(net.minecraft.network.chat.Component
                                                                .translatable(
                                                                                "message.bodycam.jammer_blocked")
                                                                .withStyle(net.minecraft.ChatFormatting.RED));
                                                return;
                                        }

                                        boolean reachActive = observer.getPersistentData()
                                                        .getBoolean("bodycam_has_reach")
                                                        && dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_REACH_ENCHANTMENT
                                                                        .get();

                                        if (!reachActive) {
                                                net.minecraft.world.phys.Vec3 dummyPos2 = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS
                                                                .getOrDefault(observer.getUUID(),
                                                                                dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS
                                                                                                .get(observer.getUUID()));
                                                boolean outOfRange2 = observer.level().dimension() != target.level()
                                                                .dimension();
                                                if (!outOfRange2 && dummyPos2 != null) {
                                                        double maxDist2 = (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE
                                                                        .get();
                                                        outOfRange2 = dummyPos2.distanceToSqr(
                                                                        target.position()) > maxDist2 * maxDist2;
                                                }
                                                if (outOfRange2) {
                                                        dev.ClasherHD.bodycam.network.BodycamResetCameraPacket
                                                                        .executeReset(observer);
                                                        dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                                                        net.minecraftforge.network.PacketDistributor.PLAYER
                                                                                        .with(() -> observer),
                                                                        new dev.ClasherHD.bodycam.network.BodycamForceClosePacket());
                                                        observer.sendSystemMessage(net.minecraft.network.chat.Component
                                                                        .translatable(
                                                                                        "message.bodycam.signal_weak")
                                                                        .withStyle(net.minecraft.ChatFormatting.RED));
                                                        return;
                                                }
                                        }

                                        if (observer.level().dimension() == target.level().dimension()) {
                                                observer.setPos(target.getX(), target.getY(), target.getZ());
                                        } else {
                                                observer.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
                                        }
                                        observer.setCamera(target);
                                } else {
                                        int ticks = observer.getPersistentData().getInt("bodycam_disconnect_ticks");
                                        ticks++;
                                        observer.getPersistentData().putInt("bodycam_disconnect_ticks", ticks);
                                        if (ticks >= 40) {
                                                dev.ClasherHD.bodycam.network.BodycamResetCameraPacket
                                                                .executeReset(observer);
                                        }
                                }
                        }
                }
        }

        @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
        public static class CommonModEvents {
                @SubscribeEvent
                public static void entityAttributes(EntityAttributeCreationEvent event) {
                        event.put(COMPASS_DUMMY.get(),
                                        dev.ClasherHD.bodycam.entity.BodycamDummyEntity.createAttributes().build());
                        event.put(HOLOGRAM_DUMMY.get(),
                                        dev.ClasherHD.bodycam.entity.BodycamDummyEntity.createAttributes().build());
                }
        }

        @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
        public static class ClientModEvents {
                @SubscribeEvent
                public static void registerRenderers(
                                net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
                        event.registerEntityRenderer(COMPASS_DUMMY.get(),
                                        dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);
                        event.registerEntityRenderer(HOLOGRAM_DUMMY.get(),
                                        dev.ClasherHD.bodycam.client.render.BodycamDummyRenderer::new);
                }
        }
}
