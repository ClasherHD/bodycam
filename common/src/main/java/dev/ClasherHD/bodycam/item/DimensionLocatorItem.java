package dev.ClasherHD.bodycam.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class DimensionLocatorItem extends Item {
    public DimensionLocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sPlayer) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("This feature is restricted to Server Operators.").withStyle(net.minecraft.ChatFormatting.RED), false);
                return InteractionResult.FAIL;
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_DIMENSION_LOCATOR.get()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("The Dimension Locator is disabled on this server.").withStyle(net.minecraft.ChatFormatting.RED), false);
                return InteractionResult.FAIL;
            }
            java.util.Map<java.util.UUID, String> dims = new java.util.HashMap<>();
            for (ServerPlayer p : ((net.minecraft.server.level.ServerLevel) sPlayer.level()).getServer().getPlayerList().getPlayers()) {
                if (p.getUUID().equals(sPlayer.getUUID())) continue;
                String dim = null;
                if (dev.ClasherHD.bodycam.util.BodycamHelper.getPersistentData(p).getBooleanOr("bodycam_active", false)) {
                    dim = dev.ClasherHD.bodycam.util.BodycamHelper.getPersistentData(p).getStringOr("bodycam_orig_dim", "");
                }
                if (dim == null || dim.isEmpty()) {
                    dim = p.level().dimension().identifier().toString();
                }
                dims.put(p.getUUID(), dim);
            }
            dev.architectury.networking.NetworkManager.sendToPlayer(sPlayer, new dev.ClasherHD.bodycam.network.DimensionLocatorResponsePacket(dims));
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
