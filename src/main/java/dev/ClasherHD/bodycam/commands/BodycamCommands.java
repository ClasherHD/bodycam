package dev.ClasherHD.bodycam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

@SuppressWarnings("null")
public class BodycamCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bodycam")
                .then(Commands.literal("config")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            int maxDist = dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get();
                            int maxWpPlayer = dev.ClasherHD.bodycam.config.ModServerConfig.MAX_WATCH_POINTS_PER_PLAYER.get();
                            int maxWpTotal = dev.ClasherHD.bodycam.config.ModServerConfig.MAX_WATCH_POINTS_TOTAL.get();
                            boolean reach = dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_REACH_ENCHANTMENT.get();
                            boolean jammer = dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_JAMMER.get();
                            boolean locator = dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_DIMENSION_LOCATOR.get();
                            boolean hologram = dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get();
                            boolean anonymizer = dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_ANONYMIZER.get();
                            boolean playerLocator = dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_PLAYER_LOCATOR.get();
                            boolean watchPoint = dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_WATCH_POINT.get();
                            boolean podest = dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_PODEST.get();
                            boolean opOnly = dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get();

                            dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                new dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket(
                                    maxDist, maxWpPlayer, maxWpTotal, reach, jammer, locator, hologram, anonymizer, playerLocator, watchPoint, podest, opOnly
                                )
                            );
                            return 1;
                        } else {
                            context.getSource().sendFailure(Component.literal("Only in-game players can run this command."));
                            return 0;
                        }
                    })
                )
                .then(Commands.literal("give")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("item", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("bodycam_monitor");
                            builder.suggest("observation_crystal");
                            builder.suggest("jammer");
                            builder.suggest("dimension_locator");
                            builder.suggest("anonymizer");
                            builder.suggest("hologram_block");
                            builder.suggest("player_locator_compass");
                            builder.suggest("observation_crystal_block");
                            builder.suggest("ancient_table");
                            builder.suggest("portable_nether_portal");
                            builder.suggest("watch_point");
                            builder.suggest("podest");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("players", EntityArgument.players())
                            .executes(context -> {
                                int count = 0;
                                String itemName = StringArgumentType.getString(context, "item");
                                for (ServerPlayer target : EntityArgument.getPlayers(context, "players")) {
                                    if (giveItem(context.getSource(), target, itemName) > 0) {
                                        count++;
                                    }
                                }
                                return count;
                            })
                        )
                    )
                )
                .then(Commands.literal("tp")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("dummy")
                        .then(Commands.argument("dummyPlayer", EntityArgument.player())
                            .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(context -> {
                                    ServerPlayer dummyOwner = EntityArgument.getPlayer(context, "dummyPlayer");
                                    dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy = getDummyEntity(dummyOwner);
                                    if (dummy == null) {
                                        context.getSource().sendFailure(Component.translatable("message.bodycam.no_dummy_found"));
                                        return 0;
                                    }
                                    Vec3 coords = Vec3Argument.getVec3(context, "pos");
                                    dummy.teleportTo(coords.x, coords.y, coords.z);
                                    dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.put(dummyOwner.getUUID(), coords);
                                    context.getSource().sendSuccess(() -> Component.literal("Teleported dummy of " + dummyOwner.getName().getString() + " to " + coords), true);
                                    return 1;
                                })
                            )
                            .then(Commands.argument("destinationPlayer", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer dummyOwner = EntityArgument.getPlayer(context, "dummyPlayer");
                                    dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy = getDummyEntity(dummyOwner);
                                    if (dummy == null) {
                                        context.getSource().sendFailure(Component.translatable("message.bodycam.no_dummy_found"));
                                        return 0;
                                    }
                                    ServerPlayer destPlayer = EntityArgument.getPlayer(context, "destinationPlayer");
                                    ServerLevel destLevel = destPlayer.serverLevel();

                                    if (dummy.level() != destLevel) {
                                        net.minecraft.world.entity.Entity newDummy = dummy.changeDimension(destLevel);
                                        if (newDummy instanceof dev.ClasherHD.bodycam.entity.BodycamDummyEntity actualDummy) {
                                            actualDummy.teleportTo(destPlayer.getX(), destPlayer.getY(), destPlayer.getZ());
                                            dummyOwner.getPersistentData().putUUID("bodycam_dummy_uuid", actualDummy.getUUID());
                                            dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.put(dummyOwner.getUUID(), destPlayer.position());
                                        }
                                    } else {
                                        dummy.teleportTo(destPlayer.getX(), destPlayer.getY(), destPlayer.getZ());
                                        dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.put(dummyOwner.getUUID(), destPlayer.position());
                                    }

                                    context.getSource().sendSuccess(() -> Component.literal("Teleported dummy of " + dummyOwner.getName().getString() + " to " + destPlayer.getName().getString()), true);
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(Commands.literal("cam")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
                                ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");

                                if (targetPlayer.getPersistentData().contains("bodycam_dummy_uuid")
                                        && targetPlayer.getPersistentData().contains("bodycam_original_dimension")) {
                                    UUID dummyId = targetPlayer.getPersistentData().getUUID("bodycam_dummy_uuid");
                                    String dimStr = targetPlayer.getPersistentData().getString("bodycam_original_dimension");
                                    ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(
                                        Registries.DIMENSION,
                                        new ResourceLocation(dimStr)
                                    );
                                    ServerLevel targetLevel = context.getSource().getServer().getLevel(dimKey);

                                    if (targetLevel != null) {
                                        net.minecraft.world.entity.Entity dummy = targetLevel.getEntity(dummyId);
                                        if (dummy != null) {
                                            sourcePlayer.teleportTo(
                                                targetLevel,
                                                dummy.getX(),
                                                dummy.getY(),
                                                dummy.getZ(),
                                                dummy.getYRot(),
                                                dummy.getXRot()
                                            );
                                            return 1;
                                        }
                                    }
                                }

                                sourcePlayer.teleportTo(
                                    targetPlayer.serverLevel(),
                                    targetPlayer.getX(),
                                    targetPlayer.getY(),
                                    targetPlayer.getZ(),
                                    targetPlayer.getYRot(),
                                    targetPlayer.getXRot()
                                );
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("remove")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("dummy")
                        .then(Commands.argument("dummyPlayer", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer p = EntityArgument.getPlayer(context, "dummyPlayer");
                                if (!p.getPersistentData().getBoolean("bodycam_active") || !p.getPersistentData().contains("bodycam_dummy_uuid")) {
                                    context.getSource().sendFailure(Component.translatable("message.bodycam.no_dummy_found"));
                                    return 0;
                                }
                                dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket.executeReset(p);
                                context.getSource().sendSuccess(() -> Component.literal("Removed dummy of " + p.getName().getString()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("all")
                        .then(Commands.literal("dummies")
                            .executes(context -> {
                                int count = 0;
                                for (ServerPlayer p : context.getSource().getServer().getPlayerList().getPlayers()) {
                                    if (p.getPersistentData().getBoolean("bodycam_active")) {
                                        dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket.executeReset(p);
                                        count++;
                                    }
                                }
                                if (count == 0) {
                                    context.getSource().sendFailure(Component.translatable("message.bodycam.no_dummy_found"));
                                    return 0;
                                }
                                final int finalCount = count;
                                context.getSource().sendSuccess(() -> Component.literal("Removed all dummies (" + finalCount + ")"), true);
                                return count;
                            })
                        )
                    )
                )
        );
    }

    private static int giveItem(CommandSourceStack source, ServerPlayer target, String itemName) {
        Item item = switch (itemName) {
            case "bodycam_monitor" -> dev.ClasherHD.bodycam.registry.ModItems.BODYCAM_MONITOR.get();
            case "observation_crystal" -> dev.ClasherHD.bodycam.registry.ModItems.OBSERVATION_CRYSTAL.get();
            case "jammer" -> dev.ClasherHD.bodycam.registry.ModItems.JAMMER.get();
            case "dimension_locator" -> dev.ClasherHD.bodycam.registry.ModItems.DIMENSION_LOCATOR.get();
            case "anonymizer" -> dev.ClasherHD.bodycam.registry.ModItems.ANONYMIZER.get();
            case "hologram_block" -> dev.ClasherHD.bodycam.registry.ModItems.HOLOGRAM_BLOCK_ITEM.get();
            case "player_locator_compass" -> dev.ClasherHD.bodycam.registry.ModItems.PLAYER_LOCATOR_COMPASS.get();
            case "observation_crystal_block" -> dev.ClasherHD.bodycam.registry.ModItems.OBSERVATION_CRYSTAL_BLOCK_ITEM.get();
            case "ancient_table" -> dev.ClasherHD.bodycam.registry.ModItems.ANCIENT_TABLE_ITEM.get();
            case "portable_nether_portal" -> dev.ClasherHD.bodycam.registry.ModItems.PORTABLE_NETHER_PORTAL.get();
            case "watch_point" -> dev.ClasherHD.bodycam.registry.ModItems.WATCH_POINT_ITEM.get();
            case "podest" -> dev.ClasherHD.bodycam.registry.ModItems.PODEST_ITEM.get();
            default -> null;
        };

        if (item == null) {
            source.sendFailure(Component.literal("Unknown bodycam item: " + itemName));
            return 0;
        }

        ItemStack stack = new ItemStack(item);
        boolean added = target.getInventory().add(stack);
        if (!added || !stack.isEmpty()) {
            ItemEntity itemEntity = target.drop(stack, false);
            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay();
                itemEntity.setTarget(target.getUUID());
            }
        }

        source.sendSuccess(() -> Component.literal("Gave 1 " + itemName + " to " + target.getName().getString()), true);
        return 1;
    }

    private static dev.ClasherHD.bodycam.entity.BodycamDummyEntity getDummyEntity(ServerPlayer player) {
        if (player == null) return null;
        if (!player.getPersistentData().getBoolean("bodycam_active")) return null;
        if (!player.getPersistentData().contains("bodycam_dummy_uuid")) return null;
        UUID dummyId = player.getPersistentData().getUUID("bodycam_dummy_uuid");
        for (ServerLevel lvl : player.server.getAllLevels()) {
            net.minecraft.world.entity.Entity e = lvl.getEntity(dummyId);
            if (e instanceof dev.ClasherHD.bodycam.entity.BodycamDummyEntity dummy) {
                return dummy;
            }
        }
        return null;
    }
}
