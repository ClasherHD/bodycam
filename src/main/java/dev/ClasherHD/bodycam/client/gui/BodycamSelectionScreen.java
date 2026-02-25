package dev.ClasherHD.bodycam.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class BodycamSelectionScreen extends Screen {

    private final boolean hasReach;

    public BodycamSelectionScreen(boolean hasReach) {
        super(Component.translatable("item.bodycam.bodycam_monitor"));
        this.hasReach = hasReach;
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        class PlayerEntry {
            final java.util.UUID uuid;
            final String name;

            PlayerEntry(java.util.UUID uuid, String name) {
                this.uuid = uuid;
                this.name = name;
            }
        }

        List<PlayerEntry> players = new java.util.ArrayList<>();

        if (this.hasReach && mc.getConnection() != null) {
            for (net.minecraft.client.multiplayer.PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                if (!info.getProfile().getId().equals(mc.player.getUUID())) {
                    players.add(new PlayerEntry(info.getProfile().getId(), info.getProfile().getName()));
                }
            }
        } else {
            for (Player p : mc.level.players()) {
                if (p != mc.player) {
                    players.add(new PlayerEntry(p.getUUID(), p.getName().getString()));
                }
            }
        }

        int maxPlayers = Math.min(6, players.size());
        int buttonWidth = 200;
        int buttonHeight = 20;
        int startX = (this.width - buttonWidth) / 2;
        int startY = (this.height - (maxPlayers * (buttonHeight + 5))) / 2;

        for (int i = 0; i < maxPlayers; i++) {
            PlayerEntry p = players.get(i);
            int y = startY + i * (buttonHeight + 5);
            this.addRenderableWidget(Button.builder(Component.literal(p.name), (btn) -> {
                try {
                    net.minecraft.client.Minecraft.getInstance().getSoundManager()
                            .play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                                    .forUI(net.minecraft.sounds.SoundEvents.LODESTONE_COMPASS_LOCK, 1.0F));
                    this.minecraft.setScreen(new BodycamViewScreen(p.uuid, p.name, this.hasReach));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).bounds(startX, y, buttonWidth, buttonHeight).build());
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, 0xDD000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.children().isEmpty()) {
            Component text = Component.translatable("gui.bodycam.signal_lost");
            int textWidth = this.font.width(text);
            graphics.drawString(this.font, text, (this.width - textWidth) / 2, this.height / 2, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
