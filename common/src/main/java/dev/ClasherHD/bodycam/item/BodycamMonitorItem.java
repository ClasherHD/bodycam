package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamRequestC2SPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class BodycamMonitorItem extends Item {

    public BodycamMonitorItem(Properties properties) {
        super(properties);
    }

    private static void syncConfiguredMaxDamage(ItemStack stack) {
        if (!ModServerConfig.isLoaded() || stack == null || stack.isEmpty() || !stack.has(DataComponents.MAX_DAMAGE)) {
            return;
        }
        int configured = ItemDurabilityHelper.getSpyglassMaxDurability();
        int currentMax = stack.getOrDefault(DataComponents.MAX_DAMAGE, configured);
        if (configured > 0 && currentMax != configured) {
            boolean wasEmpty = stack.getDamageValue() >= currentMax;
            stack.set(DataComponents.MAX_DAMAGE, configured);
            if (wasEmpty || stack.getDamageValue() > configured) {
                stack.setDamageValue(configured);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide()) {
            syncConfiguredMaxDamage(stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            syncConfiguredMaxDamage(stack);
        }
        if (ItemDurabilityHelper.isEmpty(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp
                && CreepingDeathEntity.blockObservationIfHunted(sp)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPYGLASS_USE,
                SoundSource.PLAYERS, 1.0F, 1.5F);
        if (level.isClientSide()) {
            PacketHandler.sendToServer(new SyncBodycamRequestC2SPacket(false));
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        this.stopUsing(entity);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        this.stopUsing(entity);
    }

    private void stopUsing(LivingEntity entity) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.SPYGLASS_STOP_USING,
                SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged() && !ItemDurabilityHelper.isEmpty(stack);
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
