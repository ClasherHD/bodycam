package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class ScholarEntityTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    private ScholarEntity createHeadlessScholar() {
        ScholarEntity scholar = MinecraftTestEnvironment.allocateInstance(ScholarEntity.class);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(true);
        SynchedEntityData entityData = mock(SynchedEntityData.class);
        when(entityData.get(any())).thenAnswer(inv -> {
            EntityDataAccessor<?> accessor = inv.getArgument(0);
            if (accessor == ScholarEntity.DATA_VARIANT) {
                return ScholarEntity.VARIANT_COMMON;
            }
            if (accessor == ScholarEntity.DATA_SCHOLAR_LEVEL) {
                return 1;
            }
            return (byte) 0;
        });

        MinecraftTestEnvironment.setField(scholar, "level", level);
        MinecraftTestEnvironment.setField(scholar, "entityData", entityData);
        MinecraftTestEnvironment.setField(scholar, "inventory", new SimpleContainer(8));
        MinecraftTestEnvironment.setField(scholar, "handItems", NonNullList.withSize(2, ItemStack.EMPTY));
        MinecraftTestEnvironment.setField(scholar, "armorItems", NonNullList.withSize(4, ItemStack.EMPTY));
        MinecraftTestEnvironment.setField(scholar, "handDropChances", new float[]{0.085F, 0.085F});
        MinecraftTestEnvironment.setField(scholar, "armorDropChances", new float[]{0.085F, 0.085F, 0.085F, 0.085F});
        MinecraftTestEnvironment.setField(scholar, "usedItemKeys", new HashSet<>());
        MinecraftTestEnvironment.setField(scholar, "usedEnchantFamilies", new HashSet<>());
        MinecraftTestEnvironment.setField(scholar, "restrictCenter", BlockPos.ZERO);
        return scholar;
    }

    @Test
    @DisplayName("readAdditionalSaveData clamps HomeRadius and guards against NaN and Infinity")
    void readAdditionalSaveData_clampsAndGuardsHomeRadius() {
        ScholarEntity scholar = createHeadlessScholar();

        CompoundTag infTag = new CompoundTag();
        infTag.putInt("HomeX", 10);
        infTag.putInt("HomeY", 64);
        infTag.putInt("HomeZ", -10);
        infTag.putFloat("HomeRadius", Float.POSITIVE_INFINITY);
        scholar.readAdditionalSaveData(infTag);
        assertThat(scholar.getRestrictCenter()).isEqualTo(new BlockPos(10, 64, -10));
        assertThat(scholar.getRestrictRadius()).isEqualTo(16.0F);

        CompoundTag nanTag = infTag.copy();
        nanTag.putFloat("HomeRadius", Float.NaN);
        scholar.readAdditionalSaveData(nanTag);
        assertThat(scholar.getRestrictRadius()).isEqualTo(16.0F);

        CompoundTag tooHighTag = infTag.copy();
        tooHighTag.putFloat("HomeRadius", 10000.0F);
        scholar.readAdditionalSaveData(tooHighTag);
        assertThat(scholar.getRestrictRadius()).isEqualTo(256.0F);

        CompoundTag tooLowTag = infTag.copy();
        tooLowTag.putFloat("HomeRadius", 4.0F);
        scholar.readAdditionalSaveData(tooLowTag);
        assertThat(scholar.getRestrictRadius()).isEqualTo(16.0F);

        CompoundTag validTag = infTag.copy();
        validTag.putFloat("HomeRadius", 48.0F);
        scholar.readAdditionalSaveData(validTag);
        assertThat(scholar.getRestrictRadius()).isEqualTo(48.0F);
    }
}
