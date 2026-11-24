package dev.ClasherHD.bodycam.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.creepingdeath.OpenCreepingDeathScreenS2CPacket;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@SuppressWarnings("null")
public class CreepingDeathItem extends Item {

    public static final String TAG_SKIN_UUID = "SkinUUID";
    public static final String TAG_SKIN_NAME = "SkinName";
    public static final int USER_PARTICLE_TICKS = 20;

    private static final long KNOWN_PLAYERS_CACHE_TTL_MS = 30_000L;
    private static final AtomicBoolean REFRESHING = new AtomicBoolean(false);
    private static volatile Map<UUID, String> cachedOfflinePlayers = Map.of();
    private static volatile long lastOfflineScanMs = 0L;

    public CreepingDeathItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean hasCustomSkin(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        return tag != null && PersistentDataAccess.hasUUID(tag, TAG_SKIN_UUID);
    }

    public static UUID getSkinUUID(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag != null && PersistentDataAccess.hasUUID(tag, TAG_SKIN_UUID)) {
            return PersistentDataAccess.getUUID(tag, TAG_SKIN_UUID);
        }
        return null;
    }

    public static String getSkinName(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        return tag != null ? tag.getStringOr(TAG_SKIN_NAME, "") : "";
    }

    public static void setSkin(ItemStack stack, UUID skinUUID, String skinName) {
        if (skinUUID == null || skinName == null || skinName.isEmpty()) {
            clearSkin(stack);
            return;
        }
        ItemNbtAccess.updateTag(stack, tag -> {
            PersistentDataAccess.putUUID(tag, TAG_SKIN_UUID, skinUUID);
            tag.putString(TAG_SKIN_NAME, skinName);
        });
    }

    public static void clearSkin(ItemStack stack) {
        ItemNbtAccess.updateTag(stack, tag -> {
            tag.remove(TAG_SKIN_UUID);
            tag.remove(TAG_SKIN_NAME);
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId())
                .withStyle(style -> style.withColor(0x666670).withBold(true));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sPlayer)) {
            return InteractionResult.PASS;
        }

        if (ModServerConfig.OP_ONLY_MODE.get()
                && !sPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (ModServerConfig.isDimensionBlacklisted(level)) {
            sPlayer.sendSystemMessage(
                    Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!ModServerConfig.ENABLE_CREEPING_DEATH.get()) {
            sPlayer.sendSystemMessage(
                    Component.translatable("message.bodycam.creeping_death_disabled").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        MinecraftServer server = sPlayer.level().getServer();
        boolean offHand = (hand == InteractionHand.OFF_HAND);
        if (sPlayer.isShiftKeyDown()) {
            List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> skinEntries = collectAllKnownPlayers(server);
            PacketHandler.sendToPlayer(sPlayer, new OpenCreepingDeathScreenS2CPacket(
                    OpenCreepingDeathScreenS2CPacket.MODE_SKIN,
                    offHand,
                    skinEntries,
                    getSkinUUID(stack)));
            return InteractionResult.SUCCESS;
        }

        List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> targetEntries = new ArrayList<>();
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other.getUUID().equals(sPlayer.getUUID())) {
                continue;
            }
            if (other.level().dimension() != sPlayer.level().dimension()) {
                continue;
            }
            if (other.isCreative() || other.isSpectator() || !other.isAlive()) {
                continue;
            }
            boolean alreadyHunted = CreepingDeathEntity.isTargetAlreadyHunted(server, other.getUUID());
            targetEntries.add(new OpenCreepingDeathScreenS2CPacket.PlayerEntryData(
                    other.getUUID(),
                    other.getGameProfile().name(),
                    alreadyHunted));
        }
        targetEntries.sort(Comparator.comparing(e -> e.name().toLowerCase()));

        PacketHandler.sendToPlayer(sPlayer, new OpenCreepingDeathScreenS2CPacket(
                OpenCreepingDeathScreenS2CPacket.MODE_TARGET,
                offHand,
                targetEntries,
                getSkinUUID(stack)));
        return InteractionResult.SUCCESS;
    }

    public static void invalidateKnownPlayersCache() {
        cachedOfflinePlayers = Map.of();
        lastOfflineScanMs = 0L;
    }

    public static void clearCache() {
        invalidateKnownPlayersCache();
    }

    public static List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> collectAllKnownPlayers(
            MinecraftServer server) {
        Map<UUID, String> known = new LinkedHashMap<>();

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            known.put(online.getUUID(), online.getGameProfile().name());
        }

        long nowMs = System.currentTimeMillis();
        if (lastOfflineScanMs == 0L || nowMs - lastOfflineScanMs >= KNOWN_PLAYERS_CACHE_TTL_MS) {
            refreshOfflinePlayersAsync(server);
        }
        cachedOfflinePlayers.forEach(known::putIfAbsent);

        List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> result = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : known.entrySet()) {
            result.add(new OpenCreepingDeathScreenS2CPacket.PlayerEntryData(entry.getKey(), entry.getValue(), false));
        }
        result.sort(Comparator.comparing(e -> e.name().toLowerCase()));
        return result;
    }

    public static void refreshOfflinePlayersAsync(MinecraftServer server) {
        if (server == null || !REFRESHING.compareAndSet(false, true)) {
            return;
        }
        Path userCacheFile = server.getServerDirectory().resolve("usercache.json");
        Util.ioPool().execute(() -> {
            try {
                cachedOfflinePlayers = scanUserCacheFile(userCacheFile);
                lastOfflineScanMs = System.currentTimeMillis();
            } finally {
                REFRESHING.set(false);
            }
        });
    }

    public static Map<UUID, String> scanUserCacheFile(Path userCacheFile) {
        Map<UUID, String> known = new LinkedHashMap<>();
        try {
            if (userCacheFile != null && Files.isRegularFile(userCacheFile)) {
                try (BufferedReader reader = Files.newBufferedReader(userCacheFile, StandardCharsets.UTF_8)) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (root != null && root.isJsonArray()) {
                        JsonArray arr = root.getAsJsonArray();
                        for (JsonElement el : arr) {
                            if (el != null && el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                if (obj.has("uuid") && obj.has("name")) {
                                    UUID id = UUID.fromString(obj.get("uuid").getAsString());
                                    String name = obj.get("name").getAsString();
                                    if (!name.isEmpty()) {
                                        known.putIfAbsent(id, name);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return Map.copyOf(known);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        tooltipAdder.accept(
                Component.translatable("tooltip.bodycam.creeping_death.desc").withStyle(ChatFormatting.DARK_GRAY));
        if (hasCustomSkin(stack)) {
            tooltipAdder.accept(Component.translatable("tooltip.bodycam.creeping_death.skin", getSkinName(stack))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltipAdder.accept(Component.translatable("tooltip.bodycam.creeping_death.skin_default")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
