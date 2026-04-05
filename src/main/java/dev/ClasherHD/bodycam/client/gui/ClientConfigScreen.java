package dev.ClasherHD.bodycam.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClientConfigScreen extends Screen {
    private final Screen previous;

    private EditBox colorStandard;
    private EditBox colorBlocked;
    private EditBox colorObserving;
    private EditBox colorDimension;

    private Button btnName, btnHealth, btnShift, btnDone, btnReset;
    private Button btnPreviewStd, btnPreviewBlk, btnPreviewObs, btnPreviewDim;

    private double scrollY = 0;
    private int maxScroll = 210;

    public ClientConfigScreen(Screen previous) {
        super(Component.translatable("gui.bodycam.config.title"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        this.scrollY = 0;

        this.btnName = Button
                .builder(
                        Component.translatable("gui.bodycam.config.name_overlay")
                                .append(Component.literal(
                                        ": " + dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_NAME_OVERLAY.get())),
                        btn -> {
                            dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_NAME_OVERLAY
                                    .set(!dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_NAME_OVERLAY.get());
                            btn.setMessage(Component.translatable("gui.bodycam.config.name_overlay")
                                    .append(Component.literal(": "
                                            + dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_NAME_OVERLAY.get())));
                        })
                .bounds(this.width / 2 - 100, 0, 200, 20).build();
        this.addRenderableWidget(this.btnName);

        this.btnHealth = Button.builder(
                Component.translatable("gui.bodycam.config.health_overlay")
                        .append(Component.literal(
                                ": " + dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_HEALTH_OVERLAY.get())),
                btn -> {
                    dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_HEALTH_OVERLAY
                            .set(!dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_HEALTH_OVERLAY.get());
                    btn.setMessage(Component.translatable("gui.bodycam.config.health_overlay").append(Component
                            .literal(": " + dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_HEALTH_OVERLAY.get())));
                }).bounds(this.width / 2 - 100, 0, 200, 20).build();
        this.addRenderableWidget(this.btnHealth);

        this.btnShift = Button
                .builder(
                        Component.translatable("gui.bodycam.config.shift_overlay")
                                .append(Component.literal(
                                        ": " + dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_SHIFT_OVERLAY.get())),
                        btn -> {
                            dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_SHIFT_OVERLAY
                                    .set(!dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_SHIFT_OVERLAY.get());
                            btn.setMessage(Component.translatable("gui.bodycam.config.shift_overlay")
                                    .append(Component.literal(": "
                                            + dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_SHIFT_OVERLAY.get())));
                        })
                .bounds(this.width / 2 - 100, 0, 200, 20).build();
        this.addRenderableWidget(this.btnShift);

        this.colorStandard = new EditBox(this.font, this.width / 2 - 100, 0, 200, 20,
                Component.translatable("gui.bodycam.config.standard_color"));
        this.colorStandard.setMaxLength(6);
        this.colorStandard.setValue(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_STANDARD.get());
        this.addRenderableWidget(this.colorStandard);

        this.btnPreviewStd = Button.builder(Component.literal("\u25A0"), btn -> applyColorPreview(this.colorStandard))
                .bounds(this.width / 2 + 105, 0, 20, 20).build();
        this.addRenderableWidget(this.btnPreviewStd);

        this.colorBlocked = new EditBox(this.font, this.width / 2 - 100, 0, 200, 20,
                Component.translatable("gui.bodycam.config.blocked_color"));
        this.colorBlocked.setMaxLength(6);
        this.colorBlocked.setValue(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_BLOCKED.get());
        this.addRenderableWidget(this.colorBlocked);

        this.btnPreviewBlk = Button.builder(Component.literal("\u25A0"), btn -> applyColorPreview(this.colorBlocked))
                .bounds(this.width / 2 + 105, 0, 20, 20).build();
        this.addRenderableWidget(this.btnPreviewBlk);

        this.colorObserving = new EditBox(this.font, this.width / 2 - 100, 0, 200, 20,
                Component.translatable("gui.bodycam.config.observing_color"));
        this.colorObserving.setMaxLength(6);
        this.colorObserving.setValue(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_OBSERVING.get());
        this.addRenderableWidget(this.colorObserving);

        this.btnPreviewObs = Button.builder(Component.literal("\u25A0"), btn -> applyColorPreview(this.colorObserving))
                .bounds(this.width / 2 + 105, 0, 20, 20).build();
        this.addRenderableWidget(this.btnPreviewObs);

        this.colorDimension = new EditBox(this.font, this.width / 2 - 100, 0, 200, 20,
                Component.translatable("gui.bodycam.config.dimension_color"));
        this.colorDimension.setMaxLength(6);
        this.colorDimension.setValue(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_DIMENSION.get());
        this.addRenderableWidget(this.colorDimension);

        this.btnPreviewDim = Button.builder(Component.literal("\u25A0"), btn -> applyColorPreview(this.colorDimension))
                .bounds(this.width / 2 + 105, 0, 20, 20).build();
        this.addRenderableWidget(this.btnPreviewDim);

        this.btnReset = Button.builder(Component.translatable("gui.bodycam.config.reset"), btn -> {
            dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_NAME_OVERLAY.set(true);
            dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_HEALTH_OVERLAY.set(true);
            dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_SHIFT_OVERLAY.set(true);

            this.btnName.setMessage(
                    Component.translatable("gui.bodycam.config.name_overlay").append(Component.literal(": true")));
            this.btnHealth.setMessage(
                    Component.translatable("gui.bodycam.config.health_overlay").append(Component.literal(": true")));
            this.btnShift.setMessage(
                    Component.translatable("gui.bodycam.config.shift_overlay").append(Component.literal(": true")));

            this.colorStandard.setValue("FFFFFF");
            this.colorStandard.setTextColor(0xFFFFFFFF);
            dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_STANDARD.set("FFFFFF");

            this.colorBlocked.setValue("FF5555");
            this.colorBlocked.setTextColor(0xFFFF5555);
            dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_BLOCKED.set("FF5555");

            this.colorObserving.setValue("5555FF");
            this.colorObserving.setTextColor(0xFF5555FF);
            dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_OBSERVING.set("5555FF");

            this.colorDimension.setValue("55FF55");
            this.colorDimension.setTextColor(0xFF55FF55);
            dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_DIMENSION.set("55FF55");

            dev.ClasherHD.bodycam.config.ModClientConfig.save();
        }).bounds(this.width / 2 - 100, 0, 200, 20).build();
        this.addRenderableWidget(this.btnReset);

        this.btnDone = Button.builder(Component.translatable("gui.done"), btn -> this.onClose())
                .bounds(this.width / 2 - 100, 0, 200, 20).build();
        this.addRenderableWidget(this.btnDone);

        this.updateLayout();
    }

    private void applyColorPreview(EditBox box) {
        try {
            int color = Integer.parseInt(box.getValue(), 16);
            box.setTextColor(0xFF000000 | color);
        } catch (NumberFormatException e) {
            box.setTextColor(14737632);
        }
    }

    private void updateLayout() {
        int yOffset = 30 + (int) this.scrollY;

        this.btnName.setY(yOffset);
        yOffset += 25;
        this.btnHealth.setY(yOffset);
        yOffset += 25;
        this.btnShift.setY(yOffset);
        yOffset += 35;

        this.colorStandard.setY(yOffset);
        this.btnPreviewStd.setY(yOffset);
        yOffset += 35;

        this.colorBlocked.setY(yOffset);
        this.btnPreviewBlk.setY(yOffset);
        yOffset += 35;

        this.colorObserving.setY(yOffset);
        this.btnPreviewObs.setY(yOffset);
        yOffset += 35;

        this.colorDimension.setY(yOffset);
        this.btnPreviewDim.setY(yOffset);
        yOffset += 45;

        this.btnReset.setY(yOffset);
        yOffset += 25;

        this.btnDone.setY(yOffset);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scrollY += delta * 20;
        this.scrollY = net.minecraft.util.Mth.clamp(this.scrollY, -this.maxScroll, 0);
        this.updateLayout();
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10 + (int) this.scrollY, 16777215);

        int y = 115 + (int) this.scrollY;
        pGuiGraphics.drawString(this.font, Component.translatable("gui.bodycam.config.standard_color"),
                this.width / 2 - 100, y - 10, 10526880);
        y += 35;
        pGuiGraphics.drawString(this.font, Component.translatable("gui.bodycam.config.blocked_color"),
                this.width / 2 - 100, y - 10, 10526880);
        y += 35;
        pGuiGraphics.drawString(this.font, Component.translatable("gui.bodycam.config.observing_color"),
                this.width / 2 - 100, y - 10, 10526880);
        y += 35;
        pGuiGraphics.drawString(this.font, Component.translatable("gui.bodycam.config.dimension_color"),
                this.width / 2 - 100, y - 10, 10526880);

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void onClose() {
        dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_STANDARD.set(this.colorStandard.getValue());
        dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_BLOCKED.set(this.colorBlocked.getValue());
        dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_OBSERVING.set(this.colorObserving.getValue());
        dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_DIMENSION.set(this.colorDimension.getValue());
        dev.ClasherHD.bodycam.config.ModClientConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previous);
        }
    }
}
