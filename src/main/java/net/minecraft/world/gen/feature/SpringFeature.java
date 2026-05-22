package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует источник жидкости (воды или лавы) в стене пещеры.
 * Проверяет, что вокруг точки ровно {@code rockCount} твёрдых блоков
 * и {@code holeCount} воздушных — это гарантирует, что источник
 * находится в стене, а не в открытом пространстве.
 */
public class SpringFeature extends Feature<SpringFeatureConfig> {

	public SpringFeature(Codec<SpringFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<SpringFeatureConfig> context) {
		SpringFeatureConfig config = context.getConfig();
		StructureWorldAccess world = context.getWorld();
		BlockPos pos = context.getOrigin();

		if (!world.getBlockState(pos.up()).isIn(config.validBlocks)) {
			return false;
		}

		if (config.requiresBlockBelow && !world.getBlockState(pos.down()).isIn(config.validBlocks)) {
			return false;
		}

		BlockState existing = world.getBlockState(pos);

		if (!existing.isAir() && !existing.isIn(config.validBlocks)) {
			return false;
		}

		int solidNeighbors = 0;

		if (world.getBlockState(pos.west()).isIn(config.validBlocks)) {
			solidNeighbors++;
		}

		if (world.getBlockState(pos.east()).isIn(config.validBlocks)) {
			solidNeighbors++;
		}

		if (world.getBlockState(pos.north()).isIn(config.validBlocks)) {
			solidNeighbors++;
		}

		if (world.getBlockState(pos.south()).isIn(config.validBlocks)) {
			solidNeighbors++;
		}

		if (world.getBlockState(pos.down()).isIn(config.validBlocks)) {
			solidNeighbors++;
		}

		int airNeighbors = 0;

		if (world.isAir(pos.west())) {
			airNeighbors++;
		}

		if (world.isAir(pos.east())) {
			airNeighbors++;
		}

		if (world.isAir(pos.north())) {
			airNeighbors++;
		}

		if (world.isAir(pos.south())) {
			airNeighbors++;
		}

		if (world.isAir(pos.down())) {
			airNeighbors++;
		}

		if (solidNeighbors != config.rockCount || airNeighbors != config.holeCount) {
			return false;
		}

		world.setBlockState(pos, config.state.getBlockState(), 2);
		world.scheduleFluidTick(pos, config.state.getFluid(), 0);
		return true;
	}
}
