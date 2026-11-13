package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.LogPaperItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("null")
public class LogBookScreen extends Screen {

    private static final ResourceLocation BOOK_TEXTURE = bodycam.id("textures/gui/log_book.png");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final float TEXT_SCALE = 0.58F;
    private static final int LINE_STEP = 11;
    private static final int TITLE_COLOR = 0xFF1C2D37;
    private static final int TEXT_COLOR = 0xFF2A2A2A;
    private static final int MUTED_COLOR = 0xFF5A6A75;

    private final ListTag pages;
    private int currentPage = 0;
    private PageButton forwardButton;
    private PageButton backButton;

    public LogBookScreen(CompoundTag data) {
        super(Component.translatable("item.bodycam.log_book"));
        this.pages = data != null ? data.getList("Pages", 10) : new ListTag();
    }

    @Override
    protected void init() {
        int left = (this.width - 192) / 2;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
                .bounds(this.width / 2 - 100, 196, 200, 20).build());
        this.forwardButton = this.addRenderableWidget(new LogPageButton(left + 116, 159, true, btn -> this.turnPage(1), true));
        this.backButton = this.addRenderableWidget(new LogPageButton(left + 43, 159, false, btn -> this.turnPage(-1), true));
        this.updateButtons();
    }

    private void turnPage(int delta) {
        int target = this.currentPage + delta;
        if (target >= 0 && target < Math.max(1, this.pages.size())) {
            this.currentPage = target;
            this.updateButtons();
        }
    }

    private void updateButtons() {
        this.forwardButton.visible = this.currentPage < this.pages.size() - 1;
        this.backButton.visible = this.currentPage > 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 262 || keyCode == 267) {
            this.turnPage(1);
            return true;
        }
        if (keyCode == 263 || keyCode == 266) {
            this.turnPage(-1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - 192) / 2;
        graphics.blit(BOOK_TEXTURE, left, 2, 0, 0, 192, 192);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - 192) / 2;

        int total = Math.max(1, this.pages.size());
        Component indicator = Component.translatable("book.pageIndicator", this.currentPage + 1, total);
        graphics.drawString(this.font, indicator, left - this.font.width(indicator) + 192 - 44, 18, 0xFF4A4A4A, false);

        graphics.pose().pushPose();
        graphics.pose().translate(left + 36, 30, 0);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        this.renderPage(graphics);
        graphics.pose().popPose();
    }

    private void renderPage(GuiGraphics graphics) {
        int line = 0;
        if (this.pages.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.bodycam.logbook.title_empty"), 0, line * LINE_STEP, TITLE_COLOR, false);
            line += 2;
            graphics.drawString(this.font, Component.translatable("gui.bodycam.logbook.no_data"), 0, line * LINE_STEP, TEXT_COLOR, false);
            return;
        }

        CompoundTag page = this.pages.getCompound(this.currentPage);
        ListTag entries = page.getList(LogPaperItem.TAG_ENTRIES, 10);
        String author = page.getString(LogPaperItem.TAG_AUTHOR);
        if (author.isEmpty()) {
            author = "???";
        }

        graphics.drawString(this.font, Component.translatable("gui.bodycam.logbook.sheet", this.currentPage + 1, this.pages.size()), 0, line++ * LINE_STEP, TITLE_COLOR, false);
        graphics.drawString(this.font, "- - - - - - - - - - - - - - - - - -", 0, line++ * LINE_STEP, MUTED_COLOR, false);

        if (entries.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.bodycam.logbook.no_observers"), 0, line++ * LINE_STEP, TEXT_COLOR, false);
        }

        String sameDim = Component.translatable("gui.bodycam.logbook.same_dim").getString();
        String otherDim = Component.translatable("gui.bodycam.logbook.other_dim").getString();
        String near = Component.translatable("gui.bodycam.logbook.near").getString();
        String far = Component.translatable("gui.bodycam.logbook.far").getString();

        for (int i = 0; i < entries.size() && i < 5; i++) {
            CompoundTag entry = entries.getCompound(i);
            String name = this.font.plainSubstrByWidth(entry.getString(LogPaperItem.ENTRY_NAME), 180);
            graphics.drawString(this.font, (i + 1) + ". " + name, 0, line++ * LINE_STEP, TITLE_COLOR, false);
            String time = TIME_FORMAT.format(Instant.ofEpochMilli(entry.getLong(LogPaperItem.ENTRY_TIME)));
            String detail = "   " + time + " | " + (entry.getBoolean(LogPaperItem.ENTRY_FAR) ? far : near)
                    + " | " + (entry.getBoolean(LogPaperItem.ENTRY_SAME_DIM) ? sameDim : otherDim);
            graphics.drawString(this.font, detail, 0, line++ * LINE_STEP, TEXT_COLOR, false);
        }

        int footerLine = Math.max(line, 14);
        graphics.drawString(this.font, "- - - - - - - - - - - - - - - - - -", 0, footerLine * LINE_STEP, MUTED_COLOR, false);
        graphics.drawString(this.font, Component.translatable("gui.bodycam.logbook.logged_by", author), 0, (footerLine + 1) * LINE_STEP, TITLE_COLOR, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class LogPageButton extends PageButton {
        private final boolean forward;

        public LogPageButton(int x, int y, boolean forward, Button.OnPress onPress, boolean playTurnSound) {
            super(x, y, forward, onPress, playTurnSound);
            this.forward = forward;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int u = 0;
            int v = 192;
            if (this.isHoveredOrFocused()) {
                u += 23;
            }
            if (!this.forward) {
                v += 13;
            }
            graphics.blit(BOOK_TEXTURE, this.getX(), this.getY(), u, v, 23, 13);
        }
    }
}
