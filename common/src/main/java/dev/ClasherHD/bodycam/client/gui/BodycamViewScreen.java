package dev.ClasherHD.bodycam.client.gui;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import dev.ClasherHD.bodycam.network.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.network.BodycamResetCameraPacket;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class BodycamViewScreen extends Screen {

    private final UUID targetId;
    private final String targetName;
    private final boolean hasReach;
    private final boolean isOnHologram;
    public static boolean isMonitoring = false;
    public static UUID targetUuid;
    public static String targetNameStatic;
    public static boolean lastHasReach = false;
    public static boolean lastIsOnHologram = false;

    private static final ResourceLocation HEART_CONTAINER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "hud/heart/container");
    private static final ResourceLocation HEART_FULL = ResourceLocation.fromNamespaceAndPath("minecraft",
            "hud/heart/full");
    private static final ResourceLocation HEART_HALF = ResourceLocation.fromNamespaceAndPath("minecraft",
            "hud/heart/half");
    private static final ResourceLocation HEART_ABSORBING_FULL = ResourceLocation.fromNamespaceAndPath("minecraft",
            "hud/heart/absorbing_full");
    private static final ResourceLocation HEART_ABSORBING_HALF = ResourceLocation.fromNamespaceAndPath("minecraft",
            "hud/heart/absorbing_half");

    private String cachedTargetText;
    private String cachedExitText;
    private int textTickTimer = 60;

    public UUID getTargetId() {
        return targetUuid;
    }

    public BodycamViewScreen(UUID targetId, String targetName, boolean hasReach, boolean isOnHologram) {
        super(Component.translatable("item.bodycam.bodycam_monitor"));
        this.targetId = targetId;
        this.targetName = targetName;
        BodycamViewScreen.targetUuid = targetId;
        BodycamViewScreen.targetNameStatic = targetName;
        this.hasReach = hasReach;
        this.isOnHologram = isOnHologram;
        BodycamViewScreen.lastHasReach = hasReach;
        BodycamViewScreen.lastIsOnHologram = isOnHologram;

        this.cachedTargetText = "REC: " + this.targetName;
        this.cachedExitText = Component.translatable("gui.bodycam.exit_message",
                Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage()).getString();
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive()) {
            isMonitoring = false;
            this.onClose();
            return;
        }
        if (!isMonitoring) {
            NetworkManager.sendToServer(new BodycamSetCameraPacket(this.targetId, this.hasReach, this.isOnHologram));
            isMonitoring = true;
        }
        if (mc.level != null) {
            Player targetPlayer = mc.level.getPlayerByUUID(this.targetId);
            if (targetPlayer != null && targetPlayer.isAlive()) {
                mc.setCameraEntity(targetPlayer);
            }
        }
        GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
    }

    @Override
    public void removed() {
        super.removed();
        Minecraft mc = Minecraft.getInstance();
        GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (keyCode == GLFW.GLFW_KEY_F3 || keyCode == GLFW.GLFW_KEY_F5)
            return true;
        if (mc.options.keyTogglePerspective.matches(keyCode, scanCode))
            return true;

        if (mc.options.keyShift.matches(keyCode, scanCode)) {
            if (isMonitoring) {
                isMonitoring = false;
                NetworkManager.sendToServer(new BodycamResetCameraPacket());
                this.onClose();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            mc.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background and frame rendering logic here
        super.render(graphics, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui)
            return;

        int frameThickX = (int) (this.width * 0.1);
        int frameThickY = (int) (this.height * 0.1);

        graphics.fill(0, 0, this.width, frameThickY, 0x99000000);
        graphics.fill(0, this.height - frameThickY, this.width, this.height, 0x99000000);
        graphics.fill(0, frameThickY, frameThickX, this.height - frameThickY, 0x99000000);
        graphics.fill(this.width - frameThickX, frameThickY, this.width, this.height - frameThickY, 0x99000000);

        // Name and REC overlay
        if ((System.currentTimeMillis() % 1000L) >= 500L) {
            graphics.drawString(this.font, "●", 10, 10, 0xFFFF0000, true);
        }
        graphics.drawString(this.font, this.cachedTargetText, 25, 10, 0xFFFFFFFF, true);

        // Exit message
        if (textTickTimer > 0) {
            graphics.drawString(this.font, cachedExitText, this.width - this.font.width(cachedExitText) - 10, 10,
                    0xFFFFFFFF, true);
        }

        // Health Overlay logic
        renderHealth(graphics, mc.player);
    }

    private void renderHealth(GuiGraphics graphics, Player player) {
        float health = player.getHealth();
        int maxHealthHearts = Mth.ceil(player.getMaxHealth() / 2.0f);
        int healthInt = Mth.ceil(health);

        float absorption = player.getAbsorptionAmount();
        int absorptionHearts = Mth.ceil(absorption / 2.0f);
        int absorptionInt = Mth.ceil(absorption);

        int totalHearts = maxHealthHearts + absorptionHearts;
        int xBase = this.width / 2 - (totalHearts * 4);
        int yBase = this.height - 25;

        for (int i = 0; i < maxHealthHearts; i++) {
            int heartX = xBase + i * 8;
            graphics.blitSprite(HEART_CONTAINER, heartX, yBase, 9, 9);
            if (i * 2 + 1 < healthInt) {
                graphics.blitSprite(HEART_FULL, heartX, yBase, 9, 9);
            } else if (i * 2 + 1 == healthInt) {
                graphics.blitSprite(HEART_HALF, heartX, yBase, 9, 9);
            }
        }

        for (int i = 0; i < absorptionHearts; i++) {
            int heartX = xBase + (maxHealthHearts + i) * 8;
            graphics.blitSprite(HEART_CONTAINER, heartX, yBase, 9, 9);
            if (i * 2 + 1 < absorptionInt) {
                graphics.blitSprite(HEART_ABSORBING_FULL, heartX, yBase, 9, 9);
            } else if (i * 2 + 1 == absorptionInt) {
                graphics.blitSprite(HEART_ABSORBING_HALF, heartX, yBase, 9, 9);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (textTickTimer > 0)
            textTickTimer--;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && isMonitoring) {
            Player target = mc.level.getPlayerByUUID(this.targetId);
            if (target != null) {
                if (mc.getCameraEntity() != target)
                    mc.setCameraEntity(target);
                mc.player.setPos(target.getX(), target.getY(), target.getZ());
                mc.player.setYRot(target.getYRot());
                mc.player.setXRot(target.getXRot());
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
