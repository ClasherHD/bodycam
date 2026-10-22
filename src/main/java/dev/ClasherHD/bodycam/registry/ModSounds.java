package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, bodycam.MODID);

    public static final RegistryObject<SoundEvent> MONITOR_SELECT_RESONATE1 = SOUND_EVENTS.register("monitor_select_resonate1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "monitor_select_resonate1")));
    public static final RegistryObject<SoundEvent> MONITOR_SELECT_RESONATE2 = SOUND_EVENTS.register("monitor_select_resonate2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "monitor_select_resonate2")));
    public static final RegistryObject<SoundEvent> MONITOR_SELECT_RESONATE3 = SOUND_EVENTS.register("monitor_select_resonate3",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "monitor_select_resonate3")));
    public static final RegistryObject<SoundEvent> MONITOR_SELECT_RESONATE4 = SOUND_EVENTS.register("monitor_select_resonate4",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "monitor_select_resonate4")));
    public static final RegistryObject<SoundEvent> MONITOR_SELECT_SHIMMER = SOUND_EVENTS.register("monitor_select_shimmer",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "monitor_select_shimmer")));
    public static final RegistryObject<SoundEvent> ANONYMIZER_USED1 = SOUND_EVENTS.register("anonymizer_used1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "anonymizer_used1")));
    public static final RegistryObject<SoundEvent> ANONYMIZER_USED2 = SOUND_EVENTS.register("anonymizer_used2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "anonymizer_used2")));
    public static final RegistryObject<SoundEvent> JAMMER_USED = SOUND_EVENTS.register("jammer_used",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "jammer_used")));
    public static final RegistryObject<SoundEvent> PLAYER_LOCATOR_USED = SOUND_EVENTS.register("player_locator_used",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "player_locator_used")));
    public static final RegistryObject<SoundEvent> ANCIENT_TABLE_REFUEL = SOUND_EVENTS.register("ancient_table_refuel",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "ancient_table_refuel")));
    public static final RegistryObject<SoundEvent> ANCIENT_TABLE_USE = SOUND_EVENTS.register("ancient_table_use",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "ancient_table_use")));
    public static final RegistryObject<SoundEvent> PODEST_CHARGE = SOUND_EVENTS.register("podest_charge",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "podest_charge")));
    public static final RegistryObject<SoundEvent> PODEST_PLACE = SOUND_EVENTS.register("podest_place",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "podest_place")));
    public static final RegistryObject<SoundEvent> PODEST_REMOVE = SOUND_EVENTS.register("podest_remove",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "podest_remove")));
    public static final RegistryObject<SoundEvent> WATCH_POINT_ACTIVATE = SOUND_EVENTS.register("watch_point_activate",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "watch_point_activate")));
    public static final RegistryObject<SoundEvent> WATCH_POINT_DEACTIVATE = SOUND_EVENTS.register("watch_point_deactivate",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(bodycam.MODID, "watch_point_deactivate")));
}
