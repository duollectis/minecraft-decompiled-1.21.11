package net.minecraft.world.gen.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.structure.StrongholdGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesCollector;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * {@code StrongholdStructure}.
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
						(Consumer<StructurePiecesCollector>) (collector -> addPieces(collector, context))
				)
		);
	}

	private static void addPieces(StructurePiecesCollector collector, Structure.Context context) {
		int i = 0;

		StrongholdGenerator.Start start;
		do {
			collector.clear();
			context.random().setCarverSeed(context.seed() + i++, context.chunkPos().x, context.chunkPos().z);
			StrongholdGenerator.init();
			start =
					new StrongholdGenerator.Start(
							context.random(),
							context.chunkPos().getOffsetX(2),
							context.chunkPos().getOffsetZ(2)
					);
			collector.addPiece(start);
			start.fillOpenings(start, collector, context.random());
			List<StructurePiece> list = start.pieces;

			while (!list.isEmpty()) {
				int j = context.random().nextInt(list.size());
				StructurePiece structurePiece = list.remove(j);
				structurePiece.fillOpenings(start, collector, context.random());
			}

			collector.shiftInto(
					context.chunkGenerator().getSeaLevel(),
					context.chunkGenerator().getMinimumY(),
					context.random(),
					10
			);
		}
		while (collector.isEmpty() || start.portalRoom == null);
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.STRONGHOLD;
	}
}
