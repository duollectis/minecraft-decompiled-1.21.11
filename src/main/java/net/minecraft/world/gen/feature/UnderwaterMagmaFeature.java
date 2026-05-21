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
 * {@code UnderwaterMagmaFeature}.
 */
public class UnderwaterMagmaFeature extends Feature<UnderwaterMagmaFeatureConfig> {

	public UnderwaterMagmaFeature(Codec<UnderwaterMagmaFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<UnderwaterMagmaFeatureConfig> context) {
		StructureWorldAccess structureWorldAccess = context.getWorld();
		BlockPos blockPos = context.getOrigin();
		UnderwaterMagmaFeatureConfig underwaterMagmaFeatureConfig = context.getConfig();
		Random random = context.getRandom();
		OptionalInt optionalInt = getFloorHeight(structureWorldAccess, blockPos, underwaterMagmaFeatureConfig);
		if (optionalInt.isEmpty()) {
			return false;
		}
		else {
			BlockPos blockPos2 = blockPos.withY(optionalInt.getAsInt());
			Vec3i vec3i = new Vec3i(
					underwaterMagmaFeatureConfig.placementRadiusAroundFloor,
					underwaterMagmaFeatureConfig.placementRadiusAroundFloor,
					underwaterMagmaFeatureConfig.placementRadiusAroundFloor
			);
			BlockBox blockBox = BlockBox.create(blockPos2.subtract(vec3i), blockPos2.add(vec3i));
			return BlockPos.stream(blockBox)
			               .filter(pos -> random.nextFloat()
					               < underwaterMagmaFeatureConfig.placementProbabilityPerValidPosition)
			               .filter(pos -> this.isValidPosition(structureWorldAccess, pos))
			               .mapToInt(pos -> {
				               structureWorldAccess.setBlockState(pos, Blocks.MAGMA_BLOCK.getDefaultState(), 2);
				               return 1;
			               })
			               .sum()
					> 0;
		}
	}

	private static OptionalInt getFloorHeight(
			StructureWorldAccess world,
			BlockPos pos,
			UnderwaterMagmaFeatureConfig config
	) {
		Predicate<BlockState> predicate = state -> state.isOf(Blocks.WATER);
		Predicate<BlockState> predicate2 = state -> !state.isOf(Blocks.WATER);
		Optional<CaveSurface> optional = CaveSurface.create(world, pos, config.floorSearchRange, predicate, predicate2);
		return optional.map(CaveSurface::getFloorHeight).orElseGet(OptionalInt::empty);
	}

	private boolean isValidPosition(StructureWorldAccess world, BlockPos pos) {
		if (!cannotReplace(world.getBlockState(pos)) && !this.isFaceNotFull(world, pos.down(), Direction.UP)) {
			for (Direction direction : Direction.Type.HORIZONTAL) {
				if (this.isFaceNotFull(world, pos.offset(direction), direction.getOpposite())) {
					return false;
				}
			}

			return true;
		}
		else {
			return false;
		}
	}

	private static boolean cannotReplace(BlockState state) {
		return state.isOf(Blocks.WATER) || state.isAir();
	}

	private boolean isFaceNotFull(WorldAccess world, BlockPos pos, Direction direction) {
		BlockState blockState = world.getBlockState(pos);
		VoxelShape voxelShape = blockState.getCullingFace(direction);
		return voxelShape == VoxelShapes.empty() || !Block.isShapeFullCube(voxelShape);
	}
}
