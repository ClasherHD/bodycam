package dev.ClasherHD.bodycam.neoforge;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@Mod(bodycam.MOD_ID)
public final class BodycamNeoForge {
    public BodycamNeoForge(IEventBus modBus, ModContainer container) {
        bodycam.init();
        NeoForge.EVENT_BUS.addListener(BodycamNeoForge::onAnvilUpdate);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> BodycamNeoForgeClient.init(modBus, container));
    }

    private static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (ItemDurabilityHelper.isRepairableModItem(left) || left.getItem() instanceof PortableNetherPortalItem) {
            if (right.getItem() instanceof EnchantedBookItem || right.isEnchanted()) {
                if (!ItemDurabilityHelper.isBookEnchantable(left, right)) {
                    event.setCanceled(true);
                }
            } else if (ItemStack.isSameItem(left, right)) {
                if (!right.isEnchanted() || !ItemDurabilityHelper.isBookEnchantable(left, right)) {
                    event.setCanceled(true);
                }
            }
        }
    }
}
