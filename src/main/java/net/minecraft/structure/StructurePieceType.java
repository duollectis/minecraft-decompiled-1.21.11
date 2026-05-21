package net.minecraft.structure;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.Locale;

/**
 * {@code StructurePieceType}.
 */
public interface StructurePieceType {

	StructurePieceType MINESHAFT_CORRIDOR = register((Simple) MineshaftGenerator.MineshaftCorridor::new, "MSCorridor");

	StructurePieceType MINESHAFT_CROSSING = register((Simple) MineshaftGenerator.MineshaftCrossing::new, "MSCrossing");

	StructurePieceType MINESHAFT_ROOM = register((Simple) MineshaftGenerator.MineshaftRoom::new, "MSRoom");

	StructurePieceType MINESHAFT_STAIRS = register((Simple) MineshaftGenerator.MineshaftStairs::new, "MSStairs");

	StructurePieceType
			NETHER_FORTRESS_BRIDGE_CROSSING =
			register((Simple) NetherFortressGenerator.BridgeCrossing::new, "NeBCr");

	StructurePieceType NETHER_FORTRESS_BRIDGE_END = register((Simple) NetherFortressGenerator.BridgeEnd::new, "NeBEF");

	StructurePieceType NETHER_FORTRESS_BRIDGE = register((Simple) NetherFortressGenerator.Bridge::new, "NeBS");

	StructurePieceType
			NETHER_FORTRESS_CORRIDOR_STAIRS =
			register((Simple) NetherFortressGenerator.CorridorStairs::new, "NeCCS");

	StructurePieceType
			NETHER_FORTRESS_CORRIDOR_BALCONY =
			register((Simple) NetherFortressGenerator.CorridorBalcony::new, "NeCTB");

	StructurePieceType
			NETHER_FORTRESS_CORRIDOR_EXIT =
			register((Simple) NetherFortressGenerator.CorridorExit::new, "NeCE");

	StructurePieceType
			NETHER_FORTRESS_CORRIDOR_CROSSING =
			register((Simple) NetherFortressGenerator.CorridorCrossing::new, "NeSCSC");

	StructurePieceType
			NETHER_FORTRESS_CORRIDOR_LEFT_TURN =
			register((Simple) NetherFortressGenerator.CorridorLeftTurn::new, "NeSCLT");

	StructurePieceType
			NETHER_FORTRESS_SMALL_CORRIDOR =
			register((Simple) NetherFortressGenerator.SmallCorridor::new, "NeSC");

	StructurePieceType
			NETHER_FORTRESS_CORRIDOR_RIGHT_TURN =
			register((Simple) NetherFortressGenerator.CorridorRightTurn::new, "NeSCRT");

	StructurePieceType
			NETHER_FORTRESS_CORRIDOR_NETHER_WARTS_ROOM =
			register((Simple) NetherFortressGenerator.CorridorNetherWartsRoom::new, "NeCSR");

	StructurePieceType
			NETHER_FORTRESS_BRIDGE_PLATFORM =
			register((Simple) NetherFortressGenerator.BridgePlatform::new, "NeMT");

	StructurePieceType
			NETHER_FORTRESS_BRIDGE_SMALL_CROSSING =
			register((Simple) NetherFortressGenerator.BridgeSmallCrossing::new, "NeRC");

	StructurePieceType
			NETHER_FORTRESS_BRIDGE_STAIRS =
			register((Simple) NetherFortressGenerator.BridgeStairs::new, "NeSR");

	StructurePieceType NETHER_FORTRESS_START = register((Simple) NetherFortressGenerator.Start::new, "NeStart");

	StructurePieceType STRONGHOLD_CHEST_CORRIDOR = register((Simple) StrongholdGenerator.ChestCorridor::new, "SHCC");

	StructurePieceType STRONGHOLD_SMALL_CORRIDOR = register((Simple) StrongholdGenerator.SmallCorridor::new, "SHFC");

	StructurePieceType
			STRONGHOLD_FIVE_WAY_CROSSING =
			register((Simple) StrongholdGenerator.FiveWayCrossing::new, "SH5C");

	StructurePieceType STRONGHOLD_LEFT_TURN = register((Simple) StrongholdGenerator.LeftTurn::new, "SHLT");

	StructurePieceType STRONGHOLD_LIBRARY = register((Simple) StrongholdGenerator.Library::new, "SHLi");

	StructurePieceType STRONGHOLD_PORTAL_ROOM = register((Simple) StrongholdGenerator.PortalRoom::new, "SHPR");

	StructurePieceType STRONGHOLD_PRISON_HALL = register((Simple) StrongholdGenerator.PrisonHall::new, "SHPH");

	StructurePieceType STRONGHOLD_RIGHT_TURN = register((Simple) StrongholdGenerator.RightTurn::new, "SHRT");

	StructurePieceType STRONGHOLD_SQUARE_ROOM = register((Simple) StrongholdGenerator.SquareRoom::new, "SHRC");

	StructurePieceType
			STRONGHOLD_SPIRAL_STAIRCASE =
			register((Simple) StrongholdGenerator.SpiralStaircase::new, "SHSD");

	StructurePieceType STRONGHOLD_START = register((Simple) StrongholdGenerator.Start::new, "SHStart");

	StructurePieceType STRONGHOLD_CORRIDOR = register((Simple) StrongholdGenerator.Corridor::new, "SHS");

	StructurePieceType STRONGHOLD_STAIRS = register((Simple) StrongholdGenerator.Stairs::new, "SHSSD");

	StructurePieceType JUNGLE_TEMPLE = register((Simple) JungleTempleGenerator::new, "TeJP");

	StructurePieceType OCEAN_TEMPLE = register((ManagerAware) OceanRuinGenerator.Piece::fromNbt, "ORP");

	StructurePieceType IGLOO = register((ManagerAware) IglooGenerator.Piece::new, "Iglu");

	StructurePieceType RUINED_PORTAL = register((ManagerAware) RuinedPortalStructurePiece::new, "RUPO");

	StructurePieceType SWAMP_HUT = register((Simple) SwampHutGenerator::new, "TeSH");

	StructurePieceType DESERT_TEMPLE = register((Simple) DesertTempleGenerator::new, "TeDP");

	StructurePieceType OCEAN_MONUMENT_BASE = register((Simple) OceanMonumentGenerator.Base::new, "OMB");

	StructurePieceType OCEAN_MONUMENT_CORE_ROOM = register((Simple) OceanMonumentGenerator.CoreRoom::new, "OMCR");

	StructurePieceType
			OCEAN_MONUMENT_DOUBLE_X_ROOM =
			register((Simple) OceanMonumentGenerator.DoubleXRoom::new, "OMDXR");

	StructurePieceType
			OCEAN_MONUMENT_DOUBLE_X_Y_ROOM =
			register((Simple) OceanMonumentGenerator.DoubleXYRoom::new, "OMDXYR");

	StructurePieceType
			OCEAN_MONUMENT_DOUBLE_Y_ROOM =
			register((Simple) OceanMonumentGenerator.DoubleYRoom::new, "OMDYR");

	StructurePieceType
			OCEAN_MONUMENT_DOUBLE_Y_Z_ROOM =
			register((Simple) OceanMonumentGenerator.DoubleYZRoom::new, "OMDYZR");

	StructurePieceType
			OCEAN_MONUMENT_DOUBLE_Z_ROOM =
			register((Simple) OceanMonumentGenerator.DoubleZRoom::new, "OMDZR");

	StructurePieceType OCEAN_MONUMENT_ENTRY_ROOM = register((Simple) OceanMonumentGenerator.Entry::new, "OMEntry");

	StructurePieceType
			OCEAN_MONUMENT_PENTHOUSE =
			register((Simple) OceanMonumentGenerator.Penthouse::new, "OMPenthouse");

	StructurePieceType
			OCEAN_MONUMENT_SIMPLE_ROOM =
			register((Simple) OceanMonumentGenerator.SimpleRoom::new, "OMSimple");

	StructurePieceType
			OCEAN_MONUMENT_SIMPLE_TOP_ROOM =
			register((Simple) OceanMonumentGenerator.SimpleRoomTop::new, "OMSimpleT");

	StructurePieceType OCEAN_MONUMENT_WING_ROOM = register((Simple) OceanMonumentGenerator.WingRoom::new, "OMWR");

	StructurePieceType END_CITY = register((ManagerAware) EndCityGenerator.Piece::new, "ECP");

	StructurePieceType WOODLAND_MANSION = register((ManagerAware) WoodlandMansionGenerator.Piece::new, "WMP");

	StructurePieceType BURIED_TREASURE = register((Simple) BuriedTreasureGenerator.Piece::new, "BTP");

	StructurePieceType SHIPWRECK = register((ManagerAware) ShipwreckGenerator.Piece::new, "Shipwreck");

	StructurePieceType NETHER_FOSSIL = register((ManagerAware) NetherFossilGenerator.Piece::new, "NeFos");

	StructurePieceType JIGSAW = register((StructurePieceType) PoolStructurePiece::new, "jigsaw");

	StructurePiece load(StructureContext context, NbtCompound nbt);

	private static StructurePieceType register(StructurePieceType type, String id) {
		return Registry.register(Registries.STRUCTURE_PIECE, id.toLowerCase(Locale.ROOT), type);
	}

	private static StructurePieceType register(StructurePieceType.Simple type, String id) {
		return register((StructurePieceType) type, id);
	}

	private static StructurePieceType register(StructurePieceType.ManagerAware type, String id) {
		return register((StructurePieceType) type, id);
	}

	/**
	 * {@code ManagerAware}.
	 */
	public interface ManagerAware extends StructurePieceType {

		StructurePiece load(StructureTemplateManager structureTemplateManager, NbtCompound nbt);

		@Override
		default StructurePiece load(StructureContext structureContext, NbtCompound nbtCompound) {
			return this.load(structureContext.structureTemplateManager(), nbtCompound);
		}
	}

	/**
	 * {@code Simple}.
	 */
	public interface Simple extends StructurePieceType {

		StructurePiece load(NbtCompound nbt);

		@Override
		default StructurePiece load(StructureContext structureContext, NbtCompound nbtCompound) {
			return this.load(nbtCompound);
		}
	}
}
