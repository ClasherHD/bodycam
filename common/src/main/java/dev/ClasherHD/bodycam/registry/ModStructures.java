package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.worldgen.structure.CherryHousePiece;
import dev.ClasherHD.bodycam.worldgen.structure.CherryHouseStructure;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(bodycam.MODID, Registries.STRUCTURE_TYPE);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(bodycam.MODID, Registries.STRUCTURE_PIECE);

    public static final RegistrySupplier<StructureType<CherryHouseStructure>> CHERRY_HOUSE =
            STRUCTURE_TYPES.register("cherry_house", () -> () -> CherryHouseStructure.CODEC);

    public static final RegistrySupplier<StructurePieceType> CHERRY_HOUSE_PIECE =
            PIECE_TYPES.register("cherry_house_piece", () -> CherryHousePiece::new);

    public static void register() {
        STRUCTURE_TYPES.register();
        PIECE_TYPES.register();
    }
}
