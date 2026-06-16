package dev.ClasherHD.bodycam.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class ClientConfigScreen extends Screen {
    private final Screen previous;

    private final boolean initialName;
    private final boolean initialHealth;
    private final boolean initialShift;
    private final String initialStd;
    private final String initialBlk;
    private final String initialObs;
    private final String initialDim;
    private final String initialOor;

    private boolean nameVal;
    private boolean healthVal;
    private boolean shiftVal;
    private int stdColorIdx;
    private int blkColorIdx;
    private int obsColorIdx;
    private int dimColorIdx;
    private int oorColorIdx;

    private Button btnName;
    private Button btnHealth;
    private Button btnShift;
    private Button btnReset;
    private Button btnColorStd;
    private Button btnColorBlk;
    private Button btnColorObs;
    private Button btnColorDim;
    private Button btnColorOor;
    private Button btnDone;

    private double scrollY = 0;
    private int maxScroll = 70;

    private static class PresetColor {
        final String translationKey;
        final String hex;
        final int intColor;

        PresetColor(String translationKey, String hex) {
            this.translationKey = translationKey;
            this.hex = hex;
            this.intColor = Integer.parseInt(hex, 16);
        }
    }

    private static final PresetColor[] PRESET_COLORS = new PresetColor[] {
        new PresetColor("color.bodycam.white", "FFFFFF"),
        new PresetColor("color.bodycam.gray", "AAAAAA"),
        new PresetColor("color.bodycam.dark_gray", "555555"),
        new PresetColor("color.bodycam.black", "000000"),
        new PresetColor("color.bodycam.red", "FF5555"),
        new PresetColor("color.bodycam.dark_red", "AA0000"),
        new PresetColor("color.bodycam.orange", "FFAA00"),
        new PresetColor("color.bodycam.yellow", "FFFF55"),
        new PresetColor("color.bodycam.green", "55FF55"),
        new PresetColor("color.bodycam.dark_green", "00AA00"),
        new PresetColor("color.bodycam.aqua", "55FFFF"),
        new PresetColor("color.bodycam.dark_aqua", "00AAAA"),
        new PresetColor("color.bodycam.blue", "5555FF"),
        new PresetColor("color.bodycam.dark_blue", "0000AA"),
        new PresetColor("color.bodycam.purple", "FF55FF"),
        new PresetColor("color.bodycam.dark_purple", "AA00AA"),
        new PresetColor("color.bodycam.pink", "FF8888"),
        new PresetColor("color.bodycam.brown", "8B4513")
    };

    public ClientConfigScreen(Screen previous) {
        super(Component.translatable("gui.bodycam.config.title"));
        this.previous = previous;

        this.initialName = dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_NAME_OVERLAY.get();
        this.initialHealth = dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_HEALTH_OVERLAY.get();
        this.initialShift = dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_SHIFT_OVERLAY.get();
        this.initialStd = dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_STANDARD.get();
        this.initialBlk = dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_BLOCKED.get();
        this.initialObs = dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_OBSERVING.get();
        this.initialDim = dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_DIMENSION.get();
        this.initialOor = dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_OUT_OF_RANGE.get();

        this.nameVal = this.initialName;
        this.healthVal = this.initialHealth;
        this.shiftVal = this.initialShift;
        this.stdColorIdx = this.getClosestPresetIndex(this.initialStd);
        this.blkColorIdx = this.getClosestPresetIndex(this.initialBlk);
        this.obsColorIdx = this.getClosestPresetIndex(this.initialObs);
        this.dimColorIdx = this.getClosestPresetIndex(this.initialDim);
        this.oorColorIdx = this.getClosestPresetIndex(this.initialOor);
    }

    private int getClosestPresetIndex(String hex) {
        int targetColor;
        try {
            targetColor = Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
        int r = (targetColor >> 16) & 0xFF;
        int g = (targetColor >> 8) & 0xFF;
        int b = targetColor & 0xFF;

        int closestIdx = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            PresetColor pc = PRESET_COLORS[i];
            int pr = (pc.intColor >> 16) & 0xFF;
            int pg = (pc.intColor >> 8) & 0xFF;
            int pb = pc.intColor & 0xFF;

            double dist = Math.pow(r - pr, 2) + Math.pow(g - pg, 2) + Math.pow(b - pb, 2);
            if (dist < minDistance) {
                minDistance = dist;
                closestIdx = i;
            }
        }
        return closestIdx;
    }

    private Component getColorButtonMessage(String labelKey, int presetIdx) {
        PresetColor pc = PRESET_COLORS[presetIdx];
        return Component.translatable(labelKey)
                .append(Component.literal(": "))
                .append(Component.translatable(pc.translationKey).withStyle(style -> style.withColor(pc.intColor)));
    }

    private Component getToggleButtonMessage(String labelKey, boolean value) {
        Component state = Component.translatable(value ? "options.on" : "options.off")
                .withStyle(value ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED);
        return Component.translatable(labelKey)
                .append(Component.literal(": "))
                .append(state);
    }

    @Override
    protected void init() {
        this.scrollY = 0;

        this.btnName = Button.builder(this.getToggleButtonMessage("gui.bodycam.config.name_overlay", this.nameVal), btn -> {
            this.nameVal = !this.nameVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.config.name_overlay", this.nameVal));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnName);

        this.btnHealth = Button.builder(this.getToggleButtonMessage("gui.bodycam.config.health_overlay", this.healthVal), btn -> {
            this.healthVal = !this.healthVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.config.health_overlay", this.healthVal));
        }).bounds(this.width / 2 + 5, 0, 200, 20).build();
        this.addRenderableWidget(this.btnHealth);

        this.btnShift = Button.builder(this.getToggleButtonMessage("gui.bodycam.config.shift_overlay", this.shiftVal), btn -> {
            this.shiftVal = !this.shiftVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.config.shift_overlay", this.shiftVal));
        }).bounds(this.width / 2 - 100, 0, 200, 20).build();
        this.addRenderableWidget(this.btnShift);

        this.btnReset = Button.builder(Component.translatable("gui.bodycam.config.reset"), btn -> {
            this.nameVal = true;
            this.healthVal = true;
            this.shiftVal = true;
            this.stdColorIdx = this.getClosestPresetIndex("FFFFFF");
            this.blkColorIdx = this.getClosestPresetIndex("FF5555");
            this.obsColorIdx = this.getClosestPresetIndex("5555FF");
            this.dimColorIdx = this.getClosestPresetIndex("55FF55");
            this.oorColorIdx = this.getClosestPresetIndex("FFFF55");

            this.btnName.setMessage(this.getToggleButtonMessage("gui.bodycam.config.name_overlay", true));
            this.btnHealth.setMessage(this.getToggleButtonMessage("gui.bodycam.config.health_overlay", true));
            this.btnShift.setMessage(this.getToggleButtonMessage("gui.bodycam.config.shift_overlay", true));
            this.btnColorStd.setMessage(this.getColorButtonMessage("gui.bodycam.config.standard_color", this.stdColorIdx));
            this.btnColorBlk.setMessage(this.getColorButtonMessage("gui.bodycam.config.blocked_color", this.blkColorIdx));
            this.btnColorObs.setMessage(this.getColorButtonMessage("gui.bodycam.config.observing_color", this.obsColorIdx));
            this.btnColorDim.setMessage(this.getColorButtonMessage("gui.bodycam.config.dimension_color", this.dimColorIdx));
            this.btnColorOor.setMessage(this.getColorButtonMessage("gui.bodycam.config.out_of_range_color", this.oorColorIdx));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnReset);

        this.btnColorStd = Button.builder(this.getColorButtonMessage("gui.bodycam.config.standard_color", this.stdColorIdx), btn -> {
            this.stdColorIdx = (this.stdColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.standard_color", this.stdColorIdx));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnColorStd);

        this.btnColorBlk = Button.builder(this.getColorButtonMessage("gui.bodycam.config.blocked_color", this.blkColorIdx), btn -> {
            this.blkColorIdx = (this.blkColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.blocked_color", this.blkColorIdx));
        }).bounds(this.width / 2 + 5, 0, 200, 20).build();
        this.addRenderableWidget(this.btnColorBlk);

        this.btnColorObs = Button.builder(this.getColorButtonMessage("gui.bodycam.config.observing_color", this.obsColorIdx), btn -> {
            this.obsColorIdx = (this.obsColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.observing_color", this.obsColorIdx));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnColorObs);

        this.btnColorDim = Button.builder(this.getColorButtonMessage("gui.bodycam.config.dimension_color", this.dimColorIdx), btn -> {
            this.dimColorIdx = (this.dimColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.dimension_color", this.dimColorIdx));
        }).bounds(this.width / 2 + 5, 0, 200, 20).build();
        this.addRenderableWidget(this.btnColorDim);

        this.btnColorOor = Button.builder(this.getColorButtonMessage("gui.bodycam.config.out_of_range_color", this.oorColorIdx), btn -> {
            this.oorColorIdx = (this.oorColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.out_of_range_color", this.oorColorIdx));
        }).bounds(this.width / 2 - 205, 0, 200, 20).build();
        this.addRenderableWidget(this.btnColorOor);

        this.btnDone = Button.builder(Component.translatable("gui.done"), btn -> {
            dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_NAME_OVERLAY.set(this.nameVal);
            dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_HEALTH_OVERLAY.set(this.healthVal);
            dev.ClasherHD.bodycam.config.ModClientConfig.SHOW_SHIFT_OVERLAY.set(this.shiftVal);
            dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_STANDARD.set(PRESET_COLORS[this.stdColorIdx].hex);
            dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_BLOCKED.set(PRESET_COLORS[this.blkColorIdx].hex);
            dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_OBSERVING.set(PRESET_COLORS[this.obsColorIdx].hex);
            dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_DIMENSION.set(PRESET_COLORS[this.dimColorIdx].hex);
            dev.ClasherHD.bodycam.config.ModClientConfig.COLOR_OUT_OF_RANGE.set(PRESET_COLORS[this.oorColorIdx].hex);
            dev.ClasherHD.bodycam.config.ModClientConfig.SPEC.save();
            this.onClose();
        }).bounds(this.width / 2 + 5, 0, 200, 20).build();
        this.addRenderableWidget(this.btnDone);

        this.updateLayout();
    }

    private void updateLayout() {
        int yOffset = 54 + (int) this.scrollY;

        this.btnName.setY(yOffset);
        this.btnHealth.setY(yOffset);

        this.btnShift.setY(yOffset + 26);

        this.btnColorStd.setY(yOffset + 52);
        this.btnColorBlk.setY(yOffset + 52);

        this.btnColorObs.setY(yOffset + 78);
        this.btnColorDim.setY(yOffset + 78);

        this.btnColorOor.setY(yOffset + 104);

        this.btnReset.setY(yOffset + 136);
        this.btnDone.setY(yOffset + 136);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scrollY += delta * 20;
        this.scrollY = net.minecraft.util.Mth.clamp(this.scrollY, -this.maxScroll, 0);
        this.updateLayout();
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);

        pGuiGraphics.pose().pushPose();
        float scale = 1.5f;
        pGuiGraphics.pose().scale(scale, scale, 1.0f);
        float titleX = (this.width / 2f) / scale;
        float titleY = (20f + (float) this.scrollY) / scale;
        pGuiGraphics.drawCenteredString(this.font, this.title, (int) titleX, (int) titleY, 16777215);
        pGuiGraphics.pose().popPose();

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previous);
        }
    }
}
