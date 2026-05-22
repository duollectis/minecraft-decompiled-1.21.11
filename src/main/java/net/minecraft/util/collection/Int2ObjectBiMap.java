package net.minecraft.util.collection;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Двунаправленная карта int ↔ объект, реализованная через открытую адресацию.
 * Поддерживает отображение как объект→ID, так и ID→объект.
 * Использует идентичность объектов (reference equality) для сравнения ключей.
 *
 * @param <K> тип значений карты
 */
public class Int2ObjectBiMap<K> implements IndexedIterable<K> {

	private static final int ABSENT = -1;
	private static final float LOAD_FACTOR = 0.8F;

	private @Nullable K[] values;
	private int[] ids;
	private @Nullable K[] idToValues;
	private int nextId;
	private int size;

	private Int2ObjectBiMap(int capacity) {
		values = (K[]) new Object[capacity];
		ids = new int[capacity];
		idToValues = (K[]) new Object[capacity];
	}

	private Int2ObjectBiMap(K[] values, int[] ids, K[] idToValues, int nextId, int size) {
		this.values = values;
		this.ids = ids;
		this.idToValues = idToValues;
		this.nextId = nextId;
		this.size = size;
	}

	public static <A> Int2ObjectBiMap<A> create(int expectedSize) {
		return new Int2ObjectBiMap<>((int) (expectedSize / LOAD_FACTOR));
	}

	@Override
	public int getRawId(@Nullable K value) {
		return getIdFromIndex(findIndex(value, getIdealIndex(value)));
	}

	@Override
	public @Nullable K get(int index) {
		return index >= 0 && index < idToValues.length ? idToValues[index] : null;
	}

	private int getIdFromIndex(int index) {
		return index == ABSENT ? ABSENT : ids[index];
	}

	public boolean contains(K value) {
		return getRawId(value) != ABSENT;
	}

	public boolean containsKey(int index) {
		return get(index) != null;
	}

	public int add(K value) {
		int id = nextId();
		put(value, id);
		return id;
	}

	private int nextId() {
		while (nextId < idToValues.length && idToValues[nextId] != null) {
			nextId++;
		}

		return nextId;
	}

	private void resize(int newCapacity) {
		K[] oldValues = values;
		int[] oldIds = ids;
		Int2ObjectBiMap<K> resized = new Int2ObjectBiMap<>(newCapacity);

		for (int i = 0; i < oldValues.length; i++) {
			if (oldValues[i] != null) {
				resized.put(oldValues[i], oldIds[i]);
			}
		}

		values = resized.values;
		ids = resized.ids;
		idToValues = resized.idToValues;
		nextId = resized.nextId;
		size = resized.size;
	}

	/**
	 * Регистрирует значение с явно указанным ID.
	 * При необходимости расширяет внутренние массивы.
	 *
	 * @param value значение для регистрации
	 * @param id    числовой идентификатор
	 */
	public void put(K value, int id) {
		int requiredCapacity = Math.max(id, size + 1);

		if (requiredCapacity >= values.length * LOAD_FACTOR) {
			int newCapacity = values.length << 1;

			while (newCapacity < id) {
				newCapacity <<= 1;
			}

			resize(newCapacity);
		}

		int slot = findFree(getIdealIndex(value));
		values[slot] = value;
		ids[slot] = id;
		idToValues[id] = value;
		size++;

		if (id == nextId) {
			nextId++;
		}
	}

	private int getIdealIndex(@Nullable K value) {
		return (MathHelper.idealHash(System.identityHashCode(value)) & Integer.MAX_VALUE) % values.length;
	}

	/**
	 * Ищет слот, содержащий указанное значение, начиная с позиции {@code startIndex}.
	 * Использует линейное зондирование с обёрткой по кругу.
	 */
	private int findIndex(@Nullable K value, int startIndex) {
		for (int i = startIndex; i < values.length; i++) {
			if (values[i] == value) {
				return i;
			}

			if (values[i] == null) {
				return ABSENT;
			}
		}

		for (int i = 0; i < startIndex; i++) {
			if (values[i] == value) {
				return i;
			}

			if (values[i] == null) {
				return ABSENT;
			}
		}

		return ABSENT;
	}

	private int findFree(int startIndex) {
		for (int i = startIndex; i < values.length; i++) {
			if (values[i] == null) {
				return i;
			}
		}

		for (int i = 0; i < startIndex; i++) {
			if (values[i] == null) {
				return i;
			}
		}

		throw new RuntimeException("Overflowed :(");
	}

	@Override
	public Iterator<K> iterator() {
		return Iterators.filter(Iterators.forArray(idToValues), Predicates.notNull());
	}

	public void clear() {
		Arrays.fill(values, null);
		Arrays.fill(idToValues, null);
		nextId = 0;
		size = 0;
	}

	@Override
	public int size() {
		return size;
	}

	public Int2ObjectBiMap<K> copy() {
		return new Int2ObjectBiMap<>(
			(K[]) values.clone(),
			ids.clone(),
			(K[]) idToValues.clone(),
			nextId,
			size
		);
	}
}
