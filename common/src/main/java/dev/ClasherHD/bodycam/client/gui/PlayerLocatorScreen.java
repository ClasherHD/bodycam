package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorSelectC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("null")
public class PlayerLocatorScreen extends Screen {

    private static final ResourceLocation WINDOW_TEXTURE = bodycam.id("textures/gui/gui_window.png");
    private final Map<UUID, Integer> jammers;
    private final Map<UUID, String> dimensions;
    private final Map<UUID, BlockPos> positions;
    private final UUID currentTarget;
    private final int reachLevel;

    private PlayerListWidget listWidget;
    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;

    public PlayerLocatorScreen(Map<UUID, Integer> jammers, Map<UUID, String> dimensions, Map<UUID, BlockPos> positions, UUID currentTarget, int reachLevel) {
        super(Component.translatable("gui.bodycam.player_locator"));
        this.jammers = jammers;
        this.dimensions = dimensions;
        this.positions = positions;
        this.currentTarget = currentTarget;
        this.reachLevel = reachLevel;
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

        this.listWidget = new PlayerListWidget(this.minecraft, listWidth, listHeight, listY, 40, listX);

        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            if (this.currentTarget != null && this.jammers.containsKey(this.currentTarget)) {
                for (PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                    if (info.getProfile().getId().equals(this.currentTarget)) {
                        this.listWidget.addPlayerEntry(new PlayerEntry(info, info.getProfile().getName(), true));
                        break;
                    }
                }
            }

            for (PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                if (this.minecraft.player != null && !info.getProfile().getId().equals(this.minecraft.player.getUUID()) && this.jammers.containsKey(info.getProfile().getId())) {
                    if (this.currentTarget == null || !info.getProfile().getId().equals(this.currentTarget)) {
                        this.listWidget.addPlayerEntry(new PlayerEntry(info, info.getProfile().getName(), false));
                    }
                }
            }
        }
        this.addWidget(this.listWidget);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiRenderUtils.drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 32, 32, 32, 32, 256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.listWidget != null) {
            this.listWidget.render(graphics, mouseX, mouseY, partialTick);
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiY + 10, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public class PlayerListWidget extends ObjectSelectionList<PlayerEntry> {
        private final int listX;

        public PlayerListWidget(Minecraft mc, int width, int height, int y, int itemHeight, int listX) {
            super(mc, width, height, y, itemHeight);
            this.listX = listX;
            this.setX(listX);
        }

        @Override
        protected void renderListBackground(GuiGraphics graphics) {
        }

        @Override
        protected void renderListSeparators(GuiGraphics graphics) {
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
        private final PlayerInfo playerInfo;
        private final String displayName;
        private final boolean isCurrentTarget;
        private final Button selectButton;

        public PlayerEntry(PlayerInfo playerInfo, String displayName, boolean isCurrentTarget) {
            this.playerInfo = playerInfo;
            this.displayName = displayName;
            this.isCurrentTarget = isCurrentTarget;

            this.selectButton = Button.builder(
                    Component.translatable(isCurrentTarget ? "gui.bodycam.locator_deselect" : "gui.bodycam.select_button"),
                    (btn) -> {
                        if (isCurrentTarget) {
                            PacketHandler.sendToServer(new PlayerLocatorSelectC2SPacket(null, true));
                        } else {
                            PacketHandler.sendToServer(new PlayerLocatorSelectC2SPacket(this.playerInfo.getProfile().getId(), false));
                        }
                        if (PlayerLocatorScreen.this.minecraft != null) {
                            PlayerLocatorScreen.this.minecraft.setScreen(null);
                        }
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
            boolean hasTargetPos = targetPos != null && myPos != null;
            double distSq = hasTargetPos ? myPos.distSqr(targetPos) : Double.MAX_VALUE;
            double maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
            double maxDistSq = maxDist * maxDist;

            boolean isJammed = (jammerMode == 1) || (jammerMode == 2 && (!sameDim || !hasTargetPos || distSq > maxDistSq));

            int nameColor;
            int highlightColor;
            if (isJammed) {
                nameColor = 0xFFFF5555;
                highlightColor = 0x2AFF5555;
            } else if (sameDim) {
                if ((hasTargetPos && distSq <= maxDistSq) || PlayerLocatorScreen.this.reachLevel >= 1) {
                    nameColor = 0xFF55FF55;
                    highlightColor = 0x2A55FF55;
                } else {
                    nameColor = 0xFFFFFF55;
                    highlightColor = 0x2AFFFF55;
                }
            } else {
                if (PlayerLocatorScreen.this.reachLevel >= 2) {
                    nameColor = 0xFFFFB000;
                    highlightColor = 0x2AFFB000;
                } else {
                    nameColor = 0xFFFFFF55;
                    highlightColor = 0x2AFFFF55;
                }
            }

            if (isCurrentTarget) {
                graphics.fill(left - 2, top + 1, left + width - 87, top + height - 1, highlightColor);
            }

            ResourceLocation skin = this.playerInfo.getSkin().texture();
            graphics.blit(skin, left + 4, top + 2, 32, 32, 8.0F, 8.0F, 8, 8, 64, 64);
            graphics.blit(skin, left + 4, top + 2, 32, 32, 40.0F, 8.0F, 8, 8, 64, 64);

            graphics.drawString(PlayerLocatorScreen.this.font, this.displayName, left + 44, top + 14, nameColor);

            this.selectButton.setX(left + width - 85);
            this.selectButton.setY(top + 8);
            this.selectButton.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.selectButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.displayName);
        }
    }
}
