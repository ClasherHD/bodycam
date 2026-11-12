package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.util.ModAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class ScholarEntity extends AbstractVillager {

    public static final String VARIANT_COMMON = "common";
    public static final String VARIANT_UNCOMMON = "uncommon";
    public static final String VARIANT_RARE = "rare";
    public static final String VARIANT_RARE2 = "rare2";
    public static final String VARIANT_ULTRA_RARE = "ultra_rare";

    public static final String FAMILY_NIGHTSPY = "nightspy";
    public static final String FAMILY_REACH = "reach";
    public static final String FAMILY_ANONYMITY = "anonymity";

    public static final EntityDataAccessor<String> DATA_VARIANT = SynchedEntityData.defineId(
            ScholarEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> DATA_SCHOLAR_LEVEL = SynchedEntityData.defineId(
            ScholarEntity.class, EntityDataSerializers.INT);

    private final Set<String> usedItemKeys = new HashSet<>();
    private final Set<String> usedEnchantFamilies = new HashSet<>();
    private boolean variantInitialized = false;
    private String tradesGeneratedForVariant = "";
    private UUID lastDirectPlayerAttacker = null;
    private long lastDirectPlayerHitTick = -1000L;

    public ScholarEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation groundNav) {
            groundNav.setCanOpenDoors(true);
            groundNav.setCanPassDoors(true);
            groundNav.setCanFloat(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, VARIANT_COMMON);
        this.entityData.define(DATA_SCHOLAR_LEVEL, 1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public String getVariant() {
        return normalizeVariant(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(String variant) {
        this.entityData.set(DATA_VARIANT, normalizeVariant(variant));
    }

    public static String normalizeVariant(String raw) {
        if (raw == null || raw.isEmpty()) {
            return VARIANT_COMMON;
        }
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        return switch (lower) {
            case "uncommon", "scholar_uncommon" -> VARIANT_UNCOMMON;
            case "rare", "rare1", "scholar_rare" -> VARIANT_RARE;
            case "rare2", "scholar_rare2" -> VARIANT_RARE2;
            case "ultra_rare", "ultrarare", "scholar_ultra_rare" -> VARIANT_ULTRA_RARE;
            default -> VARIANT_COMMON;
        };
    }

    public String rollRandomVariant() {
        int roll = this.random.nextInt(100_000);
        if (roll == 0) {
            return VARIANT_ULTRA_RARE;
        }
        if (roll < 1 + 100) {
            return VARIANT_RARE;
        }
        if (roll < 1 + 100 + 100) {
            return VARIANT_RARE2;
        }
        if (roll < 1 + 100 + 100 + 1_000) {
            return VARIANT_UNCOMMON;
        }
        return VARIANT_COMMON;
    }

    public int getScholarLevel() {
        return Mth.clamp(this.entityData.get(DATA_SCHOLAR_LEVEL), 1, 5);
    }

    public void setScholarLevel(int level) {
        this.entityData.set(DATA_SCHOLAR_LEVEL, Mth.clamp(level, 1, 5));
    }

    public boolean isSlimModel() {
        String v = getVariant();
        return VARIANT_COMMON.equals(v) || VARIANT_UNCOMMON.equals(v);
    }

    public double getDiscountMultiplier() {
        return switch (getVariant()) {
            case VARIANT_UNCOMMON -> 0.90D;
            case VARIANT_RARE, VARIANT_RARE2 -> 0.75D;
            case VARIANT_ULTRA_RARE -> 0.67D;
            default -> 1.00D;
        };
    }

    public void applyVariantArmor() {
        String v = getVariant();
        ItemStack head;
        ItemStack chest;
        ItemStack legs;
        ItemStack feet;

        switch (v) {
            case VARIANT_UNCOMMON -> {
                head = new ItemStack(Items.DIAMOND_HELMET);
                chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                legs = new ItemStack(Items.DIAMOND_LEGGINGS);
                feet = new ItemStack(Items.DIAMOND_BOOTS);
            }
            case VARIANT_RARE, VARIANT_RARE2 -> {
                head = new ItemStack(Items.NETHERITE_HELMET);
                chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
                legs = new ItemStack(Items.NETHERITE_LEGGINGS);
                feet = new ItemStack(Items.NETHERITE_BOOTS);
            }
            case VARIANT_ULTRA_RARE -> {
                head = new ItemStack(Items.NETHERITE_HELMET);
                chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
                legs = new ItemStack(Items.NETHERITE_LEGGINGS);
                feet = new ItemStack(Items.NETHERITE_BOOTS);
                for (ItemStack armor : new ItemStack[]{head, chest, legs, feet}) {
                    armor.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
                    armor.enchant(Enchantments.UNBREAKING, 3);
                }
                feet.enchant(Enchantments.FALL_PROTECTION, 4);
            }
            default -> {
                head = new ItemStack(Items.IRON_HELMET);
                chest = new ItemStack(Items.IRON_CHESTPLATE);
                legs = new ItemStack(Items.IRON_LEGGINGS);
                feet = new ItemStack(Items.IRON_BOOTS);
            }
        }

        for (ItemStack armor : new ItemStack[]{head, chest, legs, feet}) {
            armor.enchant(Enchantments.VANISHING_CURSE, 1);
        }

        this.setItemSlot(EquipmentSlot.HEAD, head);
        this.setItemSlot(EquipmentSlot.CHEST, chest);
        this.setItemSlot(EquipmentSlot.LEGS, legs);
        this.setItemSlot(EquipmentSlot.FEET, feet);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            SpawnGroupData spawnData, CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if (dataTag != null && dataTag.contains("Variant", Tag.TAG_STRING)) {
            setVariant(dataTag.getString("Variant"));
            this.variantInitialized = true;
        } else if (!this.variantInitialized) {
            setVariant(rollRandomVariant());
            this.variantInitialized = true;
        }
        this.setPersistenceRequired();
        if (!this.hasRestriction()) {
            this.restrictTo(this.blockPosition(), 24);
        }
        this.applyVariantArmor();
        if (this.offers == null || this.offers.isEmpty()) {
            this.offers = new MerchantOffers();
            this.updateTrades();
        }
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !this.variantInitialized) {
            setVariant(rollRandomVariant());
            this.variantInitialized = true;
            this.applyVariantArmor();
            if (!this.hasRestriction()) {
                this.restrictTo(this.blockPosition(), 24);
            }
            if (this.offers == null || this.offers.isEmpty()) {
                this.offers = new MerchantOffers();
                this.updateTrades();
            }
        }
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public boolean showProgressBar() {
        return true;
    }

    @Override
    public boolean canRestock() {
        return false;
    }

    @Override
    public int getVillagerXp() {
        int lvl = getScholarLevel();
        int minXp = VillagerData.getMinXpPerLevel(lvl);
        if (lvl >= 5) {
            return minXp;
        }
        int maxXp = VillagerData.getMaxXpPerLevel(lvl);
        if (this.offers == null || this.offers.isEmpty()) {
            return minXp;
        }
        int totalUses = 0;
        int totalMax = 0;
        for (MerchantOffer offer : this.offers) {
            int max = Math.max(1, offer.getMaxUses());
            totalUses += Math.min(max, offer.getUses());
            totalMax += max;
        }
        if (totalMax <= 0) {
            return minXp;
        }
        float ratio = Mth.clamp((float) totalUses / (float) totalMax, 0.0F, 1.0F);
        return minXp + Math.round((maxXp - minXp - 1) * ratio);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.SCHOLAR_SPAWN_EGG.get())) {
            return super.mobInteract(player, hand);
        }
        if (this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (!this.level().isClientSide()) {
                if (player instanceof ServerPlayer sp && VARIANT_ULTRA_RARE.equals(getVariant())) {
                    ModAdvancements.award(sp, ModAdvancements.ULTRA_RARE_SCHOLAR);
                }
                if (this.getOffers().isEmpty()) {
                    this.updateTrades();
                }
                if (!this.getOffers().isEmpty()) {
                    this.setTradingPlayer(player);
                    this.openTradingScreen(player, this.getDisplayName(), this.getScholarLevel());
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (this.level() instanceof ServerLevel sLevel) {
            sLevel.addFreshEntity(new ExperienceOrb(sLevel, this.getX(), this.getY() + 0.5D, this.getZ(), 3 + this.random.nextInt(4)));
        }

        if (this.getScholarLevel() < 5 && areAllOffersOutOfStock()) {
            int nextLevel = this.getScholarLevel() + 1;
            this.setScholarLevel(nextLevel);
            this.unlockTradesForLevel(nextLevel);
            this.playCelebrateSound();
            if (this.level() instanceof ServerLevel sLevel) {
                sLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2D, this.getZ(), 15, 0.4D, 0.5D, 0.4D, 0.02D);
                sLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.4D, this.getZ(), 20, 0.4D, 0.5D, 0.4D, 0.1D);
            }
        }

        if (this.getTradingPlayer() instanceof ServerPlayer sp) {
            ModAdvancements.award(sp, ModAdvancements.SCHOLAR_TRADE);
            if (this.getScholarLevel() >= 5) {
                ModAdvancements.award(sp, ModAdvancements.SCHOLAR_MASTER);
            }
            if (sp.containerMenu instanceof MerchantMenu) {
                sp.sendMerchantOffers(
                        sp.containerMenu.containerId,
                        this.getOffers(),
                        this.getScholarLevel(),
                        this.getVillagerXp(),
                        this.showProgressBar(),
                        this.canRestock()
                );
            }
        }
    }

    private boolean areAllOffersOutOfStock() {
        if (this.offers == null || this.offers.isEmpty()) {
            return false;
        }
        for (MerchantOffer offer : this.offers) {
            if (!offer.isOutOfStock()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public float getVoicePitch() {
        String v = getVariant();
        if (VARIANT_COMMON.equals(v) || VARIANT_UNCOMMON.equals(v)) {
            return (this.random.nextFloat() - this.random.nextFloat()) * 0.12F + 1.50F;
        }
        if (VARIANT_ULTRA_RARE.equals(v)) {
            return (this.random.nextFloat() - this.random.nextFloat()) * 0.10F + 0.75F;
        }
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.12F + 0.95F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SCHOLAR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SCHOLAR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SCHOLAR_DEATH.get();
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.SCHOLAR_YES.get();
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean getYesSound) {
        return getYesSound ? ModSounds.SCHOLAR_YES.get() : ModSounds.SCHOLAR_NO.get();
    }

    @Override
    public void playCelebrateSound() {
        this.playSound(ModSounds.SCHOLAR_CELEBRATE.get(), this.getSoundVolume(), this.getVoicePitch());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide()) {
            if (source.getEntity() instanceof ServerPlayer sp) {
                this.lastDirectPlayerAttacker = sp.getUUID();
                this.lastDirectPlayerHitTick = this.level().getGameTime();
            } else if (source.getDirectEntity() instanceof ServerPlayer sp) {
                this.lastDirectPlayerAttacker = sp.getUUID();
                this.lastDirectPlayerHitTick = this.level().getGameTime();
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel sLevel) {
            ServerPlayer killer = resolveIntentionalPlayerKiller(sLevel, source);
            if (killer != null && killer.isAlive() && !killer.isCreative() && !killer.isSpectator()) {
                ModAdvancements.award(killer, ModAdvancements.SCHOLAR_CURSE);
                triggerScholarCurse(sLevel, killer);
            }
        }
        super.die(source);
    }

    private ServerPlayer resolveIntentionalPlayerKiller(ServerLevel level, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer sp) {
            return sp;
        }
        if (source.getDirectEntity() instanceof ServerPlayer sp) {
            return sp;
        }
        if (source.is(DamageTypeTags.IS_FALL)
                && this.lastDirectPlayerAttacker != null
                && (level.getGameTime() - this.lastDirectPlayerHitTick) <= 100L) {
            return level.getServer().getPlayerList().getPlayer(this.lastDirectPlayerAttacker);
        }
        return null;
    }

    private void triggerScholarCurse(ServerLevel sLevel, ServerPlayer killer) {
        boolean ultraRare = VARIANT_ULTRA_RARE.equals(this.getVariant());

        sLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(), 35, 0.6D, 0.8D, 0.6D, 0.02D);
        sLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0D, this.getZ(), 25, 0.6D, 0.8D, 0.6D, 0.03D);
        sLevel.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SCHOLAR_CURSE.get(), SoundSource.HOSTILE, 2.0F, 0.65F);

        killer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 220, 0, false, false, true));
        killer.serverLevel().playSound(
                null,
                killer.getX(), killer.getY(), killer.getZ(),
                ModSounds.CREEPING_DEATH_HEARTBEAT.get(),
                SoundSource.HOSTILE,
                2.0F, 0.9F
        );

        if (!CreepingDeathEntity.isTargetAlreadyHunted(sLevel.getServer(), killer.getUUID())) {
            if (killer.getPersistentData().getBoolean("bodycam_active")) {
                BodycamResetCameraPacket.executeReset(killer);
                CreepingDeathEntity.sendHuntNoEscapeTitle(killer);
            }
            CreepingDeathEntity.scheduleScholarCurseSpawn(killer.getUUID(), this.getVariant(), ultraRare);
        } else if (ultraRare) {
            CreepingDeathEntity.scheduleOscNuke(killer.serverLevel().dimension(), killer.position());
        }
    }

    @Override
    protected void updateTrades() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
        }
        if (this.offers.isEmpty()) {
            this.usedItemKeys.clear();
            this.usedEnchantFamilies.clear();
            this.setScholarLevel(1);
            this.tradesGeneratedForVariant = this.getVariant();
            unlockTradesForLevel(1);
        }
    }

    public void unlockTradesForLevel(int tier) {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
        }
        String variant = this.getVariant();

        if (tier >= 1 && tier <= 4) {
            List<TradePoolEntry> pool = buildTierPool(variant, tier);
            boolean bookPickedInTier = false;
            for (int pick = 0; pick < 2; pick++) {
                TradePoolEntry chosen = rollFromPool(pool, !bookPickedInTier);
                if (chosen != null) {
                    if (chosen.isBook()) {
                        bookPickedInTier = true;
                        this.usedEnchantFamilies.add(chosen.enchantFamily);
                    } else {
                        this.usedItemKeys.add(chosen.itemKey);
                    }
                    this.offers.add(createOfferFromEntry(chosen, variant));
                }
            }
            return;
        }

        if (tier == 5) {
            if (VARIANT_ULTRA_RARE.equals(variant)) {
                List<TradePoolEntry> relicPool = buildUltraRareTier5RelicPool();
                TradePoolEntry chosenRelic = rollFromPool(relicPool, false);
                if (chosenRelic != null) {
                    this.usedItemKeys.add(chosenRelic.itemKey);
                    this.offers.add(createOfferFromEntry(chosenRelic, variant));
                }
                List<TradePoolEntry> godToolPool = buildUltraRareTier5GodToolPool();
                TradePoolEntry chosenGodTool = rollFromPool(godToolPool, false);
                if (chosenGodTool != null) {
                    this.usedItemKeys.add(chosenGodTool.itemKey);
                    this.offers.add(createOfferFromEntry(chosenGodTool, variant));
                }
                return;
            }

            List<TradePoolEntry> masterPool = buildStandardTier5Pool();
            boolean bookPickedInTier = false;
            TradePoolEntry firstPick = rollFromPool(masterPool, true);
            if (firstPick != null) {
                if (firstPick.isBook()) {
                    bookPickedInTier = true;
                    this.usedEnchantFamilies.add(firstPick.enchantFamily);
                } else {
                    this.usedItemKeys.add(firstPick.itemKey);
                }
                this.offers.add(createOfferFromEntry(firstPick, variant));
            }

            if (!VARIANT_COMMON.equals(variant)) {
                List<TradePoolEntry> bonusPool = buildVariantTier5BonusPool(variant);
                TradePoolEntry bonusPick = rollFromPool(bonusPool, !bookPickedInTier);
                if (bonusPick == null && bookPickedInTier) {
                    bonusPick = rollFromPool(masterPool, false);
                }
                if (bonusPick != null) {
                    if (bonusPick.isBook()) {
                        this.usedEnchantFamilies.add(bonusPick.enchantFamily);
                    } else {
                        this.usedItemKeys.add(bonusPick.itemKey);
                    }
                    this.offers.add(createOfferFromEntry(bonusPick, variant));
                }
            }
        }
    }

    private TradePoolEntry rollFromPool(List<TradePoolEntry> pool, boolean allowBooks) {
        List<TradePoolEntry> eligibleBooks = new ArrayList<>();
        List<TradePoolEntry> eligibleNonBooks = new ArrayList<>();
        double excludedBookWeight = 0.0D;

        for (TradePoolEntry entry : pool) {
            if (entry.isBook()) {
                if (!allowBooks) {
                    continue;
                }
                if (this.usedEnchantFamilies.contains(entry.enchantFamily)) {
                    excludedBookWeight += entry.weight;
                } else {
                    eligibleBooks.add(entry);
                }
            } else {
                if (!this.usedItemKeys.contains(entry.itemKey)) {
                    eligibleNonBooks.add(entry);
                }
            }
        }

        if (eligibleBooks.isEmpty() && eligibleNonBooks.isEmpty()) {
            return null;
        }

        double bonusPerNonBook = (!eligibleNonBooks.isEmpty() && allowBooks && excludedBookWeight > 0.0D)
                ? (excludedBookWeight / eligibleNonBooks.size())
                : 0.0D;

        List<TradePoolEntry> candidates = new ArrayList<>();
        List<Double> effectiveWeights = new ArrayList<>();
        double totalWeight = 0.0D;

        for (TradePoolEntry book : eligibleBooks) {
            candidates.add(book);
            effectiveWeights.add(book.weight);
            totalWeight += book.weight;
        }
        for (TradePoolEntry item : eligibleNonBooks) {
            double w = item.weight + bonusPerNonBook;
            candidates.add(item);
            effectiveWeights.add(w);
            totalWeight += w;
        }

        if (totalWeight <= 0.0D || candidates.isEmpty()) {
            return candidates.isEmpty() ? null : candidates.get(this.random.nextInt(candidates.size()));
        }

        double r = this.random.nextDouble() * totalWeight;
        double cumulative = 0.0D;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += effectiveWeights.get(i);
            if (r <= cumulative) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private MerchantOffer createOfferFromEntry(TradePoolEntry entry, String variant) {
        int baseCost = entry.minCost == entry.maxCost
                ? entry.minCost
                : entry.minCost + this.random.nextInt(entry.maxCost - entry.minCost + 1);

        int finalCost = entry.ignoreDiscount
                ? baseCost
                : Math.max(1, (int) Math.round(baseCost * getDiscountMultiplier()));
        finalCost = Mth.clamp(finalCost, 1, 64);

        ItemStack costA = new ItemStack(ModItems.OBSERVATION_CRYSTAL.get(), finalCost);
        ItemStack costB = entry.secondBuySupplier != null ? entry.secondBuySupplier.get() : ItemStack.EMPTY;

        ItemStack result = entry.resultSupplier.get();
        int count = entry.minCount == entry.maxCount
                ? entry.minCount
                : entry.minCount + this.random.nextInt(entry.maxCount - entry.minCount + 1);
        result.setCount(Mth.clamp(count, 1, result.getMaxStackSize()));

        if (entry.applyVariantDurability) {
            applyModItemDurability(result, variant);
        }

        return new MerchantOffer(costA, costB, result, 0, entry.maxUses, 5, 0.0F);
    }

    private void applyModItemDurability(ItemStack stack, String variant) {
        if (stack.getItem() instanceof PortableNetherPortalItem) {
            int charges = switch (variant) {
                case VARIANT_ULTRA_RARE -> 2;
                case VARIANT_RARE, VARIANT_RARE2 -> 1 + this.random.nextInt(2);
                case VARIANT_UNCOMMON -> this.random.nextBoolean() ? 1 : 0;
                default -> 0;
            };
            PortableNetherPortalItem.setCharges(stack, charges);
            return;
        }

        if (!stack.isDamageableItem()) {
            return;
        }

        int maxDamage = stack.getMaxDamage();
        switch (variant) {
            case VARIANT_ULTRA_RARE -> stack.setDamageValue(0);
            case VARIANT_RARE, VARIANT_RARE2 -> {
                float pct = 0.50F + this.random.nextFloat() * 0.50F;
                int remaining = Math.max(1, Math.min(maxDamage, Math.round(maxDamage * pct)));
                stack.setDamageValue(maxDamage - remaining);
            }
            case VARIANT_UNCOMMON -> {
                float pct = 0.25F + this.random.nextFloat() * 0.25F;
                int remaining = Math.max(1, Math.min(maxDamage, Math.round(maxDamage * pct)));
                stack.setDamageValue(maxDamage - remaining);
            }
            default -> ItemDurabilityHelper.setEmpty(stack);
        }
    }

    private List<TradePoolEntry> buildTierPool(String variant, int tier) {
        return switch (tier) {
            case 1 -> buildTier1Pool(variant);
            case 2 -> buildTier2Pool(variant);
            case 3 -> buildTier3Pool(variant);
            case 4 -> buildTier4Pool(variant);
            default -> buildStandardTier5Pool();
        };
    }

    private List<TradePoolEntry> buildTier1Pool(String variant) {
        List<TradePoolEntry> list = new ArrayList<>();
        if (VARIANT_ULTRA_RARE.equals(variant)) {
            list.add(TradePoolEntry.item("log_book", 35.0D, () -> new ItemStack(ModItems.LOG_BOOK.get()), 1, 1, 2, 3, 4, false));
            list.add(TradePoolEntry.item("logger", 20.0D, () -> new ItemStack(ModItems.LOGGER.get()), 1, 1, 3, 4, 1, true));
            list.add(TradePoolEntry.item("paper", 15.0D, () -> new ItemStack(Items.PAPER), 4, 6, 1, 1, 4, false));
            list.add(TradePoolEntry.item("leather", 12.0D, () -> new ItemStack(Items.LEATHER), 2, 3, 1, 2, 3, false));
            list.add(TradePoolEntry.item("spruce_log", 10.0D, () -> new ItemStack(Items.SPRUCE_LOG), 4, 8, 1, 1, 3, false));
            list.add(TradePoolEntry.item("spruce_sapling", 8.0D, () -> new ItemStack(Items.SPRUCE_SAPLING), 1, 2, 1, 1, 3, false));
            return list;
        }
        if (VARIANT_UNCOMMON.equals(variant)) {
            list.add(TradePoolEntry.item("log_book", 32.0D, () -> new ItemStack(ModItems.LOG_BOOK.get()), 1, 1, 2, 3, 1, false));
            list.add(TradePoolEntry.item("logger", 23.0D, () -> new ItemStack(ModItems.LOGGER.get()), 1, 1, 3, 4, 1, true));
            list.add(TradePoolEntry.item("paper", 15.0D, () -> new ItemStack(Items.PAPER), 4, 6, 1, 1, 4, false));
            list.add(TradePoolEntry.item("leather", 11.0D, () -> new ItemStack(Items.LEATHER), 2, 3, 1, 2, 3, false));
            list.add(TradePoolEntry.item("cherry_log", 8.0D, () -> new ItemStack(Items.CHERRY_LOG), 4, 8, 1, 1, 3, false));
            list.add(TradePoolEntry.item("pink_petals", 6.0D, () -> new ItemStack(Items.PINK_PETALS), 4, 8, 1, 1, 3, false));
            list.add(TradePoolEntry.item("cherry_sapling", 5.0D, () -> new ItemStack(Items.CHERRY_SAPLING), 1, 2, 1, 1, 3, false));
            return list;
        }
        if (VARIANT_RARE.equals(variant) || VARIANT_RARE2.equals(variant)) {
            double logBookW = VARIANT_RARE.equals(variant) ? 35.0D : 32.0D;
            double loggerW = VARIANT_RARE.equals(variant) ? 25.0D : 28.0D;
            list.add(TradePoolEntry.item("log_book", logBookW, () -> new ItemStack(ModItems.LOG_BOOK.get()), 1, 1, 2, 3, 1, false));
            list.add(TradePoolEntry.item("logger", loggerW, () -> new ItemStack(ModItems.LOGGER.get()), 1, 1, 3, 4, 1, true));
            list.add(TradePoolEntry.item("paper", 14.0D, () -> new ItemStack(Items.PAPER), 4, 6, 1, 1, 4, false));
            list.add(TradePoolEntry.item("leather", 10.0D, () -> new ItemStack(Items.LEATHER), 2, 3, 1, 2, 3, false));
            list.add(TradePoolEntry.item("cherry_log", 6.0D, () -> new ItemStack(Items.CHERRY_LOG), 4, 8, 1, 1, 3, false));
            list.add(TradePoolEntry.item("pink_petals", 5.0D, () -> new ItemStack(Items.PINK_PETALS), 4, 8, 1, 1, 3, false));
            list.add(TradePoolEntry.item("cherry_sapling", 5.0D, () -> new ItemStack(Items.CHERRY_SAPLING), 1, 2, 1, 1, 3, false));
            return list;
        }
        list.add(TradePoolEntry.item("log_book", 25.0D, () -> new ItemStack(ModItems.LOG_BOOK.get()), 1, 1, 2, 3, 1, false));
        list.add(TradePoolEntry.item("logger", 15.0D, () -> new ItemStack(ModItems.LOGGER.get()), 1, 1, 3, 4, 1, true));
        list.add(TradePoolEntry.item("paper", 20.0D, () -> new ItemStack(Items.PAPER), 4, 6, 1, 1, 4, false));
        list.add(TradePoolEntry.item("leather", 15.0D, () -> new ItemStack(Items.LEATHER), 2, 3, 1, 2, 3, false));
        list.add(TradePoolEntry.item("cherry_log", 10.0D, () -> new ItemStack(Items.CHERRY_LOG), 4, 8, 1, 1, 3, false));
        list.add(TradePoolEntry.item("pink_petals", 8.0D, () -> new ItemStack(Items.PINK_PETALS), 4, 8, 1, 1, 3, false));
        list.add(TradePoolEntry.item("cherry_sapling", 7.0D, () -> new ItemStack(Items.CHERRY_SAPLING), 1, 2, 1, 1, 3, false));
        return list;
    }

    private List<TradePoolEntry> buildTier2Pool(String variant) {
        List<TradePoolEntry> list = new ArrayList<>();
        if (VARIANT_ULTRA_RARE.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 30.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 4, 4, 7, 2));
            list.add(TradePoolEntry.book(FAMILY_REACH, 30.0D, ModEnchantments.REACH_ENCHANTMENT.get(), 2, 5, 8, 2));
            list.add(TradePoolEntry.item("amethyst_shard", 10.0D, () -> new ItemStack(Items.AMETHYST_SHARD), 4, 8, 1, 2, 3, false));
            list.add(TradePoolEntry.item("bodycam_monitor", 14.0D, () -> new ItemStack(ModItems.BODYCAM_MONITOR.get()), 1, 1, 3, 5, 1, true));
            list.add(TradePoolEntry.item("player_locator_compass", 8.0D, () -> new ItemStack(ModItems.PLAYER_LOCATOR_COMPASS.get()), 1, 1, 3, 5, 1, true));
            list.add(TradePoolEntry.item("dimension_locator", 8.0D, () -> new ItemStack(ModItems.DIMENSION_LOCATOR.get()), 1, 1, 4, 6, 1, true));
            return list;
        }
        if (VARIANT_UNCOMMON.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 30.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 1, 4, 7, 2));
            list.add(TradePoolEntry.book(FAMILY_REACH, 30.0D, ModEnchantments.REACH_ENCHANTMENT.get(), 1, 5, 8, 2));
            list.add(TradePoolEntry.item("amethyst_shard", 4.0D, () -> new ItemStack(Items.AMETHYST_SHARD), 4, 8, 1, 2, 3, false));
            list.add(TradePoolEntry.item("spyglass", 4.0D, () -> new ItemStack(Items.SPYGLASS), 2, 2, 1, 2, 2, false));
            list.add(TradePoolEntry.item("cherry_leaves", 4.0D, () -> new ItemStack(Items.CHERRY_LEAVES), 8, 12, 1, 1, 3, false));
            list.add(TradePoolEntry.item("bodycam_monitor", 9.4D, () -> new ItemStack(ModItems.BODYCAM_MONITOR.get()), 1, 1, 3, 5, 1, true));
            list.add(TradePoolEntry.item("player_locator_compass", 9.3D, () -> new ItemStack(ModItems.PLAYER_LOCATOR_COMPASS.get()), 1, 1, 3, 5, 1, true));
            list.add(TradePoolEntry.item("dimension_locator", 9.3D, () -> new ItemStack(ModItems.DIMENSION_LOCATOR.get()), 1, 1, 4, 6, 1, true));
            return list;
        }
        if (VARIANT_RARE.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 30.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 2, 4, 7, 2));
            list.add(TradePoolEntry.book(FAMILY_REACH, 30.0D, ModEnchantments.REACH_ENCHANTMENT.get(), 2, 5, 8, 2));
            list.add(TradePoolEntry.item("amethyst_shard", 4.0D, () -> new ItemStack(Items.AMETHYST_SHARD), 4, 8, 1, 2, 3, false));
            list.add(TradePoolEntry.item("spyglass", 4.0D, () -> new ItemStack(Items.SPYGLASS), 2, 2, 1, 2, 2, false));
            list.add(TradePoolEntry.item("cherry_leaves", 2.0D, () -> new ItemStack(Items.CHERRY_LEAVES), 8, 12, 1, 1, 3, false));
            list.add(TradePoolEntry.item("bodycam_monitor", 12.0D, () -> new ItemStack(ModItems.BODYCAM_MONITOR.get()), 1, 1, 3, 5, 1, true));
            list.add(TradePoolEntry.item("player_locator_compass", 10.0D, () -> new ItemStack(ModItems.PLAYER_LOCATOR_COMPASS.get()), 1, 1, 3, 5, 1, true));
            list.add(TradePoolEntry.item("dimension_locator", 8.0D, () -> new ItemStack(ModItems.DIMENSION_LOCATOR.get()), 1, 1, 4, 6, 1, true));
            return list;
        }
        if (VARIANT_RARE2.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 28.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 1, 4, 7, 2));
            list.add(TradePoolEntry.book(FAMILY_REACH, 28.0D, ModEnchantments.REACH_ENCHANTMENT.get(), 1, 5, 8, 2));
            list.add(TradePoolEntry.item("amethyst_shard", 4.0D, () -> new ItemStack(Items.AMETHYST_SHARD), 4, 8, 1, 2, 3, false));
            list.add(TradePoolEntry.item("spyglass", 4.0D, () -> new ItemStack(Items.SPYGLASS), 2, 2, 1, 2, 2, false));
            list.add(TradePoolEntry.item("cherry_leaves", 2.0D, () -> new ItemStack(Items.CHERRY_LEAVES), 8, 12, 1, 1, 3, false));
            list.add(TradePoolEntry.item("dimension_locator", 16.0D, () -> new ItemStack(ModItems.DIMENSION_LOCATOR.get()), 1, 1, 4, 6, 1, true));
            list.add(TradePoolEntry.item("bodycam_monitor", 9.0D, () -> new ItemStack(ModItems.BODYCAM_MONITOR.get()), 1, 1, 3, 5, 1, true));
            list.add(TradePoolEntry.item("player_locator_compass", 9.0D, () -> new ItemStack(ModItems.PLAYER_LOCATOR_COMPASS.get()), 1, 1, 3, 5, 1, true));
            return list;
        }
        list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 30.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 1, 4, 7, 2));
        list.add(TradePoolEntry.book(FAMILY_REACH, 30.0D, ModEnchantments.REACH_ENCHANTMENT.get(), 1, 5, 8, 2));
        list.add(TradePoolEntry.item("amethyst_shard", 7.0D, () -> new ItemStack(Items.AMETHYST_SHARD), 4, 8, 1, 2, 3, false));
        list.add(TradePoolEntry.item("spyglass", 7.0D, () -> new ItemStack(Items.SPYGLASS), 2, 2, 1, 2, 2, false));
        list.add(TradePoolEntry.item("cherry_leaves", 6.0D, () -> new ItemStack(Items.CHERRY_LEAVES), 8, 12, 1, 1, 3, false));
        list.add(TradePoolEntry.item("bodycam_monitor", 6.67D, () -> new ItemStack(ModItems.BODYCAM_MONITOR.get()), 1, 1, 3, 5, 1, true));
        list.add(TradePoolEntry.item("player_locator_compass", 6.67D, () -> new ItemStack(ModItems.PLAYER_LOCATOR_COMPASS.get()), 1, 1, 3, 5, 1, true));
        list.add(TradePoolEntry.item("dimension_locator", 6.66D, () -> new ItemStack(ModItems.DIMENSION_LOCATOR.get()), 1, 1, 4, 6, 1, true));
        return list;
    }

    private List<TradePoolEntry> buildTier3Pool(String variant) {
        List<TradePoolEntry> list = new ArrayList<>();
        if (VARIANT_ULTRA_RARE.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 30.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 4, 7, 11, 2));
            list.add(TradePoolEntry.book(FAMILY_ANONYMITY, 28.0D, ModEnchantments.ANONYMITY_ENCHANTMENT.get(), 2, 8, 12, 1));
            list.add(TradePoolEntry.item("netherite_scrap", 10.5D, () -> new ItemStack(Items.NETHERITE_SCRAP), 1, 2, 15, 22, 4, false));
            list.add(TradePoolEntry.item("ender_pearl", 10.5D, () -> new ItemStack(Items.ENDER_PEARL), 4, 8, 2, 4, 2, false));
            list.add(TradePoolEntry.item("jammer", 7.0D, () -> new ItemStack(ModItems.JAMMER.get()), 1, 1, 5, 8, 1, true));
            list.add(TradePoolEntry.item("anonymizer", 7.0D, () -> new ItemStack(ModItems.ANONYMIZER.get()), 1, 1, 5, 8, 1, true));
            list.add(TradePoolEntry.item("watch_point", 7.0D, () -> new ItemStack(ModItems.WATCH_POINT_ITEM.get()), 1, 1, 6, 9, 2, false));
            return list;
        }
        if (VARIANT_UNCOMMON.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 30.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 2, 7, 11, 2));
            list.add(TradePoolEntry.book(FAMILY_ANONYMITY, 28.0D, ModEnchantments.ANONYMITY_ENCHANTMENT.get(), 1, 8, 12, 1));
            list.add(TradePoolEntry.item("quartz", 6.0D, () -> new ItemStack(Items.QUARTZ), 4, 8, 1, 2, 3, false));
            list.add(TradePoolEntry.item("ender_pearl", 7.0D, () -> new ItemStack(Items.ENDER_PEARL), 2, 4, 2, 4, 2, false));
            list.add(TradePoolEntry.item("jammer", 9.7D, () -> new ItemStack(ModItems.JAMMER.get()), 1, 1, 5, 8, 1, true));
            list.add(TradePoolEntry.item("anonymizer", 9.7D, () -> new ItemStack(ModItems.ANONYMIZER.get()), 1, 1, 5, 8, 1, true));
            list.add(TradePoolEntry.item("watch_point", 9.6D, () -> new ItemStack(ModItems.WATCH_POINT_ITEM.get()), 1, 1, 6, 9, 1, false));
            return list;
        }
        if (VARIANT_RARE.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 32.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 3, 7, 11, 2));
            list.add(TradePoolEntry.book(FAMILY_ANONYMITY, 26.0D, ModEnchantments.ANONYMITY_ENCHANTMENT.get(), 1, 8, 12, 1));
            list.add(TradePoolEntry.item("quartz", 5.0D, () -> new ItemStack(Items.QUARTZ), 4, 8, 1, 2, 3, false));
            list.add(TradePoolEntry.item("ender_pearl", 6.0D, () -> new ItemStack(Items.ENDER_PEARL), 2, 4, 2, 4, 2, false));
            list.add(TradePoolEntry.item("jammer", 7.0D, () -> new ItemStack(ModItems.JAMMER.get()), 1, 1, 5, 8, 1, true));
            list.add(TradePoolEntry.item("anonymizer", 6.0D, () -> new ItemStack(ModItems.ANONYMIZER.get()), 1, 1, 5, 8, 1, true));
            list.add(TradePoolEntry.item("watch_point", 18.0D, () -> new ItemStack(ModItems.WATCH_POINT_ITEM.get()), 1, 1, 6, 9, 1, false));
            return list;
        }
        if (VARIANT_RARE2.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_ANONYMITY, 38.0D, ModEnchantments.ANONYMITY_ENCHANTMENT.get(), 1, 8, 12, 1));
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 20.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 2, 7, 11, 2));
            list.add(TradePoolEntry.item("crying_obsidian", 6.0D, () -> new ItemStack(Items.CRYING_OBSIDIAN), 4, 8, 2, 3, 3, false));
            list.add(TradePoolEntry.item("blaze_rod", 6.0D, () -> new ItemStack(Items.BLAZE_ROD), 2, 4, 2, 4, 3, false));
            list.add(TradePoolEntry.item("anonymizer", 13.0D, () -> new ItemStack(ModItems.ANONYMIZER.get()), 1, 1, 5, 8, 1, true));
            list.add(TradePoolEntry.item("jammer", 12.0D, () -> new ItemStack(ModItems.JAMMER.get()), 1, 1, 5, 8, 1, true));
            list.add(TradePoolEntry.item("watch_point", 5.0D, () -> new ItemStack(ModItems.WATCH_POINT_ITEM.get()), 1, 1, 6, 9, 1, false));
            return list;
        }
        list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 30.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 2, 7, 11, 2));
        list.add(TradePoolEntry.book(FAMILY_ANONYMITY, 28.0D, ModEnchantments.ANONYMITY_ENCHANTMENT.get(), 1, 8, 12, 1));
        list.add(TradePoolEntry.item("quartz", 10.5D, () -> new ItemStack(Items.QUARTZ), 4, 8, 1, 2, 3, false));
        list.add(TradePoolEntry.item("ender_pearl", 10.5D, () -> new ItemStack(Items.ENDER_PEARL), 2, 4, 2, 4, 2, false));
        list.add(TradePoolEntry.item("jammer", 7.0D, () -> new ItemStack(ModItems.JAMMER.get()), 1, 1, 5, 8, 1, true));
        list.add(TradePoolEntry.item("anonymizer", 7.0D, () -> new ItemStack(ModItems.ANONYMIZER.get()), 1, 1, 5, 8, 1, true));
        list.add(TradePoolEntry.item("watch_point", 7.0D, () -> new ItemStack(ModItems.WATCH_POINT_ITEM.get()), 1, 1, 6, 9, 1, false));
        return list;
    }

    private List<TradePoolEntry> buildTier4Pool(String variant) {
        List<TradePoolEntry> list = new ArrayList<>();
        if (VARIANT_ULTRA_RARE.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 33.33D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 4, 11, 16, 1));
            list.add(TradePoolEntry.book(FAMILY_REACH, 33.33D, ModEnchantments.REACH_ENCHANTMENT.get(), 2, 13, 18, 1));
            list.add(TradePoolEntry.item("experience_bottle", 8.33D, () -> new ItemStack(Items.EXPERIENCE_BOTTLE), 4, 8, 2, 3, 3, false));
            list.add(TradePoolEntry.item("netherite_ingot", 8.33D, () -> new ItemStack(Items.NETHERITE_INGOT), 1, 1, 24, 34, 1, false));
            list.add(TradePoolEntry.item("podest", 5.56D, () -> new ItemStack(ModItems.PODEST_ITEM.get()), 1, 1, 9, 13, 1, false));
            list.add(TradePoolEntry.item("portable_nether_portal", 5.56D, () -> new ItemStack(ModItems.PORTABLE_NETHER_PORTAL.get()), 1, 1, 11, 15, 1, true));
            list.add(TradePoolEntry.item("ancient_table", 5.56D, () -> new ItemStack(ModItems.ANCIENT_TABLE_ITEM.get()), 1, 1, 14, 20, 1, false));
            return list;
        }
        if (VARIANT_UNCOMMON.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 33.33D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 3, 11, 16, 1));
            list.add(TradePoolEntry.book(FAMILY_REACH, 33.33D, ModEnchantments.REACH_ENCHANTMENT.get(), 2, 13, 18, 1));
            list.add(TradePoolEntry.item("experience_bottle", 5.0D, () -> new ItemStack(Items.EXPERIENCE_BOTTLE), 2, 4, 2, 3, 3, false));
            list.add(TradePoolEntry.item("diamond", 5.0D, () -> new ItemStack(Items.DIAMOND), 1, 2, 3, 5, 2, false));
            list.add(TradePoolEntry.item("podest", 7.8D, () -> new ItemStack(ModItems.PODEST_ITEM.get()), 1, 1, 9, 13, 1, false));
            list.add(TradePoolEntry.item("portable_nether_portal", 7.8D, () -> new ItemStack(ModItems.PORTABLE_NETHER_PORTAL.get()), 1, 1, 11, 15, 1, true));
            list.add(TradePoolEntry.item("ancient_table", 7.74D, () -> new ItemStack(ModItems.ANCIENT_TABLE_ITEM.get()), 1, 1, 14, 20, 1, false));
            return list;
        }
        if (VARIANT_RARE.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 33.33D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 4, 11, 16, 1));
            list.add(TradePoolEntry.book(FAMILY_REACH, 33.33D, ModEnchantments.REACH_ENCHANTMENT.get(), 2, 13, 18, 1));
            list.add(TradePoolEntry.item("experience_bottle", 4.0D, () -> new ItemStack(Items.EXPERIENCE_BOTTLE), 2, 4, 2, 3, 3, false));
            list.add(TradePoolEntry.item("diamond", 4.0D, () -> new ItemStack(Items.DIAMOND), 1, 2, 3, 5, 2, false));
            list.add(TradePoolEntry.item("podest", 11.0D, () -> new ItemStack(ModItems.PODEST_ITEM.get()), 1, 1, 9, 13, 1, false));
            list.add(TradePoolEntry.item("ancient_table", 11.0D, () -> new ItemStack(ModItems.ANCIENT_TABLE_ITEM.get()), 1, 1, 14, 20, 1, false));
            list.add(TradePoolEntry.item("portable_nether_portal", 3.34D, () -> new ItemStack(ModItems.PORTABLE_NETHER_PORTAL.get()), 1, 1, 11, 15, 1, true));
            return list;
        }
        if (VARIANT_RARE2.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_REACH, 28.0D, ModEnchantments.REACH_ENCHANTMENT.get(), 2, 13, 18, 1));
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 24.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 3, 11, 16, 1));
            list.add(TradePoolEntry.book(FAMILY_ANONYMITY, 14.0D, ModEnchantments.ANONYMITY_ENCHANTMENT.get(), 2, 20, 28, 1));
            list.add(TradePoolEntry.item("crying_obsidian_t4", 4.0D, () -> new ItemStack(Items.CRYING_OBSIDIAN), 6, 10, 2, 4, 3, false));
            list.add(TradePoolEntry.item("netherite_scrap", 4.0D, () -> new ItemStack(Items.NETHERITE_SCRAP), 1, 1, 12, 16, 2, false));
            list.add(TradePoolEntry.item("portable_nether_portal", 14.0D, () -> new ItemStack(ModItems.PORTABLE_NETHER_PORTAL.get()), 1, 1, 11, 15, 1, true));
            list.add(TradePoolEntry.item("podest", 6.0D, () -> new ItemStack(ModItems.PODEST_ITEM.get()), 1, 1, 9, 13, 1, false));
            list.add(TradePoolEntry.item("ancient_table", 6.0D, () -> new ItemStack(ModItems.ANCIENT_TABLE_ITEM.get()), 1, 1, 14, 20, 1, false));
            return list;
        }
        list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 33.33D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 3, 11, 16, 1));
        list.add(TradePoolEntry.book(FAMILY_REACH, 33.33D, ModEnchantments.REACH_ENCHANTMENT.get(), 2, 13, 18, 1));
        list.add(TradePoolEntry.item("experience_bottle", 8.33D, () -> new ItemStack(Items.EXPERIENCE_BOTTLE), 2, 4, 2, 3, 3, false));
        list.add(TradePoolEntry.item("diamond", 8.33D, () -> new ItemStack(Items.DIAMOND), 1, 2, 3, 5, 2, false));
        list.add(TradePoolEntry.item("podest", 5.56D, () -> new ItemStack(ModItems.PODEST_ITEM.get()), 1, 1, 9, 13, 1, false));
        list.add(TradePoolEntry.item("portable_nether_portal", 5.56D, () -> new ItemStack(ModItems.PORTABLE_NETHER_PORTAL.get()), 1, 1, 11, 15, 1, true));
        list.add(TradePoolEntry.item("ancient_table", 5.56D, () -> new ItemStack(ModItems.ANCIENT_TABLE_ITEM.get()), 1, 1, 14, 20, 1, false));
        return list;
    }

    private List<TradePoolEntry> buildStandardTier5Pool() {
        List<TradePoolEntry> list = new ArrayList<>();
        list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 35.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 4, 18, 26, 1));
        list.add(TradePoolEntry.book(FAMILY_ANONYMITY, 35.0D, ModEnchantments.ANONYMITY_ENCHANTMENT.get(), 2, 22, 30, 1));
        list.add(TradePoolEntry.item("echo_shard", 20.0D, () -> new ItemStack(Items.ECHO_SHARD), 1, 2, 12, 18, 2, false));
        list.add(TradePoolEntry.item("eye_armor_trim", 10.0D, () -> new ItemStack(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE), 1, 1, 16, 22, 1, false));
        return list;
    }

    private List<TradePoolEntry> buildVariantTier5BonusPool(String variant) {
        List<TradePoolEntry> list = new ArrayList<>();
        if (VARIANT_UNCOMMON.equals(variant)) {
            list.add(TradePoolEntry.item("observation_crystal_block", 34.0D, () -> new ItemStack(ModItems.OBSERVATION_CRYSTAL_BLOCK_ITEM.get()), 1, 1, 9, 12, 1, false));
            list.add(TradePoolEntry.book(FAMILY_NIGHTSPY, 33.0D, ModEnchantments.NIGHTSPY_ENCHANTMENT.get(), 3, 11, 16, 1));
            list.add(TradePoolEntry.book(FAMILY_REACH, 33.0D, ModEnchantments.REACH_ENCHANTMENT.get(), 2, 13, 18, 1));
            return list;
        }
        if (VARIANT_RARE.equals(variant)) {
            list.add(TradePoolEntry.item("all_seeing_eye_deactivated", 60.0D, AllSeeingEyeItem::createDeactivatedStack, 1, 1, 20, 28, 1, false));
            list.add(TradePoolEntry.item("observation_crystal_block", 20.0D, () -> new ItemStack(ModItems.OBSERVATION_CRYSTAL_BLOCK_ITEM.get()), 1, 1, 9, 12, 1, false));
            list.add(TradePoolEntry.item("netherite_upgrade_template", 20.0D, () -> new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), 1, 1, 12, 16, 1, false));
            return list;
        }
        if (VARIANT_RARE2.equals(variant)) {
            list.add(TradePoolEntry.book(FAMILY_ANONYMITY, 45.0D, ModEnchantments.ANONYMITY_ENCHANTMENT.get(), 2, 22, 30, 1));
            list.add(TradePoolEntry.item("echo_shard_bonus", 35.0D, () -> new ItemStack(Items.ECHO_SHARD), 2, 3, 14, 20, 2, false));
            list.add(TradePoolEntry.item("all_seeing_eye_deactivated", 10.0D, AllSeeingEyeItem::createDeactivatedStack, 1, 1, 20, 28, 1, false));
            list.add(TradePoolEntry.item("nether_star", 10.0D, () -> new ItemStack(Items.NETHER_STAR), 1, 1, 24, 32, 1, false));
            return list;
        }
        return list;
    }

    private List<TradePoolEntry> buildUltraRareTier5RelicPool() {
        List<TradePoolEntry> list = new ArrayList<>();
        list.add(TradePoolEntry.item("all_seeing_eye_activated", 35.0D, AllSeeingEyeItem::createActivatedStack, 1, 1, 36, 48, 1, false));
        list.add(TradePoolEntry.item("creeping_death", 35.0D, () -> new ItemStack(ModItems.CREEPING_DEATH.get()), 1, 1, 42, 54, 1, false));
        list.add(TradePoolEntry.item("echo_shard", 20.0D, () -> new ItemStack(Items.ECHO_SHARD), 1, 2, 12, 18, 2, false));
        list.add(TradePoolEntry.item("eye_armor_trim", 10.0D, () -> new ItemStack(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE), 1, 1, 16, 22, 1, false));
        return list;
    }

    private List<TradePoolEntry> buildUltraRareTier5GodToolPool() {
        List<TradePoolEntry> list = new ArrayList<>();
        list.add(TradePoolEntry.godTool("god_soul_sword", 20.0D, ScholarEntity::createSoulSword));
        list.add(TradePoolEntry.godTool("god_world_eater", 20.0D, ScholarEntity::createWorldEater));
        list.add(TradePoolEntry.godTool("god_deforester", 20.0D, ScholarEntity::createDeforester));
        list.add(TradePoolEntry.godTool("god_chunk_destroyer", 20.0D, ScholarEntity::createChunkDestroyer));
        list.add(TradePoolEntry.godTool("god_deadshot", 10.0D, ScholarEntity::createDeadshot));
        list.add(TradePoolEntry.godTool("god_one_shot", 10.0D, ScholarEntity::createOneShot));
        return list;
    }

    public static ItemStack createSoulSword() {
        ItemStack stack = new ItemStack(Items.NETHERITE_SWORD);
        stack.enchant(Enchantments.SHARPNESS, 5);
        stack.enchant(Enchantments.FIRE_ASPECT, 2);
        stack.enchant(Enchantments.MOB_LOOTING, 3);
        stack.enchant(Enchantments.SWEEPING_EDGE, 3);
        stack.enchant(Enchantments.UNBREAKING, 3);
        stack.enchant(Enchantments.MENDING, 1);
        stack.setHoverName(Component.translatable("item.bodycam.god_tool.soul_sword")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false)));
        return stack;
    }

    public static ItemStack createWorldEater() {
        ItemStack stack = new ItemStack(Items.NETHERITE_SHOVEL);
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        stack.enchant(Enchantments.UNBREAKING, 3);
        stack.enchant(Enchantments.MENDING, 1);
        stack.setHoverName(Component.translatable("item.bodycam.god_tool.world_eater")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false)));
        return stack;
    }

    public static ItemStack createDeforester() {
        ItemStack stack = new ItemStack(Items.NETHERITE_AXE);
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        stack.enchant(Enchantments.SHARPNESS, 5);
        stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
        stack.enchant(Enchantments.UNBREAKING, 3);
        stack.enchant(Enchantments.MENDING, 1);
        stack.setHoverName(Component.translatable("item.bodycam.god_tool.deforester")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false)));
        return stack;
    }

    public static ItemStack createChunkDestroyer() {
        ItemStack stack = new ItemStack(Items.NETHERITE_PICKAXE);
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        stack.enchant(Enchantments.SILK_TOUCH, 1);
        stack.enchant(Enchantments.UNBREAKING, 3);
        stack.enchant(Enchantments.MENDING, 1);
        stack.setHoverName(Component.translatable("item.bodycam.god_tool.chunk_destroyer")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false)));
        return stack;
    }

    public static ItemStack createDeadshot() {
        ItemStack stack = new ItemStack(Items.BOW);
        stack.enchant(Enchantments.POWER_ARROWS, 5);
        stack.enchant(Enchantments.PUNCH_ARROWS, 2);
        stack.enchant(Enchantments.FLAMING_ARROWS, 1);
        stack.enchant(Enchantments.INFINITY_ARROWS, 1);
        stack.enchant(Enchantments.MENDING, 1);
        stack.enchant(Enchantments.UNBREAKING, 3);
        stack.setHoverName(Component.translatable("item.bodycam.god_tool.deadshot")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false)));
        return stack;
    }

    public static ItemStack createOneShot() {
        ItemStack stack = new ItemStack(Items.CROSSBOW);
        stack.enchant(Enchantments.QUICK_CHARGE, 3);
        stack.enchant(Enchantments.PIERCING, 4);
        stack.enchant(Enchantments.MULTISHOT, 1);
        stack.enchant(Enchantments.UNBREAKING, 3);
        stack.enchant(Enchantments.MENDING, 1);
        stack.setHoverName(Component.translatable("item.bodycam.god_tool.one_shot")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false)));
        return stack;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", this.getVariant());
        tag.putBoolean("VariantInitialized", this.variantInitialized);
        tag.putString("TradesVariant", this.tradesGeneratedForVariant);
        tag.putInt("ScholarLevel", this.getScholarLevel());

        ListTag itemKeysTag = new ListTag();
        for (String key : this.usedItemKeys) {
            itemKeysTag.add(StringTag.valueOf(key));
        }
        tag.put("UsedItemKeys", itemKeysTag);

        ListTag familiesTag = new ListTag();
        for (String fam : this.usedEnchantFamilies) {
            familiesTag.add(StringTag.valueOf(fam));
        }
        tag.put("UsedEnchantFamilies", familiesTag);

        if (this.hasRestriction()) {
            BlockPos center = this.getRestrictCenter();
            tag.putInt("HomeX", center.getX());
            tag.putInt("HomeY", center.getY());
            tag.putInt("HomeZ", center.getZ());
            tag.putFloat("HomeRadius", this.getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boolean hadExplicitVariant = tag.contains("Variant", Tag.TAG_STRING);
        if (hadExplicitVariant) {
            this.setVariant(tag.getString("Variant"));
            this.variantInitialized = true;
        } else if (tag.contains("VariantInitialized")) {
            this.variantInitialized = tag.getBoolean("VariantInitialized");
        }

        if (tag.contains("ScholarLevel")) {
            this.setScholarLevel(tag.getInt("ScholarLevel"));
        }

        this.usedItemKeys.clear();
        if (tag.contains("UsedItemKeys", Tag.TAG_LIST)) {
            ListTag list = tag.getList("UsedItemKeys", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                this.usedItemKeys.add(list.getString(i));
            }
        }

        this.usedEnchantFamilies.clear();
        if (tag.contains("UsedEnchantFamilies", Tag.TAG_LIST)) {
            ListTag list = tag.getList("UsedEnchantFamilies", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                this.usedEnchantFamilies.add(list.getString(i));
            }
        }

        if (tag.contains("HomeX") && tag.contains("HomeY") && tag.contains("HomeZ")) {
            BlockPos home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
            int radius = Math.max(16, Math.round(tag.getFloat("HomeRadius")));
            this.restrictTo(home, radius);
        }

        if (tag.contains("TradesVariant", Tag.TAG_STRING)) {
            this.tradesGeneratedForVariant = normalizeVariant(tag.getString("TradesVariant"));
        }

        this.applyVariantArmor();

        if (hadExplicitVariant && (!tag.contains("Offers", Tag.TAG_COMPOUND) || !this.getVariant().equals(this.tradesGeneratedForVariant))) {
            this.offers = new MerchantOffers();
            this.usedItemKeys.clear();
            this.usedEnchantFamilies.clear();
            this.setScholarLevel(1);
            this.tradesGeneratedForVariant = this.getVariant();
            this.unlockTradesForLevel(1);
        }
    }

    public static class TradePoolEntry {
        public final String itemKey;
        public final String enchantFamily;
        public final double weight;
        public final Supplier<ItemStack> resultSupplier;
        public final Supplier<ItemStack> secondBuySupplier;
        public final int minCount;
        public final int maxCount;
        public final int minCost;
        public final int maxCost;
        public final int maxUses;
        public final boolean applyVariantDurability;
        public final boolean ignoreDiscount;

        private TradePoolEntry(String itemKey, String enchantFamily, double weight,
                Supplier<ItemStack> resultSupplier, Supplier<ItemStack> secondBuySupplier,
                int minCount, int maxCount, int minCost, int maxCost, int maxUses,
                boolean applyVariantDurability, boolean ignoreDiscount) {
            this.itemKey = itemKey;
            this.enchantFamily = enchantFamily;
            this.weight = weight;
            this.resultSupplier = resultSupplier;
            this.secondBuySupplier = secondBuySupplier;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.minCost = minCost;
            this.maxCost = maxCost;
            this.maxUses = maxUses;
            this.applyVariantDurability = applyVariantDurability;
            this.ignoreDiscount = ignoreDiscount;
        }

        public boolean isBook() {
            return this.enchantFamily != null && !this.enchantFamily.isEmpty();
        }

        public static TradePoolEntry item(String key, double weight, Supplier<ItemStack> supplier,
                int minCount, int maxCount, int minCost, int maxCost, int maxUses, boolean applyVariantDurability) {
            return new TradePoolEntry(key, "", weight, supplier, null,
                    minCount, maxCount, minCost, maxCost, maxUses, applyVariantDurability, false);
        }

        public static TradePoolEntry book(String family, double weight, Enchantment enchantment, int level,
                int minCost, int maxCost, int maxUses) {
            return new TradePoolEntry(
                    "book_" + family + "_" + level,
                    family,
                    weight,
                    () -> EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)),
                    () -> new ItemStack(Items.BOOK, 1),
                    1, 1, minCost, maxCost, maxUses, false, false
            );
        }

        public static TradePoolEntry godTool(String key, double weight, Supplier<ItemStack> supplier) {
            return new TradePoolEntry(key, "", weight, supplier, null,
                    1, 1, 32, 32, 1, false, true);
        }
    }
}
