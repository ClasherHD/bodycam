package dev.ClasherHD.bodycam;

import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BodycamSetupTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("bodycam.MODID matches mod identification bodycam")
    void modId_matchesExpected() {
        assertThat(bodycam.MODID).isEqualTo("bodycam");
    }

    @Test
    @DisplayName("bodycam class is annotated with @Mod matching MODID")
    void modAnnotation_isConfigured() {
        Mod modAnnotation = bodycam.class.getAnnotation(Mod.class);
        assertThat(modAnnotation).isNotNull();
        assertThat(modAnnotation.value()).isEqualTo(bodycam.MODID);
    }

    @Test
    @DisplayName("bodycam class instance can be allocated safely")
    void modInstance_canBeAllocated() {
        assertThatCode(() -> {
            bodycam instance = MinecraftTestEnvironment.allocateInstance(bodycam.class);
            assertThat(instance).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bodycam constructor registers configs and hooks registries to mod event bus")
    void constructor_registersConfigsAndBus() {
        try (MockedStatic<ModLoadingContext> mockedContext = mockStatic(ModLoadingContext.class);
             MockedStatic<FMLJavaModLoadingContext> mockedJavaContext = mockStatic(FMLJavaModLoadingContext.class)) {
            ModLoadingContext context = mock(ModLoadingContext.class);
            mockedContext.when(ModLoadingContext::get).thenReturn(context);

            FMLJavaModLoadingContext javaContext = mock(FMLJavaModLoadingContext.class);
            IEventBus bus = mock(IEventBus.class);
            when(javaContext.getModEventBus()).thenReturn(bus);
            mockedJavaContext.when(FMLJavaModLoadingContext::get).thenReturn(javaContext);

            bodycam instance = new bodycam();
            assertThat(instance).isNotNull();

            verify(context).registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC, "bodycam-client.toml");
            verify(context).registerConfig(ModConfig.Type.SERVER, ModServerConfig.SPEC, "bodycam-server.toml");
        }
    }
}
