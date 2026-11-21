package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookPinScreenS2CPacket;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookScreenS2CPacket;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.ModAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@SuppressWarnings("null")
public class LogBookItem extends Item {

    public static final String TAG_PAGES = "Pages";
    public static final String TAG_PIN = "Pin";
    public static final int MAX_PAGES = 10;
    public static final int PIN_LENGTH = 4;
    public static final int ATTEMPT_COOLDOWN_TICKS = 20;

    public static final Map<UUID, Long> LAST_ATTEMPT = new ConcurrentHashMap<>();

    public LogBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int pageCount(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        return tag != null ? tag.getListOrEmpty(TAG_PAGES).size() : 0;
    }

    public static boolean hasPin(ItemStack stack) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        return tag != null && !tag.getStringOr(TAG_PIN, "").isEmpty();
    }

    public static boolean isValidPin(String pin) {
        if (pin == null || pin.length() != PIN_LENGTH) {
            return false;
        }
        for (int i = 0; i < PIN_LENGTH; i++) {
            char c = pin.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(("bodycam:logbook:" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return pin;
        }
    }

    public static boolean checkPin(ItemStack stack, String pin) {
        if (!hasPin(stack)) {
            return true;
        }
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        return tag != null && isValidPin(pin) && tag.getStringOr(TAG_PIN, "").equals(hashPin(pin));
    }

    public static boolean addPage(ItemStack book, CompoundTag pageData) {
        if (pageCount(book) >= MAX_PAGES) {
            return false;
        }
        ItemNbtAccess.updateTag(book, tag -> {
            ListTag pages = tag.getListOrEmpty(TAG_PAGES);
            pages.add(pageData.copy());
            tag.put(TAG_PAGES, pages);
        });
        return true;
    }

    public static CompoundTag removeLastPage(ItemStack book) {
        if (pageCount(book) <= 0) {
            return null;
        }
        CompoundTag readTag = ItemNbtAccess.getReadOnlyTag(book);
        if (readTag == null) {
            return null;
        }
        ListTag readPages = readTag.getListOrEmpty(TAG_PAGES);
        if (readPages.isEmpty()) {
            return null;
        }
        CompoundTag removed = readPages.getCompoundOrEmpty(readPages.size() - 1).copy();
        ItemNbtAccess.updateTag(book, tag -> {
            ListTag pages = tag.getListOrEmpty(TAG_PAGES);
            if (!pages.isEmpty()) {
                pages.remove(pages.size() - 1);
                tag.put(TAG_PAGES, pages);
            }
        });
        return removed;
    }

    public static CompoundTag buildViewData(ItemStack book) {
        CompoundTag data = new CompoundTag();
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(book);
        ListTag pages = tag != null ? tag.getListOrEmpty(TAG_PAGES).copy() : new ListTag();
        data.put(TAG_PAGES, pages);
        return data;
    }

    public static void sendBook(ServerPlayer player, ItemStack book) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        PacketHandler.sendToPlayer(player, new OpenLogBookScreenS2CPacket(buildViewData(book)));
    }

    public static void clearStaticState() {
        LAST_ATTEMPT.clear();
    }

    public static void clearPlayerCooldown(UUID playerId) {
        if (playerId != null) {
            LAST_ATTEMPT.remove(playerId);
        }
    }

    public static boolean isOnCooldown(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = LAST_ATTEMPT.get(player.getUUID());
        if (last != null && now - last < ATTEMPT_COOLDOWN_TICKS) {
            return true;
        }
        LAST_ATTEMPT.put(player.getUUID(), now);
        return false;
    }

    public static void handleUnlock(ServerPlayer player, ItemStack book, String pin, boolean offHand) {
        handleUnlock(player, book, pin);
    }

    public static void handleUnlock(ServerPlayer player, ItemStack book, String pin) {
        if (isOnCooldown(player)) {
            return;
        }
        if (checkPin(book, pin)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_UNLOCK.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
            sendBook(player, book);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_WRONG_PIN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.wrong_pin").withStyle(ChatFormatting.RED));
        }
    }

    public static void handleSetPin(ServerPlayer player, ItemStack book, String oldPin, String newPin) {
        if (isOnCooldown(player)) {
            return;
        }
        boolean hadPin = hasPin(book);
        if (!hadPin && pageCount(book) == 0) {
            player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.empty_no_pin").withStyle(ChatFormatting.RED));
            return;
        }
        if (hadPin && !checkPin(book, oldPin)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_WRONG_PIN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.wrong_pin").withStyle(ChatFormatting.RED));
            return;
        }
        if (newPin == null || newPin.isEmpty()) {
            if (hadPin) {
                ItemNbtAccess.remove(book, TAG_PIN);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_UNLOCK.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
                player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.pin_removed").withStyle(ChatFormatting.YELLOW));
            }
            return;
        }
        if (!isValidPin(newPin)) {
            player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.pin_invalid").withStyle(ChatFormatting.RED));
            return;
        }
        ItemNbtAccess.putString(book, TAG_PIN, hashPin(newPin));
        ModAdvancements.award(player, ModAdvancements.LOG_BOOK_ENCRYPTED);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_LOCK.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
        player.sendOverlayMessage(Component.translatable(hadPin ? "message.bodycam.logbook.pin_changed" : "message.bodycam.logbook.pin_set").withStyle(ChatFormatting.GREEN));
    }

    public static ItemStack createPaperFromPage(CompoundTag page) {
        ItemStack paper = new ItemStack(ModItems.LOG_PAPER.get());
        ItemNbtAccess.setTag(paper, page.copy());
        return paper;
    }

    public static void handleCartographyInteraction(ServerPlayer player, ItemStack book, ItemStack other) {
        if (hasPin(book)) {
            player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.locked").withStyle(ChatFormatting.RED));
            return;
        }
        if (other.getItem() instanceof LogPaperItem) {
            if (pageCount(book) >= MAX_PAGES) {
                player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.full").withStyle(ChatFormatting.RED));
                return;
            }
            CompoundTag readOther = ItemNbtAccess.getReadOnlyTag(other);
            CompoundTag page = readOther != null ? readOther.copy() : new CompoundTag();
            addPage(book, page);
            if (!player.isCreative()) {
                other.shrink(1);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.page_added", pageCount(book), MAX_PAGES).withStyle(ChatFormatting.GREEN));
        } else {
            CompoundTag removed = removeLastPage(book);
            if (removed == null) {
                player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.no_pages").withStyle(ChatFormatting.RED));
                return;
            }
            ItemStack paper = createPaperFromPage(removed);
            if (!player.getInventory().add(paper)) {
                player.drop(paper, false);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.sendOverlayMessage(Component.translatable("message.bodycam.logbook.page_removed", pageCount(book), MAX_PAGES).withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sPlayer) {
            boolean offHand = hand == InteractionHand.OFF_HAND;
            if (sPlayer.isShiftKeyDown()) {
                if (!hasPin(stack) && pageCount(stack) == 0) {
                    sPlayer.sendOverlayMessage(Component.translatable("message.bodycam.logbook.empty_no_pin").withStyle(ChatFormatting.RED));
                    return InteractionResult.SUCCESS;
                }
                PacketHandler.sendToPlayer(sPlayer,
                        new OpenLogBookPinScreenS2CPacket(hasPin(stack) ? OpenLogBookPinScreenS2CPacket.MODE_CHANGE : OpenLogBookPinScreenS2CPacket.MODE_SET, offHand));
            } else if (hasPin(stack)) {
                PacketHandler.sendToPlayer(sPlayer,
                        new OpenLogBookPinScreenS2CPacket(OpenLogBookPinScreenS2CPacket.MODE_UNLOCK, offHand));
            } else {
                sendBook(sPlayer, stack);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        tooltipAdder.accept(Component.translatable("tooltip.bodycam.log_book.pages", pageCount(stack), MAX_PAGES).withStyle(ChatFormatting.GRAY));
        if (hasPin(stack)) {
            tooltipAdder.accept(Component.translatable("tooltip.bodycam.log_book.locked").withStyle(ChatFormatting.RED));
        }
    }
}
