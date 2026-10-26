package dev.ClasherHD.bodycam.event;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModEventsTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("commonSetup initializes packets and enqueues chunk loading callback")
    void commonSetup_initializesPacketsAndEnqueuesWork() {
        FMLCommonSetupEvent event = mock(FMLCommonSetupEvent.class);

        assertThatCode(() -> ModEvents.commonSetup(event)).doesNotThrowAnyException();
        verify(event).enqueueWork(any(Runnable.class));
    }

    @Test
    @DisplayName("entityAttributes should register attributes for all mod dummy entity types")
    void entityAttributes_registersAttributesForAllDummies() {
        Map<EntityType<? extends LivingEntity>, AttributeSupplier> attributeMap = new HashMap<>();
        EntityAttributeCreationEvent event = new EntityAttributeCreationEvent(attributeMap);

        ModEvents.entityAttributes(event);

        assertThat(attributeMap).containsKey(ModEntityTypes.COMPASS_DUMMY.get());
        assertThat(attributeMap).containsKey(ModEntityTypes.HOLOGRAM_DUMMY.get());
        assertThat(attributeMap).containsKey(ModEntityTypes.WATCH_POINT_DUMMY.get());

        AttributeSupplier compassAttrs = attributeMap.get(ModEntityTypes.COMPASS_DUMMY.get());
        assertThat(compassAttrs.hasAttribute(Attributes.MAX_HEALTH)).isTrue();
    }

    @Test
    @DisplayName("onConfigLoad ignores other mods and sets active config for bodycam configs")
    void onConfigLoad_handlesAllTypes() {
        ModServerConfig.setActiveModConfig(null);
        ModClientConfig.setActiveModConfig(null);

        ModConfig otherModConfig = mock(ModConfig.class);
        when(otherModConfig.getModId()).thenReturn("othermod");
        ModConfigEvent otherEvent = MinecraftTestEnvironment.allocateInstance(ModConfigEvent.Loading.class);
        MinecraftTestEnvironment.setField(otherEvent, "config", otherModConfig);
        ModEvents.onConfigLoad(otherEvent);
        assertThat((ModConfig) MinecraftTestEnvironment.getField(ModServerConfig.class, "activeModConfig")).isNull();
        assertThat((ModConfig) MinecraftTestEnvironment.getField(ModClientConfig.class, "activeModConfig")).isNull();

        ModConfig serverConfig = mock(ModConfig.class);
        when(serverConfig.getModId()).thenReturn(bodycam.MODID);
        when(serverConfig.getType()).thenReturn(ModConfig.Type.SERVER);
        ModConfigEvent serverEvent = MinecraftTestEnvironment.allocateInstance(ModConfigEvent.Loading.class);
        MinecraftTestEnvironment.setField(serverEvent, "config", serverConfig);
        ModEvents.onConfigLoad(serverEvent);
        assertThat((ModConfig) MinecraftTestEnvironment.getField(ModServerConfig.class, "activeModConfig")).isSameAs(serverConfig);

        ModConfig clientConfig = mock(ModConfig.class);
        when(clientConfig.getModId()).thenReturn(bodycam.MODID);
        when(clientConfig.getType()).thenReturn(ModConfig.Type.CLIENT);
        ModConfigEvent clientEvent = MinecraftTestEnvironment.allocateInstance(ModConfigEvent.Loading.class);
        MinecraftTestEnvironment.setField(clientEvent, "config", clientConfig);
        ModEvents.onConfigLoad(clientEvent);
        assertThat((ModConfig) MinecraftTestEnvironment.getField(ModClientConfig.class, "activeModConfig")).isSameAs(clientConfig);

        ModConfig commonConfig = mock(ModConfig.class);
        when(commonConfig.getModId()).thenReturn(bodycam.MODID);
        when(commonConfig.getType()).thenReturn(ModConfig.Type.COMMON);
        ModConfigEvent commonEvent = MinecraftTestEnvironment.allocateInstance(ModConfigEvent.Loading.class);
        MinecraftTestEnvironment.setField(commonEvent, "config", commonConfig);
        ModEvents.onConfigLoad(commonEvent);
    }
}
