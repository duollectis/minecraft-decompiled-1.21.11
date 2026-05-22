package net.minecraft.particle;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.function.Supplier;

/**
 * Утилитарный класс для спавна частиц в мире.
 * Предоставляет вспомогательные методы для размещения частиц вокруг блоков,
 * по направлениям граней и с заданными скоростями.
 */
public class ParticleUtil {

	/** Половина размера блока — используется как базовый горизонтальный отступ. */
	private static final double HALF_BLOCK = 0.5;

	/** Стандартное отклонение Гаусса для скорости частиц при атаке. */
	private static final float SMASH_VELOCITY_SPREAD = 0.2F;

	/** Стандартное отклонение Гаусса для скорости частиц кольца при атаке. */
	private static final float SMASH_RING_VELOCITY_SPREAD = 0.05F;

	/** Радиус кольца частиц при атаке молотом. */
	private static final double SMASH_RING_RADIUS = 3.5;

	/** Вертикальный сдвиг центра спавна частиц при атаке молотом. */
	private static final double SMASH_CENTER_Y_OFFSET = 0.5;

	/**
	 * Спавнит частицы на всех шести гранях блока.
	 *
	 * @param world     мир
	 * @param pos       позиция блока
	 * @param effect    тип частицы
	 * @param count     провайдер количества частиц на грань
	 */
	public static void spawnParticle(World world, BlockPos pos, ParticleEffect effect, IntProvider count) {
		for (Direction direction : Direction.values()) {
			spawnParticles(world, pos, effect, count, direction, () -> getRandomVelocity(world.random), 0.55);
		}
	}

	/**
	 * Спавнит заданное количество частиц на указанной грани блока.
	 *
	 * @param world            мир
	 * @param pos              позиция блока
	 * @param effect           тип частицы
	 * @param count            провайдер количества
	 * @param direction        грань блока
	 * @param velocity         поставщик вектора скорости
	 * @param offsetMultiplier множитель смещения от центра блока
	 */
	public static void spawnParticles(
			World world,
			BlockPos pos,
			ParticleEffect effect,
			IntProvider count,
			Direction direction,
			Supplier<Vec3d> velocity,
			double offsetMultiplier
	) {
		int particleCount = count.get(world.random);

		for (int index = 0; index < particleCount; index++) {
			spawnParticle(world, pos, direction, effect, velocity.get(), offsetMultiplier);
		}
	}

	private static Vec3d getRandomVelocity(Random random) {
		return new Vec3d(
				MathHelper.nextDouble(random, -0.5, 0.5),
				MathHelper.nextDouble(random, -0.5, 0.5),
				MathHelper.nextDouble(random, -0.5, 0.5)
		);
	}

	/**
	 * Спавнит частицы вдоль заданной оси с разбросом по двум другим осям.
	 *
	 * @param axis     ось, вдоль которой распределяются частицы
	 * @param world    мир
	 * @param pos      позиция блока
	 * @param variance разброс по перпендикулярным осям
	 * @param effect   тип частицы
	 * @param range    диапазон количества частиц
	 */
	public static void spawnParticle(
			Direction.Axis axis,
			World world,
			BlockPos pos,
			double variance,
			ParticleEffect effect,
			UniformIntProvider range
	) {
		Vec3d center = Vec3d.ofCenter(pos);
		boolean isX = axis == Direction.Axis.X;
		boolean isY = axis == Direction.Axis.Y;
		boolean isZ = axis == Direction.Axis.Z;
		int particleCount = range.get(world.random);

		for (int index = 0; index < particleCount; index++) {
			double spawnX = center.x + MathHelper.nextDouble(world.random, -1.0, 1.0) * (isX ? HALF_BLOCK : variance);
			double spawnY = center.y + MathHelper.nextDouble(world.random, -1.0, 1.0) * (isY ? HALF_BLOCK : variance);
			double spawnZ = center.z + MathHelper.nextDouble(world.random, -1.0, 1.0) * (isZ ? HALF_BLOCK : variance);
			double velX = isX ? MathHelper.nextDouble(world.random, -1.0, 1.0) : 0.0;
			double velY = isY ? MathHelper.nextDouble(world.random, -1.0, 1.0) : 0.0;
			double velZ = isZ ? MathHelper.nextDouble(world.random, -1.0, 1.0) : 0.0;
			world.addParticleClient(effect, spawnX, spawnY, spawnZ, velX, velY, velZ);
		}
	}

	/**
	 * Спавнит одну частицу на грани блока со смещением от центра.
	 *
	 * @param world            мир
	 * @param pos              позиция блока
	 * @param direction        грань блока
	 * @param effect           тип частицы
	 * @param velocity         вектор скорости
	 * @param offsetMultiplier множитель смещения вдоль нормали грани
	 */
	public static void spawnParticle(
			World world,
			BlockPos pos,
			Direction direction,
			ParticleEffect effect,
			Vec3d velocity,
			double offsetMultiplier
	) {
		Vec3d center = Vec3d.ofCenter(pos);
		int offsetX = direction.getOffsetX();
		int offsetY = direction.getOffsetY();
		int offsetZ = direction.getOffsetZ();
		double spawnX = center.x + (offsetX == 0
				? MathHelper.nextDouble(world.random, -0.5, 0.5)
				: offsetX * offsetMultiplier);
		double spawnY = center.y + (offsetY == 0
				? MathHelper.nextDouble(world.random, -0.5, 0.5)
				: offsetY * offsetMultiplier);
		double spawnZ = center.z + (offsetZ == 0
				? MathHelper.nextDouble(world.random, -0.5, 0.5)
				: offsetZ * offsetMultiplier);
		double velX = offsetX == 0 ? velocity.getX() : 0.0;
		double velY = offsetY == 0 ? velocity.getY() : 0.0;
		double velZ = offsetZ == 0 ? velocity.getZ() : 0.0;
		world.addParticleClient(effect, spawnX, spawnY, spawnZ, velX, velY, velZ);
	}

	/**
	 * Спавнит одну частицу над блоком со случайным горизонтальным смещением.
	 *
	 * @param world   мир
	 * @param pos     позиция блока
	 * @param random  генератор случайных чисел
	 * @param effect  тип частицы
	 */
	public static void spawnParticle(World world, BlockPos pos, Random random, ParticleEffect effect) {
		double spawnX = pos.getX() + random.nextDouble();
		double spawnY = pos.getY() - 0.05;
		double spawnZ = pos.getZ() + random.nextDouble();
		world.addParticleClient(effect, spawnX, spawnY, spawnZ, 0.0, 0.0, 0.0);
	}

	/**
	 * Спавнит частицы вокруг блока, используя форму его коллизии для определения высоты.
	 *
	 * @param world  мир
	 * @param pos    позиция блока
	 * @param count  количество частиц
	 * @param effect тип частицы
	 */
	public static void spawnParticlesAround(WorldAccess world, BlockPos pos, int count, ParticleEffect effect) {
		BlockState blockState = world.getBlockState(pos);
		double topY = blockState.isAir()
				? 1.0
				: blockState.getOutlineShape(world, pos).getMax(Direction.Axis.Y);
		spawnParticlesAround(world, pos, count, HALF_BLOCK, topY, true, effect);
	}

	/**
	 * Спавнит частицы вокруг блока с заданными параметрами разброса.
	 *
	 * @param world            мир
	 * @param pos              позиция блока
	 * @param count            количество частиц
	 * @param horizontalOffset горизонтальный радиус разброса
	 * @param verticalOffset   вертикальный диапазон спавна
	 * @param force            если {@code true} — спавнить даже над воздухом
	 * @param effect           тип частицы
	 */
	public static void spawnParticlesAround(
			WorldAccess world,
			BlockPos pos,
			int count,
			double horizontalOffset,
			double verticalOffset,
			boolean force,
			ParticleEffect effect
	) {
		Random random = world.getRandom();

		for (int index = 0; index < count; index++) {
			double velX = random.nextGaussian() * 0.02;
			double velY = random.nextGaussian() * 0.02;
			double velZ = random.nextGaussian() * 0.02;
			double edgeOffset = HALF_BLOCK - horizontalOffset;
			double spawnX = pos.getX() + edgeOffset + random.nextDouble() * horizontalOffset * 2.0;
			double spawnY = pos.getY() + random.nextDouble() * verticalOffset;
			double spawnZ = pos.getZ() + edgeOffset + random.nextDouble() * horizontalOffset * 2.0;

			if (force || !world.getBlockState(BlockPos.ofFloored(spawnX, spawnY, spawnZ).down()).isAir()) {
				world.addParticleClient(effect, spawnX, spawnY, spawnZ, velX, velY, velZ);
			}
		}
	}

	/**
	 * Спавнит частицы разрушения блока при атаке молотом (smash attack).
	 * Создаёт два слоя: центральное облако и кольцо по периметру.
	 *
	 * @param world  мир
	 * @param pos    позиция удара
	 * @param count  общее количество частиц
	 */
	public static void spawnSmashAttackParticles(WorldAccess world, BlockPos pos, int count) {
		Vec3d center = pos.toCenterPos().add(0.0, SMASH_CENTER_Y_OFFSET, 0.0);
		BlockStateParticleEffect dustPillar =
				new BlockStateParticleEffect(ParticleTypes.DUST_PILLAR, world.getBlockState(pos));

		int centerCount = (int) (count / 3.0F);

		for (int index = 0; index < centerCount; index++) {
			double spawnX = center.x + world.getRandom().nextGaussian() / 2.0;
			double spawnZ = center.z + world.getRandom().nextGaussian() / 2.0;
			double velX = world.getRandom().nextGaussian() * SMASH_VELOCITY_SPREAD;
			double velY = world.getRandom().nextGaussian() * SMASH_VELOCITY_SPREAD;
			double velZ = world.getRandom().nextGaussian() * SMASH_VELOCITY_SPREAD;
			world.addParticleClient(dustPillar, spawnX, center.y, spawnZ, velX, velY, velZ);
		}

		int ringCount = (int) (count / 1.5F);

		for (int index = 0; index < ringCount; index++) {
			double spawnX = center.x + SMASH_RING_RADIUS * Math.cos(index) + world.getRandom().nextGaussian() / 2.0;
			double spawnZ = center.z + SMASH_RING_RADIUS * Math.sin(index) + world.getRandom().nextGaussian() / 2.0;
			double velX = world.getRandom().nextGaussian() * SMASH_RING_VELOCITY_SPREAD;
			double velY = world.getRandom().nextGaussian() * SMASH_RING_VELOCITY_SPREAD;
			double velZ = world.getRandom().nextGaussian() * SMASH_RING_VELOCITY_SPREAD;
			world.addParticleClient(dustPillar, spawnX, center.y, spawnZ, velX, velY, velZ);
		}
	}
}
