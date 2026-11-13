package dev.ClasherHD.bodycam.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket;
import dev.ClasherHD.bodycam.network.watchpoint.WatchPointLookC2SPacket;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@SuppressWarnings("null")
public class BodycamViewScreen extends Screen {
    private static final ResourceLocation HEART_CONTAINER_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation HEART_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation HEART_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final ResourceLocation HEART_ABSORBING_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full");
    private static final ResourceLocation HEART_ABSORBING_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half");

    private final UUID targetId;
    private final String targetName;
    private final boolean isOnHologram;
    private final boolean isWatchPoint;
    private final boolean isAllSeeingEye;
    public static boolean isMonitoring = false;
    public static UUID targetUuid;
    public static String targetNameStatic;
    public static boolean lastIsOnHologram = false;
    public static boolean lastIsWatchPoint = false;
    public static boolean lastIsAllSeeingEye = false;

    private boolean hasMousePos = false;
    private double lastMouseX;
    private double lastMouseY;
    private boolean packetSent = false;
    private boolean exitRequested = false;
    private int exitPendingTicks = 0;
    private long lastLookPacketTime = 0L;
    private boolean pendingLookPacket = false;
    private float currentVignetteAlpha = 0.0F;
    private float currentHitFlash = 0.0F;
    private int lastHurtTime = 0;
    private int lastDamageTier = 0;
    private long lastRenderNanoTime = System.nanoTime();
    private final String cachedExitText;
    private int textTickTimer = 60;

    public BodycamViewScreen(UUID targetId, String targetName, boolean isOnHologram) {
        this(targetId, targetName, isOnHologram, false, false);
    }

    public BodycamViewScreen(UUID targetId, String targetName, boolean isOnHologram, boolean isWatchPoint) {
        this(targetId, targetName, isOnHologram, isWatchPoint, false);
    }

    public BodycamViewScreen(UUID targetId, String targetName, boolean isOnHologram, boolean isWatchPoint, boolean isAllSeeingEye) {
        super(Component.translatable("item.bodycam.bodycam_monitor"));
        this.targetId = targetId;
        this.targetName = targetName;
        this.isOnHologram = isOnHologram;
        this.isWatchPoint = isWatchPoint;
        this.isAllSeeingEye = isAllSeeingEye;
        BodycamViewScreen.targetUuid = targetId;
        BodycamViewScreen.targetNameStatic = targetName;
        BodycamViewScreen.lastIsOnHologram = isOnHologram;
        BodycamViewScreen.lastIsWatchPoint = isWatchPoint;
        BodycamViewScreen.lastIsAllSeeingEye = isAllSeeingEye;

        this.cachedExitText = Component.translatable(
                "gui.bodycam.exit_message",
                Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage()
        ).getString();
    }

    public UUID getTargetId() {
        return this.targetId;
    }

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
        if (!this.packetSent) {
            PacketHandler.sendToServer(new BodycamSetCameraPacket(this.targetId, this.isOnHologram, this.isAllSeeingEye));
            this.packetSent = true;
        }
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
            long now = System.currentTimeMillis();
            if (now - this.lastLookPacketTime >= 50) {
                PacketHandler.sendToServer(new WatchPointLookC2SPacket(mc.player.getYRot(), mc.player.getXRot()));
                this.lastLookPacketTime = now;
                this.pendingLookPacket = false;
            } else {
                this.pendingLookPacket = true;
            }
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
            if (BodycamViewScreen.isMonitoring && !this.exitRequested) {
                this.exitRequested = true;
                this.exitPendingTicks = 0;
                PacketHandler.sendToServer(new BodycamResetCameraPacket());
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new PauseScreen(true));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void renderExitText(GuiGraphics graphics) {
        if (this.textTickTimer <= 0) {
            return;
        }
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

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int damageTier = updateAndRenderDamageVignette(graphics, mc, frameThickX, frameThickY);

            int barColor = this.isAllSeeingEye ? 0xAA221800 : 0x99000000;
            graphics.fill(0, 0, this.width, frameThickY, barColor);
            graphics.fill(0, this.height - frameThickY, this.width, this.height, barColor);
            graphics.fill(0, frameThickY, frameThickX, this.height - frameThickY, barColor);
            graphics.fill(this.width - frameThickX, frameThickY, this.width, this.height - frameThickY, barColor);
            if (this.isAllSeeingEye) {
                int borderCol = 0xCCFFAA00;
                graphics.fill(frameThickX - 2, frameThickY - 2, this.width - frameThickX + 2, frameThickY, borderCol);
                graphics.fill(frameThickX - 2, this.height - frameThickY, this.width - frameThickX + 2, this.height - frameThickY + 2, borderCol);
                graphics.fill(frameThickX - 2, frameThickY, frameThickX, this.height - frameThickY, borderCol);
                graphics.fill(this.width - frameThickX, frameThickY, this.width - frameThickX + 2, this.height - frameThickY, borderCol);
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (damageTier >= 3 && (System.currentTimeMillis() % 900L) < 600L) {
                Component alert = Component.translatable(damageTier >= 4 ? "gui.bodycam.signal_critical" : "gui.bodycam.signal_weak");
                int alertColor = damageTier >= 4 ? 0xFFFF3B3B : 0xFFFFAA22;
                int textW = this.font.width(alert);
                graphics.drawString(this.font, alert, (this.width - textW) / 2, 10, alertColor, true);
            }

            if (ModClientConfig.SHOW_NAME_OVERLAY.get()) {
                Component recComp = Component.translatable("gui.bodycam.rec", this.targetName);
                String dotChar = this.isAllSeeingEye ? "\u25c9" : "\u25cf";
                int dotColor = this.isAllSeeingEye ? 0xFFFFAA00 : 0xFFFF0000;
                int textColor = 0xFFFFFFFF;
                if ((System.currentTimeMillis() % 1000L) >= 500L) {
                    graphics.drawString(this.font, dotChar, 10, 10, dotColor, true);
                }
                graphics.drawString(this.font, recComp, 10 + this.font.width(dotChar + " "), 10, textColor, true);
            }

            if (ModClientConfig.SHOW_SHIFT_OVERLAY.get()) {
                this.renderExitText(graphics);
            }

            if (mc.player != null && ModClientConfig.SHOW_HEALTH_OVERLAY.get()) {
                float health = mc.player.getHealth();
                float maxHealth = mc.player.getMaxHealth();
                float absorption = mc.player.getAbsorptionAmount();

                int maxHealthHearts = Mth.ceil(maxHealth / 2.0F);
                int absorbHearts = Mth.ceil(absorption / 2.0F);

                int xBase = this.width / 2 - (maxHealthHearts * 4);
                int yBase = this.height - 20;

                for (int i = maxHealthHearts - 1; i >= 0; --i) {
                    int heartX = xBase + i * 8;
                    int heartY = yBase;

                    graphics.blitSprite(HEART_CONTAINER_SPRITE, heartX, heartY, 9, 9);

                    if (i * 2 + 1 < health) {
                        graphics.blitSprite(HEART_FULL_SPRITE, heartX, heartY, 9, 9);
                    } else if (i * 2 + 1 == health) {
                        graphics.blitSprite(HEART_HALF_SPRITE, heartX, heartY, 9, 9);
                    }
                }

                if (absorbHearts > 0) {
                    int absorbYBase = yBase - 10;
                    for (int i = absorbHearts - 1; i >= 0; --i) {
                        int heartX = xBase + i * 8;
                        int heartY = absorbYBase;

                        graphics.blitSprite(HEART_CONTAINER_SPRITE, heartX, heartY, 9, 9);

                        if (i * 2 + 1 < absorption) {
                            graphics.blitSprite(HEART_ABSORBING_FULL_SPRITE, heartX, heartY, 9, 9);
                        } else if (i * 2 + 1 == absorption) {
                            graphics.blitSprite(HEART_ABSORBING_HALF_SPRITE, heartX, heartY, 9, 9);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private int updateAndRenderDamageVignette(GuiGraphics graphics, Minecraft mc, int frameThickX, int frameThickY) {
        if (mc.player == null || !ModClientConfig.SHOW_DAMAGE_VIGNETTE.get()) {
            this.currentVignetteAlpha = 0.0F;
            this.currentHitFlash = 0.0F;
            return 0;
        }
        long nowNano = System.nanoTime();
        float deltaSec = Math.min(0.1F, Math.max(0.001F, (nowNano - this.lastRenderNanoTime) / 1_000_000_000.0F));
        this.lastRenderNanoTime = nowNano;

        float healthRatio = Mth.clamp(
                mc.player.getHealth() / Math.max(1.0F, mc.player.getMaxHealth()), 0.0F, 1.0F);
        int tier;
        float targetAlpha;
        if (healthRatio > 0.75F) {
            tier = 0;
            targetAlpha = 0.0F;
        } else if (healthRatio > 0.50F) {
            tier = 1;
            targetAlpha = 0.30F;
        } else if (healthRatio > 0.25F) {
            tier = 2;
            targetAlpha = 0.55F;
        } else if (healthRatio > 0.10F) {
            tier = 3;
            targetAlpha = 0.78F;
        } else {
            tier = 4;
            targetAlpha = 0.94F;
        }

        if (mc.player.hurtTime > this.lastHurtTime) {
            this.currentHitFlash = 0.25F;
            if (tier >= 1) {
                mc.player.playSound(ModSounds.SIGNAL_STATIC.get(), 0.22F, 1.7F);
            }
        } else if (tier > this.lastDamageTier && tier >= 1) {
            mc.player.playSound(ModSounds.SIGNAL_STATIC.get(), 0.25F, 1.8F);
        }
        this.lastHurtTime = mc.player.hurtTime;
        this.lastDamageTier = tier;

        float lerpRate = targetAlpha > this.currentVignetteAlpha ? 1.6F : 2.4F;
        this.currentVignetteAlpha = Mth.lerp(
                Math.min(1.0F, deltaSec * lerpRate), this.currentVignetteAlpha, targetAlpha);
        this.currentHitFlash = Math.max(0.0F, this.currentHitFlash - deltaSec * 1.4F);

        float flicker = 0.0F;
        if (this.currentVignetteAlpha > 0.02F) {
            double t = System.currentTimeMillis() / 1000.0D;
            float amp = 0.03F + tier * 0.025F;
            flicker = (float) (Math.sin(t * (7.0D + tier * 3.0D)) * 0.6D + Math.sin(t * 23.0D) * 0.4D) * amp;
        }

        float effectiveAlpha = Mth.clamp(this.currentVignetteAlpha + this.currentHitFlash + flicker, 0.0F, 0.95F);
        if (effectiveAlpha > 0.005F) {
            drawRadialRedVignette(graphics, effectiveAlpha, frameThickX, frameThickY);
        }
        return tier;
    }

    private void drawRadialRedVignette(GuiGraphics graphics, float alpha, int frameThickX, int frameThickY) {
        int outerAlpha = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        int midAlpha = Mth.clamp((int) (alpha * 145.0F), 0, 255);
        if (outerAlpha <= 0) {
            return;
        }
        float cx = this.width * 0.5F;
        float cy = this.height * 0.5F;
        float viewHalfW = Math.max(1.0F, this.width * 0.5F - frameThickX);
        float viewHalfH = Math.max(1.0F, this.height * 0.5F - frameThickY);

        float innerRx = viewHalfW * (0.48F - alpha * 0.18F);
        float innerRy = viewHalfH * (0.48F - alpha * 0.18F);
        float midRx = viewHalfW * 0.88F;
        float midRy = viewHalfH * 0.88F;
        float outerRx = this.width * 0.75F;
        float outerRy = this.height * 0.75F;
        int segments = 40;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pose = graphics.pose().last().pose();

        int r = 175;
        int g = 8;
        int b = 12;

        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0D * i) / segments;
            double a1 = (Math.PI * 2.0D * (i + 1)) / segments;
            float cos0 = (float) Math.cos(a0);
            float sin0 = (float) Math.sin(a0);
            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);

            float ix0 = cx + cos0 * innerRx;
            float iy0 = cy + sin0 * innerRy;
            float ix1 = cx + cos1 * innerRx;
            float iy1 = cy + sin1 * innerRy;

            float mx0 = cx + cos0 * midRx;
            float my0 = cy + sin0 * midRy;
            float mx1 = cx + cos1 * midRx;
            float my1 = cy + sin1 * midRy;

            float ox0 = cx + cos0 * outerRx;
            float oy0 = cy + sin0 * outerRy;
            float ox1 = cx + cos1 * outerRx;
            float oy1 = cy + sin1 * outerRy;

            buffer.addVertex(pose, ix0, iy0, 0.0F).setColor(r, g, b, 0);
            buffer.addVertex(pose, mx0, my0, 0.0F).setColor(r, g, b, midAlpha);
            buffer.addVertex(pose, mx1, my1, 0.0F).setColor(r, g, b, midAlpha);

            buffer.addVertex(pose, ix0, iy0, 0.0F).setColor(r, g, b, 0);
            buffer.addVertex(pose, mx1, my1, 0.0F).setColor(r, g, b, midAlpha);
            buffer.addVertex(pose, ix1, iy1, 0.0F).setColor(r, g, b, 0);

            buffer.addVertex(pose, mx0, my0, 0.0F).setColor(r, g, b, midAlpha);
            buffer.addVertex(pose, ox0, oy0, 0.0F).setColor(r, g, b, outerAlpha);
            buffer.addVertex(pose, ox1, oy1, 0.0F).setColor(r, g, b, outerAlpha);

            buffer.addVertex(pose, mx0, my0, 0.0F).setColor(r, g, b, midAlpha);
            buffer.addVertex(pose, ox1, oy1, 0.0F).setColor(r, g, b, outerAlpha);
            buffer.addVertex(pose, mx1, my1, 0.0F).setColor(r, g, b, midAlpha);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
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
        if (this.isWatchPoint && BodycamViewScreen.isMonitoring && this.pendingLookPacket) {
            if (mc != null && mc.player != null) {
                PacketHandler.sendToServer(new WatchPointLookC2SPacket(mc.player.getYRot(), mc.player.getXRot()));
                this.lastLookPacketTime = System.currentTimeMillis();
                this.pendingLookPacket = false;
            }
        }
        if (mc != null) {
            if (mc.getDebugOverlay().showDebugScreen()) {
                mc.getDebugOverlay().toggleOverlay();
            }
            while (mc.options.keyTogglePerspective.consumeClick()) {
            }
        }

        if (this.exitRequested) {
            this.exitPendingTicks++;
            if (this.exitPendingTicks > 40) {
                this.exitRequested = false;
                this.exitPendingTicks = 0;
            }
            return;
        }

        if (mc != null && mc.level != null && mc.player != null) {
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
