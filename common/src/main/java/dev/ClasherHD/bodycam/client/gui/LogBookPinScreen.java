package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.logbook.LogBookActionC2SPacket;
import dev.ClasherHD.bodycam.network.logbook.OpenLogBookPinScreenS2CPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

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
        EditBox box = new PinEditBox(this.font, x, y, 100, 20, Component.empty());
        box.setMaxLength(4);
        box.addFormatter((text, index) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
        box.setResponder(text -> this.updateConfirm());
        this.addRenderableWidget(box);
        return box;
    }

    @Override
    protected void init() {
        super.init();
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
        PacketHandler.sendToServer(packet);
        this.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)
                && this.confirmButton != null && this.confirmButton.active) {
            this.submit(false);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int top = this.height / 2 - 50;
        graphics.centeredText(this.font, this.title, cx, top - 12, 0xFFFFFFFF);

        Component primaryLabel = Component.translatable(this.mode == OpenLogBookPinScreenS2CPacket.MODE_SET
                ? "gui.bodycam.logbook.new_pin" : "gui.bodycam.logbook.current_pin");
        if (this.mode == OpenLogBookPinScreenS2CPacket.MODE_UNLOCK) {
            primaryLabel = Component.translatable("gui.bodycam.logbook.pin");
        }
        graphics.centeredText(this.font, primaryLabel, cx, top + 8, 0xFFA0A0A0);
        if (this.secondaryBox != null) {
            graphics.centeredText(this.font, Component.translatable("gui.bodycam.logbook.new_pin"), cx, top + 48, 0xFFA0A0A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class PinEditBox extends EditBox {
        PinEditBox(Font font, int x, int y, int width, int height, Component message) {
            super(font, x, y, width, height, message);
        }

        @Override
        public void insertText(String text) {
            if (text == null) {
                return;
            }
            if (text.isEmpty()) {
                super.insertText("");
                return;
            }
            String digits = filterDigits(text);
            if (!digits.isEmpty()) {
                super.insertText(digits);
            }
        }

        @Override
        public void setValue(String value) {
            super.setValue(value == null ? "" : filterDigits(value));
        }

        private static String filterDigits(String text) {
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (Character.isDigit(c)) {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
