package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.client.cache.ClientBodycamCache;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class PlayerSelectionScreen extends Screen {

    public static final Set<UUID> observingMe = ConcurrentHashMap.newKeySet();
    private static final ResourceLocation WINDOW_TEXTURE = new ResourceLocation("bodycam", "textures/gui/gui_window.png");
    private final int reachLevel;
    private final boolean isOnHologram;
    private PlayerListWidget listWidget;
    private net.minecraft.client.gui.components.Button modeToggleButton;
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
        super(Component.translatable("gui.bodycam.select_player"));
        this.reachLevel = reachLevel;
        this.isOnHologram = isOnHologram;
    }

    private boolean isBeyondReach(UUID targetId) {
        if (this.reachLevel >= 2) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return true;
        }
        net.minecraft.core.BlockPos targetPos = ClientBodycamCache.positions.get(targetId);
        String targetDim = ClientBodycamCache.dimensions.get(targetId);
        if (targetPos == null || targetDim == null) {
            return true;
        }
        String myDim = mc.player.level().dimension().location().toString();
        if (!myDim.equals(targetDim)) {
            return true;
        }
        if (this.reachLevel >= 1) {
            return false;
        }
        return Math.sqrt(mc.player.blockPosition().distSqr(targetPos)) > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get();
    }

    private boolean isOtherDimensionWithReach(UUID targetId) {
        Minecraft mc = Minecraft.getInstance();
        if (this.reachLevel != 1 || mc.player == null) {
            return false;
        }
        String targetDim = ClientBodycamCache.dimensions.get(targetId);
        return targetDim != null && !mc.player.level().dimension().location().toString().equals(targetDim);
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

        this.listWidget = new PlayerListWidget(this.minecraft, listWidth, this.height, listY, listY + listHeight, 40, listX);
        this.addWidget(this.listWidget);

        int toggleBtnWidth = 140;
        int toggleBtnX = this.width / 2 - toggleBtnWidth / 2;
        int toggleBtnY = this.guiY + this.guiHeight - 28;
        this.modeToggleButton = net.minecraft.client.gui.components.Button.builder(
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
            for (net.minecraft.client.multiplayer.PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
                UUID infoId = info.getProfile().getId();
                if (this.minecraft.player != null && !infoId.equals(this.minecraft.player.getUUID()) && !ClientBodycamCache.hidden.contains(infoId)) {
                    this.listWidget.addPlayerEntry(new PlayerEntry(infoId, info.getProfile().getName(), info.getSkinLocation(), false));
                }
            }
        } else if (this.minecraft.player != null) {
            ResourceLocation mySkin = DefaultPlayerSkin.getDefaultSkin(this.minecraft.player.getUUID());
            net.minecraft.client.multiplayer.PlayerInfo myInfo = this.minecraft.getConnection().getPlayerInfo(this.minecraft.player.getUUID());
            if (myInfo != null) {
                mySkin = myInfo.getSkinLocation();
            }
            for (Map.Entry<UUID, String> wpEntry : ClientBodycamCache.watchPoints.entrySet()) {
                this.listWidget.addPlayerEntry(new PlayerEntry(wpEntry.getKey(), wpEntry.getValue(), mySkin, true));
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        GuiRenderUtils.drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 32, 32, 32, 32, 256, 256);

        if (this.listWidget != null) {
            this.listWidget.render(graphics, mouseX, mouseY, partialTick);
        }

        Component headerTitle = Component.translatable(this.showingWatchPoints ? "gui.bodycam.select_watch_point" : "gui.bodycam.select_player");
        graphics.drawCenteredString(this.font, headerTitle, this.width / 2, this.guiY + 10, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
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

        public void clearPlayerEntries() {
            this.clearEntries();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.enableScissor(this.listX, this.y0, this.listX + this.width, this.y1);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseY < this.y0 || mouseY > this.y1) {
                return false;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public class PlayerEntry extends ObjectSelectionList.Entry<PlayerEntry> {
        private final UUID targetId;
        private final String displayName;
        private final ResourceLocation skinLocation;
        private final boolean isWatchPoint;
        private final net.minecraft.client.gui.components.Button selectButton;

        public PlayerEntry(UUID targetId, String displayName, ResourceLocation skinLocation, boolean isWatchPoint) {
            this.targetId = targetId;
            this.displayName = displayName;
            this.skinLocation = skinLocation;
            this.isWatchPoint = isWatchPoint;
            this.selectButton = net.minecraft.client.gui.components.Button.builder(
                    Component.translatable("gui.bodycam.select_button"),
                    (btn) -> {
                        PlayerSelectionScreen.this.listWidget.setSelected(this);
                        if (PlayerSelectionScreen.this.minecraft.level != null && PlayerSelectionScreen.this.isBeyondReach(this.targetId)) {
                            PlayerSelectionScreen.this.minecraft.player.displayClientMessage(Component.translatable("message.bodycam.signal_weak").withStyle(net.minecraft.ChatFormatting.RED), false);
                            PlayerSelectionScreen.this.minecraft.setScreen(null);
                            return;
                        }
                        try {
                            PlayerSelectionScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_RESONATE1.get(), 2.0F, 0.06F));
                            PlayerSelectionScreen.this.minecraft.getSoundManager().playDelayed(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_RESONATE2.get(), 2.0F, 0.06F), 10);
                            PlayerSelectionScreen.this.minecraft.getSoundManager().playDelayed(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_RESONATE3.get(), 2.0F, 0.06F), 20);
                            PlayerSelectionScreen.this.minecraft.getSoundManager().playDelayed(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_RESONATE4.get(), 2.0F, 0.06F), 30);
                            PlayerSelectionScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.MONITOR_SELECT_SHIMMER.get(), 2.0F, 0.25F));
                            PlayerSelectionScreen.this.minecraft.setScreen(new BodycamViewScreen(
                                    this.targetId, this.displayName, PlayerSelectionScreen.this.isOnHologram, this.isWatchPoint));
                        } catch (Exception e) {
                        }
                    }
            ).bounds(0, 0, 80, 20).build();

            if (Minecraft.getInstance().level != null) {
                int jammerMode = ClientBodycamCache.jammers.getOrDefault(this.targetId, 0);

                if (jammerMode == 1) {
                    this.selectButton.active = false;
                } else if (jammerMode == 2) {
                    net.minecraft.core.BlockPos targetPos = ClientBodycamCache.positions.get(this.targetId);
                    String targetDim = ClientBodycamCache.dimensions.get(this.targetId);
                    String myDim = Minecraft.getInstance().player.level().dimension().location().toString();

                    if (targetPos == null || targetDim == null || !myDim.equals(targetDim)
                            || Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (float) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get()) {
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
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            graphics.blit(this.skinLocation, left, top + 4, 32, 32, 8.0F, 8.0F, 8, 8, 64, 64);
            graphics.blit(this.skinLocation, left, top + 4, 32, 32, 40.0F, 8.0F, 8, 8, 64, 64);
            boolean hasAnonymizer = ClientBodycamCache.anonymizers.getOrDefault(this.targetId, false);
            int jammerMode = ClientBodycamCache.jammers.getOrDefault(this.targetId, 0);

            net.minecraft.core.BlockPos targetPos = ClientBodycamCache.positions.get(this.targetId);
            String targetDim = ClientBodycamCache.dimensions.get(this.targetId);
            String myDim = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().location().toString() : "";

            boolean isOutOfRangeOrDimension = targetPos == null || targetDim == null || !myDim.equals(targetDim);
            if (!isOutOfRangeOrDimension) {
                isOutOfRangeOrDimension = Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(targetPos)) > (double) dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.get();
            }

            boolean isBlocked = jammerMode == 1 || (jammerMode == 2 && isOutOfRangeOrDimension);

            int nameColor;
            if (!hasAnonymizer && observingMe.contains(this.targetId)) {
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
