package net.minecraft.state.property;

import it.unimi.dsi.fastutil.ints.IntImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Свойство с целочисленным значением в диапазоне [{@code min}, {@code max}] включительно.
 *
 * <p>Минимальное значение не может быть отрицательным. Список значений строится
 * через {@link IntStream#range} и хранится в {@link IntImmutableList} для
 * эффективного доступа без boxing.
 *
 * <p>Метод {@link #ordinal(Integer)} реализован как {@code value - min}, что даёт O(1)
 * без поиска по списку. Возвращает {@code -1} для значений, выходящих за пределы диапазона.
 */
public final class IntProperty extends Property<Integer> {

	private final IntImmutableList values;
	private final int min;
	private final int max;

	private IntProperty(String name, int min, int max) {
		super(name, Integer.class);
		if (min < 0) {
			throw new IllegalArgumentException("Min value of " + name + " must be 0 or greater");
		}

		if (max <= min) {
			throw new IllegalArgumentException(
				"Max value of " + name + " must be greater than min (" + min + ")"
			);
		}

		this.min = min;
		this.max = max;
		this.values = IntImmutableList.toList(IntStream.rangeClosed(min, max));
	}

	public static IntProperty of(String name, int min, int max) {
		return new IntProperty(name, min, max);
	}

	@Override
	public List<Integer> getValues() {
		return values;
	}

	@Override
	public Optional<Integer> parse(String name) {
		try {
			int parsed = Integer.parseInt(name);
			return parsed >= min && parsed <= max ? Optional.of(parsed) : Optional.empty();
		} catch (NumberFormatException ignored) {
			return Optional.empty();
		}
	}

	@Override
	public String name(Integer value) {
		return value.toString();
	}

	/**
	 * Возвращает порядковый номер значения как {@code value - min}.
	 * Если значение превышает {@link #max}, возвращает {@code -1} (недопустимое значение).
	 */
	@Override
	public int ordinal(Integer value) {
		return value <= max ? value - min : -1;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		return other instanceof IntProperty intProperty
			&& super.equals(other)
			&& values.equals(intProperty.values);
	}

	@Override
	public int computeHashCode() {
		return 31 * super.computeHashCode() + values.hashCode();
	}
}
