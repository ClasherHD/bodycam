package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("null")
public class DimensionLocatorScreen extends Screen {

    private static final Identifier WINDOW_TEXTURE = bodycam.id("textures/gui/gui_window.png");
    private DimensionListWidget listWidget;
    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;
    private final Map<UUID, String> dimensions;

    public DimensionLocatorScreen(Map<UUID, String> dimensions) {
        super(Component.translatable("gui.bodycam.dimension_locator"));
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

        this.listWidget = new DimensionListWidget(this.minecraft, listWidth, listHeight, listY, 40, listX);

        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            for (PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                if (this.dimensions != null && this.dimensions.containsKey(info.getProfile().id())) {
                    this.listWidget.addPlayerEntry(new DimensionEntry(info, info.getProfile().name(), this.dimensions.get(info.getProfile().id())));
                }
            }
        }
        this.addWidget(this.listWidget);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GuiRenderUtils.drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 32, 32, 32, 32, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (this.listWidget != null) {
            this.listWidget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        graphics.centeredText(this.font, this.title, this.width / 2, this.guiY + 10, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public class DimensionListWidget extends ObjectSelectionList<DimensionEntry> {
        private final int listX;

        public DimensionListWidget(Minecraft mc, int width, int height, int y, int itemHeight, int listX) {
            super(mc, width, height, y, itemHeight);
            this.listX = listX;
            this.setX(listX);
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
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
    }

    public class DimensionEntry extends ObjectSelectionList.Entry<DimensionEntry> {
        private final PlayerInfo playerInfo;
        private final String displayName;
        private final String dimensionPath;
        private final String formattedDimensionText;
        private final boolean isScrambled;
        private final int dimColor;

        public DimensionEntry(PlayerInfo playerInfo, String displayName, String dimensionPath) {
            this.playerInfo = playerInfo;
            this.displayName = displayName;
            this.dimensionPath = dimensionPath;
            this.dimColor = 0xFF000000 | ModClientConfig.parseHexColor(ModClientConfig.COLOR_DIMENSION.get(), 0x55FF55);
            this.isScrambled = EnchantmentLevels.SCRAMBLED_DIMENSION.equals(dimensionPath);
            if (this.isScrambled) {
                this.formattedDimensionText = "";
            } else {
                String translationKey = "dimension." + dimensionPath.replace(":", ".");
                MutableComponent dimComponent = Component.translatable(translationKey);
                String dimText = dimComponent.getString();
                if (dimText.equals(translationKey)) {
                    String pathOnly = dimensionPath;
                    if (dimensionPath.contains(":")) {
                        pathOnly = dimensionPath.split(":")[1];
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
                this.formattedDimensionText = dimText;
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            int left = this.getContentX();
            int top = this.getContentY();
            int width = this.getContentWidth();

            Identifier skin = this.playerInfo.getSkin().body().texturePath();
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, left, top + 4, 8.0F, 8.0F, 32, 32, 8, 8, 64, 64);
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, left, top + 4, 40.0F, 8.0F, 32, 32, 8, 8, 64, 64);
            graphics.text(DimensionLocatorScreen.this.font, this.displayName, left + 40, top + 16, 0xFFFFFFFF);
            if (this.isScrambled) {
                Component scrambled = Component.literal("0".repeat(10)).withStyle(ChatFormatting.OBFUSCATED);
                graphics.text(DimensionLocatorScreen.this.font, scrambled, left + width - 10 - DimensionLocatorScreen.this.font.width(scrambled), top + 16, this.dimColor);
                return;
            }
            graphics.text(DimensionLocatorScreen.this.font, this.formattedDimensionText, left + width - 10 - DimensionLocatorScreen.this.font.width(this.formattedDimensionText), top + 16, this.dimColor);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.displayName + " in " + this.dimensionPath);
        }
    }
}
