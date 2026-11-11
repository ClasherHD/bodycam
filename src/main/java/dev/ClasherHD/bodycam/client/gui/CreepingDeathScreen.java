package dev.ClasherHD.bodycam.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ClasherHD.bodycam.client.render.CreepingDeathRenderer;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.creepingdeath.CreepingDeathActionC2SPacket;
import dev.ClasherHD.bodycam.network.creepingdeath.OpenCreepingDeathScreenS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("null")
public class CreepingDeathScreen extends Screen {

    private static final ResourceLocation WINDOW_TEXTURE = new ResourceLocation("bodycam", "textures/gui/gui_window.png");

    private final int mode;
    private final boolean offHand;
    private final List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> entries;
    private final UUID selectedSkinUUID;

    private EntryListWidget listWidget;
    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;

    public CreepingDeathScreen(int mode, boolean offHand, List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> entries, UUID selectedSkinUUID) {
        super(Component.translatable(mode == OpenCreepingDeathScreenS2CPacket.MODE_SKIN
                ? "gui.bodycam.creeping_death.select_skin"
                : "gui.bodycam.creeping_death.select_target"));
        this.mode = mode;
        this.offHand = offHand;
        this.entries = entries;
        this.selectedSkinUUID = selectedSkinUUID;
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
        int bottomReserve = (this.mode == OpenCreepingDeathScreenS2CPacket.MODE_SKIN) ? 66 : 46;
        int listHeight = this.guiHeight - bottomReserve;

        this.listWidget = new EntryListWidget(this.minecraft, listWidth, this.height, listY, listY + listHeight, 40, listX);
        for (OpenCreepingDeathScreenS2CPacket.PlayerEntryData data : this.entries) {
            this.listWidget.addPlayerEntry(new PlayerRowEntry(data));
        }
        this.addWidget(this.listWidget);

        if (this.mode == OpenCreepingDeathScreenS2CPacket.MODE_SKIN) {
            int btnWidth = 170;
            int btnX = this.width / 2 - btnWidth / 2;
            int btnY = this.guiY + this.guiHeight - 28;
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.bodycam.creeping_death.reset_skin"),
                    btn -> {
                        PacketHandler.INSTANCE.sendToServer(new CreepingDeathActionC2SPacket(
                                OpenCreepingDeathScreenS2CPacket.MODE_SKIN, this.offHand, null, ""));
                        this.onClose();
                    }
            ).bounds(btnX, btnY, btnWidth, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        RenderSystem.setShaderColor(0.18F, 0.18F, 0.20F, 1.0F);
        GuiRenderUtils.drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 32, 32, 32, 32, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.listWidget != null) {
            this.listWidget.render(graphics, mouseX, mouseY, partialTick);
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiY + 10, 0xFFFFFFFF);

        if (this.entries.isEmpty() && this.mode == OpenCreepingDeathScreenS2CPacket.MODE_TARGET) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.bodycam.creeping_death.no_targets"),
                    this.width / 2,
                    this.guiY + this.guiHeight / 2 - 4,
                    0xFFFFFFFF
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public class EntryListWidget extends ObjectSelectionList<PlayerRowEntry> {
        private final int listX;

        public EntryListWidget(Minecraft mc, int width, int height, int y0, int y1, int itemHeight, int listX) {
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

        public void addPlayerEntry(PlayerRowEntry entry) {
            this.addEntry(entry);
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

    public class PlayerRowEntry extends ObjectSelectionList.Entry<PlayerRowEntry> {
        private final OpenCreepingDeathScreenS2CPacket.PlayerEntryData data;
        private final Button selectButton;

        public PlayerRowEntry(OpenCreepingDeathScreenS2CPacket.PlayerEntryData data) {
            this.data = data;
            boolean isCurrentlySelectedSkin = (CreepingDeathScreen.this.mode == OpenCreepingDeathScreenS2CPacket.MODE_SKIN
                    && data.uuid().equals(CreepingDeathScreen.this.selectedSkinUUID));

            Component btnLabel = isCurrentlySelectedSkin
                    ? Component.translatable("gui.bodycam.creeping_death.selected")
                    : Component.translatable("gui.bodycam.select_button");

            this.selectButton = Button.builder(
                    btnLabel,
                    btn -> {
                        PacketHandler.INSTANCE.sendToServer(new CreepingDeathActionC2SPacket(
                                CreepingDeathScreen.this.mode,
                                CreepingDeathScreen.this.offHand,
                                this.data.uuid(),
                                this.data.name()
                        ));
                        CreepingDeathScreen.this.onClose();
                    }
            ).bounds(0, 0, 80, 20).build();

            if (data.disabled() || isCurrentlySelectedSkin) {
                this.selectButton.active = false;
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            ResourceLocation skin = CreepingDeathRenderer.getSkinForPlayer(this.data.uuid(), this.data.name());
            graphics.blit(skin, left, top + 4, 32, 32, 8.0F, 8.0F, 8, 8, 64, 64);
            graphics.blit(skin, left, top + 4, 32, 32, 40.0F, 8.0F, 8, 8, 64, 64);

            graphics.drawString(CreepingDeathScreen.this.font, this.data.name(), left + 40, top + 16, 0xFFFFFFFF);

            this.selectButton.setX(left + width - 85);
            this.selectButton.setY(top + 10);
            this.selectButton.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.selectButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.data.name());
        }
    }
}
