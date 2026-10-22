package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.block.entity.AncientTableBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.menu.AncientTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("null")
public class AncientTableScreen extends AbstractContainerScreen<AncientTableMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            bodycam.MODID, "textures/gui/container/ancient_table_gui.png");

    private static final int ERROR_ARROW_U = 176;
    private static final int ERROR_ARROW_V = 3;
    private static final int ERROR_ARROW_WIDTH = 28;
    private static final int ERROR_ARROW_HEIGHT = 9;

    private static final int PROGRESS_ARROW_U = 176;
    private static final int PROGRESS_ARROW_V = 13;
    private static final int PROGRESS_ARROW_WIDTH = 28;
    private static final int PROGRESS_ARROW_HEIGHT = 7;

    private static final int FUEL_BAR_U = 176;
    private static final int FUEL_BAR_V = 21;
    private static final int FUEL_BAR_WIDTH = 18;
    private static final int FUEL_BAR_HEIGHT = 4;

    public AncientTableScreen(AncientTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 100;
        this.titleLabelY = 30;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AncientTableMenu.BUTTON_CRAFT);
            }
        }).bounds(leftPos + 51, topPos + 57, 18, 18).build());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int textWidth = font.width(title);
        int centeredX = 104 - textWidth / 2;
        graphics.drawString(font, title, centeredX, titleLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int fuel = menu.getFuel();
        if (fuel > 0) {
            int fuelWidth = Math.round(FUEL_BAR_WIDTH * (fuel / (float) AncientTableBlockEntity.MAX_FUEL));
            int offset = FUEL_BAR_WIDTH - fuelWidth;
            graphics.blit(TEXTURE, leftPos + 106 + offset, topPos + 47, FUEL_BAR_U + offset, FUEL_BAR_V, fuelWidth,
                    FUEL_BAR_HEIGHT);
        }

        if (menu.isShowingError()) {
            graphics.blit(TEXTURE, leftPos + 38, topPos + 23, ERROR_ARROW_U, ERROR_ARROW_V, ERROR_ARROW_WIDTH,
                    ERROR_ARROW_HEIGHT);
        } else {
            float progressRatio = menu.getProgressRatio();
            if (progressRatio > 0.0F) {
                int progressWidth = Math.round(PROGRESS_ARROW_WIDTH * progressRatio);
                graphics.blit(TEXTURE, leftPos + 38, topPos + 24, PROGRESS_ARROW_U, PROGRESS_ARROW_V, progressWidth,
                        PROGRESS_ARROW_HEIGHT);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
