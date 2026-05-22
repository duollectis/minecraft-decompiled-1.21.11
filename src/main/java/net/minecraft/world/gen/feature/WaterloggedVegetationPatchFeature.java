package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Расширение {@link VegetationPatchFeature} для подводных патчей.
 * После размещения грунта заполняет позиции без твёрдых соседей водой,
 * а растительность помечает как waterlogged.
 */
public class WaterloggedVegetationPatchFeature extends VegetationPatchFeature {

	public WaterloggedVegetationPatchFeature(Codec<VegetationPatchFeatureConfig> codec) {
		super(codec);
	}

	@Override
	protected Set<BlockPos> placeGroundAndGetPositions(
		StructureWorldAccess world,
		VegetationPatchFeatureConfig config,
		Random random,
		BlockPos pos,
		Predicate<BlockState> replaceable,
		int radiusX,
		int radiusZ
	) {
		Set<BlockPos> groundPositions = super.placeGroundAndGetPositions(world, config, random, pos, replaceable, radiusX, radiusZ);
		Set<BlockPos> waterPositions = new HashSet<>();
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (BlockPos groundPos : groundPositions) {
			if (!isSolidBlockAroundPos(world, groundPositions, groundPos, mutable)) {
				waterPositions.add(groundPos);
			}
		}

		for (BlockPos waterPos : waterPositions) {
			world.setBlockState(waterPos, Blocks.WATER.getDefaultState(), 2);
		}

		return waterPositions;
	}

	private static boolean isSolidBlockAroundPos(
		StructureWorldAccess world,
		Set<BlockPos> positions,
		BlockPos pos,
		BlockPos.Mutable mutable
	) {
		return isSolidBlockSide(world, pos, mutable, Direction.NORTH)
			|| isSolidBlockSide(world, pos, mutable, Direction.EAST)
			|| isSolidBlockSide(world, pos, mutable, Direction.SOUTH)
			|| isSolidBlockSide(world, pos, mutable, Direction.WEST)
			|| isSolidBlockSide(world, pos, mutable, Direction.DOWN);
	}

	private static boolean isSolidBlockSide(
		StructureWorldAccess world,
		BlockPos pos,
		BlockPos.Mutable mutable,
		Direction direction
	) {
		mutable.set(pos, direction);
		return !world.getBlockState(mutable).isSideSolidFullSquare(world, mutable, direction.getOpposite());
	}

	@Override
	protected boolean generateVegetationFeature(
		StructureWorldAccess world,
		VegetationPatchFeatureConfig config,
		ChunkGenerator generator,
		Random random,
		BlockPos pos
	) {
		if (!super.generateVegetationFeature(world, config, generator, random, pos.down())) {
			return false;
		}

		BlockState placed = world.getBlockState(pos);

		if (placed.contains(Properties.WATERLOGGED) && !placed.get(Properties.WATERLOGGED)) {
			world.setBlockState(pos, placed.with(Properties.WATERLOGGED, true), 2);
		}

		return true;
	}
}
