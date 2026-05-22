package net.minecraft.resource;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Отслеживает зависимости между объектами и обходит их в топологическом порядке
 * (сначала зависимости, затем зависящие от них).
 *
 * @param <K> тип ключа
 * @param <V> тип значения, реализующего {@link Dependencies}
 */
public class DependencyTracker<K, V extends DependencyTracker.Dependencies<K>> {

	private final Map<K, V> underlying = new HashMap<>();

	/**
	 * Добавляет элемент с его зависимостями в трекер.
	 *
	 * @param key   ключ элемента
	 * @param value значение с описанием зависимостей
	 * @return этот трекер (для цепочки вызовов)
	 */
	public DependencyTracker<K, V> add(K key, V value) {
		underlying.put(key, value);
		return this;
	}

	private void traverse(Multimap<K, K> parentChild, Set<K> visited, K rootKey, BiConsumer<K, V> callback) {
		if (!visited.add(rootKey)) {
			return;
		}

		parentChild.get(rootKey).forEach(child -> traverse(parentChild, visited, child, callback));

		V dependencies = underlying.get(rootKey);
		if (dependencies != null) {
			callback.accept(rootKey, dependencies);
		}
	}

	private static <K> boolean containsReverseDependency(Multimap<K, K> dependencies, K key, K dependency) {
		Collection<K> children = dependencies.get(dependency);
		return children.contains(key)
			|| children.stream().anyMatch(sub -> containsReverseDependency(dependencies, key, sub));
	}

	private static <K> void addDependency(Multimap<K, K> dependencies, K key, K dependency) {
		if (!containsReverseDependency(dependencies, key, dependency)) {
			dependencies.put(key, dependency);
		}
	}

	/**
	 * Обходит все элементы в топологическом порядке (зависимости раньше зависящих),
	 * вызывая {@code callback} для каждого.
	 *
	 * @param callback получатель пар ключ-значение в порядке обхода
	 */
	public void traverse(BiConsumer<K, V> callback) {
		Multimap<K, K> graph = HashMultimap.create();

		underlying.forEach((key, value) -> value.forDependencies(dep -> addDependency(graph, key, dep)));
		underlying.forEach((key, value) -> value.forOptionalDependencies(dep -> addDependency(graph, key, dep)));

		Set<K> visited = new HashSet<>();
		underlying.keySet().forEach(key -> traverse(graph, visited, key, callback));
	}

	/**
	 * Описывает зависимости элемента: обязательные и опциональные.
	 *
	 * @param <K> тип ключа зависимости
	 */
	public interface Dependencies<K> {

		/**
		 * Перечисляет обязательные зависимости данного элемента.
		 *
		 * @param callback получатель ключей зависимостей
		 */
		void forDependencies(Consumer<K> callback);

		/**
		 * Перечисляет опциональные зависимости данного элемента.
		 *
		 * @param callback получатель ключей зависимостей
		 */
		void forOptionalDependencies(Consumer<K> callback);
	}
}
