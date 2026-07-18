package dev.ClasherHD.bodycam.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DimensionLocatorItem extends Item {
    public DimensionLocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sPlayer) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This feature is restricted to Server Operators.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_DIMENSION_LOCATOR.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The Dimension Locator is disabled on this server.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            java.util.Map<java.util.UUID, String> dims = new java.util.HashMap<>();
            for (ServerPlayer p : sPlayer.server.getPlayerList().getPlayers()) {
                if (p.getUUID().equals(sPlayer.getUUID())) continue;
                String dim = null;
                if (dev.ClasherHD.bodycam.util.BodycamHelper.getPersistentData(p).getBoolean("bodycam_active")) {
                    dim = dev.ClasherHD.bodycam.util.BodycamHelper.getPersistentData(p).getString("bodycam_orig_dim");
                }
                if (dim == null || dim.isEmpty()) {
                    dim = p.level().dimension().location().toString();
                }
                dims.put(p.getUUID(), dim);
            }
            dev.architectury.networking.NetworkManager.sendToPlayer(sPlayer, new dev.ClasherHD.bodycam.network.DimensionLocatorResponsePacket(dims));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
