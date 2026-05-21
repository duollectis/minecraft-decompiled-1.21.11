package net.minecraft.structure;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

@FunctionalInterface
/**
 * {@code PostPlacementProcessor}.
 */
public interface PostPlacementProcessor {

	PostPlacementProcessor EMPTY = (world, structureAccessor, chunkGenerator, random, chunkBox, pos, children) -> {};

	void afterPlace(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos pos,
			StructurePiecesList children
	);
}
