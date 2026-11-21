package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.block.entity.AncientTableBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.menu.AncientTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("null")
public class AncientTableScreen extends AbstractContainerScreen<AncientTableMenu> {

    private static final Identifier TEXTURE = bodycam.id("textures/gui/container/ancient_table_gui.png");

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
        super(menu, inventory, title, 176, 166);
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int textWidth = font.width(title);
        int centeredX = 104 - textWidth / 2;
        graphics.text(font, title, centeredX, titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        int fuel = menu.getFuel();
        if (fuel > 0) {
            int fuelWidth = Math.round(FUEL_BAR_WIDTH * (fuel / (float) AncientTableBlockEntity.MAX_FUEL));
            int offset = FUEL_BAR_WIDTH - fuelWidth;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 106 + offset, topPos + 47,
                    (float) (FUEL_BAR_U + offset), (float) FUEL_BAR_V, fuelWidth, FUEL_BAR_HEIGHT, 256, 256);
        }

        if (menu.isShowingError()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 38, topPos + 23,
                    (float) ERROR_ARROW_U, (float) ERROR_ARROW_V, ERROR_ARROW_WIDTH, ERROR_ARROW_HEIGHT, 256, 256);
        } else {
            float progressRatio = menu.getProgressRatio();
            if (progressRatio > 0.0F) {
                int progressWidth = Math.round(PROGRESS_ARROW_WIDTH * progressRatio);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 38, topPos + 24,
                        (float) PROGRESS_ARROW_U, (float) PROGRESS_ARROW_V, progressWidth, PROGRESS_ARROW_HEIGHT, 256, 256);
            }
        }
    }
}
