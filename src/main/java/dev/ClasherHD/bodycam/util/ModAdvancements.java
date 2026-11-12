package dev.ClasherHD.bodycam.util;

import dev.ClasherHD.bodycam.bodycam;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

@SuppressWarnings("null")
public final class ModAdvancements {

    public static final ResourceLocation FIRST_OBSERVATION = new ResourceLocation(bodycam.MODID, "first_observation");
    public static final ResourceLocation USE_PORTABLE_PORTAL = new ResourceLocation(bodycam.MODID, "use_portable_portal");
    public static final ResourceLocation PODEST_MAX = new ResourceLocation(bodycam.MODID, "podest_max");
    public static final ResourceLocation CAUGHT_ON_LOGGER = new ResourceLocation(bodycam.MODID, "caught_on_logger");
    public static final ResourceLocation LOG_BOOK_ENCRYPTED = new ResourceLocation(bodycam.MODID, "log_book_encrypted");
    public static final ResourceLocation EMPTY_CHERRY_CHEST = new ResourceLocation(bodycam.MODID, "empty_cherry_chest");
    public static final ResourceLocation SCHOLAR_TRADE = new ResourceLocation(bodycam.MODID, "scholar_trade");
    public static final ResourceLocation SCHOLAR_MASTER = new ResourceLocation(bodycam.MODID, "scholar_master");
    public static final ResourceLocation SCHOLAR_CURSE = new ResourceLocation(bodycam.MODID, "scholar_curse");
    public static final ResourceLocation SURVIVE_CREEPING_DEATH = new ResourceLocation(bodycam.MODID, "survive_creeping_death");
    public static final ResourceLocation ULTRA_RARE_CHEST = new ResourceLocation(bodycam.MODID, "ultra_rare_chest");
    public static final ResourceLocation ULTRA_RARE_SCHOLAR = new ResourceLocation(bodycam.MODID, "ultra_rare_scholar");
    public static final ResourceLocation CRYSTAL_TRIM_SET = new ResourceLocation(bodycam.MODID, "crystal_trim_set");

    private ModAdvancements() {
    }

    public static void award(ServerPlayer player, ResourceLocation advancementId) {
        if (player == null || player.server == null || advancementId == null) {
            return;
        }
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
