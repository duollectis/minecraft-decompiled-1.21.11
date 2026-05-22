package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Базовый класс для генерации огромных грибов.
 * Подклассы реализуют форму шляпки через {@link #generateCap} и
 * радиус шляпки на каждом уровне через {@link #getCapSize}.
 */
public abstract class HugeMushroomFeature extends Feature<HugeMushroomFeatureConfig> {

	public HugeMushroomFeature(Codec<HugeMushroomFeatureConfig> codec) {
		super(codec);
	}

	protected void generateStem(
		WorldAccess world,
		Random random,
		BlockPos pos,
		HugeMushroomFeatureConfig config,
		int height,
		BlockPos.Mutable mutablePos
	) {
		for (int step = 0; step < height; step++) {
			mutablePos.set(pos).move(Direction.UP, step);
			generateStem(world, mutablePos, config.stemProvider.get(random, pos));
		}
	}

	protected void generateStem(WorldAccess world, BlockPos.Mutable pos, BlockState state) {
		BlockState current = world.getBlockState(pos);

		if (current.isAir() || current.isIn(BlockTags.REPLACEABLE_BY_MUSHROOMS)) {
			setBlockState(world, pos, state);
		}
	}

	protected int getHeight(Random random) {
		int height = random.nextInt(3) + 4;

		if (random.nextInt(12) == 0) {
			height *= 2;
		}

		return height;
	}

	/**
	 * Проверяет, можно ли сгенерировать гриб в данной позиции:
	 * основание должно быть почвой или блоком роста грибов,
	 * а весь объём шляпки — свободным (воздух или листья).
	 */
	protected boolean canGenerate(
		WorldAccess world,
		BlockPos pos,
		int height,
		BlockPos.Mutable mutablePos,
		HugeMushroomFeatureConfig config
	) {
		int baseY = pos.getY();

		if (baseY < world.getBottomY() + 1 || baseY + height + 1 > world.getTopYInclusive()) {
			return false;
		}

		BlockState baseState = world.getBlockState(pos.down());

		if (!isSoil(baseState) && !baseState.isIn(BlockTags.MUSHROOM_GROW_BLOCK)) {
			return false;
		}

		for (int dy = 0; dy <= height; dy++) {
			int capRadius = getCapSize(-1, -1, config.foliageRadius, dy);

			for (int dx = -capRadius; dx <= capRadius; dx++) {
				for (int dz = -capRadius; dz <= capRadius; dz++) {
					BlockState state = world.getBlockState(mutablePos.set(pos, dx, dy, dz));

					if (!state.isAir() && !state.isIn(BlockTags.LEAVES)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	@Override
	public boolean generate(FeatureContext<HugeMushroomFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();
		HugeMushroomFeatureConfig config = context.getConfig();
		int height = getHeight(random);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		if (!canGenerate(world, origin, height, mutable, config)) {
			return false;
		}

		generateCap(world, random, origin, height, mutable, config);
		generateStem(world, random, origin, config, height, mutable);

		return true;
	}

	protected abstract int getCapSize(int i, int j, int capSize, int y);

	protected abstract void generateCap(
		WorldAccess world,
		Random random,
		BlockPos start,
		int y,
		BlockPos.Mutable mutable,
		HugeMushroomFeatureConfig config
	);
}
