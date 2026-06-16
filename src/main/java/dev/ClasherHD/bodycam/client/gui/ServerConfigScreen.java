package dev.ClasherHD.bodycam.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class ServerConfigScreen extends Screen {
    private final Screen previous;
    private final dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket initialValues;

    private EditBox txtMaxDistance;
    private boolean reachVal;
    private boolean jammerVal;
    private boolean locatorVal;
    private boolean hologramVal;
    private boolean anonymizerVal;
    private boolean playerLocatorVal;
    private boolean opOnlyVal;

    private Button btnReach;
    private Button btnJammer;
    private Button btnLocator;
    private Button btnHologram;
    private Button btnAnonymizer;
    private Button btnPlayerLocator;
    private Button btnOpOnly;

    private Button btnBack;
    private Button btnNext;
    private Button btnReset;
    private Button btnSave;

    private double scrollY = 0;
    private int maxScroll = 80;
    private int currentPage = 1;

    public ServerConfigScreen(Screen previous, dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket values) {
        super(Component.translatable("gui.bodycam.server_config.title"));
        this.previous = previous;
        this.initialValues = values;

        this.reachVal = values.enableReachEnchantment;
        this.jammerVal = values.enableJammer;
        this.locatorVal = values.enableDimensionLocator;
        this.hologramVal = values.enableHologramBlock;
        this.anonymizerVal = values.enableAnonymizer;
        this.playerLocatorVal = values.enablePlayerLocator;
        this.opOnlyVal = values.opOnlyMode;
    }

    private Component getToggleButtonMessage(String labelKey, boolean value) {
        Component state = Component.translatable(value ? "options.on" : "options.off")
                .withStyle(value ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED);
        return Component.translatable(labelKey)
                .append(Component.literal(": "))
                .append(state);
    }

    @Override
    protected void init() {
        this.scrollY = 0;

        this.txtMaxDistance = new EditBox(this.font, this.width / 2 - 100, 0, 200, 20, Component.translatable("gui.bodycam.server_config.max_distance"));
        this.txtMaxDistance.setMaxLength(8);
        this.txtMaxDistance.setValue(String.valueOf(this.initialValues.maxMonitorDistance));
        this.addRenderableWidget(this.txtMaxDistance);

        this.btnReach = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_reach", this.reachVal), btn -> {
            this.reachVal = !this.reachVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_reach", this.reachVal));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnReach);

        this.btnJammer = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_jammer", this.jammerVal), btn -> {
            this.jammerVal = !this.jammerVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_jammer", this.jammerVal));
        }).bounds(this.width / 2 + 5, 0, 200, 20).build();
        this.addRenderableWidget(this.btnJammer);

        this.btnLocator = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_locator", this.locatorVal), btn -> {
            this.locatorVal = !this.locatorVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_locator", this.locatorVal));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnLocator);

        this.btnHologram = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_hologram", this.hologramVal), btn -> {
            this.hologramVal = !this.hologramVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_hologram", this.hologramVal));
        }).bounds(this.width / 2 + 5, 0, 200, 20).build();
        this.addRenderableWidget(this.btnHologram);

        this.btnAnonymizer = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_anonymizer", this.anonymizerVal), btn -> {
            this.anonymizerVal = !this.anonymizerVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_anonymizer", this.anonymizerVal));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnAnonymizer);

        this.btnPlayerLocator = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_player_locator", this.playerLocatorVal), btn -> {
            this.playerLocatorVal = !this.playerLocatorVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_player_locator", this.playerLocatorVal));
        }).bounds(this.width / 2 + 5, 0, 200, 20).build();
        this.addRenderableWidget(this.btnPlayerLocator);

        this.btnOpOnly = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.op_only", this.opOnlyVal), btn -> {
            this.opOnlyVal = !this.opOnlyVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.op_only", this.opOnlyVal));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnOpOnly);

        this.btnBack = Button.builder(Component.translatable("gui.bodycam.back"), btn -> {
            if (this.currentPage > 1) {
                this.currentPage--;
                this.updateVisibility();
            }
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnBack);

        this.btnNext = Button.builder(Component.translatable("gui.bodycam.next"), btn -> {
            if (this.currentPage < 2) {
                this.currentPage++;
                this.updateVisibility();
            }
        }).bounds(this.width / 2 + 5, 0, 200, 20).build();
        this.addRenderableWidget(this.btnNext);

        this.btnReset = Button.builder(Component.translatable("gui.bodycam.config.reset"), btn -> {
            this.txtMaxDistance.setValue("500");
            this.reachVal = true;
            this.jammerVal = true;
            this.locatorVal = true;
            this.hologramVal = true;
            this.anonymizerVal = true;
            this.playerLocatorVal = true;
            this.opOnlyVal = false;

            this.btnReach.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_reach", this.reachVal));
            this.btnJammer.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_jammer", this.jammerVal));
            this.btnLocator.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_locator", this.locatorVal));
            this.btnHologram.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_hologram", this.hologramVal));
            this.btnAnonymizer.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_anonymizer", this.anonymizerVal));
            this.btnPlayerLocator.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_player_locator", this.playerLocatorVal));
            this.btnOpOnly.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.op_only", this.opOnlyVal));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnReset);

        this.btnSave = Button.builder(Component.translatable("gui.done"), btn -> {
            int maxDist = this.initialValues.maxMonitorDistance;
            try {
                maxDist = Integer.parseInt(this.txtMaxDistance.getValue());
            } catch (NumberFormatException e) {
            }
            dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.sendToServer(
                    new dev.ClasherHD.bodycam.network.config.SaveServerConfigC2SPacket(
                            maxDist, this.reachVal, this.jammerVal, this.locatorVal, this.hologramVal, this.anonymizerVal, this.playerLocatorVal, this.opOnlyVal
                    )
            );
            this.onClose();
        }).bounds(this.width / 2 + 5, 0, 200, 20).build();
        this.addRenderableWidget(this.btnSave);

        this.updateLayout();
        this.updateVisibility();
    }

    private void updateVisibility() {
        boolean page1 = (this.currentPage == 1);
        boolean page2 = (this.currentPage == 2);

        this.txtMaxDistance.visible = page1;
        this.btnReach.visible = page1;
        this.btnJammer.visible = page1;
        this.btnLocator.visible = page1;
        this.btnHologram.visible = page1;
        this.btnAnonymizer.visible = page1;
        this.btnPlayerLocator.visible = page1;

        this.btnOpOnly.visible = page2;

        this.btnBack.active = page2;
        this.btnNext.active = page1;
    }

    private void updateLayout() {
        int yOffset = 49 + (int) this.scrollY;

        this.txtMaxDistance.setY(yOffset + 12);

        this.btnReach.setY(yOffset + 47);
        this.btnJammer.setY(yOffset + 47);

        this.btnLocator.setY(yOffset + 73);
        this.btnHologram.setY(yOffset + 73);

        this.btnAnonymizer.setY(yOffset + 99);
        this.btnPlayerLocator.setY(yOffset + 99);

        this.btnOpOnly.setY(yOffset + 47);

        this.btnBack.setY(yOffset + 125);
        this.btnNext.setY(yOffset + 125);

        this.btnReset.setY(yOffset + 155);
        this.btnSave.setY(yOffset + 155);
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

        pGuiGraphics.pose().pushPose();
        float scale = 1.5f;
        pGuiGraphics.pose().scale(scale, scale, 1.0f);
        float titleX = (this.width / 2f) / scale;
        float titleY = (20f + (float) this.scrollY) / scale;
        pGuiGraphics.drawCenteredString(this.font, this.title, (int) titleX, (int) titleY, 16777215);
        pGuiGraphics.pose().popPose();

        int y = 49 + (int) this.scrollY;
        if (this.currentPage == 1) {
            pGuiGraphics.drawString(this.font, Component.translatable("gui.bodycam.server_config.max_distance"), this.width / 2 - 100, y, 10526880);
        }

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previous);
        }
    }
}
