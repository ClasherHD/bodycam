package dev.ClasherHD.bodycam.compat.curios;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class CuriosCompatTest {

    @BeforeAll
    static void setup() {
        MinecraftTestEnvironment.ensureRegistriesBound();
    }

    @Test
    @DisplayName("isLoaded returns false when ModList indicates curios is not present")
    void isLoaded_returnsFalseWhenNotLoaded() {
        try (MockedStatic<ModList> mockedModList = mockStatic(ModList.class)) {
            ModList modList = mock(ModList.class);
            when(modList.isLoaded("curios")).thenReturn(false);
            mockedModList.when(ModList::get).thenReturn(modList);

            assertThat(CuriosCompat.isLoaded()).isFalse();
        }
    }

    @Test
    @DisplayName("isLoaded returns true when ModList indicates curios is present")
    void isLoaded_returnsTrueWhenLoaded() {
        try (MockedStatic<ModList> mockedModList = mockStatic(ModList.class)) {
            ModList modList = mock(ModList.class);
            when(modList.isLoaded("curios")).thenReturn(true);
            mockedModList.when(ModList::get).thenReturn(modList);

            assertThat(CuriosCompat.isLoaded()).isTrue();
        }
    }

    @Test
    @DisplayName("isLoaded throws NullPointerException when ModList.get() is null")
    void isLoaded_throwsNpeWhenModListNull() {
        try (MockedStatic<ModList> mockedModList = mockStatic(ModList.class)) {
            mockedModList.when(ModList::get).thenReturn(null);

            assertThatThrownBy(CuriosCompat::isLoaded)
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    @DisplayName("CuriosCompat fallback returns false and empty list when unloaded")
    void fallback_behavior_whenUnloaded() {
        try (MockedStatic<ModList> mockedModList = mockStatic(ModList.class)) {
            ModList modList = mock(ModList.class);
            when(modList.isLoaded("curios")).thenReturn(false);
            mockedModList.when(ModList::get).thenReturn(modList);

            assertThat(CuriosCompat.hasItem(null, Items.APPLE)).isFalse();
            assertThat(CuriosCompat.getEquippedItems(null, Items.APPLE)).isEmpty();
        }
    }

    @Test
    @DisplayName("CuriosCompat delegates to CuriosInternal when loaded and item is found")
    void loaded_itemFound() {
        try (MockedStatic<ModList> mockedModList = mockStatic(ModList.class);
             MockedStatic<CuriosApi> mockedCuriosApi = mockStatic(CuriosApi.class)) {
            ModList modList = mock(ModList.class);
            when(modList.isLoaded("curios")).thenReturn(true);
            mockedModList.when(ModList::get).thenReturn(modList);

            LivingEntity entity = mock(LivingEntity.class);
            ICuriosItemHandler handler = mock(ICuriosItemHandler.class);
            SlotResult slotResult = mock(SlotResult.class);
            ItemStack appleStack = new ItemStack(Items.APPLE);
            when(slotResult.stack()).thenReturn(appleStack);

            when(handler.findFirstCurio(Items.APPLE)).thenReturn(Optional.of(slotResult));
            when(handler.findCurios(Items.APPLE)).thenReturn(List.of(slotResult));
            mockedCuriosApi.when(() -> CuriosApi.getCuriosInventory(entity)).thenReturn(LazyOptional.of(() -> handler));

            assertThat(CuriosCompat.hasItem(entity, Items.APPLE)).isTrue();
            List<ItemStack> equipped = CuriosCompat.getEquippedItems(entity, Items.APPLE);
            assertThat(equipped).containsExactly(appleStack);
        }
    }

    @Test
    @DisplayName("CuriosCompat delegates to CuriosInternal when loaded and inventory is absent")
    void loaded_inventoryAbsent() {
        try (MockedStatic<ModList> mockedModList = mockStatic(ModList.class);
             MockedStatic<CuriosApi> mockedCuriosApi = mockStatic(CuriosApi.class)) {
            ModList modList = mock(ModList.class);
            when(modList.isLoaded("curios")).thenReturn(true);
            mockedModList.when(ModList::get).thenReturn(modList);

            LivingEntity entity = mock(LivingEntity.class);
            mockedCuriosApi.when(() -> CuriosApi.getCuriosInventory(entity)).thenReturn(LazyOptional.empty());

            assertThat(CuriosCompat.hasItem(entity, Items.APPLE)).isFalse();
            assertThat(CuriosCompat.getEquippedItems(entity, Items.APPLE)).isEmpty();
        }
    }

    @Test
    @DisplayName("CuriosCompat private constructor is accessible via reflection")
    void constructor_isPrivate() throws Exception {
        Constructor<CuriosCompat> constructor = CuriosCompat.class.getDeclaredConstructor();
        assertThat(constructor.canAccess(null)).isFalse();
        constructor.setAccessible(true);
        CuriosCompat instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }

    @Test
    @DisplayName("CuriosInternal private constructor is accessible via reflection")
    void curiosInternal_constructor_isPrivate() throws Exception {
        Constructor<?> constructor = Class.forName("dev.ClasherHD.bodycam.compat.curios.CuriosInternal").getDeclaredConstructor();
        assertThat(constructor.canAccess(null)).isFalse();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
