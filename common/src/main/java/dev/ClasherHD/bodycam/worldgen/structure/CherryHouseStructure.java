package dev.ClasherHD.bodycam.worldgen.structure;

import com.mojang.serialization.MapCodec;
import dev.ClasherHD.bodycam.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import java.util.Optional;

@SuppressWarnings("null")
public class CherryHouseStructure extends Structure {

    public static final MapCodec<CherryHouseStructure> CODEC = simpleCodec(CherryHouseStructure::new);

    public CherryHouseStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        Rotation rotation = Rotation.getRandom(context.random());

        int hCenter = sampleSurface(context, centerX, centerZ);
        int h1 = sampleSurface(context, centerX - 11, centerZ - 9);
        int h2 = sampleSurface(context, centerX + 11, centerZ - 9);
        int h3 = sampleSurface(context, centerX - 11, centerZ + 9);
        int h4 = sampleSurface(context, centerX + 11, centerZ + 9);

        int minY = Math.min(hCenter, Math.min(Math.min(h1, h2), Math.min(h3, h4)));
        int maxY = Math.max(hCenter, Math.max(Math.max(h1, h2), Math.max(h3, h4)));

        if (minY < context.chunkGenerator().getSeaLevel() || (maxY - minY) > 10) {
            return Optional.empty();
        }

        int avgY = (hCenter + h1 + h2 + h3 + h4) / 5;
        int baseY = avgY - 2;
        BlockPos startPos = new BlockPos(centerX - CherryHousePiece.ROTATION_PIVOT.getX(),
                baseY, centerZ - CherryHousePiece.ROTATION_PIVOT.getZ());

        return Optional.of(new GenerationStub(new BlockPos(centerX, avgY, centerZ), builder ->
                builder.addPiece(new CherryHousePiece(context.structureTemplateManager(), startPos, rotation))));
    }

    private static int sampleSurface(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
            RandomSource random, BoundingBox box, ChunkPos chunkPos, PiecesContainer pieces) {
        BoundingBox structBox = pieces.calculateBoundingBox();
        int baseFloorY = structBox.minY();
        int minBuildY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int minX = Math.max(box.minX(), structBox.minX());
        int maxX = Math.min(box.maxX(), structBox.maxX());
        int minZ = Math.max(box.minZ(), structBox.minZ());
        int maxZ = Math.min(box.maxZ(), structBox.maxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                cursor.set(x, baseFloorY, z);
                BlockState baseState = level.getBlockState(cursor);
                if (baseState.isAir() || !baseState.getFluidState().isEmpty()) {
                    continue;
                }
                for (int y = baseFloorY - 1; y >= minBuildY; y--) {
                    cursor.set(x, y, z);
                    BlockState existing = level.getBlockState(cursor);
                    if (!existing.isAir() && existing.getFluidState().isEmpty() && !existing.canBeReplaced()) {
                        break;
                    }
                    int depth = baseFloorY - y;
                    level.setBlock(cursor, depth <= 2 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CHERRY_HOUSE.get();
    }
}
