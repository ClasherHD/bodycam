package dev.ClasherHD.bodycam.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSelectionScreen extends Screen {

    public static final Set<UUID> observingMe = ConcurrentHashMap.newKeySet();
    private static final Identifier WINDOW_TEXTURE = Identifier.fromNamespaceAndPath("bodycam", "textures/gui/gui_window.png");
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
                if (this.minecraft.player != null && !info.getProfile().id().equals(this.minecraft.player.getUUID())) {
                    this.listWidget.addPlayerEntry(new PlayerEntry(info, info.getProfile().name()));
                }
            }
        }
        this.addWidget(this.listWidget);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 64, 64, 512, 512, 3840, 2160);
        
        if (this.listWidget != null) {
            this.listWidget.render(graphics, mouseX, mouseY, partialTick);
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiY + 10, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawNineSlice(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height, int rCW, int rCH, int uvCW, int uvCH, int texW, int texH) {
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x, y, 0.0F, 0.0F, rCW, rCH, uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + width - rCW, y, (float)(texW - uvCW), 0.0F, rCW, rCH, uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x, y + height - rCH, 0.0F, (float)(texH - uvCH), rCW, rCH, uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + width - rCW, y + height - rCH, (float)(texW - uvCW), (float)(texH - uvCH), rCW, rCH, uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + rCW, y, (float)uvCW, 0.0F, width - 2 * rCW, rCH, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + rCW, y + height - rCH, (float)uvCW, (float)(texH - uvCH), width - 2 * rCW, rCH, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x, y + rCH, 0.0F, (float)uvCH, rCW, height - 2 * rCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + width - rCW, y + rCH, (float)(texW - uvCW), (float)uvCH, rCW, height - 2 * rCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, x + rCW, y + rCH, (float)uvCW, (float)uvCH, width - 2 * rCW, height - 2 * rCH, texW - 2 * uvCW, texH - 2 * uvCH, texW, texH);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public class PlayerListWidget extends ObjectSelectionList<PlayerEntry> {
        private final int listX;

        public PlayerListWidget(Minecraft mc, int width, int height, int y0, int y1, int itemHeight, int listX) {
            super(mc, width, y1 - y0, y0, itemHeight);
            this.listX = listX;
            this.setX(listX);
        }

        @Override
        protected int scrollBarX() {
            return this.listX + this.width - 6;
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        public void addPlayerEntry(PlayerEntry entry) {
            this.addEntry(entry);
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {}

        @Override
        protected void renderListSeparators(GuiGraphics guiGraphics) {}
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
                            net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().id());
                            String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().id());
                            String myDim = Minecraft.getInstance().player.level().dimension().identifier().toString();

                            if (targetPos == null || targetDim == null || !myDim.equals(targetDim) || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                                PlayerSelectionScreen.this.minecraft.player.displayClientMessage(Component.translatable("message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED), false);
                                PlayerSelectionScreen.this.minecraft.setScreen(null);
                                return;
                            }
                        }
                        try {
                            PlayerSelectionScreen.this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.LODESTONE_COMPASS_LOCK, 1.0F));
                            PlayerSelectionScreen.this.minecraft.setScreen(new BodycamViewScreen(this.playerInfo.getProfile().id(), this.displayName, PlayerSelectionScreen.this.hasReach, PlayerSelectionScreen.this.isOnHologram));
                        } catch (Exception e) {
                        }
                    }
            ).bounds(0, 0, 80, 20).build();

            if (Minecraft.getInstance().level != null) {
                int jammerMode = dev.ClasherHD.bodycam.client.ClientBodycamCache.jammers.getOrDefault(this.playerInfo.getProfile().id(), 0);

                if (jammerMode == 1) {
                    this.selectButton.active = false;
                } else if (jammerMode == 2) {
                    net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().id());
                    String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().id());
                    String myDim = Minecraft.getInstance().player.level().dimension().identifier().toString();

                    if (targetPos == null || targetDim == null || !myDim.equals(targetDim) || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                        this.selectButton.active = false;
                    }
                }

                if (observingMe.contains(this.playerInfo.getProfile().id())) {
                    this.selectButton.active = false;
                }

                if (!PlayerSelectionScreen.this.hasReach) {
                    net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().id());
                    String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().id());
                    String myDim = Minecraft.getInstance().player.level().dimension().identifier().toString();

                    if (targetPos == null || targetDim == null || !myDim.equals(targetDim) || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                        this.selectButton.active = false;
                    }
                }
            }
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            int left = this.getContentX();
            int top = this.getContentY();
            int width = this.getContentWidth();

            if (Minecraft.getInstance().level != null) {
                boolean active = true;
                int jammerMode = dev.ClasherHD.bodycam.client.ClientBodycamCache.jammers.getOrDefault(this.playerInfo.getProfile().id(), 0);

                if (jammerMode == 1) {
                    active = false;
                } else if (jammerMode == 2) {
                    net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().id());
                    String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().id());
                    String myDim = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().identifier().toString() : "";

                    if (targetPos == null || targetDim == null || !myDim.equals(targetDim) || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                        active = false;
                    }
                }

                if (observingMe.contains(this.playerInfo.getProfile().id())) {
                    active = false;
                }

                if (!PlayerSelectionScreen.this.hasReach) {
                    net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().id());
                    String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().id());
                    String myDim = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().identifier().toString() : "";

                    if (targetPos == null || targetDim == null || !myDim.equals(targetDim) || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                        active = false;
                    }
                }

                this.selectButton.active = active;
            }

            Identifier skin = this.playerInfo.getSkin().body().texturePath();
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, left, top + 4, 8.0F, 8.0F, 32, 32, 8, 8, 64, 64);
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, left, top + 4, 40.0F, 8.0F, 32, 32, 8, 8, 64, 64);
            boolean hasAnonymizer = dev.ClasherHD.bodycam.client.ClientBodycamCache.anonymizers.getOrDefault(this.playerInfo.getProfile().id(), false);
            int jammerMode = dev.ClasherHD.bodycam.client.ClientBodycamCache.jammers.getOrDefault(this.playerInfo.getProfile().id(), 0);
            int nameColor;

            if (!hasAnonymizer && observingMe.contains(this.playerInfo.getProfile().id())) {
                nameColor = 0xFF000000 | Integer.parseInt(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_OBSERVING.get(), 16);
            } else if (jammerMode == 1) {
                nameColor = 0xFF000000 | Integer.parseInt(dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_BLOCKED.get(), 16);
            } else if (jammerMode == 2) {
                net.minecraft.core.BlockPos targetPos = dev.ClasherHD.bodycam.client.ClientBodycamCache.positions.get(this.playerInfo.getProfile().id());
                String targetDim = dev.ClasherHD.bodycam.client.ClientBodycamCache.dimensions.get(this.playerInfo.getProfile().id());
                String myDim = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().identifier().toString() : "";
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
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean focused) {
            if (this.selectButton.mouseClicked(event, focused)) {
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
