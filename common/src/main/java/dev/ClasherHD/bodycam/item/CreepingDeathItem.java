package dev.ClasherHD.bodycam.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.creepingdeath.OpenCreepingDeathScreenS2CPacket;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("null")
public class CreepingDeathItem extends Item {

    public static final String TAG_SKIN_UUID = "SkinUUID";
    public static final String TAG_SKIN_NAME = "SkinName";
    public static final int USER_PARTICLE_TICKS = 20;

    private static final long KNOWN_PLAYERS_CACHE_TTL_MS = 30_000L;
    private static volatile Map<UUID, String> cachedOfflinePlayers = Map.of();
    private static volatile long lastOfflineScanMs = 0L;
    private static final AtomicBoolean REFRESHING_OFFLINE_PLAYERS = new AtomicBoolean(false);
    private static final AtomicLong CACHE_GENERATION = new AtomicLong(0L);

    public CreepingDeathItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean hasCustomSkin(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        return tag != null && tag.hasUUID(TAG_SKIN_UUID);
    }

    public static UUID getSkinUUID(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag != null && tag.hasUUID(TAG_SKIN_UUID)) {
            return tag.getUUID(TAG_SKIN_UUID);
        }
        return null;
    }

    public static String getSkinName(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag != null && tag.contains(TAG_SKIN_NAME)) {
            return tag.getString(TAG_SKIN_NAME);
        }
        return "";
    }

    public static void setSkin(ItemStack stack, UUID skinUUID, String skinName) {
        if (skinUUID == null || skinName == null || skinName.isEmpty()) {
            clearSkin(stack);
            return;
        }
        ItemNbtAccess.updateTag(stack, tag -> {
            tag.putUUID(TAG_SKIN_UUID, skinUUID);
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
        return Component.translatable(this.getDescriptionId(stack))
                .withStyle(style -> style.withColor(0x666670).withBold(true));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer sPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (ModServerConfig.OP_ONLY_MODE.get() && !sPlayer.hasPermissions(2)) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
        if (ModServerConfig.isDimensionBlacklisted(level)) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
        if (!ModServerConfig.ENABLE_CREEPING_DEATH.get()) {
            sPlayer.sendSystemMessage(Component.translatable("message.bodycam.creeping_death_disabled").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        boolean offHand = (hand == InteractionHand.OFF_HAND);
        if (sPlayer.isShiftKeyDown()) {
            List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> skinEntries = collectAllKnownPlayers(sPlayer.server);
            PacketHandler.sendToPlayer(sPlayer, new OpenCreepingDeathScreenS2CPacket(
                    OpenCreepingDeathScreenS2CPacket.MODE_SKIN,
                    offHand,
                    skinEntries,
                    getSkinUUID(stack)
            ));
            return InteractionResultHolder.success(stack);
        }

        List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> targetEntries = new ArrayList<>();
        for (ServerPlayer other : sPlayer.server.getPlayerList().getPlayers()) {
            if (other.getUUID().equals(sPlayer.getUUID())) {
                continue;
            }
            if (other.level().dimension() != sPlayer.level().dimension()) {
                continue;
            }
            if (other.isCreative() || other.isSpectator() || !other.isAlive()) {
                continue;
            }
            boolean alreadyHunted = CreepingDeathEntity.isTargetAlreadyHunted(sPlayer.server, other.getUUID());
            targetEntries.add(new OpenCreepingDeathScreenS2CPacket.PlayerEntryData(
                    other.getUUID(),
                    other.getGameProfile().getName(),
                    alreadyHunted
            ));
        }
        targetEntries.sort(Comparator.comparing(e -> e.name().toLowerCase()));

        PacketHandler.sendToPlayer(sPlayer, new OpenCreepingDeathScreenS2CPacket(
                OpenCreepingDeathScreenS2CPacket.MODE_TARGET,
                offHand,
                targetEntries,
                getSkinUUID(stack)
        ));
        return InteractionResultHolder.success(stack);
    }

    public static void invalidateKnownPlayersCache() {
        CACHE_GENERATION.incrementAndGet();
        cachedOfflinePlayers = Map.of();
        lastOfflineScanMs = 0L;
    }

    public static void clearCache() {
        invalidateKnownPlayersCache();
    }

    public static List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> collectAllKnownPlayers(MinecraftServer server) {
        Map<UUID, String> known = new LinkedHashMap<>();

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            known.put(online.getUUID(), online.getGameProfile().getName());
        }

        long nowMs = System.currentTimeMillis();
        if (lastOfflineScanMs == 0L || nowMs - lastOfflineScanMs >= KNOWN_PLAYERS_CACHE_TTL_MS) {
            triggerAsyncOfflineScan(server);
        }
        cachedOfflinePlayers.forEach(known::putIfAbsent);

        List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> result = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : known.entrySet()) {
            result.add(new OpenCreepingDeathScreenS2CPacket.PlayerEntryData(entry.getKey(), entry.getValue(), false));
        }
        result.sort(Comparator.comparing(e -> e.name().toLowerCase()));
        return result;
    }

    private static void triggerAsyncOfflineScan(MinecraftServer server) {
        if (!REFRESHING_OFFLINE_PLAYERS.compareAndSet(false, true)) {
            return;
        }
        final long generation = CACHE_GENERATION.get();
        final Path userCacheFile = server.getServerDirectory().resolve("usercache.json");
        final Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        Util.ioPool().execute(() -> {
            try {
                Map<UUID, String> diskKnown = new LinkedHashMap<>();
                List<UUID> missingProfileIds = new ArrayList<>();
                readOfflineFiles(userCacheFile, playerDataDir, diskKnown, missingProfileIds);
                if (CACHE_GENERATION.get() != generation) {
                    return;
                }
                if (missingProfileIds.isEmpty()) {
                    cachedOfflinePlayers = Map.copyOf(diskKnown);
                    lastOfflineScanMs = System.currentTimeMillis();
                } else {
                    server.execute(() -> {
                        if (CACHE_GENERATION.get() != generation) {
                            return;
                        }
                        if (server.getProfileCache() != null) {
                            for (UUID id : missingProfileIds) {
                                Optional<GameProfile> prof = server.getProfileCache().get(id);
                                if (prof.isPresent() && prof.get().getName() != null && !prof.get().getName().isEmpty()) {
                                    diskKnown.putIfAbsent(id, prof.get().getName());
                                }
                            }
                        }
                        cachedOfflinePlayers = Map.copyOf(diskKnown);
                        lastOfflineScanMs = System.currentTimeMillis();
                    });
                }
            } finally {
                REFRESHING_OFFLINE_PLAYERS.set(false);
            }
        });
    }

    private static void readOfflineFiles(Path userCacheFile, Path playerDataDir, Map<UUID, String> known, List<UUID> missingProfileIds) {
        try {
            if (Files.isRegularFile(userCacheFile)) {
                try (BufferedReader reader = Files.newBufferedReader(userCacheFile, StandardCharsets.UTF_8)) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (root.isJsonArray()) {
                        JsonArray arr = root.getAsJsonArray();
                        for (JsonElement el : arr) {
                            if (el.isJsonObject()) {
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

        try {
            if (Files.isDirectory(playerDataDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(playerDataDir, "*.dat")) {
                    for (Path entry : stream) {
                        String fileName = entry.getFileName().toString();
                        if (fileName.endsWith(".dat")) {
                            String uuidStr = fileName.substring(0, fileName.length() - 4);
                            try {
                                UUID id = UUID.fromString(uuidStr);
                                if (!known.containsKey(id)) {
                                    missingProfileIds.add(id);
                                }
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bodycam.creeping_death.desc").withStyle(ChatFormatting.DARK_GRAY));
        if (hasCustomSkin(stack)) {
            tooltip.add(Component.translatable("tooltip.bodycam.creeping_death.skin", getSkinName(stack)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.bodycam.creeping_death.skin_default").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
