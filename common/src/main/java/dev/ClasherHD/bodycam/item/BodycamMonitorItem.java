package dev.ClasherHD.bodycam.item;


import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BodycamMonitorItem extends Item {
    public BodycamMonitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            boolean hasReach = false;
            if (dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_REACH_ENCHANTMENT.get()) {
                java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> reachEnchant = level.registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, net.minecraft.resources.Identifier.fromNamespaceAndPath("bodycam", "reach")));
                if (reachEnchant.isPresent()) {
                    hasReach = player.getItemInHand(hand).getEnchantments().getLevel(reachEnchant.get()) > 0;
                }
            }
            boolean isOnHologram = level.getBlockState(player.blockPosition().below()).is(dev.ClasherHD.bodycam.Bodycam.HOLOGRAM_BLOCK.get());
            BodycamMonitorClientHelper.sendSyncRequest(hasReach, isOnHologram);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
