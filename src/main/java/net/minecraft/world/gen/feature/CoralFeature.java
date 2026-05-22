package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Optional;

/** Базовый класс для генераторов кораллов: выбирает случайный тип кораллового блока и делегирует конкретную форму подклассу. */
public abstract class CoralFeature extends Feature<DefaultFeatureConfig> {

	public CoralFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		Random random = context.getRandom();
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Optional<Block> coralBlock = Registries.BLOCK.getRandomEntry(BlockTags.CORAL_BLOCKS, random).map(RegistryEntry::value);

		return coralBlock.isEmpty()
				? false
				: generateCoral(world, random, origin, coralBlock.get().getDefaultState());
	}

	protected abstract boolean generateCoral(WorldAccess world, Random random, BlockPos pos, BlockState state);

	protected boolean generateCoralPiece(WorldAccess world, Random random, BlockPos pos, BlockState state) {
		BlockPos above = pos.up();
		BlockState current = world.getBlockState(pos);

		if ((!current.isOf(Blocks.WATER) && !current.isIn(BlockTags.CORALS))
				|| !world.getBlockState(above).isOf(Blocks.WATER)) {
			return false;
		}

		world.setBlockState(pos, state, 3);

		if (random.nextFloat() < 0.25F) {
			Registries.BLOCK
					.getRandomEntry(BlockTags.CORALS, random)
					.map(RegistryEntry::value)
					.ifPresent(block -> world.setBlockState(above, block.getDefaultState(), 2));
		} else if (random.nextFloat() < 0.05F) {
			world.setBlockState(
					above,
					Blocks.SEA_PICKLE.getDefaultState().with(SeaPickleBlock.PICKLES, random.nextInt(4) + 1),
					2
			);
		}

		for (Direction direction : Direction.Type.HORIZONTAL) {
			if (random.nextFloat() >= 0.2F) {
				continue;
			}

			BlockPos side = pos.offset(direction);

			if (!world.getBlockState(side).isOf(Blocks.WATER)) {
				continue;
			}

			Registries.BLOCK
					.getRandomEntry(BlockTags.WALL_CORALS, random)
					.map(RegistryEntry::value)
					.ifPresent(block -> {
						BlockState wallState = block.getDefaultState();

						if (wallState.contains(DeadCoralWallFanBlock.FACING)) {
							wallState = wallState.with(DeadCoralWallFanBlock.FACING, direction);
						}

						world.setBlockState(side, wallState, 2);
					});
		}

		return true;
	}
}
