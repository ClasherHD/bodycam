package dev.ClasherHD.bodycam.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DimensionLocatorItem extends Item {

    public DimensionLocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer) {
            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This feature is restricted to Server Operators.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            if (!dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_DIMENSION_LOCATOR.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The Dimension Locator is disabled on this server.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            Map<UUID, String> dims = new HashMap<>();
            ServerPlayer sPlayer = (ServerPlayer) player;
            for (ServerPlayer onlinePlayer : sPlayer.server.getPlayerList().getPlayers()) {
                if (!onlinePlayer.getUUID().equals(player.getUUID())) {
                    if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(onlinePlayer).contains("bodycam_target_uuid") && dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(onlinePlayer).contains("bodycam_original_dimension")) {
                        dims.put(onlinePlayer.getUUID(), dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(onlinePlayer).getString("bodycam_original_dimension"));
                    } else {
                        dims.put(onlinePlayer.getUUID(), onlinePlayer.level().dimension().location().getPath());
                    }
                }
            }
            net.minecraft.network.FriendlyByteBuf outBuf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
            dev.ClasherHD.bodycam.network.DimensionLocatorResponsePacket.encode(new dev.ClasherHD.bodycam.network.DimensionLocatorResponsePacket(dims), outBuf);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sPlayer, dev.ClasherHD.bodycam.network.BodycamPacketIDs.DIMENSION_LOCATOR_PACKET_ID, outBuf);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
