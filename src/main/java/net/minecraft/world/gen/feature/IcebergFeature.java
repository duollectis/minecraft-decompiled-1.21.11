package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует айсберг на уровне моря. Айсберг состоит из надводной части (лёд/снег)
 * и подводной части. Форма определяется эллипсоидными функциями расстояния.
 * Опционально внутри вырезается полая сердцевина.
 */
public class IcebergFeature extends Feature<SingleStateFeatureConfig> {

	public IcebergFeature(Codec<SingleStateFeatureConfig> codec) {
		super(codec);
	}

	/**
	 * Генерирует айсберг в точке уровня моря. Форма айсберга определяется случайными
	 * параметрами: высота, радиус, угол поворота, наличие полой сердцевины.
	 */
	@Override
	public boolean generate(FeatureContext<SingleStateFeatureConfig> context) {
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		origin = new BlockPos(origin.getX(), context.getGenerator().getSeaLevel(), origin.getZ());
		Random random = context.getRandom();

		boolean placeSnow = random.nextDouble() > 0.7;
		BlockState iceState = context.getConfig().state;
		double rotationAngle = random.nextDouble() * 2.0 * Math.PI;
		int snowLayerThickness = 3 + random.nextInt(3);
		boolean isSmall = random.nextDouble() > 0.7;
		int maxRadius = 11;
		int aboveWaterHeight = isSmall ? random.nextInt(6) + 6 : random.nextInt(15) + 3;

		if (!isSmall && random.nextDouble() > 0.9) {
			aboveWaterHeight += random.nextInt(19) + 7;
		}

		int belowWaterHeight = Math.min(aboveWaterHeight + random.nextInt(11), 18);
		int aboveWaterFactor = Math.min(aboveWaterHeight + random.nextInt(7) - random.nextInt(5), 11);
		int scanRadius = isSmall ? maxRadius - (11 - random.nextInt(5)) : maxRadius;

		// Надводная часть айсберга
		for (int dx = -scanRadius; dx < scanRadius; dx++) {
			for (int dz = -scanRadius; dz < scanRadius; dz++) {
				for (int dy = 0; dy < aboveWaterHeight; dy++) {
					int radius = isSmall
						? calcAboveWaterRadius(dy, aboveWaterHeight, aboveWaterFactor)
						: calcAboveWaterRadiusRandom(random, dy, aboveWaterHeight, aboveWaterFactor);

					if (isSmall || dx < radius) {
						placeAt(
							world,
							random,
							origin,
							aboveWaterHeight,
							dx,
							dy,
							dz,
							radius,
							scanRadius,
							isSmall,
							snowLayerThickness,
							rotationAngle,
							placeSnow,
							iceState
						);
					}
				}
			}
		}

		removeFloatingBlocks(world, origin, aboveWaterFactor, aboveWaterHeight, isSmall, maxRadius - (11 - random.nextInt(5)));

		// Подводная часть айсберга
		for (int dx = -scanRadius; dx < scanRadius; dx++) {
			for (int dz = -scanRadius; dz < scanRadius; dz++) {
				for (int dy = -1; dy > -belowWaterHeight; dy--) {
					int underwaterScanRadius = isSmall
						? MathHelper.ceil(scanRadius * (1.0F - (float) Math.pow(dy, 2.0) / (belowWaterHeight * 8.0F)))
						: scanRadius;
					int radius = calcBelowWaterRadius(random, -dy, belowWaterHeight, aboveWaterFactor);

					if (dx < radius) {
						placeAt(
							world,
							random,
							origin,
							belowWaterHeight,
							dx,
							dy,
							dz,
							radius,
							underwaterScanRadius,
							isSmall,
							snowLayerThickness,
							rotationAngle,
							placeSnow,
							iceState
						);
					}
				}
			}
		}

		boolean generateCore = isSmall ? random.nextDouble() > 0.1 : random.nextDouble() > 0.7;

		if (generateCore) {
			generateHollowCore(random, world, aboveWaterFactor, aboveWaterHeight, origin, isSmall, scanRadius, rotationAngle, snowLayerThickness);
		}

		return true;
	}

	/**
	 * Вырезает полую сердцевину внутри айсберга, заменяя лёд воздухом (или водой под водой).
	 * Смещение сердцевины от центра задаётся случайно.
	 */
	private void generateHollowCore(
		Random random,
		WorldAccess world,
		int aboveWaterFactor,
		int aboveWaterHeight,
		BlockPos pos,
		boolean isSmall,
		int scanRadius,
		double rotationAngle,
		int snowLayerThickness
	) {
		int signX = random.nextBoolean() ? -1 : 1;
		int signZ = random.nextBoolean() ? -1 : 1;
		int offsetX = random.nextInt(Math.max(aboveWaterFactor / 2 - 2, 1));

		if (random.nextBoolean()) {
			offsetX = aboveWaterFactor / 2 + 1 - random.nextInt(Math.max(aboveWaterFactor - aboveWaterFactor / 2 - 1, 1));
		}

		int offsetZ = random.nextInt(Math.max(aboveWaterFactor / 2 - 2, 1));

		if (random.nextBoolean()) {
			offsetZ = aboveWaterFactor / 2 + 1 - random.nextInt(Math.max(aboveWaterFactor - aboveWaterFactor / 2 - 1, 1));
		}

		if (isSmall) {
			offsetX = random.nextInt(Math.max(scanRadius - 5, 1));
			offsetZ = offsetX;
		}

		BlockPos coreOffset = new BlockPos(signX * offsetX, 0, signZ * offsetZ);
		double coreAngle = isSmall ? rotationAngle + (Math.PI / 2) : random.nextDouble() * 2.0 * Math.PI;

		for (int dy = 0; dy < aboveWaterHeight - 3; dy++) {
			int radius = calcAboveWaterRadiusRandom(random, dy, aboveWaterHeight, aboveWaterFactor);
			carveHollowSection(radius, dy, pos, world, false, coreAngle, coreOffset, scanRadius, snowLayerThickness);
		}

		for (int dy = -1; dy > -aboveWaterHeight + random.nextInt(5); dy--) {
			int radius = calcBelowWaterRadius(random, -dy, aboveWaterHeight, aboveWaterFactor);
			carveHollowSection(radius, dy, pos, world, true, coreAngle, coreOffset, scanRadius, snowLayerThickness);
		}
	}

	/**
	 * Вырезает один горизонтальный слой полой сердцевины на высоте {@code y}.
	 * Форма сечения — эллипс, повёрнутый на угол {@code angle}.
	 */
	private void carveHollowSection(
		int radius,
		int y,
		BlockPos pos,
		WorldAccess world,
		boolean placeWater,
		double angle,
		BlockPos coreOffset,
		int scanRadius,
		int snowLayerThickness
	) {
		int outerRadius = radius + 1 + scanRadius / 3;
		int innerRadius = Math.min(radius - 3, 3) + snowLayerThickness / 2 - 1;

		for (int dx = -outerRadius; dx < outerRadius; dx++) {
			for (int dz = -outerRadius; dz < outerRadius; dz++) {
				double dist = getDistance(dx, dz, coreOffset, outerRadius, innerRadius, angle);

				if (dist >= 0.0) {
					continue;
				}

				BlockPos candidate = pos.add(dx, y, dz);
				BlockState state = world.getBlockState(candidate);

				if (isSnowOrIce(state) || state.isOf(Blocks.SNOW_BLOCK)) {
					if (placeWater) {
						setBlockState(world, candidate, Blocks.WATER.getDefaultState());
					} else {
						setBlockState(world, candidate, Blocks.AIR.getDefaultState());
						clearSnowAbove(world, candidate);
					}
				}
			}
		}
	}

	private void clearSnowAbove(WorldAccess world, BlockPos pos) {
		if (world.getBlockState(pos.up()).isOf(Blocks.SNOW)) {
			setBlockState(world, pos.up(), Blocks.AIR.getDefaultState());
		}
	}

	/**
	 * Размещает один блок айсберга в позиции {@code (pos + offsetX, offsetY, offsetZ)}.
	 * Выбирает между льдом и снегом в зависимости от высоты и случайности.
	 */
	private void placeAt(
		WorldAccess world,
		Random random,
		BlockPos pos,
		int height,
		int offsetX,
		int offsetY,
		int offsetZ,
		int radius,
		int scanRadius,
		boolean isSmall,
		int snowLayerThickness,
		double rotationAngle,
		boolean placeSnow,
		BlockState iceState
	) {
		double dist = isSmall
			? getDistance(
				offsetX,
				offsetZ,
				BlockPos.ORIGIN,
				scanRadius,
				decreaseValueNearTop(offsetY, height, snowLayerThickness),
				rotationAngle
			)
			: calcUnderwaterDistance(offsetX, offsetZ, BlockPos.ORIGIN, radius, random);

		if (dist >= 0.0) {
			return;
		}

		BlockPos target = pos.add(offsetX, offsetY, offsetZ);
		double innerThreshold = isSmall ? -0.5 : -6 - random.nextInt(3);

		if (dist > innerThreshold && random.nextDouble() > 0.9) {
			return;
		}

		placeBlockOrSnow(target, world, random, height - offsetY, height, isSmall, placeSnow, iceState);
	}

	private void placeBlockOrSnow(
		BlockPos pos,
		WorldAccess world,
		Random random,
		int heightRemaining,
		int height,
		boolean lessSnow,
		boolean placeSnow,
		BlockState iceState
	) {
		BlockState existing = world.getBlockState(pos);

		if (existing.isAir()
			|| existing.isOf(Blocks.SNOW_BLOCK)
			|| existing.isOf(Blocks.ICE)
			|| existing.isOf(Blocks.WATER)
		) {
			boolean allowSnow = !lessSnow || random.nextDouble() > 0.05;
			int snowDivisor = lessSnow ? 3 : 2;

			if (placeSnow
				&& !existing.isOf(Blocks.WATER)
				&& heightRemaining <= random.nextInt(Math.max(1, height / snowDivisor)) + height * 0.6
				&& allowSnow
			) {
				setBlockState(world, pos, Blocks.SNOW_BLOCK.getDefaultState());
			} else {
				setBlockState(world, pos, iceState);
			}
		}
	}

	/**
	 * Уменьшает значение {@code value} вблизи вершины айсберга (последние 3 слоя),
	 * чтобы сузить форму к верхушке.
	 */
	private int decreaseValueNearTop(int y, int height, int value) {
		if (y > 0 && height - y <= 3) {
			return value - (4 - (height - y));
		}

		return value;
	}

	/**
	 * Вычисляет расстояние для подводной части: эллипс с небольшим случайным смещением.
	 */
	private double calcUnderwaterDistance(int x, int z, BlockPos pos, int radius, Random random) {
		float scale = 10.0F * MathHelper.clamp(random.nextFloat(), 0.2F, 0.8F) / radius;
		return scale
			+ Math.pow(x - pos.getX(), 2.0)
			+ Math.pow(z - pos.getZ(), 2.0)
			- Math.pow(radius, 2.0);
	}

	/**
	 * Вычисляет расстояние до границы повёрнутого эллипса.
	 * Возвращает отрицательное значение, если точка находится внутри эллипса.
	 *
	 * @param divisor1 полуось по первому направлению (после поворота)
	 * @param divisor2 полуось по второму направлению (после поворота)
	 */
	private double getDistance(int x, int z, BlockPos pos, int divisor1, int divisor2, double angle) {
		double relX = x - pos.getX();
		double relZ = z - pos.getZ();
		double cosA = Math.cos(angle);
		double sinA = Math.sin(angle);
		return Math.pow((relX * cosA - relZ * sinA) / divisor1, 2.0)
			+ Math.pow((relX * sinA + relZ * cosA) / divisor2, 2.0)
			- 1.0;
	}

	/**
	 * Вычисляет радиус надводной части с добавлением случайности.
	 * Для высоких айсбергов применяется линейное уменьшение вместо квадратичного.
	 */
	private int calcAboveWaterRadiusRandom(Random random, int y, int height, int factor) {
		float falloff = 3.5F - random.nextFloat();
		float radius = (1.0F - (float) Math.pow(y, 2.0) / (height * falloff)) * factor;

		if (height > 15 + random.nextInt(5)) {
			int effectiveY = y < 3 + random.nextInt(6) ? y / 2 : y;
			radius = (1.0F - effectiveY / (height * falloff * 0.4F)) * factor;
		}

		return MathHelper.ceil(radius / 2.0F);
	}

	private int calcAboveWaterRadius(int y, int height, int factor) {
		float radius = (1.0F - (float) Math.pow(y, 2.0) / height) * factor;
		return MathHelper.ceil(radius / 2.0F);
	}

	private int calcBelowWaterRadius(Random random, int y, int height, int factor) {
		float falloff = 1.0F + random.nextFloat() / 2.0F;
		float radius = (1.0F - y / (height * falloff)) * factor;
		return MathHelper.ceil(radius / 2.0F);
	}

	private static boolean isSnowOrIce(BlockState state) {
		return state.isOf(Blocks.PACKED_ICE) || state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.BLUE_ICE);
	}

	private boolean isAirBelow(BlockView world, BlockPos pos) {
		return world.getBlockState(pos.down()).isAir();
	}

	/**
	 * Удаляет висящие в воздухе блоки льда и снега после генерации формы айсберга.
	 * Блок считается висящим, если под ним воздух или если у него 3+ не-ледяных соседа.
	 */
	private void removeFloatingBlocks(WorldAccess world, BlockPos pos, int factor, int height, boolean isSmall, int scanRadius) {
		int range = isSmall ? scanRadius : factor / 2;

		for (int dx = -range; dx <= range; dx++) {
			for (int dz = -range; dz <= range; dz++) {
				for (int dy = 0; dy <= height; dy++) {
					BlockPos candidate = pos.add(dx, dy, dz);
					BlockState state = world.getBlockState(candidate);

					if (!isSnowOrIce(state) && !state.isOf(Blocks.SNOW)) {
						continue;
					}

					if (isAirBelow(world, candidate)) {
						setBlockState(world, candidate, Blocks.AIR.getDefaultState());
						setBlockState(world, candidate.up(), Blocks.AIR.getDefaultState());
					} else if (isSnowOrIce(state)) {
						BlockState[] neighbors = new BlockState[]{
							world.getBlockState(candidate.west()),
							world.getBlockState(candidate.east()),
							world.getBlockState(candidate.north()),
							world.getBlockState(candidate.south())
						};
						int nonIceNeighbors = 0;

						for (BlockState neighbor : neighbors) {
							if (!isSnowOrIce(neighbor)) {
								nonIceNeighbors++;
							}
						}

						if (nonIceNeighbors >= 3) {
							setBlockState(world, candidate, Blocks.AIR.getDefaultState());
						}
					}
				}
			}
		}
	}
}
