package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(bodycam.MODID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> BODYCAM_TAB = TABS.register("bodycam_tab",
            () -> CreativeTabRegistry.create(builder -> builder
                    .title(Component.translatable("creativetab.bodycam_tab"))
                    .icon(() -> new ItemStack(ModItems.BODYCAM_MONITOR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.OBSERVATION_CRYSTAL.get());
                        output.accept(ModItems.OBSERVATION_CRYSTAL_BLOCK.get());
                        output.accept(ModItems.ANCIENT_TABLE.get());
                        output.accept(ModItems.PODEST.get());
                        output.accept(ModItems.WATCH_POINT.get());
                        output.accept(ModItems.HOLOGRAM_BLOCK.get());
                        output.accept(ModItems.BODYCAM_MONITOR.get());
                        output.accept(ModItems.PLAYER_LOCATOR_COMPASS.get());
                        output.accept(ModItems.DIMENSION_LOCATOR.get());
                        output.accept(ModItems.JAMMER.get());
                        output.accept(ModItems.ANONYMIZER.get());
                        output.accept(ModItems.PORTABLE_NETHER_PORTAL.get());
                        output.accept(ModItems.LOGGER.get());
                        output.accept(ModItems.LOG_PAPER.get());
                        output.accept(ModItems.LOG_BOOK.get());
                        output.accept(AllSeeingEyeItem.createActivatedStack());
                        output.accept(AllSeeingEyeItem.createDeactivatedStack());
                        output.accept(ModItems.CREEPING_DEATH.get());
                        output.accept(ModItems.CHERRY_HOUSE_EXPLORER_MAP.get());
                        output.accept(ModItems.SCHOLAR_SPAWN_EGG.get());
                        var enchantments = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT);
                        enchantments.get(ModEnchantments.REACH).ifPresent(holder -> {
                            for (int level = 1; level <= 2; level++) {
                                output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, level)));
                            }
                        });
                        enchantments.get(ModEnchantments.NIGHTSPY).ifPresent(holder -> {
                            for (int level = 1; level <= 4; level++) {
                                output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, level)));
                            }
                        });
                        enchantments.get(ModEnchantments.ANONYMITY).ifPresent(holder -> {
                            for (int level = 1; level <= 2; level++) {
                                output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, level)));
                            }
                        });
                    })));

    public static final RegistrySupplier<CreativeModeTab> BODYCAM_UNBREAKABLE_TAB = TABS.register("bodycam_unbreakable_tab",
            () -> CreativeTabRegistry.create(builder -> builder
                    .title(Component.translatable("creativetab.bodycam_unbreakable_tab"))
                    .icon(() -> ItemDurabilityHelper.createUnbreakableStack(ModItems.BODYCAM_MONITOR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.BODYCAM_MONITOR.get()));
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.PLAYER_LOCATOR_COMPASS.get()));
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.DIMENSION_LOCATOR.get()));
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.JAMMER.get()));
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.ANONYMIZER.get()));
                        output.accept(ItemDurabilityHelper.createUnbreakableStack(ModItems.LOGGER.get()));
                    })));

    public static void register() {
        TABS.register();
    }
}
