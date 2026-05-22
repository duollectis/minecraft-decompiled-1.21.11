package net.minecraft.entity.passive;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.DyeColor;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;

/**
 * Определяет вероятности цветов шерсти овцы при спавне в зависимости от климата биома.
 * Каждый климат имеет свой «доминирующий» цвет с редким шансом розового (1/500).
 */
public class SheepColors {

	private static final SpawnConfig TEMPERATE = new SpawnConfig(
			createCombinedSelector(
					poolBuilder()
							.add(createSingleSelector(DyeColor.BLACK), 5)
							.add(createSingleSelector(DyeColor.GRAY), 5)
							.add(createSingleSelector(DyeColor.LIGHT_GRAY), 5)
							.add(createSingleSelector(DyeColor.BROWN), 3)
							.add(createDefaultSelector(DyeColor.WHITE), 82)
							.build()
			)
	);
	private static final SpawnConfig WARM = new SpawnConfig(
			createCombinedSelector(
					poolBuilder()
							.add(createSingleSelector(DyeColor.GRAY), 5)
							.add(createSingleSelector(DyeColor.LIGHT_GRAY), 5)
							.add(createSingleSelector(DyeColor.WHITE), 5)
							.add(createSingleSelector(DyeColor.BLACK), 3)
							.add(createDefaultSelector(DyeColor.BROWN), 82)
							.build()
			)
	);
	private static final SpawnConfig COLD = new SpawnConfig(
			createCombinedSelector(
					poolBuilder()
							.add(createSingleSelector(DyeColor.LIGHT_GRAY), 5)
							.add(createSingleSelector(DyeColor.GRAY), 5)
							.add(createSingleSelector(DyeColor.WHITE), 5)
							.add(createSingleSelector(DyeColor.BROWN), 3)
							.add(createDefaultSelector(DyeColor.BLACK), 82)
							.build()
			)
	);

	/**
	 * Выбирает цвет шерсти для спавна овцы с учётом биома и случайности.
	 *
	 * @param biome  биом, в котором спавнится овца
	 * @param random источник случайности
	 * @return выбранный цвет шерсти
	 */
	public static DyeColor select(RegistryEntry<Biome> biome, Random random) {
		SpawnConfig config = getSpawnConfig(biome);
		return config.colors().get(random);
	}

	private static SpawnConfig getSpawnConfig(RegistryEntry<Biome> biome) {
		if (biome.isIn(BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS)) {
			return WARM;
		}

		return biome.isIn(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS) ? COLD : TEMPERATE;
	}

	/**
	 * Создаёт селектор с доминирующим цветом и редким шансом розового (1/500).
	 */
	private static ColorSelector createDefaultSelector(DyeColor color) {
		return createCombinedSelector(
				poolBuilder()
						.add(createSingleSelector(color), 499)
						.add(createSingleSelector(DyeColor.PINK), 1)
						.build()
		);
	}

	private static ColorSelector createCombinedSelector(Pool<ColorSelector> pool) {
		if (pool.isEmpty()) {
			throw new IllegalArgumentException("List must be non-empty");
		}

		return random -> pool.get(random).get(random);
	}

	private static ColorSelector createSingleSelector(DyeColor color) {
		return random -> color;
	}

	private static Pool.Builder<ColorSelector> poolBuilder() {
		return Pool.builder();
	}

	@FunctionalInterface
	interface ColorSelector {

		DyeColor get(Random random);
	}

	record SpawnConfig(ColorSelector colors) {
	}
}
