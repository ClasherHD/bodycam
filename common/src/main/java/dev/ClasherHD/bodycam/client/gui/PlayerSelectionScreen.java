package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.cache.ClientBodycamCache;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class PlayerSelectionScreen extends Screen {
    public static final Set<UUID> observingMe = ConcurrentHashMap.newKeySet();
    private static final Identifier WINDOW_TEXTURE = bodycam.id("textures/gui/gui_window.png");
    private final int reachLevel;
    private final boolean isOnHologram;
    private final boolean isAllSeeingEye;
    private boolean targetSelected = false;
    private PlayerListWidget listWidget;
    private Button modeToggleButton;
    private boolean showingWatchPoints = false;
    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;

    private int colorObserving;
    private int colorBlocked;
    private int colorOtherDimension;
    private int colorOutOfRange;
    private int colorStandard;

    public PlayerSelectionScreen(int reachLevel, boolean isOnHologram) {
        this(reachLevel, isOnHologram, false);
    }

    public PlayerSelectionScreen(int reachLevel, boolean isOnHologram, boolean isAllSeeingEye) {
        super(Component.translatable("gui.bodycam.select_player"));
        this.reachLevel = reachLevel;
        this.isOnHologram = isOnHologram;
        this.isAllSeeingEye = isAllSeeingEye;
    }

    private boolean isBeyondReach(UUID targetId) {
        if (this.isAllSeeingEye || this.reachLevel >= 2) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return true;
        }
        BlockPos targetPos = ClientBodycamCache.positions.get(targetId);
        String targetDim = ClientBodycamCache.dimensions.get(targetId);
        if (targetPos == null || targetDim == null) {
            return true;
        }
        String myDim = mc.player.level().dimension().identifier().toString();
        if (!myDim.equals(targetDim)) {
            return true;
        }
        if (this.reachLevel >= 1) {
            return false;
        }
        return Math.sqrt(mc.player.blockPosition().distSqr(targetPos)) > (double) ModServerConfig.MAX_MONITOR_DISTANCE.get();
    }

    private boolean isOtherDimensionWithReach(UUID targetId) {
        Minecraft mc = Minecraft.getInstance();
        if (this.reachLevel != 1 || mc.player == null) {
            return false;
        }
        String targetDim = ClientBodycamCache.dimensions.get(targetId);
        return targetDim != null && !mc.player.level().dimension().identifier().toString().equals(targetDim);
    }

    @Override
    protected void init() {
        super.init();
        this.guiWidth = (int) (this.width * 0.7);
        this.guiHeight = (int) (this.height * 0.7);
        this.guiX = (this.width - this.guiWidth) / 2;
        this.guiY = (this.height - this.guiHeight) / 2;

        this.colorObserving = 0xFF000000 | ModClientConfig.parseHexColor(ModClientConfig.COLOR_OBSERVING.get(), 0x5555FF);
        this.colorBlocked = 0xFF000000 | ModClientConfig.parseHexColor(ModClientConfig.COLOR_BLOCKED.get(), 0xFF5555);
        this.colorOtherDimension = 0xFF000000 | ModClientConfig.parseHexColor(ModClientConfig.COLOR_OTHER_DIMENSION.get(), 0xFFAA00);
        this.colorOutOfRange = 0xFF000000 | ModClientConfig.parseHexColor(ModClientConfig.COLOR_OUT_OF_RANGE.get(), 0xFFFF55);
        this.colorStandard = 0xFF000000 | ModClientConfig.parseHexColor(ModClientConfig.COLOR_STANDARD.get(), 0xFFFFFF);

        int listX = this.guiX + 20;
        int listY = this.guiY + 30;
        int listWidth = this.guiWidth - 40;
        int listHeight = this.guiHeight - 66;

        this.listWidget = new PlayerListWidget(this.minecraft, listWidth, listHeight, listY, 40, listX);
        this.addWidget(this.listWidget);

        int toggleBtnWidth = 140;
        int toggleBtnX = this.width / 2 - toggleBtnWidth / 2;
        int toggleBtnY = this.guiY + this.guiHeight - 28;
        this.modeToggleButton = Button.builder(
                Component.translatable(this.showingWatchPoints ? "gui.bodycam.switch_to_players" : "gui.bodycam.switch_to_watch_points"),
                btn -> {
                    this.showingWatchPoints = !this.showingWatchPoints;
                    this.populateCurrentList();
                }
        ).bounds(toggleBtnX, toggleBtnY, toggleBtnWidth, 20).build();
        this.addRenderableWidget(this.modeToggleButton);

        this.populateCurrentList();
    }

    private void populateCurrentList() {
        if (this.listWidget == null) {
            return;
        }
        this.listWidget.clearPlayerEntries();
        this.listWidget.setScrollAmount(0.0D);

        if (this.modeToggleButton != null) {
            this.modeToggleButton.setMessage(Component.translatable(
                    this.showingWatchPoints ? "gui.bodycam.switch_to_players" : "gui.bodycam.switch_to_watch_points"));
        }

        if (this.minecraft == null || this.minecraft.getConnection() == null) {
            return;
        }

        if (!this.showingWatchPoints) {
            for (PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                UUID infoId = info.getProfile().id();
                if (this.minecraft.player != null && !infoId.equals(this.minecraft.player.getUUID())
                        && (this.isAllSeeingEye || !ClientBodycamCache.hidden.contains(infoId))) {
                    this.listWidget.addPlayerEntry(new PlayerEntry(infoId, info.getProfile().name(), info.getSkin().body().texturePath(), false));
                }
            }
        } else if (this.minecraft.player != null) {
            Identifier mySkin = DefaultPlayerSkin.get(this.minecraft.player.getUUID()).body().texturePath();
            PlayerInfo myInfo = this.minecraft.getConnection().getPlayerInfo(this.minecraft.player.getUUID());
            if (myInfo != null) {
                mySkin = myInfo.getSkin().body().texturePath();
            }
            synchronized (ClientBodycamCache.watchPoints) {
                for (Map.Entry<UUID, String> wpEntry : ClientBodycamCache.watchPoints.entrySet()) {
                    this.listWidget.addPlayerEntry(new PlayerEntry(wpEntry.getKey(), wpEntry.getValue(), mySkin, true));
                }
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (this.isAllSeeingEye && !this.targetSelected && !BodycamViewScreen.isMonitoring) {
            PacketHandler.sendToServer(new BodycamResetCameraPacket());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int windowTint = this.isAllSeeingEye ? 0xFFFFD94D : 0xFFFFFFFF;
        GuiRenderUtils.drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 32, 32, 32, 32, 256, 256, windowTint);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (this.listWidget != null) {
            this.listWidget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        Component headerTitle = Component.translatable(this.showingWatchPoints ? "gui.bodycam.select_watch_point" : "gui.bodycam.select_player");
        if (this.isAllSeeingEye) {
            headerTitle = Component.literal("\u25c9 ").append(headerTitle).append(Component.literal(" \u25c9"));
        }
        graphics.centeredText(this.font, headerTitle, this.width / 2, this.guiY + 10, 0xFFFFFFFF);
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
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
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

        public void clearPlayerEntries() {
            this.clearEntries();
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getBottom());
            super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.disableScissor();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.y() < this.getY() || event.y() > this.getBottom()) {
                return false;
            }
            return super.mouseClicked(event, doubleClick);
        }
    }

    public class PlayerEntry extends ObjectSelectionList.Entry<PlayerEntry> {
        private final UUID targetId;
        private final String displayName;
        private final Identifier skinLocation;
        private final boolean isWatchPoint;
        private final Button selectButton;

        public PlayerEntry(UUID targetId, String displayName, Identifier skinLocation, boolean isWatchPoint) {
            this.targetId = targetId;
            this.displayName = displayName;
            this.skinLocation = skinLocation;
            this.isWatchPoint = isWatchPoint;
            this.selectButton = Button.builder(
                    Component.translatable("gui.bodycam.select_button"),
                    (btn) -> {
                        PlayerSelectionScreen.this.listWidget.setSelected(this);
                        if (PlayerSelectionScreen.this.minecraft.level != null && PlayerSelectionScreen.this.isBeyondReach(this.targetId)) {
                            PlayerSelectionScreen.this.minecraft.player.sendSystemMessage(Component.translatable("message.bodycam.signal_weak").withStyle(ChatFormatting.RED));
                            PlayerSelectionScreen.this.minecraft.gui.setScreen(null);
                            return;
                        }
                        try {
                            PlayerSelectionScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_RESONATE1.get(), 2.0F, 0.06F));
                            PlayerSelectionScreen.this.minecraft.getSoundManager().playDelayed(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_RESONATE2.get(), 2.0F, 0.06F), 10);
                            PlayerSelectionScreen.this.minecraft.getSoundManager().playDelayed(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_RESONATE3.get(), 2.0F, 0.06F), 20);
                            PlayerSelectionScreen.this.minecraft.getSoundManager().playDelayed(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_RESONATE4.get(), 2.0F, 0.06F), 30);
                            PlayerSelectionScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_SHIMMER.get(), 2.0F, 0.25F));
                            PlayerSelectionScreen.this.targetSelected = true;
                            PlayerSelectionScreen.this.minecraft.gui.setScreen(new BodycamViewScreen(
                                    this.targetId, this.displayName, PlayerSelectionScreen.this.isOnHologram, this.isWatchPoint, PlayerSelectionScreen.this.isAllSeeingEye));
                        } catch (Exception e) {
                        }
                    }
            ).bounds(0, 0, 80, 20).build();

            if (!PlayerSelectionScreen.this.isAllSeeingEye && Minecraft.getInstance().level != null) {
                int jammerMode = ClientBodycamCache.jammers.getOrDefault(this.targetId, 0);

                if (jammerMode == 1) {
                    this.selectButton.active = false;
                } else if (jammerMode == 2) {
                    BlockPos targetPos = ClientBodycamCache.positions.get(this.targetId);
                    String targetDim = ClientBodycamCache.dimensions.get(this.targetId);
                    String myDim = Minecraft.getInstance().player.level().dimension().identifier().toString();

                    if (targetPos == null || targetDim == null || !myDim.equals(targetDim)
                            || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
                        this.selectButton.active = false;
                    }
                }

                if (observingMe.contains(this.targetId)) {
                    this.selectButton.active = false;
                }

                if (PlayerSelectionScreen.this.isBeyondReach(this.targetId)) {
                    this.selectButton.active = false;
                }
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            int left = this.getContentX();
            int top = this.getContentY();
            int width = this.getContentWidth();

            graphics.blit(RenderPipelines.GUI_TEXTURED, this.skinLocation, left, top + 4, 8.0F, 8.0F, 32, 32, 8, 8, 64, 64);
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.skinLocation, left, top + 4, 40.0F, 8.0F, 32, 32, 8, 8, 64, 64);
            boolean hasAnonymizer = ClientBodycamCache.anonymizers.getOrDefault(this.targetId, false);
            int jammerMode = ClientBodycamCache.jammers.getOrDefault(this.targetId, 0);

            BlockPos targetPos = ClientBodycamCache.positions.get(this.targetId);
            String targetDim = ClientBodycamCache.dimensions.get(this.targetId);
            String myDim = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().identifier().toString() : "";

            boolean isOutOfRangeOrDimension = targetPos == null || targetDim == null || !myDim.equals(targetDim);
            if (!isOutOfRangeOrDimension && Minecraft.getInstance().player != null) {
                isOutOfRangeOrDimension = Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (double) ModServerConfig.MAX_MONITOR_DISTANCE.get();
            }

            boolean isBlocked = jammerMode == 1 || (jammerMode == 2 && isOutOfRangeOrDimension);

            int nameColor;
            if (PlayerSelectionScreen.this.isAllSeeingEye) {
                nameColor = 0xFFFFFFFF;
            } else if (!hasAnonymizer && observingMe.contains(this.targetId)) {
                nameColor = PlayerSelectionScreen.this.colorObserving;
            } else if (isBlocked) {
                nameColor = PlayerSelectionScreen.this.colorBlocked;
            } else if (PlayerSelectionScreen.this.isOtherDimensionWithReach(this.targetId)) {
                nameColor = PlayerSelectionScreen.this.colorOtherDimension;
            } else if (PlayerSelectionScreen.this.isBeyondReach(this.targetId)) {
                nameColor = PlayerSelectionScreen.this.colorOutOfRange;
            } else {
                nameColor = PlayerSelectionScreen.this.colorStandard;
            }

            graphics.text(PlayerSelectionScreen.this.font, this.displayName, left + 40, top + 16, nameColor);

            this.selectButton.setX(left + width - 85);
            this.selectButton.setY(top + 10);
            this.selectButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.selectButton.mouseClicked(event, doubleClick);
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.displayName);
        }
    }
}
