package net.minecraft.util;

import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Утилита для топологической сортировки ориентированного графа.
 * Использует алгоритм обхода в глубину (DFS) с отслеживанием посещённых вершин.
 */
public final class TopologicalSorts {

	private TopologicalSorts() {
	}

	/**
	 * Рекурсивно обходит граф от вершины {@code current} и добавляет её в обратном порядке.
	 * Возвращает {@code true}, если обнаружен цикл (вершина уже находится в стеке обхода).
	 *
	 * @param successors           карта смежности: вершина → множество её преемников
	 * @param visited              множество полностью обработанных вершин
	 * @param visiting             множество вершин, находящихся в текущем стеке DFS
	 * @param reversedOrderConsumer потребитель вершин в обратном топологическом порядке
	 * @param current              текущая обрабатываемая вершина
	 * @return {@code true} при обнаружении цикла, иначе {@code false}
	 */
	public static <T> boolean sort(
			Map<T, Set<T>> successors,
			Set<T> visited,
			Set<T> visiting,
			Consumer<T> reversedOrderConsumer,
			T current
	) {
		if (visited.contains(current)) {
			return false;
		}

		if (visiting.contains(current)) {
			return true;
		}

		visiting.add(current);

		for (T successor : successors.getOrDefault(current, ImmutableSet.of())) {
			if (sort(successors, visited, visiting, reversedOrderConsumer, successor)) {
				return true;
			}
		}

		visiting.remove(current);
		visited.add(current);
		reversedOrderConsumer.accept(current);
		return false;
	}
}
