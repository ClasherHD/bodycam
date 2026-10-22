package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, bodycam.MODID);

    public static final RegistryObject<CreativeModeTab> BODYCAM_TAB = CREATIVE_MODE_TABS.register("bodycam_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.BODYCAM_MONITOR.get()))
                    .title(Component.translatable("creativetab.bodycam_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.OBSERVATION_CRYSTAL.get());
                        output.accept(ModItems.OBSERVATION_CRYSTAL_BLOCK_ITEM.get());
                        output.accept(ModItems.ANCIENT_TABLE_ITEM.get());
                        output.accept(ModItems.PODEST_ITEM.get());
                        output.accept(ModItems.WATCH_POINT_ITEM.get());
                        output.accept(ModItems.HOLOGRAM_BLOCK_ITEM.get());
                        output.accept(ModItems.BODYCAM_MONITOR.get());
                        output.accept(ModItems.PLAYER_LOCATOR_COMPASS.get());
                        output.accept(ModItems.DIMENSION_LOCATOR.get());
                        output.accept(ModItems.JAMMER.get());
                        output.accept(ModItems.ANONYMIZER.get());
                        output.accept(ModItems.PORTABLE_NETHER_PORTAL.get());
                        for (int level = 1; level <= 2; level++) {
                            output.accept(EnchantedBookItem.createForEnchantment(
                                    new EnchantmentInstance(ModEnchantments.REACH_ENCHANTMENT.get(), level)));
                        }
                        for (int level = 1; level <= 4; level++) {
                            output.accept(EnchantedBookItem.createForEnchantment(
                                    new EnchantmentInstance(ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), level)));
                        }
                        for (int level = 1; level <= 2; level++) {
                            output.accept(EnchantedBookItem.createForEnchantment(
                                    new EnchantmentInstance(ModEnchantments.ANONYMITY_ENCHANTMENT.get(), level)));
                        }
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> BODYCAM_UNBREAKABLE_TAB = CREATIVE_MODE_TABS.register("bodycam_unbreakable_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(BODYCAM_TAB.getKey())
                    .icon(() -> ItemDurabilityHelper.createUnbreakableStack(ModItems.BODYCAM_MONITOR.get()))
                    .title(Component.translatable("creativetab.bodycam_unbreakable_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.BODYCAM_MONITOR.get()));
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.PLAYER_LOCATOR_COMPASS.get()));
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.DIMENSION_LOCATOR.get()));
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.JAMMER.get()));
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.ANONYMIZER.get()));
                    })
                    .build());
}
