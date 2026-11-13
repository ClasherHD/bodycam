package dev.ClasherHD.bodycam.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("null")
public class ClientConfigScreen extends Screen {
    private static final ResourceLocation WINDOW_TEXTURE = bodycam.id("textures/gui/gui_window.png");

    private final Screen previous;

    private final boolean initialName;
    private final boolean initialHealth;
    private final boolean initialShift;
    private final boolean initialVignette;
    private final String initialStd;
    private final String initialBlk;
    private final String initialObs;
    private final String initialDim;
    private final String initialOor;
    private final String initialOtd;

    private boolean nameVal;
    private boolean healthVal;
    private boolean shiftVal;
    private boolean vignetteVal;
    private int stdColorIdx;
    private int blkColorIdx;
    private int obsColorIdx;
    private int dimColorIdx;
    private int oorColorIdx;
    private int otdColorIdx;

    private Button btnName;
    private Button btnHealth;
    private Button btnShift;
    private Button btnVignette;
    private Button btnReset;
    private Button btnColorStd;
    private Button btnColorBlk;
    private Button btnColorObs;
    private Button btnColorDim;
    private Button btnColorOor;
    private Button btnColorOtd;
    private Button btnDone;

    private int guiWidth;
    private int guiHeight;
    private int guiX;
    private int guiY;

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

        this.initialName = ModClientConfig.SHOW_NAME_OVERLAY.get();
        this.initialHealth = ModClientConfig.SHOW_HEALTH_OVERLAY.get();
        this.initialShift = ModClientConfig.SHOW_SHIFT_OVERLAY.get();
        this.initialVignette = ModClientConfig.SHOW_DAMAGE_VIGNETTE.get();
        this.initialStd = ModClientConfig.COLOR_STANDARD.get();
        this.initialBlk = ModClientConfig.COLOR_BLOCKED.get();
        this.initialObs = ModClientConfig.COLOR_OBSERVING.get();
        this.initialDim = ModClientConfig.COLOR_DIMENSION.get();
        this.initialOor = ModClientConfig.COLOR_OUT_OF_RANGE.get();
        this.initialOtd = ModClientConfig.COLOR_OTHER_DIMENSION.get();

        this.nameVal = this.initialName;
        this.healthVal = this.initialHealth;
        this.shiftVal = this.initialShift;
        this.vignetteVal = this.initialVignette;
        this.stdColorIdx = this.getClosestPresetIndex(this.initialStd);
        this.blkColorIdx = this.getClosestPresetIndex(this.initialBlk);
        this.obsColorIdx = this.getClosestPresetIndex(this.initialObs);
        this.dimColorIdx = this.getClosestPresetIndex(this.initialDim);
        this.oorColorIdx = this.getClosestPresetIndex(this.initialOor);
        this.otdColorIdx = this.getClosestPresetIndex(this.initialOtd);
    }

    private int getClosestPresetIndex(String hex) {
        int targetColor = ModClientConfig.parseHexColor(hex, 0xFFFFFF);
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
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
        return Component.translatable(labelKey)
                .append(Component.literal(": "))
                .append(state);
    }

    @Override
    protected void init() {
        this.guiWidth = Math.min(430, this.width - 12);
        this.guiHeight = 186;
        this.guiX = (this.width - this.guiWidth) / 2;
        this.guiY = (this.height - this.guiHeight) / 2;

        int colWidth = (this.guiWidth - 30) / 2;
        int leftColX = this.guiX + 12;
        int rightColX = this.guiX + 12 + colWidth + 6;

        this.btnName = Button.builder(this.getToggleButtonMessage("gui.bodycam.config.name_overlay", this.nameVal), btn -> {
            this.nameVal = !this.nameVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.config.name_overlay", this.nameVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnName);

        this.btnHealth = Button.builder(this.getToggleButtonMessage("gui.bodycam.config.health_overlay", this.healthVal), btn -> {
            this.healthVal = !this.healthVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.config.health_overlay", this.healthVal));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnHealth);

        this.btnShift = Button.builder(this.getToggleButtonMessage("gui.bodycam.config.shift_overlay", this.shiftVal), btn -> {
            this.shiftVal = !this.shiftVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.config.shift_overlay", this.shiftVal));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnShift);

        this.btnVignette = Button.builder(this.getToggleButtonMessage("gui.bodycam.config.damage_vignette", this.vignetteVal), btn -> {
            this.vignetteVal = !this.vignetteVal;
            btn.setMessage(this.getToggleButtonMessage("gui.bodycam.config.damage_vignette", this.vignetteVal));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnVignette);

        this.btnReset = Button.builder(Component.translatable("gui.bodycam.config.reset"), btn -> {
            this.nameVal = true;
            this.healthVal = true;
            this.shiftVal = true;
            this.vignetteVal = true;
            this.stdColorIdx = this.getClosestPresetIndex("FFFFFF");
            this.blkColorIdx = this.getClosestPresetIndex("FF5555");
            this.obsColorIdx = this.getClosestPresetIndex("5555FF");
            this.dimColorIdx = this.getClosestPresetIndex("55FF55");
            this.oorColorIdx = this.getClosestPresetIndex("FFFF55");
            this.otdColorIdx = this.getClosestPresetIndex("FFAA00");

            this.btnName.setMessage(this.getToggleButtonMessage("gui.bodycam.config.name_overlay", true));
            this.btnHealth.setMessage(this.getToggleButtonMessage("gui.bodycam.config.health_overlay", true));
            this.btnShift.setMessage(this.getToggleButtonMessage("gui.bodycam.config.shift_overlay", true));
            this.btnVignette.setMessage(this.getToggleButtonMessage("gui.bodycam.config.damage_vignette", true));
            this.btnColorStd.setMessage(this.getColorButtonMessage("gui.bodycam.config.standard_color", this.stdColorIdx));
            this.btnColorBlk.setMessage(this.getColorButtonMessage("gui.bodycam.config.blocked_color", this.blkColorIdx));
            this.btnColorObs.setMessage(this.getColorButtonMessage("gui.bodycam.config.observing_color", this.obsColorIdx));
            this.btnColorDim.setMessage(this.getColorButtonMessage("gui.bodycam.config.dimension_color", this.dimColorIdx));
            this.btnColorOor.setMessage(this.getColorButtonMessage("gui.bodycam.config.out_of_range_color", this.oorColorIdx));
            this.btnColorOtd.setMessage(this.getColorButtonMessage("gui.bodycam.config.other_dimension_color", this.otdColorIdx));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnReset);

        this.btnColorStd = Button.builder(this.getColorButtonMessage("gui.bodycam.config.standard_color", this.stdColorIdx), btn -> {
            this.stdColorIdx = (this.stdColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.standard_color", this.stdColorIdx));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnColorStd);

        this.btnColorBlk = Button.builder(this.getColorButtonMessage("gui.bodycam.config.blocked_color", this.blkColorIdx), btn -> {
            this.blkColorIdx = (this.blkColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.blocked_color", this.blkColorIdx));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnColorBlk);

        this.btnColorObs = Button.builder(this.getColorButtonMessage("gui.bodycam.config.observing_color", this.obsColorIdx), btn -> {
            this.obsColorIdx = (this.obsColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.observing_color", this.obsColorIdx));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnColorObs);

        this.btnColorDim = Button.builder(this.getColorButtonMessage("gui.bodycam.config.dimension_color", this.dimColorIdx), btn -> {
            this.dimColorIdx = (this.dimColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.dimension_color", this.dimColorIdx));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnColorDim);

        this.btnColorOor = Button.builder(this.getColorButtonMessage("gui.bodycam.config.out_of_range_color", this.oorColorIdx), btn -> {
            this.oorColorIdx = (this.oorColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.out_of_range_color", this.oorColorIdx));
        }).bounds(leftColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnColorOor);

        this.btnColorOtd = Button.builder(this.getColorButtonMessage("gui.bodycam.config.other_dimension_color", this.otdColorIdx), btn -> {
            this.otdColorIdx = (this.otdColorIdx + 1) % PRESET_COLORS.length;
            btn.setMessage(this.getColorButtonMessage("gui.bodycam.config.other_dimension_color", this.otdColorIdx));
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnColorOtd);

        this.btnDone = Button.builder(Component.translatable("gui.done"), btn -> {
            ModClientConfig.SHOW_NAME_OVERLAY.set(this.nameVal);
            ModClientConfig.SHOW_HEALTH_OVERLAY.set(this.healthVal);
            ModClientConfig.SHOW_SHIFT_OVERLAY.set(this.shiftVal);
            ModClientConfig.SHOW_DAMAGE_VIGNETTE.set(this.vignetteVal);
            ModClientConfig.COLOR_STANDARD.set(PRESET_COLORS[this.stdColorIdx].hex);
            ModClientConfig.COLOR_BLOCKED.set(PRESET_COLORS[this.blkColorIdx].hex);
            ModClientConfig.COLOR_OBSERVING.set(PRESET_COLORS[this.obsColorIdx].hex);
            ModClientConfig.COLOR_DIMENSION.set(PRESET_COLORS[this.dimColorIdx].hex);
            ModClientConfig.COLOR_OUT_OF_RANGE.set(PRESET_COLORS[this.oorColorIdx].hex);
            ModClientConfig.COLOR_OTHER_DIMENSION.set(PRESET_COLORS[this.otdColorIdx].hex);
            ModClientConfig.save();
            this.onClose();
        }).bounds(rightColX, 0, colWidth, 20).build();
        this.addRenderableWidget(this.btnDone);

        this.updateLayout();
    }

    private void updateLayout() {
        this.btnName.setY(this.guiY + 28);
        this.btnHealth.setY(this.guiY + 28);

        this.btnShift.setY(this.guiY + 51);
        this.btnVignette.setY(this.guiY + 51);

        this.btnColorStd.setY(this.guiY + 78);
        this.btnColorBlk.setY(this.guiY + 78);

        this.btnColorObs.setY(this.guiY + 101);
        this.btnColorDim.setY(this.guiY + 101);

        this.btnColorOor.setY(this.guiY + 124);
        this.btnColorOtd.setY(this.guiY + 124);

        this.btnReset.setY(this.guiY + 154);
        this.btnDone.setY(this.guiY + 154);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        RenderSystem.setShaderColor(0.32F, 0.32F, 0.35F, 1.0F);
        GuiRenderUtils.drawNineSlice(graphics, WINDOW_TEXTURE, this.guiX, this.guiY, this.guiWidth, this.guiHeight, 32, 32, 32, 32, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.guiY + 10, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previous);
        }
    }
}
