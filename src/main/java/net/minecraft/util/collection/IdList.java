package net.minecraft.util.collection;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Список с двусторонним отображением объект ↔ числовой ID.
 * Поддерживает явное задание ID через {@link #set(Object, int)}
 * и автоматическое назначение через {@link #add(Object)}.
 *
 * @param <T> тип элементов
 */
public class IdList<T> implements IndexedIterable<T> {

	private int nextId;
	private final Reference2IntMap<T> idMap;
	private final List<T> list;

	public IdList() {
		this(512);
	}

	public IdList(int initialSize) {
		list = Lists.newArrayListWithExpectedSize(initialSize);
		idMap = new Reference2IntOpenHashMap<>(initialSize);
		idMap.defaultReturnValue(ABSENT_RAW_ID);
	}

	/**
	 * Регистрирует элемент с явно указанным ID.
	 * Если список короче, чем {@code id}, он расширяется нулями.
	 *
	 * @param value элемент для регистрации
	 * @param id    числовой идентификатор
	 */
	public void set(T value, int id) {
		idMap.put(value, id);

		while (list.size() <= id) {
			list.add(null);
		}

		list.set(id, value);

		if (nextId <= id) {
			nextId = id + 1;
		}
	}

	public void add(T value) {
		set(value, nextId);
	}

	@Override
	public int getRawId(T value) {
		return idMap.getInt(value);
	}

	@Override
	public final @Nullable T get(int index) {
		return index >= 0 && index < list.size() ? list.get(index) : null;
	}

	@Override
	public Iterator<T> iterator() {
		return Iterators.filter(list.iterator(), Objects::nonNull);
	}

	public boolean containsKey(int index) {
		return get(index) != null;
	}

	@Override
	public int size() {
		return idMap.size();
	}
}
