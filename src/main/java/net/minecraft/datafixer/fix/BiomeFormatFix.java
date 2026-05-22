package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Конвертирует плоский 2D массив биомов (256 элементов, 16×16 блоков) в 3D формат
 * (1024 элемента, 4×64×4 ячейки), введённый в 1.16 для поддержки вертикальных биомов.
 */
public class BiomeFormatFix extends DataFix {

	/** Размер старого плоского массива биомов (16x16 блоков). */
	private static final int LEGACY_BIOME_ARRAY_SIZE = 256;

	/** Размер нового 3D массива биомов (4x64x4 = 1024 ячейки). */
	private static final int NEW_BIOME_ARRAY_SIZE = 1024;

	/** Количество вертикальных секций для копирования (64 / 4 = 16, но здесь 64 слоя по 16 ячеек). */
	private static final int VERTICAL_COPY_COUNT = 64;

	/** Горизонтальный размер биом-сетки (4x4 ячейки на чанк). */
	private static final int BIOME_GRID_SIZE = 4;

	public BiomeFormatFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		OpticFinder<?> levelFinder = chunkType.findField("Level");

		return fixTypeEverywhereTyped(
			"Leaves fix",
			chunkType,
			typed -> typed.updateTyped(
				levelFinder,
				levelTyped -> levelTyped.update(DSL.remainderFinder(), this::convertBiomes)
			)
		);
	}

	/**
	 * Конвертирует плоский массив биомов 16x16 (256 элементов) в 3D формат 4x64x4 (1024 элемента).
	 * Для каждой из 4x4 горизонтальных ячеек берётся центральный биом из старого массива
	 * и копируется на все 64 вертикальных уровня.
	 */
	private Dynamic<?> convertBiomes(Dynamic<?> level) {
		Optional<IntStream> biomesOpt = level.get("Biomes").asIntStreamOpt().result();

		if (biomesOpt.isEmpty()) {
			return level;
		}

		int[] oldBiomes = biomesOpt.get().toArray();

		if (oldBiomes.length != LEGACY_BIOME_ARRAY_SIZE) {
			return level;
		}

		int[] newBiomes = new int[NEW_BIOME_ARRAY_SIZE];

		for (int x = 0; x < BIOME_GRID_SIZE; x++) {
			for (int z = 0; z < BIOME_GRID_SIZE; z++) {
				int centerX = (z << 2) + 2;
				int centerZ = (x << 2) + 2;
				int oldIndex = centerZ << 4 | centerX;
				newBiomes[x << 2 | z] = oldBiomes[oldIndex];
			}
		}

		for (int layer = 1; layer < VERTICAL_COPY_COUNT; layer++) {
			System.arraycopy(newBiomes, 0, newBiomes, layer * BIOME_GRID_SIZE * BIOME_GRID_SIZE, BIOME_GRID_SIZE * BIOME_GRID_SIZE);
		}

		return level.set("Biomes", level.createIntList(Arrays.stream(newBiomes)));
	}
}
