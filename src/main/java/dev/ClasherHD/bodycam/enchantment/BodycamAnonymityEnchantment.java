package dev.ClasherHD.bodycam.enchantment;

import dev.ClasherHD.bodycam.item.AnonymizerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

@SuppressWarnings("null")
public class BodycamAnonymityEnchantment extends Enchantment {
    public BodycamAnonymityEnchantment() {
        super(Enchantment.Rarity.VERY_RARE, EnchantmentCategory.BREAKABLE,
                new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND });
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public int getMinCost(int level) {
        return 15 + level * 15;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 50;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public Component getFullname(int level) {
        String key = level >= EnchantmentLevels.TRUE_ANONYMITY_LEVEL
                ? "enchantment.bodycam.true_anonymity"
                : this.getDescriptionId();
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof AnonymizerItem;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return stack.getItem() instanceof AnonymizerItem;
    }
}
