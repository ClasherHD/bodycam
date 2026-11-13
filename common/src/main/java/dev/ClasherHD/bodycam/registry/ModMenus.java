package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.menu.AncientTableMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(bodycam.MODID, Registries.MENU);

    public static final RegistrySupplier<MenuType<AncientTableMenu>> ANCIENT_TABLE =
            MENUS.register("ancient_table",
                    () -> MenuRegistry.ofExtended(AncientTableMenu::new));

    public static void register() {
        MENUS.register();
    }
}
