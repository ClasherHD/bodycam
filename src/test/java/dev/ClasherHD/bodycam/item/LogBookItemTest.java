package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class LogBookItemTest {

    private static LogBookItem item;

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        item = (LogBookItem) ModItems.LOG_BOOK.get();
    }

    @Test
    @DisplayName("LogBook properties should have max stack size of 1")
    void properties_haveExpectedDefaults() {
        ItemStack stack = new ItemStack(item);
        assertThat(stack.getMaxStackSize()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000", "1234", "9999", "5432"})
    @DisplayName("isValidPin should return true for valid 4-digit PIN strings")
    void isValidPin_validNumeric4Digits_returnsTrue(String pin) {
        assertThat(LogBookItem.isValidPin(pin)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "12345", "abcd", "12a4", " 123", "123 ", "!@#$", "----"})
    @DisplayName("isValidPin should return false for invalid PIN strings")
    void isValidPin_invalidStrings_returnsFalse(String pin) {
        assertThat(LogBookItem.isValidPin(pin)).isFalse();
    }

    @Test
    @DisplayName("isValidPin should return false for null")
    void isValidPin_null_returnsFalse() {
        assertThat(LogBookItem.isValidPin(null)).isFalse();
    }

    @Test
    @DisplayName("hasPin and checkPin should accurately reflect PIN state")
    void pin_stateAndChecking_behaveAsExpected() {
        ItemStack book = new ItemStack(item);
        assertThat(LogBookItem.hasPin(book)).isFalse();
        assertThat(LogBookItem.checkPin(book, null)).isTrue();
        assertThat(LogBookItem.checkPin(book, "1234")).isTrue();

        // Simulate set pin via tag
        CompoundTag tag = book.getOrCreateTag();
        // hash for "1234" is sha256("bodycam:logbook:1234")
        // We can use handleSetPin logic directly or check checkPin with the hash
        String hash;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest("bodycam:logbook:1234".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            hash = sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        tag.putString(LogBookItem.TAG_PIN, hash);
        assertThat(LogBookItem.hasPin(book)).isTrue();
        assertThat(LogBookItem.checkPin(book, "1234")).isTrue();
        assertThat(LogBookItem.checkPin(book, "9999")).isFalse();
        assertThat(LogBookItem.checkPin(book, "abcd")).isFalse();
        assertThat(LogBookItem.checkPin(book, null)).isFalse();
    }

    @Test
    @DisplayName("pageCount, addPage, and MAX_PAGES cap should operate correctly")
    void pages_addAndMaxLimit() {
        ItemStack book = new ItemStack(item);
        assertThat(LogBookItem.pageCount(book)).isEqualTo(0);

        for (int i = 0; i < LogBookItem.MAX_PAGES; i++) {
            CompoundTag page = new CompoundTag();
            page.putString("Author", "Tester" + i);
            boolean added = LogBookItem.addPage(book, page);
            assertThat(added).isTrue();
            assertThat(LogBookItem.pageCount(book)).isEqualTo(i + 1);
        }

        // Exceeding MAX_PAGES should be rejected
        CompoundTag extraPage = new CompoundTag();
        extraPage.putString("Author", "Overflow");
        boolean overflowAdded = LogBookItem.addPage(book, extraPage);
        assertThat(overflowAdded).isFalse();
        assertThat(LogBookItem.pageCount(book)).isEqualTo(LogBookItem.MAX_PAGES);
    }

    @Test
    @DisplayName("removeLastPage should return removed page and decrement count")
    void removeLastPage_removesAndDecrements() {
        ItemStack book = new ItemStack(item);
        assertThat(LogBookItem.removeLastPage(book)).isNull();

        CompoundTag page1 = new CompoundTag();
        page1.putString("Author", "Page1");
        CompoundTag page2 = new CompoundTag();
        page2.putString("Author", "Page2");

        LogBookItem.addPage(book, page1);
        LogBookItem.addPage(book, page2);
        assertThat(LogBookItem.pageCount(book)).isEqualTo(2);

        CompoundTag removed = LogBookItem.removeLastPage(book);
        assertThat(removed).isNotNull();
        assertThat(removed.getString("Author")).isEqualTo("Page2");
        assertThat(LogBookItem.pageCount(book)).isEqualTo(1);

        CompoundTag removedFirst = LogBookItem.removeLastPage(book);
        assertThat(removedFirst).isNotNull();
        assertThat(removedFirst.getString("Author")).isEqualTo("Page1");
        assertThat(LogBookItem.pageCount(book)).isEqualTo(0);

        assertThat(LogBookItem.removeLastPage(book)).isNull();
    }

    @Test
    @DisplayName("buildViewData should extract pages tag")
    void buildViewData_extractsPages() {
        ItemStack book = new ItemStack(item);
        CompoundTag emptyView = LogBookItem.buildViewData(book);
        assertThat(emptyView.getList(LogBookItem.TAG_PAGES, 10)).isEmpty();

        CompoundTag page = new CompoundTag();
        page.putString("Text", "Hello");
        LogBookItem.addPage(book, page);

        CompoundTag view = LogBookItem.buildViewData(book);
        assertThat(view.getList(LogBookItem.TAG_PAGES, 10)).hasSize(1);
    }

    @Test
    @DisplayName("createPaperFromPage should create LogPaperItem stack with page data")
    void createPaperFromPage_createsItemWithTag() {
        CompoundTag page = new CompoundTag();
        page.putString("Author", "Player1");
        page.putString("CustomField", "Value");

        ItemStack paper = LogBookItem.createPaperFromPage(page);
        assertThat(paper.getItem()).isInstanceOf(LogPaperItem.class);
        assertThat(paper.hasTag()).isTrue();
        assertThat(paper.getTag().getString("Author")).isEqualTo("Player1");
        assertThat(paper.getTag().getString("CustomField")).isEqualTo("Value");
    }

    @Test
    @DisplayName("appendHoverText should show page count and locked status")
    void appendHoverText_displaysPagesAndLockStatus() {
        ItemStack book = new ItemStack(item);
        List<Component> tooltip = new ArrayList<>();
        item.appendHoverText(book, null, tooltip, TooltipFlag.NORMAL);

        assertThat(tooltip).anyMatch(c -> c.getString().contains("tooltip.bodycam.log_book.pages"));

        book.getOrCreateTag().putString(LogBookItem.TAG_PIN, "dummyhash");
        List<Component> lockedTooltip = new ArrayList<>();
        item.appendHoverText(book, null, lockedTooltip, TooltipFlag.NORMAL);
        assertThat(lockedTooltip).anyMatch(c -> c.getString().contains("tooltip.bodycam.log_book.locked"));
    }

    @Test
    @DisplayName("clearStaticState should clear cooldown tracking without error")
    void clearStaticState_cleansUp() {
        LogBookItem.clearStaticState();
    }
}
