package net.minecraft.world.gen.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Function;

/**
 * Конфигурация формы генерации чанка: диапазон высот и размеры ячеек шума.
 * Все предустановленные конфигурации (SURFACE, NETHER и т.д.) валидируются при создании.
 */
public record GenerationShapeConfig(int minimumY, int height, int horizontalSize, int verticalSize) {

	public static final Codec<GenerationShapeConfig> CODEC = RecordCodecBuilder
		.<GenerationShapeConfig>create(
			instance -> instance
				.group(
					Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
						.fieldOf("min_y")
						.forGetter(GenerationShapeConfig::minimumY),
					Codec.intRange(0, DimensionType.MAX_HEIGHT)
						.fieldOf("height")
						.forGetter(GenerationShapeConfig::height),
					Codec.intRange(1, 4)
						.fieldOf("size_horizontal")
						.forGetter(GenerationShapeConfig::horizontalSize),
					Codec.intRange(1, 4)
						.fieldOf("size_vertical")
						.forGetter(GenerationShapeConfig::verticalSize)
				)
				.apply(instance, GenerationShapeConfig::new)
		)
		.comapFlatMap(GenerationShapeConfig::checkHeight, Function.identity());

	public static final GenerationShapeConfig SURFACE = create(-64, 384, 1, 2);
	public static final GenerationShapeConfig NETHER = create(0, 128, 1, 2);
	public static final GenerationShapeConfig END = create(0, 128, 2, 1);
	public static final GenerationShapeConfig CAVES = create(-64, 192, 1, 2);
	public static final GenerationShapeConfig FLOATING_ISLANDS = create(0, 256, 2, 1);

	/**
	 * Создаёт конфигурацию с валидацией параметров высоты.
	 * Высота должна быть кратна 16, а сумма minimumY + height не должна превышать MAX_COLUMN_HEIGHT + 1.
	 */
	public static GenerationShapeConfig create(int minimumY, int height, int horizontalSize, int verticalSize) {
		GenerationShapeConfig config = new GenerationShapeConfig(minimumY, height, horizontalSize, verticalSize);
		checkHeight(config).error().ifPresent(error -> {
			throw new IllegalStateException(error.message());
		});
		return config;
	}

	public int verticalCellBlockCount() {
		return BiomeCoords.toBlock(verticalSize());
	}

	public int horizontalCellBlockCount() {
		return BiomeCoords.toBlock(horizontalSize());
	}

	public GenerationShapeConfig trimHeight(HeightLimitView world) {
		int trimmedMinY = Math.max(minimumY, world.getBottomY());
		int trimmedHeight = Math.min(minimumY + height, world.getTopYInclusive() + 1) - trimmedMinY;
		return new GenerationShapeConfig(trimmedMinY, trimmedHeight, horizontalSize, verticalSize);
	}

	private static DataResult<GenerationShapeConfig> checkHeight(GenerationShapeConfig config) {
		if (config.minimumY() + config.height() > DimensionType.MAX_COLUMN_HEIGHT + 1) {
			return DataResult.error(
				() -> "min_y + height cannot be higher than: " + (DimensionType.MAX_COLUMN_HEIGHT + 1)
			);
		}

		if (config.height() % 16 != 0) {
			return DataResult.error(() -> "height has to be a multiple of 16");
		}

		return config.minimumY() % 16 != 0
			? DataResult.error(() -> "min_y has to be a multiple of 16")
			: DataResult.success(config);
	}
}
