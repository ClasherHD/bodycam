package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.locator.DimensionLocatorResponsePacket;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DimensionLocatorItem extends Item {

    public DimensionLocatorItem(Properties properties) {
        super(properties);
    }

    private static void syncConfiguredMaxDamage(ItemStack stack) {
        if (!ModServerConfig.isLoaded() || stack == null || stack.isEmpty() || !stack.has(DataComponents.MAX_DAMAGE)) {
            return;
        }
        int configured = ItemDurabilityHelper.getDimensionLocatorMaxDurability();
        int currentMax = stack.getOrDefault(DataComponents.MAX_DAMAGE, configured);
        if (configured > 0 && currentMax != configured) {
            boolean wasEmpty = stack.getDamageValue() >= currentMax;
            stack.set(DataComponents.MAX_DAMAGE, configured);
            if (wasEmpty || stack.getDamageValue() > configured) {
                stack.setDamageValue(configured);
            }
        }
    }

    public static Map<UUID, String> collectPlayerDimensions(ServerPlayer observer) {
        Map<UUID, String> dims = new HashMap<>();
        MinecraftServer server = observer != null ? observer.level().getServer() : null;
        if (server == null) {
            return dims;
        }
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (!onlinePlayer.getUUID().equals(observer.getUUID()) && !EnchantmentLevels.isHiddenFromSelection(onlinePlayer)) {
                CompoundTag pData = PersistentDataAccess.get(onlinePlayer);
                String origDim = pData.contains("bodycam_orig_dim")
                        ? pData.getStringOr("bodycam_orig_dim", "")
                        : pData.getStringOr("bodycam_original_dimension", "");
                if (pData.contains("bodycam_target_uuid") && !origDim.isEmpty()) {
                    dims.put(onlinePlayer.getUUID(), origDim);
                } else {
                    dims.put(onlinePlayer.getUUID(), onlinePlayer.level().dimension().identifier().toString());
                }
                if (EnchantmentLevels.isDimensionScrambled(onlinePlayer)) {
                    dims.put(onlinePlayer.getUUID(), EnchantmentLevels.SCRAMBLED_DIMENSION);
                }
            }
        }
        return dims;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        syncConfiguredMaxDamage(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            syncConfiguredMaxDamage(stack);
        }
        if (ItemDurabilityHelper.isEmpty(stack)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sPlayer) {
            if (ModServerConfig.OP_ONLY_MODE.get() && !sPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                sPlayer.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (ModServerConfig.isDimensionBlacklisted(level)) {
                sPlayer.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (!ModServerConfig.ENABLE_DIMENSION_LOCATOR.get()) {
                sPlayer.sendSystemMessage(Component.translatable("message.bodycam.locator_disabled").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            ItemDurabilityHelper.damagePerUse(stack, sPlayer, 1);
            PacketHandler.sendToPlayer(sPlayer, new DimensionLocatorResponsePacket(collectPlayerDimensions(sPlayer)));
        }
        return InteractionResult.SUCCESS;
    }

    public static void handleSelectDimension(ServerPlayer player, String dimension) {
        if (player == null || dimension == null) {
            return;
        }
        Identifier dimId = Identifier.tryParse(dimension);
        MinecraftServer server = player.level().getServer();
        if (dimId == null || server == null) {
            return;
        }
        ServerLevel targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (targetLevel == null || ModServerConfig.isDimensionBlacklisted(targetLevel)) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack target = main.getItem() instanceof DimensionLocatorItem ? main : (off.getItem() instanceof DimensionLocatorItem ? off : ItemStack.EMPTY);
        if (!target.isEmpty() && !ItemDurabilityHelper.isEmpty(target)) {
            String normalizedDim = dimId.toString();
            ItemNbtAccess.putString(target, "SelectedDimension", normalizedDim);
            player.sendSystemMessage(Component.translatable("message.bodycam.dimension_selected", normalizedDim).withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        if (!player.level().isClientSide()) {
            syncConfiguredMaxDamage(stack);
        }
        ItemDurabilityHelper.setEmpty(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged() && !ItemDurabilityHelper.isEmpty(stack);
    }
}
