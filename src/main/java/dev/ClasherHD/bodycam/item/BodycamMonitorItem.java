package dev.ClasherHD.bodycam.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class BodycamMonitorItem extends Item {
    public BodycamMonitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            boolean hasReach = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    dev.ClasherHD.bodycam.bodycam.REACH_ENCHANTMENT.get(), player.getItemInHand(hand)) > 0;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.sendToServer(new dev.ClasherHD.bodycam.network.SyncBodycamRequestC2SPacket(hasReach, false));
            });
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
