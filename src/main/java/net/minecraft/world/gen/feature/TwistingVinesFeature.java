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
 * Генерирует колонны закрученных лоз (twisting vines) в Нижнем мире.
 * Лозы растут снизу вверх от блоков нетеррака, искажённого нилиума или блоков бородавок.
 */
public class TwistingVinesFeature extends Feature<TwistingVinesFeatureConfig> {

	private static final int VINE_AGE_MIN = 17;
	private static final int VINE_AGE_MAX = 25;

	public TwistingVinesFeature(Codec<TwistingVinesFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<TwistingVinesFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();

		if (isNotSuitable(world, origin)) {
			return false;
		}

		Random random = context.getRandom();
		TwistingVinesFeatureConfig config = context.getConfig();
		int spreadWidth = config.spreadWidth();
		int spreadHeight = config.spreadHeight();
		int maxHeight = config.maxHeight();
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int attempt = 0; attempt < spreadWidth * spreadWidth; attempt++) {
			mutable
				.set(origin)
				.move(
					MathHelper.nextInt(random, -spreadWidth, spreadWidth),
					MathHelper.nextInt(random, -spreadHeight, spreadHeight),
					MathHelper.nextInt(random, -spreadWidth, spreadWidth)
				);

			if (!canGenerate(world, mutable) || isNotSuitable(world, mutable)) {
				continue;
			}

			int height = MathHelper.nextInt(random, 1, maxHeight);

			if (random.nextInt(6) == 0) {
				height *= 2;
			}

			if (random.nextInt(5) == 0) {
				height = 1;
			}

			generateVineColumn(world, random, mutable, height, VINE_AGE_MIN, VINE_AGE_MAX);
		}

		return true;
	}

	/**
	 * Опускает позицию вниз до первого не-воздушного блока, затем поднимает на 1.
	 * Возвращает {@code false}, если достигнут предел высоты мира.
	 */
	private static boolean canGenerate(WorldAccess world, BlockPos.Mutable pos) {
		do {
			pos.move(0, -1, 0);

			if (world.isOutOfHeightLimit(pos)) {
				return false;
			}
		} while (world.getBlockState(pos).isAir());

		pos.move(0, 1, 0);
		return true;
	}

	/**
	 * Генерирует вертикальную колонну закрученных лоз заданной длины.
	 * Последний блок колонны получает случайный возраст из диапазона [{@code minAge}, {@code maxAge}].
	 */
	public static void generateVineColumn(
		WorldAccess world,
		Random random,
		BlockPos.Mutable pos,
		int maxLength,
		int minAge,
		int maxAge
	) {
		for (int step = 1; step <= maxLength; step++) {
			if (!world.isAir(pos)) {
				break;
			}

			if (step == maxLength || !world.isAir(pos.up())) {
				world.setBlockState(
					pos,
					Blocks.TWISTING_VINES
						.getDefaultState()
						.with(AbstractPlantStemBlock.AGE, MathHelper.nextInt(random, minAge, maxAge)),
					2
				);
				break;
			}

			world.setBlockState(pos, Blocks.TWISTING_VINES_PLANT.getDefaultState(), 2);
			pos.move(Direction.UP);
		}
	}

	private static boolean isNotSuitable(WorldAccess world, BlockPos pos) {
		if (!world.isAir(pos)) {
			return true;
		}

		BlockState below = world.getBlockState(pos.down());
		return !below.isOf(Blocks.NETHERRACK)
			&& !below.isOf(Blocks.WARPED_NYLIUM)
			&& !below.isOf(Blocks.WARPED_WART_BLOCK);
	}
}
