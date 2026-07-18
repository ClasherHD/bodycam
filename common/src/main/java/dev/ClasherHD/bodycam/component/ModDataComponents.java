package dev.ClasherHD.bodycam.component;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.UUIDUtil;
import java.util.UUID;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create("bodycam", Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<Integer>> JAMMER_MODE = DATA_COMPONENT_TYPES.register("jammer_mode", () -> 
            DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final RegistrySupplier<DataComponentType<Boolean>> ANONYMIZER_ACTIVE = DATA_COMPONENT_TYPES.register("anonymizer_active", () -> 
            DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final RegistrySupplier<DataComponentType<Boolean>> IS_MASTER = DATA_COMPONENT_TYPES.register("is_master", () -> 
            DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());

    public static final RegistrySupplier<DataComponentType<UUID>> ACTIVE_ID = DATA_COMPONENT_TYPES.register("active_id", () -> 
            DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    public static void register() {
        DATA_COMPONENT_TYPES.register();
    }
}
