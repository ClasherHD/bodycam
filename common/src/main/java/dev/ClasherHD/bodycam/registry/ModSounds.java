package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(bodycam.MODID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> MONITOR_SELECT_RESONATE1 = registerSound("monitor_select_resonate1");
    public static final RegistrySupplier<SoundEvent> MONITOR_SELECT_RESONATE2 = registerSound("monitor_select_resonate2");
    public static final RegistrySupplier<SoundEvent> MONITOR_SELECT_RESONATE3 = registerSound("monitor_select_resonate3");
    public static final RegistrySupplier<SoundEvent> MONITOR_SELECT_RESONATE4 = registerSound("monitor_select_resonate4");
    public static final RegistrySupplier<SoundEvent> MONITOR_SELECT_SHIMMER = registerSound("monitor_select_shimmer");
    public static final RegistrySupplier<SoundEvent> ANONYMIZER_USED1 = registerSound("anonymizer_used1");
    public static final RegistrySupplier<SoundEvent> ANONYMIZER_USED2 = registerSound("anonymizer_used2");
    public static final RegistrySupplier<SoundEvent> JAMMER_USED = registerSound("jammer_used");
    public static final RegistrySupplier<SoundEvent> PLAYER_LOCATOR_USED = registerSound("player_locator_used");
    public static final RegistrySupplier<SoundEvent> ANCIENT_TABLE_REFUEL = registerSound("ancient_table_refuel");
    public static final RegistrySupplier<SoundEvent> ANCIENT_TABLE_USE = registerSound("ancient_table_use");
    public static final RegistrySupplier<SoundEvent> PODEST_CHARGE = registerSound("podest_charge");
    public static final RegistrySupplier<SoundEvent> PODEST_PLACE = registerSound("podest_place");
    public static final RegistrySupplier<SoundEvent> PODEST_REMOVE = registerSound("podest_remove");
    public static final RegistrySupplier<SoundEvent> WATCH_POINT_ACTIVATE = registerSound("watch_point_activate");
    public static final RegistrySupplier<SoundEvent> WATCH_POINT_DEACTIVATE = registerSound("watch_point_deactivate");
    public static final RegistrySupplier<SoundEvent> LOG_BOOK_LOCK = registerSound("log_book_lock");
    public static final RegistrySupplier<SoundEvent> LOG_BOOK_UNLOCK = registerSound("log_book_unlock");
    public static final RegistrySupplier<SoundEvent> LOG_BOOK_WRONG_PIN = registerSound("log_book_wrong_pin");
    public static final RegistrySupplier<SoundEvent> LOGGER_START = registerSound("logger_start");
    public static final RegistrySupplier<SoundEvent> LOGGER_STOP = registerSound("logger_stop");
    public static final RegistrySupplier<SoundEvent> LOGGER_CANCEL = registerSound("logger_cancel");
    public static final RegistrySupplier<SoundEvent> ALL_SEEING_EYE_USE = registerFixedSound("all_seeing_eye_use", 50.0F);
    public static final RegistrySupplier<SoundEvent> ALL_SEEING_EYE_EXPIRE = registerSound("all_seeing_eye_expire");
    public static final RegistrySupplier<SoundEvent> CREEPING_DEATH_USE = registerSound("creeping_death_use");
    public static final RegistrySupplier<SoundEvent> CREEPING_DEATH_HEARTBEAT = registerSound("creeping_death_heartbeat");
    public static final RegistrySupplier<SoundEvent> CREEPING_DEATH_NUKE = registerFixedSound("creeping_death_nuke", 100.0F);
    public static final RegistrySupplier<SoundEvent> SCHOLAR_AMBIENT = registerSound("scholar_ambient");
    public static final RegistrySupplier<SoundEvent> SCHOLAR_YES = registerSound("scholar_yes");
    public static final RegistrySupplier<SoundEvent> SCHOLAR_NO = registerSound("scholar_no");
    public static final RegistrySupplier<SoundEvent> SCHOLAR_CELEBRATE = registerSound("scholar_celebrate");
    public static final RegistrySupplier<SoundEvent> SCHOLAR_HURT = registerSound("scholar_hurt");
    public static final RegistrySupplier<SoundEvent> SCHOLAR_DEATH = registerSound("scholar_death");
    public static final RegistrySupplier<SoundEvent> SCHOLAR_CURSE = registerSound("scholar_curse");
    public static final RegistrySupplier<SoundEvent> TRIM_OBSERVATION_ALERT = registerSound("trim_observation_alert");
    public static final RegistrySupplier<SoundEvent> SIGNAL_STATIC = registerSound("signal_static");

    private static RegistrySupplier<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(bodycam.id(name)));
    }

    private static RegistrySupplier<SoundEvent> registerFixedSound(String name, float range) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createFixedRangeEvent(bodycam.id(name), range));
    }

    public static void register() {
        SOUND_EVENTS.register();
    }
}
