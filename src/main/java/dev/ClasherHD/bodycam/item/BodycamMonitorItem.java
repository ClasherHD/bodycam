package dev.ClasherHD.bodycam.item;


import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class BodycamMonitorItem extends Item {
    public BodycamMonitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            boolean hasReach = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    dev.ClasherHD.bodycam.BodycamFabric.REACH_ENCHANTMENT, player.getItemInHand(hand)) > 0;
            net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
            buf.writeInt(0);
            buf.writeBoolean(hasReach);
            buf.writeBoolean(false);
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(dev.ClasherHD.bodycam.network.BodycamPacketIDs.REQUEST_CAMERA_PACKET_ID, buf);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
}
