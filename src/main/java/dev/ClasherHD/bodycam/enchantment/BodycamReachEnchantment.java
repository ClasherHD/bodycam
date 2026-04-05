package dev.ClasherHD.bodycam.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class BodycamReachEnchantment extends Enchantment {
    public BodycamReachEnchantment() {
        super(Enchantment.Rarity.RARE, EnchantmentCategory.BREAKABLE,
                new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND });
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getMinCost(int level) {
        return 15;
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public boolean canEnchant(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof dev.ClasherHD.bodycam.item.BodycamMonitorItem;
    }
}
