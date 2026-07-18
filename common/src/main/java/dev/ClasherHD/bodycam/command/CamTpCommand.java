package dev.ClasherHD.bodycam.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.ClasherHD.bodycam.util.BodycamHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class CamTpCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("camtp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
                                    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");

                                    // Try to find the dummy entity first
                                    if (BodycamHelper.getPersistentData(targetPlayer).contains("bodycam_dummy_uuid")
                                            && BodycamHelper.getPersistentData(targetPlayer).contains("bodycam_orig_dim")) {
                                        UUID dummyId = BodycamHelper.getPersistentData(targetPlayer).getUUID("bodycam_dummy_uuid");
                                        String dimStr = BodycamHelper.getPersistentData(targetPlayer).getString("bodycam_orig_dim");
                                        ResourceKey<Level> dimKey = ResourceKey.create(
                                                Registries.DIMENSION,
                                                ResourceLocation.parse(dimStr));
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
                                                        dummy.getXRot());
                                                return 1;
                                            }
                                        }
                                    }

                                    // Fallback: teleport to the player directly
                                    sourcePlayer.teleportTo(
                                            targetPlayer.serverLevel(),
                                            targetPlayer.getX(),
                                            targetPlayer.getY(),
                                            targetPlayer.getZ(),
                                            targetPlayer.getYRot(),
                                            targetPlayer.getXRot());
                                    return 1;
                                }))
        );
    }
}
