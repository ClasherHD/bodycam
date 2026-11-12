package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookPinScreenS2CPacket;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookS2CPacket;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class LogBookItem extends Item {

    public static final String TAG_PAGES = "Pages";
    public static final String TAG_PIN = "Pin";
    public static final int MAX_PAGES = 10;
    public static final int PIN_LENGTH = 4;
    public static final int ATTEMPT_COOLDOWN_TICKS = 20;

    private static final Map<UUID, Long> LAST_ATTEMPT = new ConcurrentHashMap<>();

    public LogBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int pageCount(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getList(TAG_PAGES, 10).size() : 0;
    }

    public static boolean hasPin(ItemStack stack) {
        return stack.hasTag() && !stack.getTag().getString(TAG_PIN).isEmpty();
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
        return isValidPin(pin) && stack.getTag().getString(TAG_PIN).equals(hashPin(pin));
    }

    public static boolean addPage(ItemStack book, CompoundTag pageData) {
        if (pageCount(book) >= MAX_PAGES) {
            return false;
        }
        CompoundTag tag = book.getOrCreateTag();
        ListTag pages = tag.getList(TAG_PAGES, 10);
        pages.add(pageData.copy());
        tag.put(TAG_PAGES, pages);
        return true;
    }

    public static CompoundTag removeLastPage(ItemStack book) {
        if (pageCount(book) <= 0) {
            return null;
        }
        ListTag pages = book.getTag().getList(TAG_PAGES, 10);
        CompoundTag removed = pages.getCompound(pages.size() - 1).copy();
        pages.remove(pages.size() - 1);
        book.getTag().put(TAG_PAGES, pages);
        return removed;
    }

    public static CompoundTag buildViewData(ItemStack book) {
        CompoundTag data = new CompoundTag();
        ListTag pages = book.hasTag() ? book.getTag().getList(TAG_PAGES, 10).copy() : new ListTag();
        data.put(TAG_PAGES, pages);
        return data;
    }

    public static void sendBook(ServerPlayer player, ItemStack book) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new OpenLogBookS2CPacket(buildViewData(book)));
    }

    public static void clearStaticState() {
        LAST_ATTEMPT.clear();
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

    public static void handleUnlock(ServerPlayer player, ItemStack book, String pin) {
        if (isOnCooldown(player)) {
            return;
        }
        if (checkPin(book, pin)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_UNLOCK.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
            sendBook(player, book);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_WRONG_PIN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable("message.bodycam.logbook.wrong_pin").withStyle(ChatFormatting.RED), true);
        }
    }

    public static void handleSetPin(ServerPlayer player, ItemStack book, String oldPin, String newPin) {
        if (isOnCooldown(player)) {
            return;
        }
        boolean hadPin = hasPin(book);
        if (!hadPin && pageCount(book) == 0) {
            player.displayClientMessage(Component.translatable("message.bodycam.logbook.empty_no_pin").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (hadPin && !checkPin(book, oldPin)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_WRONG_PIN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable("message.bodycam.logbook.wrong_pin").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (newPin == null || newPin.isEmpty()) {
            if (hadPin) {
                book.getTag().remove(TAG_PIN);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_UNLOCK.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
                player.displayClientMessage(Component.translatable("message.bodycam.logbook.pin_removed").withStyle(ChatFormatting.YELLOW), true);
            }
            return;
        }
        if (!isValidPin(newPin)) {
            player.displayClientMessage(Component.translatable("message.bodycam.logbook.pin_invalid").withStyle(ChatFormatting.RED), true);
            return;
        }
        book.getOrCreateTag().putString(TAG_PIN, hashPin(newPin));
        dev.ClasherHD.bodycam.util.ModAdvancements.award(player, dev.ClasherHD.bodycam.util.ModAdvancements.LOG_BOOK_ENCRYPTED);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOG_BOOK_LOCK.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
        player.displayClientMessage(Component.translatable(hadPin ? "message.bodycam.logbook.pin_changed" : "message.bodycam.logbook.pin_set").withStyle(ChatFormatting.GREEN), true);
    }

    public static ItemStack createPaperFromPage(CompoundTag page) {
        ItemStack paper = new ItemStack(ModItems.LOG_PAPER.get());
        paper.setTag(page.copy());
        return paper;
    }

    public static void handleCartographyInteraction(ServerPlayer player, ItemStack book, ItemStack other) {
        if (hasPin(book)) {
            player.displayClientMessage(Component.translatable("message.bodycam.logbook.locked").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (other.getItem() instanceof LogPaperItem) {
            if (pageCount(book) >= MAX_PAGES) {
                player.displayClientMessage(Component.translatable("message.bodycam.logbook.full").withStyle(ChatFormatting.RED), true);
                return;
            }
            CompoundTag page = other.hasTag() ? other.getTag().copy() : new CompoundTag();
            addPage(book, page);
            if (!player.isCreative()) {
                other.shrink(1);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.bodycam.logbook.page_added", pageCount(book), MAX_PAGES).withStyle(ChatFormatting.GREEN), true);
        } else {
            CompoundTag removed = removeLastPage(book);
            if (removed == null) {
                player.displayClientMessage(Component.translatable("message.bodycam.logbook.no_pages").withStyle(ChatFormatting.RED), true);
                return;
            }
            ItemStack paper = createPaperFromPage(removed);
            if (!player.getInventory().add(paper)) {
                player.drop(paper, false);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.bodycam.logbook.page_removed", pageCount(book), MAX_PAGES).withStyle(ChatFormatting.YELLOW), true);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player instanceof ServerPlayer sPlayer) {
            boolean offHand = hand == InteractionHand.OFF_HAND;
            if (sPlayer.isShiftKeyDown()) {
                if (!hasPin(stack) && pageCount(stack) == 0) {
                    sPlayer.displayClientMessage(Component.translatable("message.bodycam.logbook.empty_no_pin").withStyle(ChatFormatting.RED), true);
                    return InteractionResultHolder.sidedSuccess(stack, false);
                }
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sPlayer),
                        new OpenLogBookPinScreenS2CPacket(hasPin(stack) ? OpenLogBookPinScreenS2CPacket.MODE_CHANGE : OpenLogBookPinScreenS2CPacket.MODE_SET, offHand));
            } else if (hasPin(stack)) {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sPlayer),
                        new OpenLogBookPinScreenS2CPacket(OpenLogBookPinScreenS2CPacket.MODE_UNLOCK, offHand));
            } else {
                sendBook(sPlayer, stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bodycam.log_book.pages", pageCount(stack), MAX_PAGES).withStyle(ChatFormatting.GRAY));
        if (hasPin(stack)) {
            tooltip.add(Component.translatable("tooltip.bodycam.log_book.locked").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }
}
