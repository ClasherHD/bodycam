package dev.ClasherHD.bodycam.worldgen.structure;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.entity.ScholarEntity;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

@SuppressWarnings("null")
public class CherryHousePiece extends TemplateStructurePiece {

    public static final ResourceLocation TEMPLATE_ID = new ResourceLocation(bodycam.MODID, "cherry_house");
    public static final BlockPos ROTATION_PIVOT = new BlockPos(13, 0, 11);
    public static final BlockPos SCHOLAR_LOCAL_POS = new BlockPos(13, 3, 7);

    private static final ResourceLocation LOOT_COMMON = new ResourceLocation(bodycam.MODID, "chests/cherry_house_common");
    private static final ResourceLocation LOOT_UNCOMMON = new ResourceLocation(bodycam.MODID, "chests/cherry_house_uncommon");
    private static final ResourceLocation LOOT_RARE = new ResourceLocation(bodycam.MODID, "chests/cherry_house_rare");
    private static final ResourceLocation LOOT_ULTRA_RARE = new ResourceLocation(bodycam.MODID, "chests/cherry_house_ultra_rare");

    private boolean scholarSpawned = false;

    public CherryHousePiece(StructureTemplateManager templateManager, BlockPos pos, Rotation rotation) {
        super(ModStructures.CHERRY_HOUSE_PIECE.get(), 0, templateManager, TEMPLATE_ID,
                TEMPLATE_ID.toString(), makeSettings(rotation), pos);
    }

    public CherryHousePiece(StructureTemplateManager templateManager, CompoundTag tag) {
        super(ModStructures.CHERRY_HOUSE_PIECE.get(), tag, templateManager,
                id -> makeSettings(Rotation.valueOf(tag.getString("Rot"))));
        this.scholarSpawned = tag.getBoolean("ScholarSpawned");
    }

    private static StructurePlaceSettings makeSettings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(ROTATION_PIVOT)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", this.placeSettings.getRotation().name());
        tag.putBoolean("ScholarSpawned", this.scholarSpawned);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pivot);
        if (!this.scholarSpawned) {
            BlockPos worldSpawnPos = StructureTemplate.transform(
                    SCHOLAR_LOCAL_POS,
                    this.placeSettings.getMirror(),
                    this.placeSettings.getRotation(),
                    this.placeSettings.getRotationPivot()
            ).offset(this.templatePosition);

            if (box.isInside(worldSpawnPos)) {
                this.scholarSpawned = true;
                ScholarEntity scholar = ModEntityTypes.SCHOLAR.get().create(level.getLevel());
                if (scholar != null) {
                    scholar.moveTo(
                            worldSpawnPos.getX() + 0.5D,
                            worldSpawnPos.getY(),
                            worldSpawnPos.getZ() + 0.5D,
                            random.nextFloat() * 360.0F,
                            0.0F
                    );
                    scholar.finalizeSpawn(
                            level,
                            level.getCurrentDifficultyAt(worldSpawnPos),
                            MobSpawnType.STRUCTURE,
                            null,
                            null
                    );
                    scholar.restrictTo(worldSpawnPos, 24);
                    scholar.setPersistenceRequired();
                    level.addFreshEntityWithPassengers(scholar);
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        boolean isChest = "chest_common".equals(name) || "chest_uncommon".equals(name) || "chest_rare".equals(name);
        if (!isChest) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        ResourceLocation lootTable = null;
        switch (name) {
            case "chest_common" -> {
                if (random.nextFloat() >= 0.10F) {
                    lootTable = LOOT_COMMON;
                }
            }
            case "chest_uncommon" -> {
                if (random.nextFloat() >= 0.05F) {
                    lootTable = LOOT_UNCOMMON;
                }
            }
            case "chest_rare" -> {
                int rareRoll = random.nextInt(100_000);
                if (rareRoll == 0) {
                    lootTable = LOOT_ULTRA_RARE;
                } else if (rareRoll > 1_000) {
                    lootTable = LOOT_RARE;
                }
            }
        }

        Direction facing = determineChestFacing(level, pos);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 3);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (lootTable != null) {
            if (blockEntity instanceof RandomizableContainerBlockEntity container) {
                container.setLootTable(lootTable, random.nextLong());
                if (LOOT_ULTRA_RARE.equals(lootTable)) {
                    container.getPersistentData().putBoolean("BodycamUltraRareCherryChest", true);
                    container.setChanged();
                }
            }
        } else if (blockEntity != null) {
            blockEntity.getPersistentData().putBoolean("BodycamEmptyCherryChest", true);
            blockEntity.setChanged();
        }
    }

    private Direction determineChestFacing(ServerLevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos front = pos.relative(dir);
            BlockPos back = pos.relative(dir.getOpposite());
            if (isOpenSpace(level, front) && hasCollider(level, back)) {
                return dir;
            }
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (isOpenSpace(level, pos.relative(dir))) {
                return dir;
            }
        }
        return this.placeSettings.getRotation().rotate(Direction.NORTH);
    }

    private boolean isOpenSpace(ServerLevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
    }

    private boolean hasCollider(ServerLevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }
}
