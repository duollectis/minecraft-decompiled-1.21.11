package net.minecraft.world.gen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.structure.ShipwreckGenerator;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;

/**
 * Структура затонувшего корабля. Поддерживает два режима: обычный (на дне океана)
 * и выброшенный на берег ({@code beached}), где корабль размещается на поверхности.
 */
public class ShipwreckStructure extends Structure {

	public static final MapCodec<ShipwreckStructure> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							configCodecBuilder(instance),
							Codec.BOOL.fieldOf("is_beached").forGetter(shipwreckStructure -> shipwreckStructure.beached)
					)
					.apply(instance, ShipwreckStructure::new)
	);
	public final boolean beached;

	public ShipwreckStructure(Structure.Config config, boolean beached) {
		super(config);
		this.beached = beached;
	}

	@Override
	public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		Heightmap.Type heightmapType = beached ? Heightmap.Type.WORLD_SURFACE_WG : Heightmap.Type.OCEAN_FLOOR_WG;
		return getStructurePosition(context, heightmapType, collector -> addPieces(collector, context));
	}

	private void addPieces(StructurePiecesCollector collector, Structure.Context context) {
		BlockRotation blockRotation = BlockRotation.random(context.random());
		BlockPos startPos = new BlockPos(context.chunkPos().getStartX(), 90, context.chunkPos().getStartZ());
		ShipwreckGenerator.Piece piece = ShipwreckGenerator.addParts(
				context.structureTemplateManager(), startPos, blockRotation, collector, context.random(), beached
		);

		if (piece.isTooLargeForNormalGeneration()) {
			BlockBox blockBox = piece.getBoundingBox();
			int groundedY;

			if (beached) {
				int minCornerY = Structure.getMinCornerHeight(
						context,
						blockBox.getMinX(),
						blockBox.getBlockCountX(),
						blockBox.getMinZ(),
						blockBox.getBlockCountZ()
				);
				groundedY = piece.findGroundedY(minCornerY, context.random());
			} else {
				groundedY = Structure.getAverageCornerHeights(
						context,
						blockBox.getMinX(),
						blockBox.getBlockCountX(),
						blockBox.getMinZ(),
						blockBox.getBlockCountZ()
				);
			}

			piece.setY(groundedY);
		}
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.SHIPWRECK;
	}
}
