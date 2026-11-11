package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.worldgen.structure.CherryHousePiece;
import dev.ClasherHD.bodycam.worldgen.structure.CherryHouseStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, bodycam.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, bodycam.MODID);

    public static final RegistryObject<StructureType<CherryHouseStructure>> CHERRY_HOUSE_TYPE =
            STRUCTURE_TYPES.register("cherry_house", () -> () -> CherryHouseStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CHERRY_HOUSE_PIECE =
            STRUCTURE_PIECES.register("cherry_house_piece",
                    () -> (StructurePieceType.StructureTemplateType) CherryHousePiece::new);
}
