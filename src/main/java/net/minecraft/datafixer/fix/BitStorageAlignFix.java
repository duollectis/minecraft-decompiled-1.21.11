package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.stream.LongStream;

/**
 * Перевыравнивает упакованные битовые массивы (BlockStates, Heightmaps) в чанках
 * из старого формата, где элементы могли пересекать границы long, в новый выровненный
 * формат, где каждый long содержит только целое число элементов без пересечений.
 */
public class BitStorageAlignFix extends DataFix {

	private static final int MAX_BLOCK_STATE_ID = 4096;
	private static final int MAX_HEIGHT_VALUE = 256;
	private static final int HEIGHT_VALUE_BITS = 9;
	private static final int MIN_PALETTE_BITS = 4;
	private static final int BITS_PER_LONG = 64;

	public BitStorageAlignFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		Type<?> levelType = chunkType.findFieldType("Level");
		OpticFinder<?> levelFinder = DSL.fieldFinder("Level", levelType);
		OpticFinder<?> sectionsFinder = levelFinder.type().findField("Sections");
		Type<?> sectionType = ((ListType<?>) sectionsFinder.type()).getElement();
		OpticFinder<?> sectionFinder = DSL.typeFinder(sectionType);
		Type<Pair<String, Dynamic<?>>> blockStateType = DSL.named(TypeReferences.BLOCK_STATE.typeName(), DSL.remainderType());
		OpticFinder<List<Pair<String, Dynamic<?>>>> paletteFinder = DSL.fieldFinder("Palette", DSL.list(blockStateType));

		return fixTypeEverywhereTyped(
			"BitStorageAlignFix",
			chunkType,
			getOutputSchema().getType(TypeReferences.CHUNK),
			chunk -> chunk.updateTyped(
				levelFinder,
				level -> fixHeightmaps(fixLevel(sectionsFinder, sectionFinder, paletteFinder, level))
			)
		);
	}

	private Typed<?> fixHeightmaps(Typed<?> level) {
		return level.update(
			DSL.remainderFinder(),
			levelDynamic -> levelDynamic.update(
				"Heightmaps",
				heightmaps -> heightmaps.updateMapValues(
					entry -> entry.mapSecond(
						heightmap -> fixBitStorageArray(levelDynamic, heightmap, MAX_HEIGHT_VALUE, HEIGHT_VALUE_BITS)
					)
				)
			)
		);
	}

	private static Typed<?> fixLevel(
		OpticFinder<?> sectionsFinder,
		OpticFinder<?> sectionFinder,
		OpticFinder<List<Pair<String, Dynamic<?>>>> paletteFinder,
		Typed<?> level
	) {
		return level.updateTyped(
			sectionsFinder,
			sections -> sections.updateTyped(
				sectionFinder,
				section -> {
					int paletteBits = section.getOptional(paletteFinder)
						.map(palette -> Math.max(MIN_PALETTE_BITS, DataFixUtils.ceillog2(palette.size())))
						.orElse(0);

					if (paletteBits == 0 || MathHelper.isPowerOfTwo(paletteBits)) {
						return section;
					}

					return section.update(
						DSL.remainderFinder(),
						sectionDynamic -> sectionDynamic.update(
							"BlockStates",
							states -> fixBitStorageArray(sectionDynamic, states, MAX_BLOCK_STATE_ID, paletteBits)
						)
					);
				}
			)
		);
	}

	private static Dynamic<?> fixBitStorageArray(
		Dynamic<?> context,
		Dynamic<?> storage,
		int maxValue,
		int elementBits
	) {
		long[] packed = storage.asLongStream().toArray();
		long[] repacked = resizePackedIntArray(maxValue, elementBits, packed);
		return context.createLongList(LongStream.of(repacked));
	}

	/**
	 * Перепаковывает массив long[], хранящий целые числа по elementBits бит каждое,
	 * из старого формата (элементы могут пересекать границы long) в новый выровненный формат
	 * (каждый long содержит только целое число элементов, без пересечений).
	 *
	 * @param maxValue    максимальное количество элементов (определяет размер выходного массива)
	 * @param elementBits количество бит на один элемент
	 * @param elements    входной массив в старом формате
	 */
	public static long[] resizePackedIntArray(int maxValue, int elementBits, long[] elements) {
		if (elements.length == 0) {
			return elements;
		}

		long mask = (1L << elementBits) - 1L;
		int elemsPerLong = BITS_PER_LONG / elementBits;
		int outputSize = (maxValue + elemsPerLong - 1) / elemsPerLong;
		long[] output = new long[outputSize];

		int outputIndex = 0;
		int bitOffset = 0;
		long currentOutput = 0L;
		int currentInputIndex = 0;
		long currentInput = elements[0];
		long nextInput = elements.length > 1 ? elements[1] : 0L;

		for (int elemIndex = 0; elemIndex < maxValue; elemIndex++) {
			int bitPos = elemIndex * elementBits;
			int longIndex = bitPos >> 6;
			int nextLongIndex = (elemIndex + 1) * elementBits - 1 >> 6;
			int bitShift = bitPos ^ longIndex << 6;

			if (longIndex != currentInputIndex) {
				currentInput = nextInput;
				nextInput = longIndex + 1 < elements.length ? elements[longIndex + 1] : 0L;
				currentInputIndex = longIndex;
			}

			long value;

			if (longIndex == nextLongIndex) {
				value = currentInput >>> bitShift & mask;
			} else {
				int bitsInCurrent = BITS_PER_LONG - bitShift;
				value = (currentInput >>> bitShift | nextInput << bitsInCurrent) & mask;
			}

			int nextBitOffset = bitOffset + elementBits;

			if (nextBitOffset >= BITS_PER_LONG) {
				output[outputIndex++] = currentOutput;
				currentOutput = value;
				bitOffset = elementBits;
			} else {
				currentOutput |= value << bitOffset;
				bitOffset = nextBitOffset;
			}
		}

		if (currentOutput != 0L) {
			output[outputIndex] = currentOutput;
		}

		return output;
	}
}
