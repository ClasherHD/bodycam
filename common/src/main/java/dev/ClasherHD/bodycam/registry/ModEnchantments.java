package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> NIGHTSPY =
            ResourceKey.create(Registries.ENCHANTMENT, bodycam.id("nightspy"));
    public static final ResourceKey<Enchantment> REACH =
            ResourceKey.create(Registries.ENCHANTMENT, bodycam.id("reach"));
    public static final ResourceKey<Enchantment> ANONYMITY =
            ResourceKey.create(Registries.ENCHANTMENT, bodycam.id("anonymity"));

    public static void register() {
    }
}
