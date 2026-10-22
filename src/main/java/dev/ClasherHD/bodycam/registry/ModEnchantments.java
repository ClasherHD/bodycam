package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.enchantment.BodycamAnonymityEnchantment;
import dev.ClasherHD.bodycam.enchantment.BodycamNightspyEnchantment;
import dev.ClasherHD.bodycam.enchantment.BodycamReachEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, bodycam.MODID);

    public static final RegistryObject<Enchantment> REACH_ENCHANTMENT = ENCHANTMENTS.register("reach",
            () -> new BodycamReachEnchantment());

    public static final RegistryObject<Enchantment> NIGHTSPY_ENCHANTMENT = ENCHANTMENTS.register("nightspy",
            () -> new BodycamNightspyEnchantment());

    public static final RegistryObject<Enchantment> ANONYMITY_ENCHANTMENT = ENCHANTMENTS.register("anonymity",
            () -> new BodycamAnonymityEnchantment());
}
