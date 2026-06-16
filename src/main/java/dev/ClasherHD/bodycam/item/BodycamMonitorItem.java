package dev.ClasherHD.bodycam.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@SuppressWarnings({"null", "deprecation"})
public class BodycamMonitorItem extends Item {
    public BodycamMonitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.SPYGLASS_USE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
        if (level.isClientSide()) {
            boolean hasReach = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    dev.ClasherHD.bodycam.registry.ModEnchantments.REACH_ENCHANTMENT.get(), player.getItemInHand(hand)) > 0;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE
                        .sendToServer(new dev.ClasherHD.bodycam.network.bodycam.SyncBodycamRequestC2SPacket(hasReach, false));
            });
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
        this.stopUsing(entity);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity, int timeLeft) {
        this.stopUsing(entity);
    }

    private void stopUsing(net.minecraft.world.entity.LivingEntity entity) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                net.minecraft.sounds.SoundEvents.SPYGLASS_STOP_USING,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
}
