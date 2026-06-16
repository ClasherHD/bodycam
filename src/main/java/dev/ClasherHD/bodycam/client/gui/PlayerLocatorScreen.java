package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorSelectC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("null")
public class PlayerLocatorScreen extends Screen {

    private static final ResourceLocation WINDOW_TEXTURE = new ResourceLocation("bodycam", "textures/gui/gui_window.png");
    private final Map<UUID, Integer> jammers;
    private final Map<UUID, String> dimensions;
    private final Map<UUID, BlockPos> positions;
    private final UUID currentTarget;
    private final boolean hasReach;

    private PlayerListWidget listWidget;
    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;

    public PlayerLocatorScreen(Map<UUID, Integer> jammers, Map<UUID, String> dimensions, Map<UUID, BlockPos> positions, UUID currentTarget, boolean hasReach) {
        super(Component.translatable("gui.bodycam.player_locator"));
        this.jammers = jammers;
        this.dimensions = dimensions;
        this.positions = positions;
        this.currentTarget = currentTarget;
        this.hasReach = hasReach;
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
            // First, check if the current target is online and add them to the top
            if (this.currentTarget != null) {
                for (net.minecraft.client.multiplayer.PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                    if (info.getProfile().getId().equals(this.currentTarget)) {
                        this.listWidget.addPlayerEntry(new PlayerEntry(info, info.getProfile().getName(), true));
                        break;
                    }
                }
            }

            // Then, add all other online players (excluding ourselves and the current target)
            for (net.minecraft.client.multiplayer.PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                if (this.minecraft.player != null && !info.getProfile().getId().equals(this.minecraft.player.getUUID())) {
                    if (this.currentTarget == null || !info.getProfile().getId().equals(this.currentTarget)) {
                        this.listWidget.addPlayerEntry(new PlayerEntry(info, info.getProfile().getName(), false));
                    }
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
        private final boolean isCurrentTarget;
        private final net.minecraft.client.gui.components.Button selectButton;

        public PlayerEntry(net.minecraft.client.multiplayer.PlayerInfo playerInfo, String displayName, boolean isCurrentTarget) {
            this.playerInfo = playerInfo;
            this.displayName = displayName;
            this.isCurrentTarget = isCurrentTarget;

            this.selectButton = net.minecraft.client.gui.components.Button.builder(
                    Component.translatable(isCurrentTarget ? "gui.bodycam.locator_deselect" : "gui.bodycam.select_button"),
                    (btn) -> {
                        if (isCurrentTarget) {
                            PacketHandler.INSTANCE.sendToServer(new PlayerLocatorSelectC2SPacket(null, true));
                        } else {
                            PacketHandler.INSTANCE.sendToServer(new PlayerLocatorSelectC2SPacket(this.playerInfo.getProfile().getId(), false));
                        }
                        PlayerLocatorScreen.this.minecraft.setScreen(null);
                    }
            ).bounds(0, 0, 80, 20).build();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            UUID uuid = this.playerInfo.getProfile().getId();
            int jammerMode = PlayerLocatorScreen.this.jammers.getOrDefault(uuid, 0);
            String targetDim = PlayerLocatorScreen.this.dimensions.get(uuid);
            BlockPos targetPos = PlayerLocatorScreen.this.positions.get(uuid);
            String myDim = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().location().toString() : "";
            BlockPos myPos = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.blockPosition() : BlockPos.ZERO;

            boolean sameDim = myDim.equals(targetDim);
            double distSq = (targetPos != null && myPos != null) ? myPos.distSqr(targetPos) : 0;
            double maxDist = dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get();
            double maxDistSq = maxDist * maxDist;

            boolean isJammed = (jammerMode == 1) || (jammerMode == 2 && (!sameDim || distSq > maxDistSq));

            int nameColor;
            int highlightColor;
            if (isJammed) {
                nameColor = 0xFFFF5555; // Red
                highlightColor = 0x2AFF5555;
            } else if (sameDim) {
                if (distSq <= maxDistSq) {
                    nameColor = 0xFF55FF55; // Green
                    highlightColor = 0x2A55FF55;
                } else {
                    nameColor = 0xFFFFFF55; // Yellow
                    highlightColor = 0x2AFFFF55;
                }
            } else {
                if (PlayerLocatorScreen.this.hasReach) {
                    nameColor = 0xFFFFB000; // Orange
                    highlightColor = 0x2AFFB000;
                } else {
                    nameColor = 0xFFFFFF55; // Yellow
                    highlightColor = 0x2AFFFF55;
                }
            }

            if (isCurrentTarget) {
                // Glow highlight for active tracked player (dynamically colored)
                graphics.fill(left - 2, top + 1, left + width - 87, top + height - 1, highlightColor);
            }

            ResourceLocation skin = this.playerInfo.getSkinLocation();
            graphics.blit(skin, left + 4, top + 2, 32, 32, 8.0F, 8.0F, 8, 8, 64, 64);
            graphics.blit(skin, left + 4, top + 2, 32, 32, 40.0F, 8.0F, 8, 8, 64, 64);

            graphics.drawString(PlayerLocatorScreen.this.font, this.displayName, left + 44, top + 14, nameColor);

            this.selectButton.setX(left + width - 85);
            this.selectButton.setY(top + 8);
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
