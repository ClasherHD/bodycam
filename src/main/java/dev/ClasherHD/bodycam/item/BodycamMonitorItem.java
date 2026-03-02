package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.gui.BodycamSelectionScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
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
            boolean hasReach = false;
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            if (mainHand.getItem() instanceof BodycamMonitorItem
                    && net.minecraft.world.item.enchantment.EnchantmentHelper
                            .getItemEnchantmentLevel(bodycam.REACH_ENCHANTMENT, mainHand) > 0) {
                hasReach = true;
            } else if (offHand.getItem() instanceof BodycamMonitorItem
                    && net.minecraft.world.item.enchantment.EnchantmentHelper
                            .getItemEnchantmentLevel(bodycam.REACH_ENCHANTMENT, offHand) > 0) {
                hasReach = true;
            }
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                openScreen(hasReach);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private void openScreen(boolean hasReach) {
        Minecraft.getInstance().setScreen(new BodycamSelectionScreen(hasReach));
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
