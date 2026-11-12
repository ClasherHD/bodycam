package dev.ClasherHD.bodycam.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("null")
public class ServerConfigScreen extends Screen {
    private static final ResourceLocation WINDOW_TEXTURE = new ResourceLocation("bodycam", "textures/gui/gui_window.png");

    private final Screen previous;
    private final dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket initialValues;

    private EditBox txtMaxDistance;
    private EditBox txtMaxWatchPointsPlayer;
    private EditBox txtMaxWatchPointsTotal;
    private boolean reachVal;
    private boolean jammerVal;
    private boolean locatorVal;
    private boolean hologramVal;
    private boolean anonymizerVal;
    private boolean playerLocatorVal;
    private boolean watchPointVal;
    private boolean podestVal;
    private boolean opOnlyVal;
    private boolean loggerVal;
    private boolean portablePortalVal;
    private boolean allSeeingEyeVal;
    private boolean creepingDeathVal;
    private boolean scholarCurseVal;
    private boolean creepingDeathNukeVal;

    private Button btnReach;
    private Button btnJammer;
    private Button btnLocator;
    private Button btnHologram;
    private Button btnAnonymizer;
    private Button btnPlayerLocator;
    private Button btnWatchPoint;
    private Button btnPodest;
    private Button btnOpOnly;
    private Button btnLogger;
    private Button btnPortablePortal;
    private Button btnAllSeeingEye;
    private Button btnCreepingDeath;
    private Button btnScholarCurse;
    private Button btnCreepingDeathNuke;

    private Button btnBack;
    private Button btnNext;
    private Button btnReset;
    private Button btnSave;

    private int currentPage = 1;
    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;

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
        this.watchPointVal = values.enableWatchPoint;
        this.podestVal = values.enablePodest;
        this.opOnlyVal = values.opOnlyMode;
        this.loggerVal = values.enableLogger;
        this.portablePortalVal = values.enablePortablePortal;
        this.allSeeingEyeVal = values.enableAllSeeingEye;
        this.creepingDeathVal = values.enableCreepingDeath;
        this.scholarCurseVal = values.enableScholarCurse;
        this.creepingDeathNukeVal = values.enableCreepingDeathNuke;
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
        this.guiWidth = Math.min(430, this.width - 12);
        this.guiHeight = 212;
        this.guiX = (this.width - this.guiWidth) / 2;
        this.guiY = (this.height - this.guiHeight) / 2;

        int colWidth = (this.guiWidth - 30) / 2;
        int leftColX = this.guiX + 12;
        int rightColX = this.guiX + 12 + colWidth + 6;
        int centerColX = this.guiX + (this.guiWidth - colWidth) / 2;

        this.txtMaxDistance = new EditBox(this.font, centerColX, 0, colWidth, 18, Component.translatable("gui.bodycam.server_config.max_distance"));
        this.txtMaxDistance.setMaxLength(8);
        this.txtMaxDistance.setValue(String.valueOf(this.initialValues.maxMonitorDistance));
        this.addRenderableWidget(this.txtMaxDistance);

        this.txtMaxWatchPointsPlayer = new EditBox(this.font, leftColX, 0, colWidth, 18, Component.translatable("gui.bodycam.server_config.max_wp_player"));
        this.txtMaxWatchPointsPlayer.setMaxLength(4);
        this.txtMaxWatchPointsPlayer.setValue(String.valueOf(this.initialValues.maxWatchPointsPerPlayer));
        this.addRenderableWidget(this.txtMaxWatchPointsPlayer);

        this.txtMaxWatchPointsTotal = new EditBox(this.font, rightColX, 0, colWidth, 18, Component.translatable("gui.bodycam.server_config.max_wp_total"));
        this.txtMaxWatchPointsTotal.setMaxLength(5);
        this.txtMaxWatchPointsTotal.setValue(String.valueOf(this.initialValues.maxWatchPointsTotal));
        this.addRenderableWidget(this.txtMaxWatchPointsTotal);

        this.btnReach = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_reach", this.reachVal), btn -> {
            this.reachVal = !this.reachVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_reach", this.reachVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnReach);

        this.btnJammer = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_jammer", this.jammerVal), btn -> {
            this.jammerVal = !this.jammerVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_jammer", this.jammerVal));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnJammer);

        this.btnLocator = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_locator", this.locatorVal), btn -> {
            this.locatorVal = !this.locatorVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_locator", this.locatorVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnLocator);

        this.btnHologram = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_hologram", this.hologramVal), btn -> {
            this.hologramVal = !this.hologramVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_hologram", this.hologramVal));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnHologram);

        this.btnAnonymizer = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_anonymizer", this.anonymizerVal), btn -> {
            this.anonymizerVal = !this.anonymizerVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_anonymizer", this.anonymizerVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnAnonymizer);

        this.btnPlayerLocator = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_player_locator", this.playerLocatorVal), btn -> {
            this.playerLocatorVal = !this.playerLocatorVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_player_locator", this.playerLocatorVal));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnPlayerLocator);

        this.btnLogger = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_logger", this.loggerVal), btn -> {
            this.loggerVal = !this.loggerVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_logger", this.loggerVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnLogger);

        this.btnPortablePortal = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_portable_portal", this.portablePortalVal), btn -> {
            this.portablePortalVal = !this.portablePortalVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_portable_portal", this.portablePortalVal));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnPortablePortal);

        this.btnWatchPoint = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_watch_point", this.watchPointVal), btn -> {
            this.watchPointVal = !this.watchPointVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_watch_point", this.watchPointVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnWatchPoint);

        this.btnPodest = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_podest", this.podestVal), btn -> {
            this.podestVal = !this.podestVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_podest", this.podestVal));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnPodest);

        this.btnAllSeeingEye = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_all_seeing_eye", this.allSeeingEyeVal), btn -> {
            this.allSeeingEyeVal = !this.allSeeingEyeVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_all_seeing_eye", this.allSeeingEyeVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnAllSeeingEye);

        this.btnCreepingDeath = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_creeping_death", this.creepingDeathVal), btn -> {
            this.creepingDeathVal = !this.creepingDeathVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_creeping_death", this.creepingDeathVal));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnCreepingDeath);

        this.btnScholarCurse = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_scholar_curse", this.scholarCurseVal), btn -> {
            this.scholarCurseVal = !this.scholarCurseVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_scholar_curse", this.scholarCurseVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnScholarCurse);

        this.btnCreepingDeathNuke = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.enable_creeping_death_nuke", this.creepingDeathNukeVal), btn -> {
            this.creepingDeathNukeVal = !this.creepingDeathNukeVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_creeping_death_nuke", this.creepingDeathNukeVal));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnCreepingDeathNuke);

        this.btnOpOnly = Button.builder(this.getToggleButtonMessage("gui.bodycam.server_config.op_only", this.opOnlyVal), btn -> {
            this.opOnlyVal = !this.opOnlyVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.op_only", this.opOnlyVal));
        }).bounds(centerColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnOpOnly);

        this.btnBack = Button.builder(Component.translatable("gui.bodycam.back"), btn -> {
            if (this.currentPage > 1) {
                this.currentPage--;
                this.updateVisibility();
            }
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnBack);

        this.btnNext = Button.builder(Component.translatable("gui.bodycam.next"), btn -> {
            if (this.currentPage < 2) {
                this.currentPage++;
                this.updateVisibility();
            }
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnNext);

        this.btnReset = Button.builder(Component.translatable("gui.bodycam.config.reset"), btn -> {
            this.txtMaxDistance.setValue("500");
            this.txtMaxWatchPointsPlayer.setValue("5");
            this.txtMaxWatchPointsTotal.setValue("50");
            this.reachVal = true;
            this.jammerVal = true;
            this.locatorVal = true;
            this.hologramVal = true;
            this.anonymizerVal = true;
            this.playerLocatorVal = true;
            this.watchPointVal = true;
            this.podestVal = true;
            this.opOnlyVal = false;
            this.loggerVal = true;
            this.portablePortalVal = true;
            this.allSeeingEyeVal = true;
            this.creepingDeathVal = true;
            this.scholarCurseVal = true;
            this.creepingDeathNukeVal = true;

            this.btnReach.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_reach", this.reachVal));
            this.btnJammer.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_jammer", this.jammerVal));
            this.btnLocator.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_locator", this.locatorVal));
            this.btnHologram.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_hologram", this.hologramVal));
            this.btnAnonymizer.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_anonymizer", this.anonymizerVal));
            this.btnPlayerLocator.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_player_locator", this.playerLocatorVal));
            this.btnWatchPoint.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_watch_point", this.watchPointVal));
            this.btnPodest.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_podest", this.podestVal));
            this.btnOpOnly.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.op_only", this.opOnlyVal));
            this.btnLogger.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_logger", this.loggerVal));
            this.btnPortablePortal.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_portable_portal", this.portablePortalVal));
            this.btnAllSeeingEye.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_all_seeing_eye", this.allSeeingEyeVal));
            this.btnCreepingDeath.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_creeping_death", this.creepingDeathVal));
            this.btnScholarCurse.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_scholar_curse", this.scholarCurseVal));
            this.btnCreepingDeathNuke.setMessage(this.getToggleButtonMessage("gui.bodycam.server_config.enable_creeping_death_nuke", this.creepingDeathNukeVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnReset);

        this.btnSave = Button.builder(Component.translatable("gui.done"), btn -> {
            int maxDist = this.initialValues.maxMonitorDistance;
            int maxWpPlayer = this.initialValues.maxWatchPointsPerPlayer;
            int maxWpTotal = this.initialValues.maxWatchPointsTotal;
            try {
                maxDist = Integer.parseInt(this.txtMaxDistance.getValue());
            } catch (NumberFormatException e) {
            }
            try {
                maxWpPlayer = Integer.parseInt(this.txtMaxWatchPointsPlayer.getValue());
            } catch (NumberFormatException e) {
            }
            try {
                maxWpTotal = Integer.parseInt(this.txtMaxWatchPointsTotal.getValue());
            } catch (NumberFormatException e) {
            }
            dev.ClasherHD.bodycam.network.PacketHandler.INSTANCE.sendToServer(
                    new dev.ClasherHD.bodycam.network.config.SaveServerConfigC2SPacket(
                            maxDist, maxWpPlayer, maxWpTotal,
                            this.reachVal, this.jammerVal, this.locatorVal, this.hologramVal,
                            this.anonymizerVal, this.playerLocatorVal, this.watchPointVal, this.podestVal, this.opOnlyVal,
                            this.loggerVal, this.portablePortalVal, this.allSeeingEyeVal,
                            this.creepingDeathVal, this.scholarCurseVal, this.creepingDeathNukeVal
                    )
            );
            this.onClose();
        }).bounds(rightColX, 0, colWidth, 20).build();
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
        this.btnLogger.visible = page1;
        this.btnPortablePortal.visible = page1;

        this.txtMaxWatchPointsPlayer.visible = page2;
        this.txtMaxWatchPointsTotal.visible = page2;
        this.btnWatchPoint.visible = page2;
        this.btnPodest.visible = page2;
        this.btnAllSeeingEye.visible = page2;
        this.btnCreepingDeath.visible = page2;
        this.btnScholarCurse.visible = page2;
        this.btnCreepingDeathNuke.visible = page2;
        this.btnOpOnly.visible = page2;

        this.btnBack.active = page2;
        this.btnNext.active = page1;
    }

    private void updateLayout() {
        this.txtMaxDistance.setY(this.guiY + 34);

        this.btnReach.setY(this.guiY + 58);
        this.btnJammer.setY(this.guiY + 58);

        this.btnLocator.setY(this.guiY + 81);
        this.btnHologram.setY(this.guiY + 81);

        this.btnAnonymizer.setY(this.guiY + 104);
        this.btnPlayerLocator.setY(this.guiY + 104);

        this.btnLogger.setY(this.guiY + 127);
        this.btnPortablePortal.setY(this.guiY + 127);

        this.txtMaxWatchPointsPlayer.setY(this.guiY + 34);
        this.txtMaxWatchPointsTotal.setY(this.guiY + 34);

        this.btnWatchPoint.setY(this.guiY + 58);
        this.btnPodest.setY(this.guiY + 58);

        this.btnAllSeeingEye.setY(this.guiY + 81);
        this.btnCreepingDeath.setY(this.guiY + 81);

        this.btnScholarCurse.setY(this.guiY + 104);
        this.btnCreepingDeathNuke.setY(this.guiY + 104);

        this.btnOpOnly.setY(this.guiY + 127);

        this.btnBack.setY(this.guiY + 155);
        this.btnNext.setY(this.guiY + 155);

        this.btnReset.setY(this.guiY + 180);
        this.btnSave.setY(this.guiY + 180);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.minecraft != null && this.minecraft.level == null) {
            this.renderDirtBackground(pGuiGraphics);
        }

        RenderSystem.setShaderColor(0.32F, 0.32F, 0.35F, 1.0F);
        GuiRenderUtils.drawNineSlice(pGuiGraphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 32, 32, 32, 32, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiY + 10, 0xFFFFFFFF);

        int colWidth = (this.guiWidth - 30) / 2;
        int leftColX = this.guiX + 12;
        int rightColX = this.guiX + 12 + colWidth + 6;
        int centerColX = this.guiX + (this.guiWidth - colWidth) / 2;
        int labelY = this.guiY + 23;

        if (this.currentPage == 1) {
            pGuiGraphics.drawString(this.font, Component.translatable("gui.bodycam.server_config.max_distance"), centerColX, labelY, 0xFFCCCCCC);
        } else if (this.currentPage == 2) {
            pGuiGraphics.drawString(this.font, Component.translatable("gui.bodycam.server_config.max_wp_player"), leftColX, labelY, 0xFFCCCCCC);
            pGuiGraphics.drawString(this.font, Component.translatable("gui.bodycam.server_config.max_wp_total"), rightColX, labelY, 0xFFCCCCCC);
        }

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previous);
        }
    }
}
