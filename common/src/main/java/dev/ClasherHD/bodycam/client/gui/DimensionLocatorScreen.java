package dev.ClasherHD.bodycam.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Map;
import java.util.UUID;

public class DimensionLocatorScreen extends Screen {

    private static final Identifier WINDOW_TEXTURE = Identifier.fromNamespaceAndPath("bodycam", "textures/gui/gui_window.png");
    private DimensionListWidget listWidget;
    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;
    private final Map<UUID, String> dimensions;

    public DimensionLocatorScreen(Map<UUID, String> dimensions) {
        super(Component.literal("Dimension Locator"));
        this.dimensions = dimensions;
    }

    @Override
    protected void init() {
        super.init();
        this.guiWidth = (int) (this.width * 0.7);
        this.guiHeight = (int) (this.height * 0.7);
        this.guiX = (this.width - this.guiWidth) / 2;
        this.guiY = (this.height - this.guiHeight) / 2;

        int listX = this.guiX + 20;
        int listY = this.guiY + 30;
        int listWidth = this.guiWidth - 40;
        int listHeight = this.guiHeight - 50;

        this.listWidget = new DimensionListWidget(this.minecraft, listWidth, this.height, listY, listY + listHeight, 40, listX);
        
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            for (net.minecraft.client.multiplayer.PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                if (this.dimensions.containsKey(info.getProfile().id())) {
                    this.listWidget.addPlayerEntry(new DimensionEntry(info, info.getProfile().name(), this.dimensions.get(info.getProfile().id())));
                }
            }
        }
        this.addWidget(this.listWidget);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 64, 64, 512, 512, 3840, 2160);
        
        if (this.listWidget != null) {
            this.listWidget.render(graphics, mouseX, mouseY, partialTick);
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiY + 10, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawNineSlice(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height, int rCW, int rCH, int uvCW, int uvCH, int texW, int texH) {
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x, y, 0.0F, 0.0F, rCW, rCH, uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + width - rCW, y, (float)(texW - uvCW), 0.0F, rCW, rCH, uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x, y + height - rCH, 0.0F, (float)(texH - uvCH), rCW, rCH, uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + width - rCW, y + height - rCH, (float)(texW - uvCW), (float)(texH - uvCH), rCW, rCH, uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + rCW, y, (float)uvCW, 0.0F, width - 2 * rCW, rCH, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + rCW, y + height - rCH, (float)uvCW, (float)(texH - uvCH), width - 2 * rCW, rCH, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x, y + rCH, 0.0F, (float)uvCH, rCW, height - 2 * rCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + width - rCW, y + rCH, (float)(texW - uvCW), (float)uvCH, rCW, height - 2 * rCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + rCW, y + rCH, (float)uvCW, (float)uvCH, width - 2 * rCW, height - 2 * rCH, texW - 2 * uvCW, texH - 2 * uvCH, texW, texH);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public class DimensionListWidget extends ObjectSelectionList<DimensionEntry> {
        private final int listX;

        public DimensionListWidget(Minecraft mc, int width, int height, int y0, int y1, int itemHeight, int listX) {
            super(mc, width, y1 - y0, y0, itemHeight);
            this.listX = listX;
            this.setX(listX);
        }

        @Override
        protected int scrollBarX() {
            return this.listX + this.width - 6;
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        public void addPlayerEntry(DimensionEntry entry) {
            this.addEntry(entry);
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {}

        @Override
        protected void renderListSeparators(GuiGraphics guiGraphics) {}
    }

    public class DimensionEntry extends ObjectSelectionList.Entry<DimensionEntry> {
        private final net.minecraft.client.multiplayer.PlayerInfo playerInfo;
        private final String displayName;
        private final String dimensionPath;

        public DimensionEntry(net.minecraft.client.multiplayer.PlayerInfo playerInfo, String displayName, String dimensionPath) {
            this.playerInfo = playerInfo;
            this.displayName = displayName;
            this.dimensionPath = dimensionPath;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            int left = this.getContentX();
            int top = this.getContentY();
            int width = this.getContentWidth();

            Identifier skin = this.playerInfo.getSkin().body().texturePath();
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, left, top + 4, 8.0F, 8.0F, 32, 32, 8, 8, 64, 64);
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, left, top + 4, 40.0F, 8.0F, 32, 32, 8, 8, 64, 64);
            graphics.drawString(DimensionLocatorScreen.this.font, this.displayName, left + 40, top + 16, 0xFFFFFFFF);
            String translationKey = "dimension." + this.dimensionPath.replace(":", ".");
            net.minecraft.network.chat.MutableComponent dimComponent = net.minecraft.network.chat.Component.translatable(translationKey);
            String dimText = dimComponent.getString();
            
            if (dimText.equals(translationKey)) {
                String pathOnly = this.dimensionPath;
                if (this.dimensionPath.contains(":")) {
                    pathOnly = this.dimensionPath.split(":")[1];
                }
                String[] words = pathOnly.split("_");
                StringBuilder formatted = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0) {
                        formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
                    }
                }
                dimText = formatted.toString().trim();
            }

            graphics.drawString(DimensionLocatorScreen.this.font, dimText, left + width - 10 - DimensionLocatorScreen.this.font.width(dimText), top + 16, 0xFF000000 | Integer.parseInt(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_DIMENSION.get(), 16));
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean focused) {
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.displayName + " in " + this.dimensionPath);
        }
    }
}
