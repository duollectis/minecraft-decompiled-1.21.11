package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.CaveSurface;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * Генерирует блоки магмы на дне подводных пещер.
 * Ищет поверхность пола в водяном столбе, затем размещает магму
 * в радиусе {@code placementRadiusAroundFloor} с заданной вероятностью.
 */
public class UnderwaterMagmaFeature extends Feature<UnderwaterMagmaFeatureConfig> {

	public UnderwaterMagmaFeature(Codec<UnderwaterMagmaFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<UnderwaterMagmaFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		UnderwaterMagmaFeatureConfig config = context.getConfig();
		Random random = context.getRandom();

		OptionalInt floorY = getFloorHeight(world, origin, config);

		if (floorY.isEmpty()) {
			return false;
		}

		BlockPos floorPos = origin.withY(floorY.getAsInt());
		Vec3i radius = new Vec3i(
			config.placementRadiusAroundFloor,
			config.placementRadiusAroundFloor,
			config.placementRadiusAroundFloor
		);
		BlockBox searchBox = BlockBox.create(floorPos.subtract(radius), floorPos.add(radius));

		return BlockPos.stream(searchBox)
			.filter(pos -> random.nextFloat() < config.placementProbabilityPerValidPosition)
			.filter(pos -> isValidPosition(world, pos))
			.mapToInt(pos -> {
				world.setBlockState(pos, Blocks.MAGMA_BLOCK.getDefaultState(), 2);
				return 1;
			})
			.sum() > 0;
	}

	private static OptionalInt getFloorHeight(
		StructureWorldAccess world,
		BlockPos pos,
		UnderwaterMagmaFeatureConfig config
	) {
		Predicate<BlockState> isWater = state -> state.isOf(Blocks.WATER);
		Predicate<BlockState> isNotWater = state -> !state.isOf(Blocks.WATER);
		Optional<CaveSurface> surface = CaveSurface.create(world, pos, config.floorSearchRange, isWater, isNotWater);
		return surface.map(CaveSurface::getFloorHeight).orElseGet(OptionalInt::empty);
	}

	private boolean isValidPosition(StructureWorldAccess world, BlockPos pos) {
		if (cannotReplace(world.getBlockState(pos))) {
			return false;
		}

		if (isFaceNotFull(world, pos.down(), Direction.UP)) {
			return false;
		}

		for (Direction direction : Direction.Type.HORIZONTAL) {
			if (isFaceNotFull(world, pos.offset(direction), direction.getOpposite())) {
				return false;
			}
		}

		return true;
	}

	private static boolean cannotReplace(BlockState state) {
		return state.isOf(Blocks.WATER) || state.isAir();
	}

	private boolean isFaceNotFull(WorldAccess world, BlockPos pos, Direction direction) {
		VoxelShape face = world.getBlockState(pos).getCullingFace(direction);
		return face == VoxelShapes.empty() || !Block.isShapeFullCube(face);
	}
}
