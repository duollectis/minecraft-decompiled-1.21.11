package net.minecraft.block.pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Строитель трёхмерного шаблона блоков {@link BlockPattern}.
 * <p>
 * Шаблон задаётся последовательностью «проходов» (aisle) — двумерных срезов,
 * где каждый символ строки соответствует предикату, зарегистрированному через {@link #where}.
 * Пробел {@code ' '} зарезервирован как «любой блок» и не требует явной регистрации.
 */
public class BlockPatternBuilder {

	private final List<String[]> aisles = Lists.newArrayList();
	private final Map<Character, Predicate<@Nullable CachedBlockPosition>> charMap = Maps.newHashMap();
	private int height;
	private int width;
	private final CharSet keysMissingPredicates = new CharOpenHashSet();

	private BlockPatternBuilder() {
		charMap.put(' ', pos -> true);
	}

	/**
	 * Добавляет очередной «проход» (срез по оси глубины) к шаблону.
	 * <p>
	 * Все проходы должны иметь одинаковые размеры (высоту и ширину).
	 * Символы, для которых ещё не зарегистрированы предикаты, помечаются
	 * как отсутствующие и вызовут ошибку при вызове {@link #build()}.
	 *
	 * @param pattern строки прохода, где каждый символ — ключ предиката
	 * @return этот строитель для цепочки вызовов
	 * @throws IllegalArgumentException если проход пустой или не совпадает по размерам с предыдущими
	 */
	public BlockPatternBuilder aisle(String... pattern) {
		if (ArrayUtils.isEmpty(pattern) || StringUtils.isEmpty(pattern[0])) {
			throw new IllegalArgumentException("Empty pattern for aisle");
		}

		if (aisles.isEmpty()) {
			height = pattern.length;
			width = pattern[0].length();
		}

		if (pattern.length != height) {
			throw new IllegalArgumentException(
				"Expected aisle with height of " + height + ", but was given one with a height of " + pattern.length + ")"
			);
		}

		for (String row : pattern) {
			if (row.length() != width) {
				throw new IllegalArgumentException(
					"Not all rows in the given aisle are the correct width (expected " + width
						+ ", found one with " + row.length() + ")"
				);
			}

			for (char symbol : row.toCharArray()) {
				if (charMap.containsKey(symbol) == false) {
					keysMissingPredicates.add(symbol);
				}
			}
		}

		aisles.add(pattern);
		return this;
	}

	/**
	 * Создаёт новый экземпляр строителя.
	 *
	 * @return новый {@link BlockPatternBuilder}
	 */
	public static BlockPatternBuilder start() {
		return new BlockPatternBuilder();
	}

	/**
	 * Регистрирует предикат для символа шаблона.
	 *
	 * @param key       символ, используемый в строках {@link #aisle}
	 * @param predicate условие проверки блока; получает {@code null} если чанк не загружен
	 * @return этот строитель для цепочки вызовов
	 */
	public BlockPatternBuilder where(char key, Predicate<@Nullable CachedBlockPosition> predicate) {
		charMap.put(key, predicate);
		keysMissingPredicates.remove(key);
		return this;
	}

	/**
	 * Собирает и возвращает готовый {@link BlockPattern}.
	 *
	 * @return скомпилированный шаблон блоков
	 * @throws IllegalStateException если для каких-либо символов не зарегистрированы предикаты
	 */
	public BlockPattern build() {
		return new BlockPattern(bakePredicates());
	}

	@SuppressWarnings("unchecked")
	private Predicate<CachedBlockPosition>[][][] bakePredicates() {
		if (keysMissingPredicates.isEmpty() == false) {
			throw new IllegalStateException(
				"Predicates for character(s) " + keysMissingPredicates + " are missing"
			);
		}

		Predicate<CachedBlockPosition>[][][] predicates = (Predicate<CachedBlockPosition>[][][]) Array.newInstance(
			Predicate.class,
			aisles.size(),
			height,
			width
		);

		for (int aisleIndex = 0; aisleIndex < aisles.size(); aisleIndex++) {
			for (int rowIndex = 0; rowIndex < height; rowIndex++) {
				for (int colIndex = 0; colIndex < width; colIndex++) {
					predicates[aisleIndex][rowIndex][colIndex] =
						charMap.get(aisles.get(aisleIndex)[rowIndex].charAt(colIndex));
				}
			}
		}

		return predicates;
	}
}
