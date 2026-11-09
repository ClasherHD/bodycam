package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.logbook.LogBookActionC2SPacket;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookPinScreenS2CPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

@SuppressWarnings("null")
public class LogBookPinScreen extends Screen {

    private final int mode;
    private final boolean offHand;
    private EditBox primaryBox;
    private EditBox secondaryBox;
    private Button confirmButton;

    public LogBookPinScreen(int mode, boolean offHand) {
        super(Component.translatable(switch (mode) {
            case OpenLogBookPinScreenS2CPacket.MODE_SET -> "gui.bodycam.logbook.set_pin";
            case OpenLogBookPinScreenS2CPacket.MODE_CHANGE -> "gui.bodycam.logbook.change_pin";
            default -> "gui.bodycam.logbook.enter_pin";
        }));
        this.mode = mode;
        this.offHand = offHand;
    }

    private EditBox createPinBox(int x, int y) {
        EditBox box = new EditBox(this.font, x, y, 100, 20, Component.empty());
        box.setMaxLength(4);
        box.setFilter(text -> text.chars().allMatch(Character::isDigit));
        box.setFormatter((text, index) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
        box.setResponder(text -> this.updateConfirm());
        this.addRenderableWidget(box);
        return box;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = this.height / 2 - 50;

        this.primaryBox = this.createPinBox(cx - 50, top + 20);
        this.setInitialFocus(this.primaryBox);

        if (this.mode == OpenLogBookPinScreenS2CPacket.MODE_CHANGE) {
            this.secondaryBox = this.createPinBox(cx - 50, top + 60);
        }

        int buttonsY = top + (this.secondaryBox != null ? 90 : 50);

        this.confirmButton = Button.builder(CommonComponents.GUI_OK, btn -> this.submit(false))
                .bounds(cx - 102, buttonsY, 100, 20).build();
        this.addRenderableWidget(this.confirmButton);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                .bounds(cx + 2, buttonsY, 100, 20).build());

        if (this.mode == OpenLogBookPinScreenS2CPacket.MODE_CHANGE) {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.bodycam.logbook.remove_pin"), btn -> this.submit(true))
                    .bounds(cx - 102, buttonsY + 24, 204, 20).build());
        }

        this.updateConfirm();
    }

    private void updateConfirm() {
        boolean valid = this.primaryBox != null && this.primaryBox.getValue().length() == 4;
        if (this.secondaryBox != null) {
            valid = valid && this.secondaryBox.getValue().length() == 4;
        }
        if (this.confirmButton != null) {
            this.confirmButton.active = valid;
        }
    }

    private void submit(boolean removePin) {
        LogBookActionC2SPacket packet;
        String primary = this.primaryBox.getValue();
        if (this.mode == OpenLogBookPinScreenS2CPacket.MODE_UNLOCK) {
            packet = new LogBookActionC2SPacket(LogBookActionC2SPacket.ACTION_UNLOCK, this.offHand, primary, "");
        } else if (this.mode == OpenLogBookPinScreenS2CPacket.MODE_SET) {
            packet = new LogBookActionC2SPacket(LogBookActionC2SPacket.ACTION_SET_PIN, this.offHand, "", primary);
        } else {
            String newPin = removePin ? "" : this.secondaryBox.getValue();
            packet = new LogBookActionC2SPacket(LogBookActionC2SPacket.ACTION_SET_PIN, this.offHand, primary, newPin);
        }
        PacketHandler.INSTANCE.sendToServer(packet);
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && this.confirmButton != null && this.confirmButton.active) {
            this.submit(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int cx = this.width / 2;
        int top = this.height / 2 - 50;
        graphics.drawCenteredString(this.font, this.title, cx, top - 12, 0xFFFFFF);

        Component primaryLabel = Component.translatable(this.mode == OpenLogBookPinScreenS2CPacket.MODE_SET
                ? "gui.bodycam.logbook.new_pin" : "gui.bodycam.logbook.current_pin");
        if (this.mode == OpenLogBookPinScreenS2CPacket.MODE_UNLOCK) {
            primaryLabel = Component.translatable("gui.bodycam.logbook.pin");
        }
        graphics.drawCenteredString(this.font, primaryLabel, cx, top + 8, 0xA0A0A0);
        if (this.secondaryBox != null) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.bodycam.logbook.new_pin"), cx, top + 48, 0xA0A0A0);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
