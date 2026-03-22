package dev.ClasherHD.bodycam.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;

public class DimensionLocatorScreen extends Screen {

    private static final ResourceLocation WINDOW_TEXTURE = new ResourceLocation("bodycam", "textures/gui/gui_window.png");
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
                if (this.dimensions.containsKey(info.getProfile().getId())) {
                    this.listWidget.addPlayerEntry(new DimensionEntry(info, info.getProfile().getName(), this.dimensions.get(info.getProfile().getId())));
                }
            }
        }
        this.addWidget(this.listWidget);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 64, 64, 512, 512, 3840, 2160);
        
        if (this.listWidget != null) {
            this.listWidget.render(graphics, mouseX, mouseY, partialTick);
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiY + 10, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int rCW, int rCH, int uvCW, int uvCH, int texW, int texH) {
        graphics.blit(texture, x, y, rCW, rCH, 0, 0, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + width - rCW, y, rCW, rCH, texW - uvCW, 0, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x, y + height - rCH, rCW, rCH, 0, texH - uvCH, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + width - rCW, y + height - rCH, rCW, rCH, texW - uvCW, texH - uvCH, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + rCW, y, width - 2 * rCW, rCH, uvCW, 0, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + rCW, y + height - rCH, width - 2 * rCW, rCH, uvCW, texH - uvCH, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(texture, x, y + rCH, rCW, height - 2 * rCH, 0, uvCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(texture, x + width - rCW, y + rCH, rCW, height - 2 * rCH, texW - uvCW, uvCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(texture, x + rCW, y + rCH, width - 2 * rCW, height - 2 * rCH, uvCW, uvCH, texW - 2 * uvCW, texH - 2 * uvCH, texW, texH);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public class DimensionListWidget extends ObjectSelectionList<DimensionEntry> {
        private final int listX;

        public DimensionListWidget(Minecraft mc, int width, int height, int y0, int y1, int itemHeight, int listX) {
            super(mc, width, height, y0, y1, itemHeight);
            this.listX = listX;
            this.setLeftPos(listX);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.listX + this.width - 6;
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        public void addPlayerEntry(DimensionEntry entry) {
            this.addEntry(entry);
        }
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
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            ResourceLocation skin = this.playerInfo.getSkinLocation();
            RenderSystem.setShaderTexture(0, skin);
            graphics.blit(skin, left, top + 4, 32, 32, 8.0F, 8.0F, 8, 8, 64, 64);
            graphics.blit(skin, left, top + 4, 32, 32, 40.0F, 8.0F, 8, 8, 64, 64);
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
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.displayName + " in " + this.dimensionPath);
        }
    }
}
