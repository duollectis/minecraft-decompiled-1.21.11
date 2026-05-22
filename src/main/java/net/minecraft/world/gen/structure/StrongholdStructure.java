package net.minecraft.world.gen.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.structure.StrongholdGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesCollector;

import java.util.List;
import java.util.Optional;

/**
 * Структура крепости. Генерирует лабиринт комнат с порталом в Энд.
 * Повторяет генерацию до тех пор, пока не будет создана комната с порталом.
 */
public class StrongholdStructure extends Structure {

	public static final MapCodec<StrongholdStructure> CODEC = createCodec(StrongholdStructure::new);

	public StrongholdStructure(Structure.Config config) {
		super(config);
	}

	@Override
	public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		return Optional.of(
				new Structure.StructurePosition(
						context.chunkPos().getStartPos(),
						collector -> addPieces(collector, context)
				)
		);
	}

	private static void addPieces(StructurePiecesCollector collector, Structure.Context context) {
		int attempt = 0;

		StrongholdGenerator.Start start;
		do {
			collector.clear();
			context.random().setCarverSeed(context.seed() + attempt++, context.chunkPos().x, context.chunkPos().z);
			StrongholdGenerator.init();
			start = new StrongholdGenerator.Start(
					context.random(),
					context.chunkPos().getOffsetX(2),
					context.chunkPos().getOffsetZ(2)
			);
			collector.addPiece(start);
			start.fillOpenings(start, collector, context.random());
			List<StructurePiece> pendingPieces = start.pieces;

			while (!pendingPieces.isEmpty()) {
				int randomIndex = context.random().nextInt(pendingPieces.size());
				StructurePiece piece = pendingPieces.remove(randomIndex);
				piece.fillOpenings(start, collector, context.random());
			}

			collector.shiftInto(
					context.chunkGenerator().getSeaLevel(),
					context.chunkGenerator().getMinimumY(),
					context.random(),
					10
			);
		} while (collector.isEmpty() || start.portalRoom == null);
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.STRONGHOLD;
	}
}
