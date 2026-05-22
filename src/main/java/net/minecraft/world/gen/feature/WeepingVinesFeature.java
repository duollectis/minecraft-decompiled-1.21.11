package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.AbstractPlantStemBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует плачущие лозы (weeping vines) в Нижнем мире.
 * Сначала распространяет блоки блока нетерровых бородавок вокруг точки,
 * затем генерирует колонны лоз, свисающих вниз.
 */
public class WeepingVinesFeature extends Feature<DefaultFeatureConfig> {

	private static final int VINE_AGE_MIN = 17;
	private static final int VINE_AGE_MAX = 25;
	private static final int WART_SPREAD_ATTEMPTS = 200;
	private static final int VINE_SPREAD_ATTEMPTS = 100;

	private static final Direction[] DIRECTIONS = Direction.values();

	public WeepingVinesFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();

		if (!world.isAir(origin)) {
			return false;
		}

		BlockState above = world.getBlockState(origin.up());

		if (!above.isOf(Blocks.NETHERRACK) && !above.isOf(Blocks.NETHER_WART_BLOCK)) {
			return false;
		}

		generateNetherWartBlocksInArea(world, random, origin);
		generateVinesInArea(world, random, origin);
		return true;
	}

	private void generateNetherWartBlocksInArea(WorldAccess world, Random random, BlockPos pos) {
		world.setBlockState(pos, Blocks.NETHER_WART_BLOCK.getDefaultState(), 2);
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockPos.Mutable neighborCheck = new BlockPos.Mutable();

		for (int attempt = 0; attempt < WART_SPREAD_ATTEMPTS; attempt++) {
			mutable.set(
				pos,
				random.nextInt(6) - random.nextInt(6),
				random.nextInt(2) - random.nextInt(5),
				random.nextInt(6) - random.nextInt(6)
			);

			if (!world.isAir(mutable)) {
				continue;
			}

			int wartNeighbors = 0;

			for (Direction direction : DIRECTIONS) {
				BlockState neighbor = world.getBlockState(neighborCheck.set(mutable, direction));

				if (neighbor.isOf(Blocks.NETHERRACK) || neighbor.isOf(Blocks.NETHER_WART_BLOCK)) {
					wartNeighbors++;
				}

				if (wartNeighbors > 1) {
					break;
				}
			}

			if (wartNeighbors == 1) {
				world.setBlockState(mutable, Blocks.NETHER_WART_BLOCK.getDefaultState(), 2);
			}
		}
	}

	private void generateVinesInArea(WorldAccess world, Random random, BlockPos pos) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int attempt = 0; attempt < VINE_SPREAD_ATTEMPTS; attempt++) {
			mutable.set(
				pos,
				random.nextInt(8) - random.nextInt(8),
				random.nextInt(2) - random.nextInt(7),
				random.nextInt(8) - random.nextInt(8)
			);

			if (!world.isAir(mutable)) {
				continue;
			}

			BlockState above = world.getBlockState(mutable.up());

			if (!above.isOf(Blocks.NETHERRACK) && !above.isOf(Blocks.NETHER_WART_BLOCK)) {
				continue;
			}

			int length = MathHelper.nextInt(random, 1, 8);

			if (random.nextInt(6) == 0) {
				length *= 2;
			}

			if (random.nextInt(5) == 0) {
				length = 1;
			}

			generateVineColumn(world, random, mutable, length, VINE_AGE_MIN, VINE_AGE_MAX);
		}
	}

	/**
	 * Генерирует вертикальную колонну плачущих лоз, свисающих вниз.
	 * Последний блок получает случайный возраст из диапазона [{@code minAge}, {@code maxAge}].
	 */
	public static void generateVineColumn(
		WorldAccess world,
		Random random,
		BlockPos.Mutable pos,
		int length,
		int minAge,
		int maxAge
	) {
		for (int step = 0; step <= length; step++) {
			if (!world.isAir(pos)) {
				break;
			}

			if (step == length || !world.isAir(pos.down())) {
				world.setBlockState(
					pos,
					Blocks.WEEPING_VINES
						.getDefaultState()
						.with(AbstractPlantStemBlock.AGE, MathHelper.nextInt(random, minAge, maxAge)),
					2
				);
				break;
			}

			world.setBlockState(pos, Blocks.WEEPING_VINES_PLANT.getDefaultState(), 2);
			pos.move(Direction.DOWN);
		}
	}
}
