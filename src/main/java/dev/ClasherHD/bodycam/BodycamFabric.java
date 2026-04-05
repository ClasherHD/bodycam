package dev.ClasherHD.bodycam;

import dev.ClasherHD.bodycam.block.HologramBlock;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.item.DimensionLocatorItem;
import dev.ClasherHD.bodycam.item.JammerItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BodycamFabric implements ModInitializer {

    public static final String MOD_ID = "bodycam";

    public static final java.util.Map<java.util.UUID, LockData> POSITION_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();

    public static class LockData {
        public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> targetDim;
        public double x, y, z;
        public float yRot, xRot;
        public LockData(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, double x, double y, double z, float yaw, float pitch) {
            this.targetDim = dim; this.x = x; this.y = y; this.z = z; this.yRot = yaw; this.xRot = pitch;
        }
    }

    public static final Item BODYCAM_MONITOR = new BodycamMonitorItem(new Item.Properties().stacksTo(1));
    public static final Item OBSERVATION_CRYSTAL = new Item(new Item.Properties());
    public static final Item JAMMER = new JammerItem(new Item.Properties().stacksTo(1));
    public static final Item DIMENSION_LOCATOR = new DimensionLocatorItem(new Item.Properties().stacksTo(1));
    public static final Item ANONYMIZER = new AnonymizerItem(new Item.Properties().stacksTo(1));

    public static final Block HOLOGRAM_BLOCK = new HologramBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).noOcclusion().strength(0.3F).sound(SoundType.GLASS));
    public static final Item HOLOGRAM_BLOCK_ITEM = new BlockItem(HOLOGRAM_BLOCK, new Item.Properties());

    public static final CreativeModeTab BODYCAM_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            new ResourceLocation(MOD_ID, "bodycam_tab"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(BODYCAM_MONITOR))
                    .title(Component.translatable("creativetab.bodycam_tab"))
                    .build());

    public static final net.minecraft.world.item.enchantment.Enchantment REACH_ENCHANTMENT = new dev.ClasherHD.bodycam.enchantment.BodycamReachEnchantment();

    @Override
    public void onInitialize() {
        dev.ClasherHD.bodycam.entity.EntityRegistryFabric.registerEntities();
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(dev.ClasherHD.bodycam.entity.EntityRegistryFabric.COMPASS_DUMMY, dev.ClasherHD.bodycam.entity.BodycamDummyEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(dev.ClasherHD.bodycam.entity.EntityRegistryFabric.HOLOGRAM_DUMMY, dev.ClasherHD.bodycam.entity.BodycamDummyEntity.createAttributes());
        Registry.register(BuiltInRegistries.ENCHANTMENT, new ResourceLocation(MOD_ID, "reach"), REACH_ENCHANTMENT);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "bodycam_monitor"), BODYCAM_MONITOR);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "observation_crystal"), OBSERVATION_CRYSTAL);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "jammer"), JAMMER);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "dimension_locator"), DIMENSION_LOCATOR);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "anonymizer"), ANONYMIZER);

        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, "hologram_block"), HOLOGRAM_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "hologram_block"), HOLOGRAM_BLOCK_ITEM);

        ItemGroupEvents.modifyEntriesEvent(BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(BODYCAM_TAB).get()).register(content -> {
            content.accept(BODYCAM_MONITOR);
            content.accept(OBSERVATION_CRYSTAL);
            content.accept(JAMMER);
            content.accept(DIMENSION_LOCATOR);
            content.accept(ANONYMIZER);
            content.accept(HOLOGRAM_BLOCK_ITEM);
            content.accept(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(new net.minecraft.world.item.enchantment.EnchantmentInstance(REACH_ENCHANTMENT, 1)));
        });

        dev.ClasherHD.bodycam.network.ServerNetworking.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).getBoolean("bodycam_active") && dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).contains("bodycam_target_uuid")) {
                    java.util.UUID targetUuid = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).getUUID("bodycam_target_uuid");
                    net.minecraft.server.level.ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
                    if (target == null) {
                        dev.ClasherHD.bodycam.network.BodycamResetCameraPacket.executeReset(player);
                        continue;
                    }
                    boolean hasReach = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).getBoolean("bodycam_has_reach");
                    boolean isHologram = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).getBoolean("bodycam_is_hologram");
                    if (!hasReach && !isHologram) {
                        net.minecraft.world.phys.Vec3 origPos = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.get(player.getUUID());
                        double distance = (origPos != null) ? target.position().distanceTo(origPos) : 0;
                        if (player.level() != target.level() || (origPos != null && distance > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get())) {
                            dev.ClasherHD.bodycam.network.BodycamResetCameraPacket.executeReset(player);
                            continue;
                        }
                    } else {
                        if (player.level() != target.level()) {
                            dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putBoolean("bodycam_internal_tp", true);
                            player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
                            dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putBoolean("bodycam_internal_tp", false);
                            player.setCamera(target);
                        }
                    }
                    player.setPos(target.getX(), target.getY(), target.getZ());
                }
            }
        });

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            net.minecraft.server.level.ServerPlayer player = handler.getPlayer();
            if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).getBoolean("bodycam_active") || dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).contains("bodycam_dummy_uuid")) {
                dev.ClasherHD.bodycam.network.BodycamResetCameraPacket.executeReset(player);
            }
        });

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.minecraft.server.level.ServerPlayer player = handler.getPlayer();
            if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).getBoolean("bodycam_active") || dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).contains("bodycam_dummy_uuid")) {
                dev.ClasherHD.bodycam.network.BodycamResetCameraPacket.executeReset(player);
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, dev.ClasherHD.bodycam.network.BodycamPacketIDs.RESET_CAMERA_PACKET_ID, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
                dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putBoolean("bodycam_active", false);
            }
        });

        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.commands.Commands.literal("camtp")
                    .requires(source -> source.hasPermission(2))
                    .then(net.minecraft.commands.Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                            .executes(context -> {
                                net.minecraft.server.level.ServerPlayer executor = context.getSource().getPlayerOrException();
                                net.minecraft.server.level.ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");

                                if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target).getBoolean("bodycam_active") && dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target).contains("bodycam_dummy_uuid") && dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target).contains("bodycam_original_dimension")) {
                                    String dimName = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target).getString("bodycam_original_dimension");
                                    java.util.UUID dummyUuid = dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(target).getUUID("bodycam_dummy_uuid");
                                    net.minecraft.server.level.ServerLevel targetWorld = null;
                                    for (net.minecraft.server.level.ServerLevel level : context.getSource().getServer().getAllLevels()) {
                                        if (level.dimension().location().getPath().equals(dimName) || level.dimension().location().toString().equals(dimName)) {
                                            targetWorld = level;
                                            break;
                                        }
                                    }
                                    if (targetWorld != null) {
                                        net.minecraft.world.entity.Entity dummy = targetWorld.getEntity(dummyUuid);
                                        if (dummy != null) {
                                            executor.teleportTo(targetWorld, dummy.getX(), dummy.getY(), dummy.getZ(), dummy.getYRot(), dummy.getXRot());
                                            return 1;
                                        }
                                    }
                                }
                                executor.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
                                return 1;
                            })));
        });
    }
}
