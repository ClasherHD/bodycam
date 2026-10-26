package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@SuppressWarnings("null")
public class PortalChannelOverlay implements IGuiOverlay {

    public static final PortalChannelOverlay INSTANCE = new PortalChannelOverlay();
    private PortalChannelSoundInstance activeSound;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            stopSound(mc);
            return;
        }

        if (player.isUsingItem()) {
            ItemStack usedItem = player.getUseItem();
            if (usedItem.getItem() instanceof PortableNetherPortalItem) {
                int totalDuration = usedItem.getUseDuration();
                int remainingTicks = player.getUseItemRemainingTicks();
                int ticksUsed = Math.max(0, totalDuration - remainingTicks);

                if (ticksUsed >= 1 && (activeSound == null || activeSound.isStopped())) {
                    activeSound = new PortalChannelSoundInstance(player);
                    mc.getSoundManager().play(activeSound);
                }

                float progress = Math.min(1.0F, (ticksUsed + partialTick) / (float) totalDuration);
                float alpha = progress * progress * 0.8F + 0.2F;

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                graphics.setColor(1.0F, 1.0F, 1.0F, alpha);

                TextureAtlasSprite sprite = mc.getBlockRenderer().getBlockModel(Blocks.NETHER_PORTAL.defaultBlockState())
                        .getParticleIcon(net.minecraftforge.client.model.data.ModelData.EMPTY);
                graphics.blit(0, 0, -90, screenWidth, screenHeight, sprite);

                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
                return;
            }
        }

        stopSound(mc);
    }

    private void stopSound(Minecraft mc) {
        if (activeSound != null) {
            activeSound.stopSound();
            mc.getSoundManager().stop(activeSound);
            activeSound = null;
        }
    }

    public static class PortalChannelSoundInstance extends AbstractTickableSoundInstance {
        private final Player player;

        public PortalChannelSoundInstance(Player player) {
            super(SoundEvents.PORTAL_TRIGGER, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = false;
            this.delay = 0;
            this.volume = 0.25F;
            this.pitch = player.getRandom().nextFloat() * 0.4F + 0.8F;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        }

        public void stopSound() {
            this.stop();
        }

        @Override
        public void tick() {
            if (this.player.isRemoved() || !this.player.isUsingItem() || !(this.player.getUseItem().getItem() instanceof PortableNetherPortalItem)) {
                this.stop();
            }
        }
    }
}
