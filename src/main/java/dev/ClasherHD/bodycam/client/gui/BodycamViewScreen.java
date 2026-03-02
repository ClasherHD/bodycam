package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.network.PacketHandler;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class BodycamViewScreen extends Screen {

    private final UUID targetId;
    private final String targetName;
    private final boolean hasReach;
    public static boolean isMonitoring = false;
    public static UUID targetUuid;
    public static String targetNameStatic;
    public static boolean hasReachStatic;

    public BodycamViewScreen(UUID targetId, String targetName, boolean hasReach) {
        super(Component.translatable("item.bodycam.bodycam_monitor"));
        this.targetId = targetId;
        this.targetName = targetName;
        BodycamViewScreen.targetUuid = targetId;
        BodycamViewScreen.targetNameStatic = targetName;
        this.hasReach = hasReach;
        BodycamViewScreen.hasReachStatic = hasReach;
    }

    public UUID getTargetId() {
        return this.targetId;
    }

    private static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation(
            "minecraft", "textures/gui/icons.png");
    private int fadeTicks = 0;
    private boolean manualExit = false;
    private int targetLostTicks = 0;

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

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(this.targetId);
        ClientPlayNetworking.send(PacketHandler.SET_CAMERA_ID, buf);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyShift.matches(keyCode, scanCode)) {
            if (BodycamViewScreen.isMonitoring) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                ClientPlayNetworking.send(PacketHandler.RESET_CAMERA_ID, buf);
            }
            this.manualExit = true;
            this.onClose();
            return true;
        }
        if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new PauseScreen(true));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (this.minecraft != null && this.minecraft.options.hideGui) {
            return;
        }

        try {
            graphics.drawString(this.font, "CAM: " + this.targetName, 10, 10, 0xFFFFFF, false);

            if (this.fadeTicks < 100) {
                float alpha = 1.0F - (this.fadeTicks / 100.0F);
                int alphaInt = (int) (alpha * 255.0F);
                int color = (alphaInt << 24) | 0xFFFFFF;
                String exitText = "Beenden mit SHIFT";
                graphics.drawString(this.font, exitText, this.width - this.font.width(exitText) - 10, 10, color, true);
            }

            if (mc.player != null) {
                float health = mc.player.getHealth();
                float maxHealth = mc.player.getMaxHealth();
                float absorption = mc.player.getAbsorptionAmount();

                int maxHealthHearts = Mth.ceil(maxHealth / 2.0F);
                int absorbHearts = Mth.ceil(absorption / 2.0F);

                int xBase = this.width / 2 - 91;
                int yBase = this.height - 39;

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
    public void onClose() {
        if (BodycamViewScreen.isMonitoring) {
            BodycamViewScreen.isMonitoring = false;
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                mc.setCameraEntity(mc.player);
            }
        }
        super.onClose();
    }

    @Override
    public void removed() {
        Minecraft mc = Minecraft.getInstance();
        if (BodycamViewScreen.isMonitoring && (this.manualExit || mc.player == null || !mc.player.isAlive())) {
            BodycamViewScreen.isMonitoring = false;
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fadeTicks < 100) {
            this.fadeTicks++;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive()) {
            if (BodycamViewScreen.isMonitoring) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                ClientPlayNetworking.send(PacketHandler.RESET_CAMERA_ID, buf);
            }
            BodycamViewScreen.isMonitoring = false;
            mc.setScreen(null);
            return;
        }

        if (mc.level != null) {
            Player targetPlayer = mc.level.getPlayerByUUID(this.targetId);
            if (targetPlayer == null || (!this.hasReach
                    && (mc.player.level() != targetPlayer.level() || mc.player.distanceTo(targetPlayer) > 500.0D))) {
                this.targetLostTicks++;
                if (this.targetLostTicks > 20) {
                    if (BodycamViewScreen.isMonitoring) {
                        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                        ClientPlayNetworking.send(PacketHandler.RESET_CAMERA_ID, buf);
                    }
                    this.manualExit = true;
                    this.onClose();
                    return;
                }
            } else {
                this.targetLostTicks = 0;
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
