package dev.ClasherHD.bodycam.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSelectionScreen extends Screen {

    public static final Set<UUID> observingMe = ConcurrentHashMap.newKeySet();
    private static final ResourceLocation WINDOW_TEXTURE = new ResourceLocation("bodycam", "textures/gui/gui_window.png");
    private final boolean hasReach;
    private final boolean isOnHologram;
    private PlayerListWidget listWidget;
    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;

    public PlayerSelectionScreen(boolean hasReach, boolean isOnHologram) {
        super(Component.translatable("gui.bodycam.select_player"));
        this.hasReach = hasReach;
        this.isOnHologram = isOnHologram;
    }

    @Override
    protected void init() {
        super.init();
        this.guiWidth = (int) (this.width * 0.7);
        this.guiHeight = (int) (this.height * 0.7);
        this.guiX = (this.width - this.guiWidth) / 2;
        this.guiY = (this.height - this.guiHeight) / 2;

        int listX = this.guiX + 20;
        int listY = this.guiY + 30;
        int listWidth = this.guiWidth - 40;
        int listHeight = this.guiHeight - 50;

        this.listWidget = new PlayerListWidget(this.minecraft, listWidth, this.height, listY, listY + listHeight, 40, listX);
        
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            for (net.minecraft.client.multiplayer.PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                if (this.minecraft.player != null && !info.getProfile().getId().equals(this.minecraft.player.getUUID())) {
                    this.listWidget.addPlayerEntry(new PlayerEntry(info, info.getProfile().getName()));
                }
            }
        }
        this.addWidget(this.listWidget);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 64, 64, 512, 512, 3840, 2160);
        
        if (this.listWidget != null) {
            this.listWidget.render(graphics, mouseX, mouseY, partialTick);
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiY + 10, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int rCW, int rCH, int uvCW, int uvCH, int texW, int texH) {
        graphics.blit(texture, x, y, rCW, rCH, 0, 0, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + width - rCW, y, rCW, rCH, texW - uvCW, 0, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x, y + height - rCH, rCW, rCH, 0, texH - uvCH, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + width - rCW, y + height - rCH, rCW, rCH, texW - uvCW, texH - uvCH, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + rCW, y, width - 2 * rCW, rCH, uvCW, 0, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + rCW, y + height - rCH, width - 2 * rCW, rCH, uvCW, texH - uvCH, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(texture, x, y + rCH, rCW, height - 2 * rCH, 0, uvCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(texture, x + width - rCW, y + rCH, rCW, height - 2 * rCH, texW - uvCW, uvCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(texture, x + rCW, y + rCH, width - 2 * rCW, height - 2 * rCH, uvCW, uvCH, texW - 2 * uvCW, texH - 2 * uvCH, texW, texH);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public class PlayerListWidget extends ObjectSelectionList<PlayerEntry> {
        private final int listX;

        public PlayerListWidget(Minecraft mc, int width, int height, int y0, int y1, int itemHeight, int listX) {
            super(mc, width, height, y0, y1, itemHeight);
            this.listX = listX;
            this.setLeftPos(listX);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.listX + this.width - 6;
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        public void addPlayerEntry(PlayerEntry entry) {
            this.addEntry(entry);
        }
        

    }

    public class PlayerEntry extends ObjectSelectionList.Entry<PlayerEntry> {
        private final net.minecraft.client.multiplayer.PlayerInfo playerInfo;
        private final String displayName;
        private final net.minecraft.client.gui.components.Button selectButton;

        public PlayerEntry(net.minecraft.client.multiplayer.PlayerInfo playerInfo, String displayName) {
            this.playerInfo = playerInfo;
            this.displayName = displayName;
            this.selectButton = net.minecraft.client.gui.components.Button.builder(
                    Component.translatable("gui.bodycam.select_button"),
                    (btn) -> {
                        PlayerSelectionScreen.this.listWidget.setSelected(this);
                        if (!PlayerSelectionScreen.this.hasReach && PlayerSelectionScreen.this.minecraft.level != null) {
                            net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().getId());
                            String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().getId());
                            String myDim = Minecraft.getInstance().player.level().dimension().location().toString();

                            if (targetPos == null || targetDim == null || !myDim.equals(targetDim) || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                                PlayerSelectionScreen.this.minecraft.player.displayClientMessage(Component.translatable("message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED), false);
                                PlayerSelectionScreen.this.minecraft.setScreen(null);
                                return;
                            }
                        }
                        try {
                            PlayerSelectionScreen.this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.LODESTONE_COMPASS_LOCK, 1.0F));
                            PlayerSelectionScreen.this.minecraft.setScreen(new BodycamViewScreen(this.playerInfo.getProfile().getId(), this.displayName, PlayerSelectionScreen.this.hasReach, PlayerSelectionScreen.this.isOnHologram));
                        } catch (Exception e) {
                        }
                    }
            ).bounds(0, 0, 80, 20).build();

            if (Minecraft.getInstance().level != null) {
                int jammerMode = dev.ClasherHD.bodycam.client.ClientBodycamCache.jammers.getOrDefault(this.playerInfo.getProfile().getId(), 0);

                if (jammerMode == 1) {
                    this.selectButton.active = false;
                } else if (jammerMode == 2) {
                    net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().getId());
                    String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().getId());
                    String myDim = Minecraft.getInstance().player.level().dimension().location().toString();

                    if (targetPos == null || targetDim == null || !myDim.equals(targetDim) || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                        this.selectButton.active = false;
                    }
                }

                if (observingMe.contains(this.playerInfo.getProfile().getId())) {
                    this.selectButton.active = false;
                }

                if (!PlayerSelectionScreen.this.hasReach) {
                    net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().getId());
                    String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().getId());
                    String myDim = Minecraft.getInstance().player.level().dimension().location().toString();

                    if (targetPos == null || targetDim == null || !myDim.equals(targetDim) || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                        this.selectButton.active = false;
                    }
                }
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            ResourceLocation skin = this.playerInfo.getSkinLocation();
            RenderSystem.setShaderTexture(0, skin);
            graphics.blit(skin, left, top + 4, 32, 32, 8.0F, 8.0F, 8, 8, 64, 64);
            graphics.blit(skin, left, top + 4, 32, 32, 40.0F, 8.0F, 8, 8, 64, 64);
            boolean hasAnonymizer = dev.ClasherHD.bodycam.client.ClientBodycamCache.anonymizers.getOrDefault(this.playerInfo.getProfile().getId(), false);
            int jammerMode = dev.ClasherHD.bodycam.client.ClientBodycamCache.jammers.getOrDefault(this.playerInfo.getProfile().getId(), 0);
            int nameColor;

            if (!hasAnonymizer && observingMe.contains(this.playerInfo.getProfile().getId())) {
                nameColor = 0xFF000000 | Integer.parseInt(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_OBSERVING.get(), 16);
            } else if (jammerMode == 1) {
                nameColor = 0xFF000000 | Integer.parseInt(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_BLOCKED.get(), 16);
            } else if (jammerMode == 2) {
                net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().getId());
                String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().getId());
                String myDim = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().location().toString() : "";
                boolean blocked = targetPos == null || targetDim == null || !myDim.equals(targetDim);
                if (!blocked) {
                    blocked = Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get();
                }
                nameColor = blocked ? (0xFF000000 | Integer.parseInt(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_BLOCKED.get(), 16))
                        : (0xFF000000 | Integer.parseInt(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_STANDARD.get(), 16));
            } else {
                nameColor = 0xFF000000 | Integer.parseInt(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_STANDARD.get(), 16);
            }

            graphics.drawString(PlayerSelectionScreen.this.font, this.displayName, left + 40, top + 16, nameColor);
            
            this.selectButton.setX(left + width - 85);
            this.selectButton.setY(top + 10);
            this.selectButton.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.selectButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.displayName);
        }
    }
}
