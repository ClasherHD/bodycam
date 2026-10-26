package dev.ClasherHD.bodycam.test;

import dev.ClasherHD.bodycam.registry.ModBlockEntities;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModCreativeTabs;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModMenuTypes;
import dev.ClasherHD.bodycam.registry.ModRecipes;
import dev.ClasherHD.bodycam.registry.ModSounds;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings({"deprecation", "null"})
public final class MinecraftTestEnvironment {
    private static volatile boolean bootstrapped = false;
    private static volatile boolean registriesBound = false;

    private MinecraftTestEnvironment() {}

    public static synchronized void ensureBootstrapped() {
        if (!bootstrapped) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            initializeFMLLoader();
            initializeNetworkEventBus();
            try {
                dev.ClasherHD.bodycam.network.PacketHandler.init();
            } catch (Throwable ignored) {
            }
            bootstrapped = true;
        }
        ensureRegistriesBound();
    }

    private static org.mockito.MockedStatic<?> serviceLoaderUtilsMock;

    public static void initializeFMLLoader() {
        try {
            Class<?> sluClass = Class.forName("cpw.mods.modlauncher.util.ServiceLoaderUtils");
            if (serviceLoaderUtilsMock == null) {
                serviceLoaderUtilsMock = org.mockito.Mockito.mockStatic(sluClass);
                serviceLoaderUtilsMock.when(() -> cpw.mods.modlauncher.util.ServiceLoaderUtils.streamServiceLoader(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                        .thenAnswer(inv -> java.util.stream.Stream.of(new TestBindingsProvider()));
            }
        } catch (Throwable ignored) {
        }
    }

    public static void initializeNetworkEventBus() {
        try {
            Class<?> helperClass = Class.forName("net.minecraftforge.eventbus.api.EventListenerHelper");
            Field f = helperClass.getDeclaredField("listeners");
            f.setAccessible(true);
            Object lockHelper = f.get(null);
            Field mapField = lockHelper.getClass().getDeclaredField("map");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) mapField.get(lockHelper);

            Class<?> listenerListClass = Class.forName("net.minecraftforge.eventbus.ListenerList");
            java.lang.reflect.Constructor<?> llCtor = listenerListClass.getDeclaredConstructor();
            llCtor.setAccessible(true);

            Class<?> networkEventClass = Class.forName("net.minecraftforge.network.NetworkEvent");
            map.put(networkEventClass, llCtor.newInstance());
            for (Class<?> inner : networkEventClass.getDeclaredClasses()) {
                map.put(inner, llCtor.newInstance());
            }
        } catch (Throwable ignored) {
        }
    }

    public static synchronized void ensureRegistriesBound() {
        if (!bootstrapped) {
            ensureBootstrapped();
            return;
        }
        if (registriesBound) {
            return;
        }
        try {
            bindDeferredRegister(ModBlocks.BLOCKS, BuiltInRegistries.BLOCK);
            bindDeferredRegister(ModItems.ITEMS, BuiltInRegistries.ITEM);
            bindDeferredRegister(ModBlockEntities.BLOCK_ENTITIES, BuiltInRegistries.BLOCK_ENTITY_TYPE);
            bindDeferredRegister(ModEntityTypes.ENTITY_TYPES, BuiltInRegistries.ENTITY_TYPE);
            bindDeferredRegister(ModMenuTypes.MENUS, BuiltInRegistries.MENU);
            bindDeferredRegister(ModEnchantments.ENCHANTMENTS, BuiltInRegistries.ENCHANTMENT);
            bindDeferredRegister(ModSounds.SOUND_EVENTS, BuiltInRegistries.SOUND_EVENT);
            bindDeferredRegister(ModRecipes.RECIPE_TYPES, BuiltInRegistries.RECIPE_TYPE);
            bindDeferredRegister(ModRecipes.SERIALIZERS, BuiltInRegistries.RECIPE_SERIALIZER);
            bindDeferredRegister(ModCreativeTabs.CREATIVE_MODE_TABS, BuiltInRegistries.CREATIVE_MODE_TAB);
            try {
                Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
                Field attributesField = forgeModClass.getDeclaredField("ATTRIBUTES");
                attributesField.setAccessible(true);
                DeferredRegister<?> forgeAttributes = (DeferredRegister<?>) attributesField.get(null);
                bindDeferredRegister(forgeAttributes, BuiltInRegistries.ATTRIBUTE);
            } catch (Throwable ignored) {
            }
            registriesBound = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to bind mod registries for testing", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void bindDeferredRegister(DeferredRegister<?> register, Registry<?> builtinRegistry) throws Exception {
        if (builtinRegistry != null) {
            setField(builtinRegistry, "locked", false);
            setField(builtinRegistry, "frozen", false);
        }

        Field entriesField = DeferredRegister.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        Map<RegistryObject<?>, Supplier<?>> entries =
                (Map<RegistryObject<?>, Supplier<?>>) entriesField.get(register);
        for (Map.Entry<RegistryObject<?>, Supplier<?>> entry : entries.entrySet()) {
            RegistryObject<?> ro = entry.getKey();
            Supplier<?> supplier = entry.getValue();
            Object val = supplier.get();
            setField(ro, "value", val);
            if (builtinRegistry != null && val != null) {
                try {
                    Registry.register((Registry) builtinRegistry, ro.getId(), val);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void setField(Object obj, String fieldName, Object val) {
        Class<?> c = (obj instanceof Class<?> clazz) ? clazz : obj.getClass();
        Object target = (obj instanceof Class<?>) ? null : obj;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, val);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(Object obj, String fieldName) {
        Class<?> c = (obj instanceof Class<?> clazz) ? clazz : obj.getClass();
        Object target = (obj instanceof Class<?>) ? null : obj;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return (T) f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Field " + fieldName + " not found on " + obj);
    }

    @SuppressWarnings("unchecked")
    public static <T> T allocateInstance(Class<T> clazz) {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
            return (T) unsafe.allocateInstance(clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate instance of " + clazz, e);
        }
    }
}
