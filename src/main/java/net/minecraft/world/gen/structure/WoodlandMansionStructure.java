package net.minecraft.world.gen.structure;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.structure.StructurePiecesList;
import net.minecraft.structure.WoodlandMansionGenerator;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Структура лесного особняка. Генерирует многоэтажное здание из случайных комнат.
 * После размещения заполняет пустоты под фундаментом булыжником.
 */
public class WoodlandMansionStructure extends Structure {

	public static final MapCodec<WoodlandMansionStructure> CODEC = createCodec(WoodlandMansionStructure::new);

	public WoodlandMansionStructure(Structure.Config config) {
		super(config);
	}

	@Override
	public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		BlockRotation blockRotation = BlockRotation.random(context.random());
		BlockPos pos = getShiftedPos(context, blockRotation);

		return pos.getY() < 60
				? Optional.empty()
				: Optional.of(
						new Structure.StructurePosition(
								pos,
								collector -> addPieces(collector, context, pos, blockRotation)
						)
				);
	}

	private void addPieces(
			StructurePiecesCollector collector,
			Structure.Context context,
			BlockPos pos,
			BlockRotation rotation
	) {
		List<WoodlandMansionGenerator.Piece> pieces = Lists.newLinkedList();
		WoodlandMansionGenerator.addPieces(context.structureTemplateManager(), pos, rotation, pieces, context.random());
		pieces.forEach(collector::addPiece);
	}

	@Override
	public void postPlace(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox box,
			ChunkPos chunkPos,
			StructurePiecesList pieces
	) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int worldBottomY = world.getBottomY();
		BlockBox structureBox = pieces.getBoundingBox();
		int mansionFloorY = structureBox.getMinY();

		for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
			for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
				mutable.set(x, mansionFloorY, z);
				if (!world.isAir(mutable) && structureBox.contains(mutable) && pieces.contains(mutable)) {
					for (int y = mansionFloorY - 1; y > worldBottomY; y--) {
						mutable.setY(y);
						if (!world.isAir(mutable) && !world.getBlockState(mutable).isLiquid()) {
							break;
						}

						world.setBlockState(mutable, Blocks.COBBLESTONE.getDefaultState(), 2);
					}
				}
			}
		}
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.WOODLAND_MANSION;
	}
}
