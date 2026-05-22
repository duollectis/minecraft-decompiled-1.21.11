package net.minecraft.client.font;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.function.IntFunction;

/**
 * Двухуровневый массив для хранения глифов по кодовым точкам Unicode.
 * Кодовая точка разбивается на старший байт (индекс строки) и младший байт (индекс в строке),
 * что обеспечивает O(1) доступ без хеширования и экономит память за счёт разделяемой пустой строки.
 */
@Environment(EnvType.CLIENT)
public class GlyphContainer<T> {

	private static final int ROW_SHIFT = 8;
	private static final int ENTRIES_PER_ROW = 256;
	private static final int LAST_ENTRY_NUM_IN_ROW = 255;
	private static final int LAST_ROW_NUM = 4351;
	private static final int NUM_ROWS = 4352;

	private final T[] defaultRow;
	private final @Nullable T[][] rows;
	private final IntFunction<T[]> makeRow;

	public GlyphContainer(IntFunction<T[]> makeRow, IntFunction<T[][]> makeScroll) {
		defaultRow = (T[]) ((Object[]) makeRow.apply(ENTRIES_PER_ROW));
		rows = (T[][]) ((Object[][]) makeScroll.apply(NUM_ROWS));
		Arrays.fill(rows, defaultRow);
		this.makeRow = makeRow;
	}

	public void clear() {
		Arrays.fill(rows, defaultRow);
	}

	public @Nullable T get(int codePoint) {
		int rowIndex = codePoint >> ROW_SHIFT;
		int colIndex = codePoint & LAST_ENTRY_NUM_IN_ROW;
		return rows[rowIndex][colIndex];
	}

	public @Nullable T put(int codePoint, T glyph) {
		int rowIndex = codePoint >> ROW_SHIFT;
		int colIndex = codePoint & LAST_ENTRY_NUM_IN_ROW;
		T[] row = rows[rowIndex];

		if (row == defaultRow) {
			row = (T[]) ((Object[]) makeRow.apply(ENTRIES_PER_ROW));
			rows[rowIndex] = row;
			row[colIndex] = glyph;
			return null;
		}

		T previous = row[colIndex];
		row[colIndex] = glyph;
		return previous;
	}

	/**
	 * Возвращает существующее значение для кодовой точки, либо вычисляет и сохраняет новое.
	 * Ленивая инициализация строки происходит только при первой записи в неё.
	 *
	 * @param codePoint кодовая точка Unicode
	 * @param ifAbsent функция для вычисления значения при его отсутствии
	 * @return существующее или только что вычисленное значение
	 */
	public T computeIfAbsent(int codePoint, IntFunction<T> ifAbsent) {
		int rowIndex = codePoint >> ROW_SHIFT;
		int colIndex = codePoint & LAST_ENTRY_NUM_IN_ROW;
		T[] row = rows[rowIndex];
		T existing = row[colIndex];

		if (existing != null) {
			return existing;
		}

		if (row == defaultRow) {
			row = (T[]) ((Object[]) makeRow.apply(ENTRIES_PER_ROW));
			rows[rowIndex] = row;
		}

		T computed = ifAbsent.apply(codePoint);
		row[colIndex] = computed;
		return computed;
	}

	public @Nullable T remove(int codePoint) {
		int rowIndex = codePoint >> ROW_SHIFT;
		int colIndex = codePoint & LAST_ENTRY_NUM_IN_ROW;
		T[] row = rows[rowIndex];

		if (row == defaultRow) {
			return null;
		}

		T removed = row[colIndex];
		row[colIndex] = null;
		return removed;
	}

	public void forEachGlyph(GlyphContainer.GlyphConsumer<T> glyphConsumer) {
		for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
			T[] row = rows[rowIndex];
			if (row == defaultRow) {
				continue;
			}

			for (int colIndex = 0; colIndex < row.length; colIndex++) {
				T glyph = row[colIndex];
				if (glyph != null) {
					int codePoint = rowIndex << ROW_SHIFT | colIndex;
					glyphConsumer.accept(codePoint, glyph);
				}
			}
		}
	}

	public IntSet getProvidedGlyphs() {
		IntOpenHashSet result = new IntOpenHashSet();
		forEachGlyph((codePoint, glyph) -> result.add(codePoint));
		return result;
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface GlyphConsumer<T> {

		void accept(int codePoint, T glyph);
	}
}
