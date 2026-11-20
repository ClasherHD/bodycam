package dev.ClasherHD.bodycam.client.render.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.cache.ClientBodycamCache;
import dev.ClasherHD.bodycam.client.cache.ClientLocatorCache;
import dev.ClasherHD.bodycam.item.AllSeeingEyeItem;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.item.LogBookItem;
import dev.ClasherHD.bodycam.item.LoggerItem;
import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import dev.ClasherHD.bodycam.mixin.ConditionalItemModelPropertiesAccessor;
import dev.ClasherHD.bodycam.mixin.RangeSelectItemModelPropertiesAccessor;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.NeedleDirectionHelper;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@SuppressWarnings("null")
public final class BodycamItemModelProperties {

    private BodycamItemModelProperties() {
    }

    public static void registerDirect() {
        registerConditional(ConditionalItemModelPropertiesAccessor.bodycam$getIdMapper()::put);
        registerRangeSelect(RangeSelectItemModelPropertiesAccessor.bodycam$getIdMapper()::put);
    }

    public static void registerConditional(
            BiConsumer<Identifier, MapCodec<? extends ConditionalItemModelProperty>> registrar
    ) {
        for (SimpleCondition condition : SimpleCondition.values()) {
            registrar.accept(condition.id, condition.codec);
        }
    }

    public static void registerRangeSelect(
            BiConsumer<Identifier, MapCodec<? extends RangeSelectItemModelProperty>> registrar
    ) {
        registrar.accept(JammerModeProperty.ID, JammerModeProperty.MAP_CODEC);
        registrar.accept(PlayerLocatorStateProperty.ID, PlayerLocatorStateProperty.MAP_CODEC);
        registrar.accept(PlayerLocatorCompassAngleProperty.ID, PlayerLocatorCompassAngleProperty.MAP_CODEC);
    }

    public enum SimpleCondition implements ConditionalItemModelProperty {
        EMPTY("empty", ItemDurabilityHelper::isEmpty),
        ACTIVE("active", BodycamItemModelProperties::isItemActive),
        PAGES_EMPTY("pages_empty", stack -> LogBookItem.pageCount(stack) <= 0),
        LOCKED("locked", LogBookItem::hasPin),
        CHARGED("charged", stack -> PortableNetherPortalItem.getCharges(stack) > 0),
        DEACTIVATED("deactivated", stack -> !AllSeeingEyeItem.isActivated(stack));

        private final Identifier id;
        private final MapCodec<SimpleCondition> codec;
        private final Predicate<ItemStack> predicate;

        SimpleCondition(String path, Predicate<ItemStack> predicate) {
            this.id = bodycam.id(path);
            this.predicate = predicate;
            this.codec = MapCodec.unit(this);
        }

        @Override
        public boolean get(
                ItemStack stack,
                @Nullable ClientLevel level,
                @Nullable LivingEntity entity,
                int seed,
                ItemDisplayContext displayContext
        ) {
            return this.predicate.test(stack);
        }

        @Override
        public MapCodec<SimpleCondition> type() {
            return this.codec;
        }
    }

    private static boolean isItemActive(ItemStack stack) {
        if (stack.getItem() instanceof JammerItem) {
            return !ItemDurabilityHelper.isEmpty(stack) && JammerItem.getMode(stack) > 0;
        }
        if (stack.getItem() instanceof AnonymizerItem) {
            return !ItemDurabilityHelper.isEmpty(stack) && ItemNbtAccess.getBoolean(stack, "AnonymizerActive");
        }
        if (stack.getItem() instanceof LoggerItem) {
            return LoggerItem.isActive(stack);
        }
        return false;
    }

    public static final class JammerModeProperty implements RangeSelectItemModelProperty {
        public static final Identifier ID = bodycam.id("mode");
        public static final JammerModeProperty INSTANCE = new JammerModeProperty();
        public static final MapCodec<JammerModeProperty> MAP_CODEC = MapCodec.unit(INSTANCE);

        private JammerModeProperty() {
        }

        @Override
        public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
            return JammerItem.getMode(stack);
        }

        @Override
        public MapCodec<JammerModeProperty> type() {
            return MAP_CODEC;
        }
    }

    public static final class PlayerLocatorStateProperty implements RangeSelectItemModelProperty {
        public static final Identifier ID = bodycam.id("state");
        public static final PlayerLocatorStateProperty INSTANCE = new PlayerLocatorStateProperty();
        public static final MapCodec<PlayerLocatorStateProperty> MAP_CODEC = MapCodec.unit(INSTANCE);

        private PlayerLocatorStateProperty() {
        }

        @Override
        public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
            if (ItemDurabilityHelper.isEmpty(stack)) {
                return 0.0F;
            }
            CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
            return tag != null ? tag.getIntOr("LocatorState", 0) : 0.0F;
        }

        @Override
        public MapCodec<PlayerLocatorStateProperty> type() {
            return MAP_CODEC;
        }
    }

    public static final class PlayerLocatorCompassAngleProperty extends NeedleDirectionHelper
            implements RangeSelectItemModelProperty {
        public static final Identifier ID = bodycam.id("angle");
        public static final MapCodec<PlayerLocatorCompassAngleProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Codec.BOOL.optionalFieldOf("wobble", true).forGetter(PlayerLocatorCompassAngleProperty::isWobble)
                ).apply(instance, PlayerLocatorCompassAngleProperty::new)
        );

        private final NeedleDirectionHelper.Wobbler wobbler;
        private final NeedleDirectionHelper.Wobbler noTargetWobbler;
        private final RandomSource random = RandomSource.create();

        public PlayerLocatorCompassAngleProperty(boolean wobble) {
            super(wobble);
            this.wobbler = this.newWobbler(0.8F);
            this.noTargetWobbler = this.newWobbler(0.8F);
        }

        private boolean isWobble() {
            return this.wobble();
        }

        @Override
        public MapCodec<PlayerLocatorCompassAngleProperty> type() {
            return MAP_CODEC;
        }

        @Override
        protected float calculate(ItemStack stack, ClientLevel level, int seed, ItemOwner owner) {
            GlobalPos targetPos = resolveTargetPos(stack, level);
            long gameTime = level.getGameTime();
            if (!isValidCompassTargetPos(owner, targetPos)) {
                return getRandomlySpinningRotation(seed, gameTime);
            }
            return getRotationTowardsTarget(owner, gameTime, targetPos.pos());
        }

        @Nullable
        private static GlobalPos resolveTargetPos(ItemStack stack, ClientLevel level) {
            CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
            if (tag == null) {
                return null;
            }
            int state = tag.getIntOr("LocatorState", 0);
            if (state == 1) {
                if (!PersistentDataAccess.hasUUID(tag, "LocatorTargetUUID")) {
                    return null;
                }
                UUID targetId = PersistentDataAccess.getUUID(tag, "LocatorTargetUUID");
                BlockPos pos = ClientBodycamCache.positions.get(targetId);
                String dimStr = ClientBodycamCache.dimensions.get(targetId);
                if (pos == null || dimStr == null || dimStr.isEmpty()) {
                    return null;
                }
                Identifier dimId = Identifier.tryParse(dimStr);
                if (dimId == null) {
                    return null;
                }
                return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimId), pos);
            }
            if (state == 3) {
                if (ClientLocatorCache.structureTarget != null && ClientLocatorCache.structureTargetDimension != null) {
                    return GlobalPos.of(ClientLocatorCache.structureTargetDimension, ClientLocatorCache.structureTarget);
                }
                if (tag.contains("LocatorCachedStructX") && tag.contains("LocatorCachedStructZ")) {
                    int x = tag.getIntOr("LocatorCachedStructX", 0);
                    int y = tag.getIntOr("LocatorCachedPortalY", 0);
                    int z = tag.getIntOr("LocatorCachedStructZ", 0);
                    String dimStr = tag.getStringOr("LocatorLastMyDim", "");
                    Identifier dimId = dimStr.isEmpty() ? null : Identifier.tryParse(dimStr);
                    ResourceKey<Level> dimKey = dimId != null ? ResourceKey.create(Registries.DIMENSION, dimId) : level.dimension();
                    return GlobalPos.of(dimKey, new BlockPos(x, y, z));
                }
            }
            return null;
        }

        private float getRandomlySpinningRotation(int seed, long gameTime) {
            if (this.noTargetWobbler.shouldUpdate(gameTime)) {
                this.noTargetWobbler.update(gameTime, this.random.nextFloat());
            }
            float angle = this.noTargetWobbler.rotation() + (hash(seed) / 2.1474836E9F);
            return Mth.positiveModulo(angle, 1.0F);
        }

        private float getRotationTowardsTarget(ItemOwner owner, long gameTime, BlockPos target) {
            float targetAngle = (float) getAngleFromEntityToPos(owner, target);
            float visualYaw = Mth.positiveModulo(owner.getVisualRotationYInDegrees() / 360.0F, 1.0F);
            float result;
            if (owner.asLivingEntity() instanceof Player player
                    && player.isLocalPlayer()
                    && player.level().tickRateManager().runsNormally()) {
                if (this.wobbler.shouldUpdate(gameTime)) {
                    this.wobbler.update(gameTime, 0.5F - (visualYaw - 0.25F));
                }
                result = targetAngle + this.wobbler.rotation();
            } else {
                result = 0.5F - (visualYaw - 0.25F) - targetAngle;
            }
            return Mth.positiveModulo(result, 1.0F);
        }

        private static boolean isValidCompassTargetPos(ItemOwner owner, @Nullable GlobalPos target) {
            return target != null
                    && target.dimension() == owner.level().dimension()
                    && target.pos().distToCenterSqr(owner.position()) >= 1.0E-5D;
        }

        private static double getAngleFromEntityToPos(ItemOwner owner, BlockPos pos) {
            Vec3 center = Vec3.atCenterOf(pos);
            Vec3 ownerPos = owner.position();
            return Math.atan2(center.z() - ownerPos.z(), center.x() - ownerPos.x()) / (Math.PI * 2.0D);
        }

        private static int hash(int value) {
            return value * 1327217883;
        }
    }
}
