package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.LogPaperItem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("null")
public class LogBookScreen extends Screen {

    private static final Identifier BOOK_TEXTURE = bodycam.id("textures/gui/log_book.png");
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
        this.pages = data != null ? data.getListOrEmpty("Pages") : new ListTag();
    }

    @Override
    protected void init() {
        super.init();
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
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
            this.turnPage(1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_PAGE_UP) {
            this.turnPage(-1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - 192) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, left, 2, 0.0F, 0.0F, 192, 192, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - 192) / 2;

        int total = Math.max(1, this.pages.size());
        Component indicator = Component.translatable("book.pageIndicator", this.currentPage + 1, total);
        graphics.text(this.font, indicator, left - this.font.width(indicator) + 192 - 44, 18, 0xFF4A4A4A, false);

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (left + 36), 30.0F);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
        this.renderPage(graphics);
        graphics.pose().popMatrix();
    }

    private void renderPage(GuiGraphicsExtractor graphics) {
        int line = 0;
        if (this.pages.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.bodycam.logbook.title_empty"), 0, line * LINE_STEP, TITLE_COLOR, false);
            line += 2;
            graphics.text(this.font, Component.translatable("gui.bodycam.logbook.no_data"), 0, line * LINE_STEP, TEXT_COLOR, false);
            return;
        }

        CompoundTag page = this.pages.getCompoundOrEmpty(this.currentPage);
        ListTag entries = page.getListOrEmpty(LogPaperItem.TAG_ENTRIES);
        String author = page.getStringOr(LogPaperItem.TAG_AUTHOR, "");
        if (author.isEmpty()) {
            author = "???";
        }

        graphics.text(this.font, Component.translatable("gui.bodycam.logbook.sheet", this.currentPage + 1, this.pages.size()), 0, line++ * LINE_STEP, TITLE_COLOR, false);
        graphics.text(this.font, "- - - - - - - - - - - - - - - - - -", 0, line++ * LINE_STEP, MUTED_COLOR, false);

        if (entries.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.bodycam.logbook.no_observers"), 0, line++ * LINE_STEP, TEXT_COLOR, false);
        }

        String sameDim = Component.translatable("gui.bodycam.logbook.same_dim").getString();
        String otherDim = Component.translatable("gui.bodycam.logbook.other_dim").getString();
        String near = Component.translatable("gui.bodycam.logbook.near").getString();
        String far = Component.translatable("gui.bodycam.logbook.far").getString();

        for (int i = 0; i < entries.size() && i < 5; i++) {
            CompoundTag entry = entries.getCompoundOrEmpty(i);
            String name = this.font.plainSubstrByWidth(entry.getStringOr(LogPaperItem.ENTRY_NAME, ""), 180);
            graphics.text(this.font, (i + 1) + ". " + name, 0, line++ * LINE_STEP, TITLE_COLOR, false);
            String time = TIME_FORMAT.format(Instant.ofEpochMilli(entry.getLongOr(LogPaperItem.ENTRY_TIME, 0L)));
            String detail = "   " + time + " | " + (entry.getBooleanOr(LogPaperItem.ENTRY_FAR, false) ? far : near)
                    + " | " + (entry.getBooleanOr(LogPaperItem.ENTRY_SAME_DIM, false) ? sameDim : otherDim);
            graphics.text(this.font, detail, 0, line++ * LINE_STEP, TEXT_COLOR, false);
        }

        int footerLine = Math.max(line, 14);
        graphics.text(this.font, "- - - - - - - - - - - - - - - - - -", 0, footerLine * LINE_STEP, MUTED_COLOR, false);
        graphics.text(this.font, Component.translatable("gui.bodycam.logbook.logged_by", author), 0, (footerLine + 1) * LINE_STEP, TITLE_COLOR, false);
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
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int u = 0;
            int v = 192;
            if (this.isHoveredOrFocused()) {
                u += 23;
            }
            if (!this.forward) {
                v += 13;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, this.getX(), this.getY(), (float) u, (float) v, 23, 13, 256, 256);
        }
    }
}
