package dev.ClasherHD.bodycam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import dev.ClasherHD.bodycam.compat.curios.CuriosCompat;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.enchantment.NightspyEffectHandler;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.event.ServerEvents;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket;
import dev.ClasherHD.bodycam.network.config.ReloadClientConfigS2CPacket;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("null")
public class BodycamCommands {

    private static final SuggestionProvider<CommandSourceStack> WATCH_POINT_SUGGESTIONS = (context, builder) -> {
        List<WatchPointDummyEntity> active = WatchPointDummyEntity.getAllActive(context.getSource().getServer());
        for (int i = 0; i < active.size(); i++) {
            WatchPointDummyEntity wp = active.get(i);
            builder.suggest(String.valueOf(i + 1));
            builder.suggest(wp.getUUID().toString().substring(0, 8));
            String name = wp.getWatchPointName();
            if (name != null && !name.isEmpty() && !name.contains(" ")) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bodycam")
                .then(Commands.literal("config")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            int maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
                            int maxWpPlayer = ModServerConfig.MAX_WATCH_POINTS_PER_PLAYER.get();
                            int maxWpTotal = ModServerConfig.MAX_WATCH_POINTS_TOTAL.get();
                            boolean reach = ModServerConfig.ENABLE_REACH_ENCHANTMENT.get();
                            boolean jammer = ModServerConfig.ENABLE_JAMMER.get();
                            boolean locator = ModServerConfig.ENABLE_DIMENSION_LOCATOR.get();
                            boolean hologram = ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get();
                            boolean anonymizer = ModServerConfig.ENABLE_ANONYMIZER.get();
                            boolean playerLocator = ModServerConfig.ENABLE_PLAYER_LOCATOR.get();
                            boolean watchPoint = ModServerConfig.ENABLE_WATCH_POINT.get();
                            boolean podest = ModServerConfig.ENABLE_PODEST.get();
                            boolean opOnly = ModServerConfig.OP_ONLY_MODE.get();

                            PacketHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                new OpenServerConfigS2CPacket(
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
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> executeReload(context.getSource()))
                )
                .then(Commands.literal("reset")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> executeEmergencyReset(context.getSource(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeEmergencyReset(context.getSource(), EntityArgument.getPlayer(context, "player")))
                    )
                )
                .then(Commands.literal("status")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> executeStatus(context.getSource(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeStatus(context.getSource(), EntityArgument.getPlayer(context, "player")))
                    )
                )
                .then(Commands.literal("watchpoint")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("list")
                        .executes(context -> executeWatchPointList(context.getSource(), null))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> executeWatchPointList(context.getSource(), EntityArgument.getPlayer(context, "player")))
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.literal("all")
                            .executes(context -> executeWatchPointRemoveAll(context.getSource()))
                        )
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .executes(context -> executeWatchPointRemovePos(context.getSource(), BlockPosArgument.getBlockPos(context, "pos")))
                        )
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                            .suggests(WATCH_POINT_SUGGESTIONS)
                            .executes(context -> executeWatchPointRemoveById(context.getSource(), StringArgumentType.getString(context, "id")))
                        )
                    )
                    .then(Commands.literal("tp")
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                            .suggests(WATCH_POINT_SUGGESTIONS)
                            .executes(context -> executeWatchPointTp(context.getSource(), StringArgumentType.getString(context, "id")))
                        )
                    )
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
                                    BodycamDummyEntity dummy = getDummyEntity(dummyOwner);
                                    if (dummy == null) {
                                        context.getSource().sendFailure(Component.translatable("message.bodycam.no_dummy_found"));
                                        return 0;
                                    }
                                    Vec3 coords = Vec3Argument.getVec3(context, "pos");
                                    dummy.teleportTo(coords.x, coords.y, coords.z);
                                    BodycamDummyEntity.DUMMY_POS.put(dummyOwner.getUUID(), coords);
                                    context.getSource().sendSuccess(() -> Component.literal("Teleported dummy of " + dummyOwner.getName().getString() + " to " + coords), true);
                                    return 1;
                                })
                            )
                            .then(Commands.argument("destinationPlayer", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer dummyOwner = EntityArgument.getPlayer(context, "dummyPlayer");
                                    BodycamDummyEntity dummy = getDummyEntity(dummyOwner);
                                    if (dummy == null) {
                                        context.getSource().sendFailure(Component.translatable("message.bodycam.no_dummy_found"));
                                        return 0;
                                    }
                                    ServerPlayer destPlayer = EntityArgument.getPlayer(context, "destinationPlayer");
                                    ServerLevel destLevel = destPlayer.serverLevel();

                                    if (dummy.level() != destLevel) {
                                        Entity newDummy = dummy.changeDimension(destLevel);
                                        if (newDummy instanceof BodycamDummyEntity actualDummy) {
                                            actualDummy.teleportTo(destPlayer.getX(), destPlayer.getY(), destPlayer.getZ());
                                            dummyOwner.getPersistentData().putUUID("bodycam_dummy_uuid", actualDummy.getUUID());
                                            BodycamDummyEntity.DUMMY_POS.put(dummyOwner.getUUID(), destPlayer.position());
                                        }
                                    } else {
                                        dummy.teleportTo(destPlayer.getX(), destPlayer.getY(), destPlayer.getZ());
                                        BodycamDummyEntity.DUMMY_POS.put(dummyOwner.getUUID(), destPlayer.position());
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

                                String dimStr = targetPlayer.getPersistentData().contains("bodycam_orig_dim")
                                        ? targetPlayer.getPersistentData().getString("bodycam_orig_dim")
                                        : (targetPlayer.getPersistentData().contains("bodycam_original_dimension")
                                            ? targetPlayer.getPersistentData().getString("bodycam_original_dimension")
                                            : null);

                                if (targetPlayer.getPersistentData().contains("bodycam_dummy_uuid") && dimStr != null) {
                                    UUID dummyId = targetPlayer.getPersistentData().getUUID("bodycam_dummy_uuid");
                                    ResourceKey<Level> dimKey = ResourceKey.create(
                                        Registries.DIMENSION,
                                        new ResourceLocation(dimStr)
                                    );
                                    ServerLevel targetLevel = context.getSource().getServer().getLevel(dimKey);

                                    if (targetLevel != null) {
                                        Entity dummy = targetLevel.getEntity(dummyId);
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
                                BodycamResetCameraPacket.executeReset(p);
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
                                        BodycamResetCameraPacket.executeReset(p);
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

    private static int executeReload(CommandSourceStack source) {
        boolean serverOk = ModServerConfig.reloadFromDisk();
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ReloadClientConfigS2CPacket());
        if (serverOk) {
            source.sendSuccess(() -> Component.translatable("command.bodycam.reload.success").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("command.bodycam.reload.failed"));
            return 0;
        }
    }

    private static int executeEmergencyReset(CommandSourceStack source, ServerPlayer target) {
        boolean wasActive = target.getPersistentData().getBoolean("bodycam_active")
                || target.getPersistentData().contains("bodycam_dummy_uuid")
                || target.getPersistentData().contains("bodycam_target_uuid");

        BodycamResetCameraPacket.executeReset(target);

        for (ServerLevel lvl : target.server.getAllLevels()) {
            List<BodycamDummyEntity> toDiscard = new ArrayList<>();
            for (Entity entity : lvl.getAllEntities()) {
                if (entity instanceof BodycamDummyEntity dummy
                        && !(dummy instanceof WatchPointDummyEntity)
                        && target.getUUID().equals(dummy.getOwnerUUID())) {
                    toDiscard.add(dummy);
                }
            }
            for (BodycamDummyEntity dummy : toDiscard) {
                dummy.discard();
            }
        }

        if (!wasActive && target.isSpectator()) {
            int savedGm = target.getPersistentData().contains("bodycam_original_gamemode")
                    ? target.getPersistentData().getInt("bodycam_original_gamemode")
                    : GameType.SURVIVAL.getId();
            GameType restored = GameType.byId(savedGm);
            target.setGameMode(restored != null && restored != GameType.SPECTATOR ? restored : GameType.SURVIVAL);
        }

        target.setCamera(target);
        target.setInvisible(false);
        NightspyEffectHandler.clear(target);
        target.getPersistentData().remove("bodycam_active");
        target.getPersistentData().remove("bodycam_watching_watchpoint");
        target.getPersistentData().remove("bodycam_target_uuid");
        target.getPersistentData().remove("bodycam_dummy_uuid");
        target.getPersistentData().remove("bodycam_disconnect_ticks");
        target.getPersistentData().remove("bodycam_reach_level");
        target.getPersistentData().remove("bodycam_original_dimension");
        target.getPersistentData().remove("bodycam_original_gamemode");
        target.getPersistentData().remove("bodycam_orig_dim");
        target.getPersistentData().remove("bodycam_orig_x");
        target.getPersistentData().remove("bodycam_orig_y");
        target.getPersistentData().remove("bodycam_orig_z");
        target.getPersistentData().remove("bodycam_orig_yrot");
        target.getPersistentData().remove("bodycam_orig_xrot");
        ServerEvents.POSITION_LOCKS.remove(target.getUUID());

        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> target),
                new BodycamForceClosePacket()
        );

        source.sendSuccess(() -> Component.translatable("command.bodycam.reset.success", target.getName().getString())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int executeStatus(CommandSourceStack source, ServerPlayer target) {
        MinecraftServer server = source.getServer();

        String observingDesc = Component.translatable("command.bodycam.status.none").getString();
        if (target.getPersistentData().getBoolean("bodycam_active")
                && target.getPersistentData().contains("bodycam_target_uuid")) {
            UUID observedId = target.getPersistentData().getUUID("bodycam_target_uuid");
            if (target.getPersistentData().getBoolean("bodycam_watching_watchpoint")) {
                WatchPointDummyEntity wp = WatchPointDummyEntity.findByUUID(server, observedId);
                observingDesc = wp != null ? "Watch Point: " + wp.getSelectionDisplayName() : "Watch Point (" + observedId.toString().substring(0, 8) + ")";
            } else {
                ServerPlayer observedPlayer = server.getPlayerList().getPlayer(observedId);
                observingDesc = observedPlayer != null ? observedPlayer.getName().getString() : observedId.toString().substring(0, 8);
            }
        }

        List<String> observers = new ArrayList<>();
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (online.getPersistentData().getBoolean("bodycam_active")
                    && online.getPersistentData().contains("bodycam_target_uuid")
                    && target.getUUID().equals(online.getPersistentData().getUUID("bodycam_target_uuid"))) {
                observers.add(online.getName().getString());
            }
        }
        String observersStr = observers.isEmpty()
                ? Component.translatable("command.bodycam.status.none").getString()
                : String.join(", ", observers);

        int personalJammer = 0;
        boolean personalAnonymizer = false;
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            ItemStack s = target.getInventory().getItem(i);
            if (s.getItem() instanceof JammerItem && !ItemDurabilityHelper.isEmpty(s) && s.hasTag()) {
                personalJammer = PodestBlockEntity.combineJammerModes(personalJammer, s.getTag().getInt("JammerMode"));
            }
            if (s.getItem() instanceof AnonymizerItem && !ItemDurabilityHelper.isEmpty(s) && s.hasTag() && s.getTag().getBoolean("AnonymizerActive")) {
                personalAnonymizer = true;
            }
        }
        for (ItemStack curio : CuriosCompat.getEquippedItems(target, ModItems.JAMMER.get())) {
            if (!ItemDurabilityHelper.isEmpty(curio) && curio.hasTag()) {
                personalJammer = PodestBlockEntity.combineJammerModes(personalJammer, curio.getTag().getInt("JammerMode"));
            }
        }
        for (ItemStack curio : CuriosCompat.getEquippedItems(target, ModItems.ANONYMIZER.get())) {
            if (!ItemDurabilityHelper.isEmpty(curio) && curio.hasTag() && curio.getTag().getBoolean("AnonymizerActive")) {
                personalAnonymizer = true;
            }
        }

        int podestJammer = PodestBlockEntity.getPodestJammerMode(target);
        int effectiveJammer = PodestBlockEntity.combineJammerModes(personalJammer, podestJammer);
        boolean podestAnonymizer = PodestBlockEntity.hasPodestAnonymizer(target);
        boolean effectiveAnonymizer = personalAnonymizer || podestAnonymizer;
        int anonymityLevel = EnchantmentLevels.activeAnonymity(target);
        boolean isProtected = effectiveJammer > 0 || effectiveAnonymizer || anonymityLevel > 0;

        String jammerText = switch (effectiveJammer) {
            case 1 -> "On" + (podestJammer == 1 && personalJammer != 1 ? " (Podest)" : "");
            case 2 -> "Limited" + (podestJammer == 2 && personalJammer == 0 ? " (Podest)" : "");
            default -> "Off";
        };
        String anonText = (effectiveAnonymizer ? "On" : "Off")
                + (podestAnonymizer && !personalAnonymizer ? " (Podest)" : "")
                + (anonymityLevel == 1 ? " [Anonymity]" : (anonymityLevel >= 2 ? " [True Anonymity]" : ""));

        final String finalObserving = observingDesc;
        source.sendSuccess(() -> Component.translatable("command.bodycam.status.header", target.getName().getString())
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.translatable("command.bodycam.status.observing", finalObserving)
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.translatable("command.bodycam.status.observed_by", observersStr)
                .withStyle(observers.isEmpty() ? ChatFormatting.GRAY : ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.translatable("command.bodycam.status.jammer", jammerText)
                .withStyle(effectiveJammer > 0 ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.translatable("command.bodycam.status.anonymizer", anonText)
                .withStyle(effectiveAnonymizer ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.translatable("command.bodycam.status.protected", isProtected ? "Yes" : "No")
                .withStyle(isProtected ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int executeWatchPointList(CommandSourceStack source, ServerPlayer ownerFilter) {
        List<WatchPointDummyEntity> list = ownerFilter != null
                ? WatchPointDummyEntity.getActiveForOwner(source.getServer(), ownerFilter.getUUID())
                : WatchPointDummyEntity.getAllActive(source.getServer());

        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.bodycam.watchpoint.none")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.bodycam.watchpoint.list_header", list.size())
                .withStyle(ChatFormatting.GOLD), false);

        List<WatchPointDummyEntity> allActive = WatchPointDummyEntity.getAllActive(source.getServer());
        for (WatchPointDummyEntity wp : list) {
            int globalIndex = allActive.indexOf(wp) + 1;
            String shortId = wp.getUUID().toString().substring(0, 8);
            BlockPos pos = wp.getWatchPointPos() != null ? wp.getWatchPointPos() : wp.blockPosition();
            String dim = wp.level().dimension().location().toString();
            String ownerName = wp.getOwnerName() != null && !wp.getOwnerName().isEmpty() ? wp.getOwnerName() : "Unknown";
            String name = wp.getSelectionDisplayName();

            MutableComponent entry = Component.literal("#" + globalIndex + " [" + shortId + "] ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + ownerName + ") ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " [" + dim + "]")
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.GREEN)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bodycam watchpoint tp " + shortId))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("/bodycam watchpoint tp " + shortId)))));
            source.sendSuccess(() -> entry, false);
        }
        return list.size();
    }

    private static WatchPointDummyEntity resolveWatchPoint(MinecraftServer server, String idQuery) {
        if (idQuery == null || idQuery.isBlank()) {
            return null;
        }
        String trimmed = idQuery.trim();
        List<WatchPointDummyEntity> all = WatchPointDummyEntity.getAllActive(server);
        try {
            int index = Integer.parseInt(trimmed);
            if (index >= 1 && index <= all.size()) {
                return all.get(index - 1);
            }
        } catch (NumberFormatException ignored) {
        }
        try {
            UUID exactUuid = UUID.fromString(trimmed);
            WatchPointDummyEntity byUuid = WatchPointDummyEntity.findByUUID(server, exactUuid);
            if (byUuid != null) {
                return byUuid;
            }
        } catch (IllegalArgumentException ignored) {
        }
        String lower = trimmed.toLowerCase();
        for (WatchPointDummyEntity wp : all) {
            if (wp.getUUID().toString().toLowerCase().startsWith(lower)) {
                return wp;
            }
            if (wp.getWatchPointName() != null && wp.getWatchPointName().equalsIgnoreCase(trimmed)) {
                return wp;
            }
            if (wp.getSelectionDisplayName().equalsIgnoreCase(trimmed)) {
                return wp;
            }
        }
        return null;
    }

    private static int executeWatchPointRemoveAll(CommandSourceStack source) {
        List<WatchPointDummyEntity> all = WatchPointDummyEntity.getAllActive(source.getServer());
        if (all.isEmpty()) {
            source.sendFailure(Component.translatable("command.bodycam.watchpoint.none"));
            return 0;
        }
        for (WatchPointDummyEntity wp : all) {
            wp.discard();
        }
        int count = all.size();
        source.sendSuccess(() -> Component.translatable("command.bodycam.watchpoint.removed_all", count)
                .withStyle(ChatFormatting.GREEN), true);
        return count;
    }

    private static int executeWatchPointRemovePos(CommandSourceStack source, BlockPos pos) {
        ServerLevel level = source.getLevel();
        if (level.getBlockEntity(pos) instanceof WatchPointBlockEntity be) {
            be.despawnDummy(level);
            source.sendSuccess(() -> Component.translatable("command.bodycam.watchpoint.removed_pos",
                    pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        for (WatchPointDummyEntity wp : WatchPointDummyEntity.getAllActive(source.getServer())) {
            BlockPos wpPos = wp.getWatchPointPos();
            if (pos.equals(wpPos) || pos.equals(wp.blockPosition())) {
                String name = wp.getSelectionDisplayName();
                wp.discard();
                source.sendSuccess(() -> Component.translatable("command.bodycam.watchpoint.removed", name)
                        .withStyle(ChatFormatting.GREEN), true);
                return 1;
            }
        }
        source.sendFailure(Component.translatable("command.bodycam.watchpoint.not_found",
                pos.getX() + " " + pos.getY() + " " + pos.getZ()));
        return 0;
    }

    private static int executeWatchPointRemoveById(CommandSourceStack source, String idQuery) {
        WatchPointDummyEntity wp = resolveWatchPoint(source.getServer(), idQuery);
        if (wp == null) {
            source.sendFailure(Component.translatable("command.bodycam.watchpoint.not_found", idQuery));
            return 0;
        }
        String name = wp.getSelectionDisplayName();
        wp.discard();
        source.sendSuccess(() -> Component.translatable("command.bodycam.watchpoint.removed", name)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int executeWatchPointTp(CommandSourceStack source, String idQuery) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WatchPointDummyEntity wp = resolveWatchPoint(source.getServer(), idQuery);
        if (wp == null || !(wp.level() instanceof ServerLevel targetLevel)) {
            source.sendFailure(Component.translatable("command.bodycam.watchpoint.not_found", idQuery));
            return 0;
        }
        String name = wp.getSelectionDisplayName();
        player.teleportTo(targetLevel, wp.getX(), wp.getY(), wp.getZ(), wp.getYRot(), wp.getXRot());
        source.sendSuccess(() -> Component.translatable("command.bodycam.watchpoint.teleported", name)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int giveItem(CommandSourceStack source, ServerPlayer target, String itemName) {
        Item item = switch (itemName) {
            case "bodycam_monitor" -> ModItems.BODYCAM_MONITOR.get();
            case "observation_crystal" -> ModItems.OBSERVATION_CRYSTAL.get();
            case "jammer" -> ModItems.JAMMER.get();
            case "dimension_locator" -> ModItems.DIMENSION_LOCATOR.get();
            case "anonymizer" -> ModItems.ANONYMIZER.get();
            case "hologram_block" -> ModItems.HOLOGRAM_BLOCK_ITEM.get();
            case "player_locator_compass" -> ModItems.PLAYER_LOCATOR_COMPASS.get();
            case "observation_crystal_block" -> ModItems.OBSERVATION_CRYSTAL_BLOCK_ITEM.get();
            case "ancient_table" -> ModItems.ANCIENT_TABLE_ITEM.get();
            case "portable_nether_portal" -> ModItems.PORTABLE_NETHER_PORTAL.get();
            case "watch_point" -> ModItems.WATCH_POINT_ITEM.get();
            case "podest" -> ModItems.PODEST_ITEM.get();
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

    private static BodycamDummyEntity getDummyEntity(ServerPlayer player) {
        if (player == null) return null;
        if (!player.getPersistentData().getBoolean("bodycam_active")) return null;
        if (!player.getPersistentData().contains("bodycam_dummy_uuid")) return null;
        UUID dummyId = player.getPersistentData().getUUID("bodycam_dummy_uuid");
        for (ServerLevel lvl : player.server.getAllLevels()) {
            Entity e = lvl.getEntity(dummyId);
            if (e instanceof BodycamDummyEntity dummy) {
                return dummy;
            }
        }
        return null;
    }
}
