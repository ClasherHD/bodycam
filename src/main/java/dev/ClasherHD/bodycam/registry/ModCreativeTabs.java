package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
                        output.accept(ModItems.BODYCAM_MONITOR.get());
                        output.accept(ModItems.PLAYER_LOCATOR_COMPASS.get());
                        output.accept(ModItems.DIMENSION_LOCATOR.get());
                        output.accept(ModItems.JAMMER.get());
                        output.accept(ModItems.ANONYMIZER.get());
                        output.accept(ModItems.HOLOGRAM_BLOCK_ITEM.get());
                    })
                    .build());
}
