package net.minecraft.recipe;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectIterable;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Реализует двудольное сопоставление (bipartite matching) между доступными предметами
 * и ингредиентами рецепта. Используется для проверки возможности крафта и подсчёта
 * максимального количества крафтов с учётом имеющихся ресурсов.
 */
public class RecipeMatcher<T> {

	public final Reference2IntOpenHashMap<T> available = new Reference2IntOpenHashMap();

	boolean hasAtLeast(T input, int minimum) {
		return available.getInt(input) >= minimum;
	}

	void consume(T input, int count) {
		int previous = available.addTo(input, -count);

		if (previous < count) {
			throw new IllegalStateException("Took " + count + " items, but only had " + previous);
		}
	}

	void addInput(T input, int count) {
		available.addTo(input, count);
	}

	public boolean match(
		List<? extends RecipeMatcher.RawIngredient<T>> ingredients,
		int quantity,
		RecipeMatcher.@Nullable ItemCallback<T> itemCallback
	) {
		return new Matcher(ingredients).match(quantity, itemCallback);
	}

	public int countCrafts(
		List<? extends RecipeMatcher.RawIngredient<T>> ingredients,
		int max,
		RecipeMatcher.@Nullable ItemCallback<T> itemCallback
	) {
		return new Matcher(ingredients).countCrafts(max, itemCallback);
	}

	public void clear() {
		available.clear();
	}

	public void add(T input, int count) {
		addInput(input, count);
	}

	List<T> createItemRequirementList(Iterable<? extends RecipeMatcher.RawIngredient<T>> ingredients) {
		List<T> result = new ArrayList<>();
		ObjectIterator<Entry<T>> iterator = Reference2IntMaps.fastIterable(available).iterator();

		while (iterator.hasNext()) {
			Entry<T> entry = iterator.next();

			if (entry.getIntValue() > 0 && anyAccept(ingredients, entry.getKey())) {
				result.add(entry.getKey());
			}
		}

		return result;
	}

	private static <T> boolean anyAccept(Iterable<? extends RecipeMatcher.RawIngredient<T>> ingredients, T item) {
		for (RecipeMatcher.RawIngredient<T> ingredient : ingredients) {
			if (ingredient.acceptsItem(item)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Вычисляет верхнюю оценку максимального числа крафтов без учёта двудольного сопоставления:
	 * для каждого ингредиента берётся максимальное количество подходящего предмета.
	 * Реальный максимум может быть меньше из-за конкуренции ингредиентов за одни предметы.
	 */
	@VisibleForTesting
	public int getMaximumCrafts(List<? extends RecipeMatcher.RawIngredient<T>> ingredients) {
		int minimum = Integer.MAX_VALUE;
		ObjectIterable<Entry<T>> entries = Reference2IntMaps.fastIterable(available);

		outer:
		for (RecipeMatcher.RawIngredient<T> ingredient : ingredients) {
			int bestCount = 0;
			ObjectIterator<Entry<T>> iterator = entries.iterator();

			while (iterator.hasNext()) {
				Entry<T> entry = iterator.next();
				int count = entry.getIntValue();

				if (count > bestCount && ingredient.acceptsItem(entry.getKey())) {
					bestCount = count;

					if (bestCount >= minimum) {
						continue outer;
					}
				}
			}

			minimum = bestCount;

			if (bestCount == 0) {
				break;
			}
		}

		return minimum;
	}

	@FunctionalInterface
	public interface ItemCallback<T> {

		void accept(T item);
	}

	@FunctionalInterface
	public interface RawIngredient<T> {

		boolean acceptsItem(T entry);
	}

	/**
	 * Реализует алгоритм поиска увеличивающего пути (augmenting path) в двудольном графе
	 * для сопоставления ингредиентов рецепта с доступными предметами.
	 * Состояние хранится компактно в одном {@link BitSet} для минимизации аллокаций.
	 */
	class Matcher {

		private final List<? extends RecipeMatcher.RawIngredient<T>> ingredients;
		private final int totalIngredients;
		private final List<T> requiredItems;
		private final int totalRequiredItems;
		private final BitSet bits;
		private final IntList ingredientItemLookup = new IntArrayList();

		public Matcher(final List<? extends RecipeMatcher.RawIngredient<T>> ingredients) {
			this.ingredients = ingredients;
			totalIngredients = ingredients.size();
			requiredItems = RecipeMatcher.this.createItemRequirementList(ingredients);
			totalRequiredItems = requiredItems.size();
			bits = new BitSet(
				getVisitedIngredientIndexCount()
					+ getVisitedItemIndexCount()
					+ getRequirementIndexCount()
					+ getItemMatchIndexCount()
					+ getMissingIndexCount()
			);
			initItemMatch();
		}

		private void initItemMatch() {
			for (int ingredientIndex = 0; ingredientIndex < totalIngredients; ingredientIndex++) {
				RecipeMatcher.RawIngredient<T> ingredient = ingredients.get(ingredientIndex);

				for (int itemIndex = 0; itemIndex < totalRequiredItems; itemIndex++) {
					if (ingredient.acceptsItem(requiredItems.get(itemIndex))) {
						setMatch(itemIndex, ingredientIndex);
					}
				}
			}
		}

		/**
		 * Выполняет двудольное сопоставление для {@code quantity} крафтов.
		 * Использует алгоритм Хопкрофта-Карпа (поиск увеличивающих путей).
		 * Возвращает {@code true}, если все ингредиенты удалось сопоставить.
		 */
		public boolean match(int quantity, RecipeMatcher.@Nullable ItemCallback<T> itemCallback) {
			if (quantity <= 0) {
				return true;
			}

			int matchedCount = 0;

			while (true) {
				IntList path = tryFindIngredientItemLookup(quantity);

				if (path == null) {
					boolean allMatched = matchedCount == totalIngredients;
					boolean shouldCallback = allMatched && itemCallback != null;
					clearVisited();
					clearRequirements();

					for (int ingredientIndex = 0; ingredientIndex < totalIngredients; ingredientIndex++) {
						for (int itemIndex = 0; itemIndex < totalRequiredItems; itemIndex++) {
							if (isMissing(itemIndex, ingredientIndex)) {
								markNotMissing(itemIndex, ingredientIndex);
								RecipeMatcher.this.addInput(requiredItems.get(itemIndex), quantity);

								if (shouldCallback) {
									itemCallback.accept(requiredItems.get(itemIndex));
								}

								break;
							}
						}
					}

					assert bits
						.get(getMissingIndexOffset(), getMissingIndexOffset() + getMissingIndexCount())
						.isEmpty();

					return allMatched;
				}

				int itemIndex = path.getInt(0);
				RecipeMatcher.this.consume(requiredItems.get(itemIndex), quantity);
				int lastIndex = path.size() - 1;
				unfulfillRequirement(path.getInt(lastIndex));
				matchedCount++;

				for (int pathIndex = 0; pathIndex < path.size() - 1; pathIndex++) {
					if (isItem(pathIndex)) {
						int item = path.getInt(pathIndex);
						int ingredient = path.getInt(pathIndex + 1);
						markMissing(item, ingredient);
					} else {
						int item = path.getInt(pathIndex + 1);
						int ingredient = path.getInt(pathIndex);
						markNotMissing(item, ingredient);
					}
				}
			}
		}

		private static boolean isItem(int index) {
			return (index & 1) == 0;
		}

		private @Nullable IntList tryFindIngredientItemLookup(int min) {
			clearVisited();

			for (int itemIndex = 0; itemIndex < totalRequiredItems; itemIndex++) {
				if (RecipeMatcher.this.hasAtLeast(requiredItems.get(itemIndex), min)) {
					IntList path = findIngredientItemLookup(itemIndex);

					if (path != null) {
						return path;
					}
				}
			}

			return null;
		}

		private @Nullable IntList findIngredientItemLookup(int itemIndex) {
			ingredientItemLookup.clear();
			markItemVisited(itemIndex);
			ingredientItemLookup.add(itemIndex);

			while (!ingredientItemLookup.isEmpty()) {
				int size = ingredientItemLookup.size();

				if (isItem(size - 1)) {
					int item = ingredientItemLookup.getInt(size - 1);

					for (int ingredientIndex = 0; ingredientIndex < totalIngredients; ingredientIndex++) {
						if (!hasVisitedIngredient(ingredientIndex)
							&& matches(item, ingredientIndex)
							&& !isMissing(item, ingredientIndex)
						) {
							markIngredientVisited(ingredientIndex);
							ingredientItemLookup.add(ingredientIndex);
							break;
						}
					}
				} else {
					int ingredientIndex = ingredientItemLookup.getInt(size - 1);

					if (!getRequirement(ingredientIndex)) {
						return ingredientItemLookup;
					}

					for (int itemIdx = 0; itemIdx < totalRequiredItems; itemIdx++) {
						if (!isRequirementUnfulfilled(itemIdx) && isMissing(itemIdx, ingredientIndex)) {
							assert matches(itemIdx, ingredientIndex);

							markItemVisited(itemIdx);
							ingredientItemLookup.add(itemIdx);
							break;
						}
					}
				}

				int newSize = ingredientItemLookup.size();

				if (newSize == size) {
					ingredientItemLookup.removeInt(newSize - 1);
				}
			}

			return null;
		}

		private int getVisitedIngredientIndexOffset() {
			return 0;
		}

		private int getVisitedIngredientIndexCount() {
			return totalIngredients;
		}

		private int getVisitedItemIndexOffset() {
			return getVisitedIngredientIndexOffset() + getVisitedIngredientIndexCount();
		}

		private int getVisitedItemIndexCount() {
			return totalRequiredItems;
		}

		private int getRequirementIndexOffset() {
			return getVisitedItemIndexOffset() + getVisitedItemIndexCount();
		}

		private int getRequirementIndexCount() {
			return totalIngredients;
		}

		private int getItemMatchIndexOffset() {
			return getRequirementIndexOffset() + getRequirementIndexCount();
		}

		private int getItemMatchIndexCount() {
			return totalIngredients * totalRequiredItems;
		}

		private int getMissingIndexOffset() {
			return getItemMatchIndexOffset() + getItemMatchIndexCount();
		}

		private int getMissingIndexCount() {
			return totalIngredients * totalRequiredItems;
		}

		private boolean getRequirement(int ingredientId) {
			return bits.get(getRequirementIndex(ingredientId));
		}

		private void unfulfillRequirement(int ingredientId) {
			bits.set(getRequirementIndex(ingredientId));
		}

		private int getRequirementIndex(int ingredientId) {
			assert ingredientId >= 0 && ingredientId < totalIngredients;

			return getRequirementIndexOffset() + ingredientId;
		}

		private void clearRequirements() {
			clear(getRequirementIndexOffset(), getRequirementIndexCount());
		}

		private void setMatch(int itemIndex, int ingredientIndex) {
			bits.set(getMatchIndex(itemIndex, ingredientIndex));
		}

		private boolean matches(int itemIndex, int ingredientIndex) {
			return bits.get(getMatchIndex(itemIndex, ingredientIndex));
		}

		private int getMatchIndex(int itemIndex, int ingredientIndex) {
			assert itemIndex >= 0 && itemIndex < totalRequiredItems;
			assert ingredientIndex >= 0 && ingredientIndex < totalIngredients;

			return getItemMatchIndexOffset() + itemIndex * totalIngredients + ingredientIndex;
		}

		private boolean isMissing(int itemIndex, int ingredientIndex) {
			return bits.get(getMissingIndex(itemIndex, ingredientIndex));
		}

		private void markMissing(int itemIndex, int ingredientIndex) {
			int index = getMissingIndex(itemIndex, ingredientIndex);

			assert !bits.get(index);

			bits.set(index);
		}

		private void markNotMissing(int itemIndex, int ingredientIndex) {
			int index = getMissingIndex(itemIndex, ingredientIndex);

			assert bits.get(index);

			bits.clear(index);
		}

		private int getMissingIndex(int itemIndex, int ingredientIndex) {
			assert itemIndex >= 0 && itemIndex < totalRequiredItems;
			assert ingredientIndex >= 0 && ingredientIndex < totalIngredients;

			return getMissingIndexOffset() + itemIndex * totalIngredients + ingredientIndex;
		}

		private void markIngredientVisited(int index) {
			bits.set(getVisitedIngredientIndex(index));
		}

		private boolean hasVisitedIngredient(int index) {
			return bits.get(getVisitedIngredientIndex(index));
		}

		private int getVisitedIngredientIndex(int index) {
			assert index >= 0 && index < totalIngredients;

			return getVisitedIngredientIndexOffset() + index;
		}

		private void markItemVisited(int index) {
			bits.set(getVisitedItemIndex(index));
		}

		private boolean isRequirementUnfulfilled(int index) {
			return bits.get(getVisitedItemIndex(index));
		}

		private int getVisitedItemIndex(int index) {
			assert index >= 0 && index < totalRequiredItems;

			return getVisitedItemIndexOffset() + index;
		}

		private void clearVisited() {
			clear(getVisitedIngredientIndexOffset(), getVisitedIngredientIndexCount());
			clear(getVisitedItemIndexOffset(), getVisitedItemIndexCount());
		}

		private void clear(int start, int count) {
			bits.clear(start, start + count);
		}

		/**
		 * Бинарным поиском находит максимальное количество крафтов в диапазоне [0, max].
		 * После нахождения максимума вызывает {@code match} с колбэком для уведомления
		 * о потреблённых предметах.
		 */
		public int countCrafts(int max, RecipeMatcher.@Nullable ItemCallback<T> itemCallback) {
			int low = 0;
			int high = Math.min(max, RecipeMatcher.this.getMaximumCrafts(ingredients)) + 1;

			while (true) {
				int mid = (low + high) / 2;

				if (match(mid, null)) {
					if (high - low <= 1) {
						if (mid > 0) {
							match(mid, itemCallback);
						}

						return mid;
					}

					low = mid;
				} else {
					high = mid;
				}
			}
		}
	}
}
