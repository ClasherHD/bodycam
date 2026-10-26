package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.block.AncientTableBlock;
import dev.ClasherHD.bodycam.block.HologramBlock;
import dev.ClasherHD.bodycam.block.PodestBlock;
import dev.ClasherHD.bodycam.block.WatchPointBlock;
import dev.ClasherHD.bodycam.enchantment.BodycamAnonymityEnchantment;
import dev.ClasherHD.bodycam.enchantment.BodycamNightspyEnchantment;
import dev.ClasherHD.bodycam.enchantment.BodycamReachEnchantment;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.item.DimensionLocatorItem;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.PlayerLocatorCompassItem;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModRegistriesTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("ModItems should have all registered items present and of correct types")
    void modItems_arePresentAndCorrect() {
        assertThat(ModItems.BODYCAM_MONITOR.isPresent()).isTrue();
        assertThat(ModItems.BODYCAM_MONITOR.get()).isInstanceOf(BodycamMonitorItem.class);

        assertThat(ModItems.JAMMER.isPresent()).isTrue();
        assertThat(ModItems.JAMMER.get()).isInstanceOf(JammerItem.class);

        assertThat(ModItems.DIMENSION_LOCATOR.isPresent()).isTrue();
        assertThat(ModItems.DIMENSION_LOCATOR.get()).isInstanceOf(DimensionLocatorItem.class);

        assertThat(ModItems.ANONYMIZER.isPresent()).isTrue();
        assertThat(ModItems.ANONYMIZER.get()).isInstanceOf(AnonymizerItem.class);

        assertThat(ModItems.PLAYER_LOCATOR_COMPASS.isPresent()).isTrue();
        assertThat(ModItems.PLAYER_LOCATOR_COMPASS.get()).isInstanceOf(PlayerLocatorCompassItem.class);

        assertThat(ModItems.PORTABLE_NETHER_PORTAL.isPresent()).isTrue();
        assertThat(ModItems.PORTABLE_NETHER_PORTAL.get()).isInstanceOf(PortableNetherPortalItem.class);

        assertThat(ModItems.OBSERVATION_CRYSTAL.isPresent()).isTrue();
        assertThat(ModItems.HOLOGRAM_BLOCK_ITEM.isPresent()).isTrue();
        assertThat(ModItems.ANCIENT_TABLE_ITEM.isPresent()).isTrue();
        assertThat(ModItems.WATCH_POINT_ITEM.isPresent()).isTrue();
        assertThat(ModItems.PODEST_ITEM.isPresent()).isTrue();
    }

    @Test
    @DisplayName("ModBlocks should have all registered blocks present and of correct types")
    void modBlocks_arePresentAndCorrect() {
        assertThat(ModBlocks.HOLOGRAM_BLOCK.isPresent()).isTrue();
        assertThat(ModBlocks.HOLOGRAM_BLOCK.get()).isInstanceOf(HologramBlock.class);

        assertThat(ModBlocks.ANCIENT_TABLE.isPresent()).isTrue();
        assertThat(ModBlocks.ANCIENT_TABLE.get()).isInstanceOf(AncientTableBlock.class);

        assertThat(ModBlocks.PODEST.isPresent()).isTrue();
        assertThat(ModBlocks.PODEST.get()).isInstanceOf(PodestBlock.class);

        assertThat(ModBlocks.WATCH_POINT.isPresent()).isTrue();
        assertThat(ModBlocks.WATCH_POINT.get()).isInstanceOf(WatchPointBlock.class);

        assertThat(ModBlocks.OBSERVATION_CRYSTAL_BLOCK.isPresent()).isTrue();
    }

    @Test
    @DisplayName("ModBlockEntities, ModEntityTypes, and ModMenuTypes should be properly registered")
    void modContainersAndEntities_arePresentAndCorrect() {
        assertThat(ModBlockEntities.ANCIENT_TABLE.isPresent()).isTrue();
        assertThat(ModBlockEntities.PODEST.isPresent()).isTrue();
        assertThat(ModBlockEntities.WATCH_POINT.isPresent()).isTrue();

        assertThat(ModEntityTypes.COMPASS_DUMMY.isPresent()).isTrue();
        assertThat(ModEntityTypes.HOLOGRAM_DUMMY.isPresent()).isTrue();
        assertThat(ModEntityTypes.WATCH_POINT_DUMMY.isPresent()).isTrue();

        assertThat(ModMenuTypes.ANCIENT_TABLE.isPresent()).isTrue();
    }

    @Test
    @DisplayName("ModEnchantments should be present and instantiate respective enchantment classes")
    void modEnchantments_arePresentAndCorrect() {
        assertThat(ModEnchantments.ANONYMITY_ENCHANTMENT.isPresent()).isTrue();
        assertThat(ModEnchantments.ANONYMITY_ENCHANTMENT.get()).isInstanceOf(BodycamAnonymityEnchantment.class);

        assertThat(ModEnchantments.NIGHTSPY_ENCHANTMENT.isPresent()).isTrue();
        assertThat(ModEnchantments.NIGHTSPY_ENCHANTMENT.get()).isInstanceOf(BodycamNightspyEnchantment.class);

        assertThat(ModEnchantments.REACH_ENCHANTMENT.isPresent()).isTrue();
        assertThat(ModEnchantments.REACH_ENCHANTMENT.get()).isInstanceOf(BodycamReachEnchantment.class);
    }

    @Test
    @DisplayName("ModSounds should register sound events with matching resource locations")
    void modSounds_arePresentAndCorrect() {
        assertThat(ModSounds.ANONYMIZER_USED1.isPresent()).isTrue();
        assertThat(ModSounds.ANONYMIZER_USED2.isPresent()).isTrue();
        assertThat(ModSounds.JAMMER_USED.isPresent()).isTrue();
        assertThat(ModSounds.PLAYER_LOCATOR_USED.isPresent()).isTrue();
        assertThat(ModSounds.ANCIENT_TABLE_USE.isPresent()).isTrue();
        assertThat(ModSounds.ANCIENT_TABLE_REFUEL.isPresent()).isTrue();
        assertThat(ModSounds.PODEST_CHARGE.isPresent()).isTrue();
        assertThat(ModSounds.PODEST_PLACE.isPresent()).isTrue();
        assertThat(ModSounds.PODEST_REMOVE.isPresent()).isTrue();
        assertThat(ModSounds.WATCH_POINT_ACTIVATE.isPresent()).isTrue();
        assertThat(ModSounds.WATCH_POINT_DEACTIVATE.isPresent()).isTrue();
    }

    @Test
    @DisplayName("ModRecipes should register ancient table crafting recipe type and serializer")
    void modRecipes_arePresentAndCorrect() {
        assertThat(ModRecipes.ANCIENT_TABLE_CRAFTING_TYPE.isPresent()).isTrue();
        assertThat(ModRecipes.ANCIENT_TABLE_CRAFTING_SERIALIZER.isPresent()).isTrue();
    }

    @Test
    @DisplayName("ModCreativeTabs should register both normal and unbreakable creative tabs")
    void modCreativeTabs_arePresentAndCorrect() {
        assertThat(ModCreativeTabs.BODYCAM_TAB.isPresent()).isTrue();
        assertThat(ModCreativeTabs.BODYCAM_TAB.get()).isInstanceOf(net.minecraft.world.item.CreativeModeTab.class);

        assertThat(ModCreativeTabs.BODYCAM_UNBREAKABLE_TAB.isPresent()).isTrue();
        assertThat(ModCreativeTabs.BODYCAM_UNBREAKABLE_TAB.get()).isInstanceOf(net.minecraft.world.item.CreativeModeTab.class);
    }
}
