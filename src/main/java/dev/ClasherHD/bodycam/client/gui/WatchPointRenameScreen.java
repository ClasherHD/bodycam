package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.watchpoint.RenameWatchPointC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("null")
public class WatchPointRenameScreen extends Screen {

    private static final ResourceLocation WINDOW_TEXTURE = new ResourceLocation("bodycam", "textures/gui/gui_window.png");

    private final BlockPos pos;
    private final String initialName;
    private EditBox nameField;
    private int guiWidth = 220;
    private int guiHeight = 100;
    private int guiX;
    private int guiY;

    public WatchPointRenameScreen(BlockPos pos, String initialName) {
        super(Component.translatable("gui.bodycam.watch_point.rename_title"));
        this.pos = pos;
        this.initialName = initialName == null ? "" : initialName;
    }

    @Override
    protected void init() {
        super.init();
        this.guiX = (this.width - this.guiWidth) / 2;
        this.guiY = (this.height - this.guiHeight) / 2;

        this.nameField = new EditBox(this.font, this.guiX + 20, this.guiY + 34, this.guiWidth - 40, 20, Component.translatable("gui.bodycam.watch_point.rename_title"));
        this.nameField.setMaxLength(32);
        this.nameField.setValue(this.initialName);
        this.nameField.setFocused(true);
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> saveAndClose())
                .bounds(this.guiX + 20, this.guiY + 64, 85, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> onClose())
                .bounds(this.guiX + this.guiWidth - 105, this.guiY + 64, 85, 20)
                .build());
    }

    private void saveAndClose() {
        String name = this.nameField != null ? this.nameField.getValue().trim() : "";
        PacketHandler.INSTANCE.sendToServer(new RenameWatchPointC2SPacket(this.pos, name));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        GuiRenderUtils.drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 32, 32, 32, 32, 256, 256);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiY + 14, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
