package dev.ClasherHD.bodycam.client.render;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class HumanoidRenderHelperTest {

    @AfterEach
    void tearDown() {
        HumanoidRenderHelper.clearCache();
    }

    @Test
    void createStaticSkin_cachesByTextureAndModelType() {
        Identifier tex = Identifier.fromNamespaceAndPath("bodycam", "textures/entity/scholar_common.png");

        PlayerSkin slimFirst = HumanoidRenderHelper.createStaticSkin(tex, true);
        PlayerSkin slimSecond = HumanoidRenderHelper.createStaticSkin(tex, true);
        PlayerSkin wide = HumanoidRenderHelper.createStaticSkin(tex, false);

        assertThat(slimFirst).isSameAs(slimSecond);
        assertThat(slimFirst).isNotSameAs(wide);
        assertThat(slimFirst.model()).isEqualTo(PlayerModelType.SLIM);
        assertThat(wide.model()).isEqualTo(PlayerModelType.WIDE);
    }

    @Test
    void clearCache_canBeCalledSafely() {
        HumanoidRenderHelper.clearCache();
    }
}
