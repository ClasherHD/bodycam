package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.watchpoint.WatchPointLookC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("null")
public class BodycamViewScreen extends Screen {

    private final java.util.UUID targetId;
    private final String targetName;
    private final boolean isOnHologram;
    private final boolean isWatchPoint;
    public static boolean isMonitoring = false;
    public static java.util.UUID targetUuid;
    public static String targetNameStatic;
    public static boolean lastIsOnHologram = false;
    public static boolean lastIsWatchPoint = false;

    private boolean hasMousePos = false;
    private double lastMouseX;
    private double lastMouseY;

    public BodycamViewScreen(java.util.UUID targetId, String targetName, boolean isOnHologram) {
        this(targetId, targetName, isOnHologram, false);
    }

    public BodycamViewScreen(java.util.UUID targetId, String targetName, boolean isOnHologram, boolean isWatchPoint) {
        super(Component.translatable("item.bodycam.bodycam_monitor"));
        this.targetId = targetId;
        this.targetName = targetName;
        this.isOnHologram = isOnHologram;
        this.isWatchPoint = isWatchPoint;
        BodycamViewScreen.targetUuid = targetId;
        BodycamViewScreen.targetNameStatic = targetName;
        BodycamViewScreen.lastIsOnHologram = isOnHologram;
        BodycamViewScreen.lastIsWatchPoint = isWatchPoint;

        this.cachedExitText = Component.translatable("gui.bodycam.exit_message", Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage()).getString();
    }

    public java.util.UUID getTargetId() {
        return this.targetId;
    }

    private static final net.minecraft.resources.ResourceLocation GUI_ICONS_LOCATION = new net.minecraft.resources.ResourceLocation(
            "minecraft", "textures/gui/icons.png");
    private String cachedExitText;
    private int textTickTimer = 60;

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive()) {
            BodycamViewScreen.isMonitoring = false;
            this.onClose();
            return;
        }
        BodycamViewScreen.isMonitoring = true;
        this.hasMousePos = false;
        PacketHandler.INSTANCE.sendToServer(new dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket(this.targetId, this.isOnHologram));
        if (mc.level != null) {
            if (this.isWatchPoint) {
                mc.setCameraEntity(mc.player);
            } else {
                Player targetPlayer = mc.level.getPlayerByUUID(this.targetId);
                if (targetPlayer != null && targetPlayer.isAlive()) {
                    mc.setCameraEntity(targetPlayer);
                }
            }
        }
        if (this.minecraft != null) {
            int cursorMode = this.isWatchPoint ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_HIDDEN;
            GLFW.glfwSetInputMode(this.minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, cursorMode);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (this.minecraft != null) {
            GLFW.glfwSetInputMode(this.minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        if (!this.isWatchPoint || !BodycamViewScreen.isMonitoring) {
            return;
        }
        if (!this.hasMousePos) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            this.hasMousePos = true;
            return;
        }
        double dx = mouseX - this.lastMouseX;
        double dy = mouseY - this.lastMouseY;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double sens = mc.options.sensitivity().get() * 0.6D + 0.2D;
            double factor = sens * sens * sens * 8.0D;
            double invert = mc.options.invertYMouse().get() ? -1.0D : 1.0D;
            mc.player.turn(dx * factor, dy * factor * invert);
            PacketHandler.INSTANCE.sendToServer(new WatchPointLookC2SPacket(mc.player.getYRot(), mc.player.getXRot()));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F3 || keyCode == GLFW.GLFW_KEY_F5) {
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyTogglePerspective.matches(keyCode, scanCode)) {
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyShift.matches(keyCode, scanCode)) {
            if (BodycamViewScreen.isMonitoring) {
                PacketHandler.INSTANCE.sendToServer(new dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket());
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
    }

    private void renderExitText(GuiGraphics graphics) {
        if (this.textTickTimer <= 0) return;
        graphics.drawString(this.font, this.cachedExitText, this.width - this.font.width(this.cachedExitText) - 10, 10, 0xFFFFFFFF, true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (this.minecraft != null && this.minecraft.options.hideGui) {
            return;
        }

        try {
            int frameThickX = (int) (this.width * 0.1);
            int frameThickY = (int) (this.height * 0.1);

            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

            graphics.fill(0, 0, this.width, frameThickY, 0x99000000);
            graphics.fill(0, this.height - frameThickY, this.width, this.height, 0x99000000);
            graphics.fill(0, frameThickY, frameThickX, this.height - frameThickY, 0x99000000);
            graphics.fill(this.width - frameThickX, frameThickY, this.width, this.height - frameThickY, 0x99000000);

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_NAME_OVERLAY.get()) {
                Component recComp = Component.translatable("gui.bodycam.rec", this.targetName);
                if ((System.currentTimeMillis() % 1000L) >= 500L) {
                    graphics.drawString(this.font, "\u25cf", 10, 10, 0xFFFF0000, true);
                }
                graphics.drawString(this.font, recComp, 10 + this.font.width("\u25cf "), 10, 0xFFFFFFFF, true);
            }

            if (dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_SHIFT_OVERLAY.get()) {
                this.renderExitText(graphics);
            }

            if (mc.player != null && dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_HEALTH_OVERLAY.get()) {
                float health = mc.player.getHealth();
                float maxHealth = mc.player.getMaxHealth();
                float absorption = mc.player.getAbsorptionAmount();

                int maxHealthHearts = net.minecraft.util.Mth.ceil(maxHealth / 2.0F);
                int absorbHearts = net.minecraft.util.Mth.ceil(absorption / 2.0F);

                int xBase = this.width / 2 - (maxHealthHearts * 4);
                int yBase = this.height - 20;

                for (int i = maxHealthHearts - 1; i >= 0; --i) {
                    int heartX = xBase + i * 8;
                    int heartY = yBase;

                    int uOffset = 16;
                    int vOffset = 0;

                    graphics.blit(GUI_ICONS_LOCATION, heartX, heartY, uOffset, vOffset, 9, 9);

                    if (i * 2 + 1 < health) {
                        graphics.blit(GUI_ICONS_LOCATION, heartX, heartY, uOffset + 36, vOffset, 9, 9);
                    } else if (i * 2 + 1 == health) {
                        graphics.blit(GUI_ICONS_LOCATION, heartX, heartY, uOffset + 45, vOffset, 9, 9);
                    }
                }

                if (absorbHearts > 0) {
                    int absorbYBase = yBase - 10;
                    for (int i = absorbHearts - 1; i >= 0; --i) {
                        int heartX = xBase + i * 8;
                        int heartY = absorbYBase;

                        graphics.blit(GUI_ICONS_LOCATION, heartX, heartY, 16, 0, 9, 9);

                        if (i * 2 + 1 < absorption) {
                            graphics.blit(GUI_ICONS_LOCATION, heartX, heartY, 16 + 144, 0, 9, 9);
                        } else if (i * 2 + 1 == absorption) {
                            graphics.blit(GUI_ICONS_LOCATION, heartX, heartY, 16 + 153, 0, 9, 9);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.textTickTimer > 0) {
            this.textTickTimer--;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            if (mc.options.renderDebug) {
                mc.options.renderDebug = false;
            }
            while (mc.options.keyTogglePerspective.consumeClick()) {}
        }

        if (mc.level != null && mc.player != null) {
            if (this.isWatchPoint) {
                if (mc.getCameraEntity() != mc.player) {
                    mc.setCameraEntity(mc.player);
                }
                mc.player.setDeltaMovement(Vec3.ZERO);
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity instanceof WatchPointDummyEntity wp && wp.getUUID().equals(this.targetId)) {
                        mc.player.setPos(wp.getX(), wp.getY(), wp.getZ());
                        wp.setYRot(mc.player.getYRot());
                        wp.setXRot(mc.player.getXRot());
                        wp.yRotO = mc.player.yRotO;
                        wp.xRotO = mc.player.xRotO;
                        wp.yHeadRot = mc.player.getYRot();
                        wp.yHeadRotO = mc.player.yRotO;
                        wp.yBodyRot = mc.player.getYRot();
                        wp.yBodyRotO = mc.player.yRotO;
                        break;
                    }
                }
            } else {
                Player targetPlayer = mc.level.getPlayerByUUID(this.targetId);
                if (targetPlayer != null) {
                    if (mc.getCameraEntity() != targetPlayer) {
                        mc.setCameraEntity(targetPlayer);
                    }
                    mc.player.setPos(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
                    mc.player.setYRot(targetPlayer.getYRot());
                    mc.player.setXRot(targetPlayer.getXRot());
                    mc.player.yRotO = targetPlayer.yRotO;
                    mc.player.xRotO = targetPlayer.xRotO;
                    mc.player.yHeadRot = targetPlayer.yHeadRot;
                    mc.player.yHeadRotO = targetPlayer.yHeadRotO;
                }
            }
        }
    }
}
