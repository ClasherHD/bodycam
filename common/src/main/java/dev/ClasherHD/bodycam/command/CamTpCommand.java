package dev.ClasherHD.bodycam.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.ClasherHD.bodycam.util.BodycamHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class CamTpCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("camtp")
                        .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
                                    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");

                                    if (BodycamHelper.getPersistentData(targetPlayer).contains("bodycam_dummy_uuid")
                                            && BodycamHelper.getPersistentData(targetPlayer).contains("bodycam_orig_dim")) {
                                        UUID dummyId = BodycamHelper.getPersistentData(targetPlayer).read("bodycam_dummy_uuid", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
                                        String dimStr = BodycamHelper.getPersistentData(targetPlayer).getStringOr("bodycam_orig_dim", "");
                                        ResourceKey<Level> dimKey = ResourceKey.create(
                                                Registries.DIMENSION,
                                                Identifier.parse(dimStr));
                                        ServerLevel targetLevel = context.getSource().getServer().getLevel(dimKey);

                                        if (targetLevel != null) {
                                            Entity dummy = targetLevel.getEntity(dummyId);
                                            if (dummy != null) {
                                                sourcePlayer.teleportTo(
                                                        targetLevel,
                                                        dummy.getX(),
                                                        dummy.getY(),
                                                        dummy.getZ(),
                                                        java.util.Collections.emptySet(),
                                                        dummy.getYRot(),
                                                        dummy.getXRot(),
                                                        true);
                                                return 1;
                                            }
                                        }
                                    }

                                    sourcePlayer.teleportTo(
                                            (ServerLevel) targetPlayer.level(),
                                            targetPlayer.getX(),
                                            targetPlayer.getY(),
                                            targetPlayer.getZ(),
                                            java.util.Collections.emptySet(),
                                            targetPlayer.getYRot(),
                                            targetPlayer.getXRot(),
                                            true);
                                    return 1;
                                }))
        );
    }
}
