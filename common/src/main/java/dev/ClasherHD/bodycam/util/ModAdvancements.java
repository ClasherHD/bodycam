package dev.ClasherHD.bodycam.util;

import dev.ClasherHD.bodycam.bodycam;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ModAdvancements {
    public static final ResourceLocation FIRST_OBSERVATION = bodycam.id("first_observation");
    public static final ResourceLocation USE_PORTABLE_PORTAL = bodycam.id("use_portable_portal");
    public static final ResourceLocation PODEST_MAX = bodycam.id("podest_max");
    public static final ResourceLocation CAUGHT_ON_LOGGER = bodycam.id("caught_on_logger");
    public static final ResourceLocation LOG_BOOK_ENCRYPTED = bodycam.id("log_book_encrypted");
    public static final ResourceLocation EMPTY_CHERRY_CHEST = bodycam.id("empty_cherry_chest");
    public static final ResourceLocation SCHOLAR_TRADE = bodycam.id("scholar_trade");
    public static final ResourceLocation SCHOLAR_MASTER = bodycam.id("scholar_master");
    public static final ResourceLocation SCHOLAR_CURSE = bodycam.id("scholar_curse");
    public static final ResourceLocation SURVIVE_CREEPING_DEATH = bodycam.id("survive_creeping_death");
    public static final ResourceLocation ULTRA_RARE_CHEST = bodycam.id("ultra_rare_chest");
    public static final ResourceLocation ULTRA_RARE_SCHOLAR = bodycam.id("ultra_rare_scholar");
    public static final ResourceLocation CRYSTAL_TRIM_SET = bodycam.id("crystal_trim_set");

    private ModAdvancements() {
    }

    public static void award(ServerPlayer player, ResourceLocation advancementId) {
        if (player == null || player.server == null || advancementId == null) {
            return;
        }
        AdvancementHolder holder = player.server.getAdvancements().get(advancementId);
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (progress.isDone()) {
            return;
        }
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }
}
