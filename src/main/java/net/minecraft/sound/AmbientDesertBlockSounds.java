package net.minecraft.sound;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * Утилитарный класс для воспроизведения окружающих звуков пустынного биома.
 *
 * <p>Управляет тремя типами звуков: шелест песка, сухая трава и мёртвый куст.
 * Каждый тип имеет собственную вероятность срабатывания и условия проверки блоков.
 */
public class AmbientDesertBlockSounds {

	private static final int SAND_SOUND_CHANCE = 2100;
	private static final int DRY_GRASS_SOUND_CHANCE = 200;
	private static final int DEAD_BUSH_SOUND_CHANCE = 130;
	private static final int DEAD_BUSH_BADLANDS_PENALTY_CHANCE = 3;
	private static final int REQUIRED_SAND_CHECK_DIRECTIONS = 3;
	private static final int SAND_CHECK_HORIZONTAL_DISTANCE = 8;
	private static final int SAND_CHECK_VERTICAL_DISTANCE = 5;
	private static final int HORIZONTAL_DIRECTIONS = 4;

	/**
	 * Пытается воспроизвести звук шелеста песка на указанной позиции.
	 *
	 * <p>Звук воспроизводится только если над блоком находится воздух,
	 * выпал нужный шанс и вокруг достаточно песчаных блоков.
	 *
	 * @param world  мир, в котором проверяется позиция
	 * @param pos    позиция блока-источника звука
	 * @param random генератор случайных чисел
	 */
	public static void tryPlaySandSounds(World world, BlockPos pos, Random random) {
		if (!world.getBlockState(pos.up()).isOf(Blocks.AIR)) {
			return;
		}

		if (random.nextInt(SAND_SOUND_CHANCE) != 0 || !canPlaySandSoundsAt(world, pos)) {
			return;
		}

		world.playSoundClient(
			pos.getX(),
			pos.getY(),
			pos.getZ(),
			SoundEvents.BLOCK_SAND_IDLE,
			SoundCategory.AMBIENT,
			1.0F,
			1.0F,
			false
		);
	}

	/**
	 * Пытается воспроизвести звук сухой травы на указанной позиции.
	 *
	 * @param world  мир, в котором проверяется позиция
	 * @param pos    позиция блока-источника звука
	 * @param random генератор случайных чисел
	 */
	public static void tryPlayDryGrassSounds(World world, BlockPos pos, Random random) {
		if (random.nextInt(DRY_GRASS_SOUND_CHANCE) == 0 && triggersDryVegetationSounds(world, pos.down())) {
			world.playSoundClient(SoundEvents.BLOCK_DRY_GRASS_AMBIENT, SoundCategory.AMBIENT, 1.0F, 1.0F);
		}
	}

	/**
	 * Пытается воспроизвести звук мёртвого куста на указанной позиции.
	 *
	 * <p>В бэдлендах (красный песок или терракота) звук подавляется с вероятностью 2/3,
	 * чтобы снизить частоту срабатывания в этом биоме.
	 *
	 * @param world  мир, в котором проверяется позиция
	 * @param pos    позиция блока-источника звука
	 * @param random генератор случайных чисел
	 */
	public static void tryPlayDeadBushSounds(World world, BlockPos pos, Random random) {
		if (random.nextInt(DEAD_BUSH_SOUND_CHANCE) != 0) {
			return;
		}

		BlockState below = world.getBlockState(pos.down());
		boolean isBadlands = below.isOf(Blocks.RED_SAND) || below.isIn(BlockTags.TERRACOTTA);

		if (isBadlands && random.nextInt(DEAD_BUSH_BADLANDS_PENALTY_CHANCE) != 0) {
			return;
		}

		if (triggersDryVegetationSounds(world, pos.down())) {
			world.playSoundClient(
				pos.getX(),
				pos.getY(),
				pos.getZ(),
				SoundEvents.BLOCK_DEADBUSH_IDLE,
				SoundCategory.AMBIENT,
				1.0F,
				1.0F,
				false
			);
		}
	}

	/**
	 * Проверяет, находится ли блок и блок под ним в теге сухой растительности пустыни.
	 *
	 * @param world мир для проверки
	 * @param pos   позиция нижнего блока (уже сдвинутая вниз)
	 * @return {@code true}, если оба блока входят в тег сухой растительности
	 */
	public static boolean triggersDryVegetationSounds(World world, BlockPos pos) {
		return world.getBlockState(pos).isIn(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS)
			&& world.getBlockState(pos.down()).isIn(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS);
	}

	/**
	 * Проверяет, достаточно ли вокруг позиции песчаных блоков для воспроизведения звука.
	 *
	 * <p>Алгоритм ранней отсечки: если оставшихся непроверенных направлений плюс
	 * уже найденных не хватает до порога {@link #REQUIRED_SAND_CHECK_DIRECTIONS},
	 * проверка прерывается досрочно.
	 *
	 * @param world мир для проверки
	 * @param pos   центральная позиция
	 * @return {@code true}, если минимум 3 из 4 горизонтальных направлений содержат песок
	 */
	private static boolean canPlaySandSoundsAt(World world, BlockPos pos) {
		int found = 0;
		int checked = 0;
		BlockPos.Mutable mutable = pos.mutableCopy();

		for (Direction direction : Direction.Type.HORIZONTAL) {
			mutable.set(pos).move(direction, SAND_CHECK_HORIZONTAL_DISTANCE);

			if (checkForSandSoundTriggers(world, mutable) && ++found >= REQUIRED_SAND_CHECK_DIRECTIONS) {
				return true;
			}

			checked++;
			int remaining = HORIZONTAL_DIRECTIONS - checked;

			if (remaining + found < REQUIRED_SAND_CHECK_DIRECTIONS) {
				return false;
			}
		}

		return false;
	}

	/**
	 * Проверяет наличие песчаного блока в указанной горизонтальной позиции.
	 *
	 * <p>Если поверхность далеко от ожидаемой высоты (более {@link #SAND_CHECK_VERTICAL_DISTANCE} блоков),
	 * выполняется расширенный вертикальный поиск в диапазоне 10 блоков вниз.
	 * Иначе проверяется только верхний блок поверхности.
	 *
	 * @param world мир для проверки
	 * @param pos   изменяемая позиция (будет модифицирована в процессе проверки)
	 * @return {@code true}, если найден песчаный блок под воздухом
	 */
	private static boolean checkForSandSoundTriggers(World world, BlockPos.Mutable pos) {
		int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos) - 1;

		if (Math.abs(surfaceY - pos.getY()) > SAND_CHECK_VERTICAL_DISTANCE) {
			pos.move(Direction.UP, SAND_CHECK_VERTICAL_DISTANCE + 1);
			BlockState above = world.getBlockState(pos);
			pos.move(Direction.DOWN);

			for (int step = 0; step < 10; step++) {
				BlockState current = world.getBlockState(pos);

				if (above.isAir() && triggersSandSounds(current)) {
					return true;
				}

				above = current;
				pos.move(Direction.DOWN);
			}

			return false;
		}

		boolean isAirAboveSurface = world.getBlockState(pos.setY(surfaceY + 1)).isAir();
		return isAirAboveSurface && triggersSandSounds(world.getBlockState(pos.setY(surfaceY)));
	}

	private static boolean triggersSandSounds(BlockState state) {
		return state.isIn(BlockTags.TRIGGERS_AMBIENT_DESERT_SAND_BLOCK_SOUNDS);
	}
}
