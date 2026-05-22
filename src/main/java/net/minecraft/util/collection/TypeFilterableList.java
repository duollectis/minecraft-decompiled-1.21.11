package net.minecraft.util.collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.util.Util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Список с поддержкой фильтрации по типу элементов.
 * При добавлении элемент автоматически регистрируется во всех подходящих
 * типовых подсписках. Подсписки создаются лениво при первом запросе.
 *
 * @param <T> базовый тип элементов
 */
public class TypeFilterableList<T> extends AbstractCollection<T> {

	private final Map<Class<?>, List<T>> elementsByType = Maps.newHashMap();
	private final Class<T> elementType;
	private final List<T> allElements = Lists.newArrayList();

	public TypeFilterableList(Class<T> elementType) {
		this.elementType = elementType;
		elementsByType.put(elementType, allElements);
	}

	@Override
	public boolean add(T element) {
		boolean added = false;

		for (Entry<Class<?>, List<T>> entry : elementsByType.entrySet()) {
			if (entry.getKey().isInstance(element)) {
				added |= entry.getValue().add(element);
			}
		}

		return added;
	}

	@Override
	public boolean remove(Object object) {
		boolean removed = false;

		for (Entry<Class<?>, List<T>> entry : elementsByType.entrySet()) {
			if (entry.getKey().isInstance(object)) {
				removed |= entry.getValue().remove(object);
			}
		}

		return removed;
	}

	@Override
	public boolean contains(Object object) {
		return getAllOfType(object.getClass()).contains(object);
	}

	/**
	 * Возвращает неизменяемую коллекцию всех элементов указанного типа.
	 * Подсписок создаётся лениво при первом обращении.
	 *
	 * @param type тип для фильтрации (должен быть подтипом базового типа)
	 * @throws IllegalArgumentException если {@code type} не является подтипом базового типа
	 */
	@SuppressWarnings("unchecked")
	public <S> Collection<S> getAllOfType(Class<S> type) {
		if (!elementType.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Don't know how to search for " + type);
		}

		List<? extends T> filtered = elementsByType.computeIfAbsent(
			type,
			typeClass -> allElements
				.stream()
				.filter(typeClass::isInstance)
				.collect(Util.toArrayList())
		);

		return (Collection<S>) Collections.unmodifiableCollection(filtered);
	}

	@Override
	public Iterator<T> iterator() {
		return allElements.isEmpty()
			? Collections.emptyIterator()
			: Iterators.unmodifiableIterator(allElements.iterator());
	}

	public List<T> copy() {
		return ImmutableList.copyOf(allElements);
	}

	@Override
	public int size() {
		return allElements.size();
	}
}
