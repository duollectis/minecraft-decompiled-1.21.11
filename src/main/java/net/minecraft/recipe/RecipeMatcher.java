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
 * {@code RecipeMatcher}.
 */
public class RecipeMatcher<T> {

	public final Reference2IntOpenHashMap<T> available = new Reference2IntOpenHashMap();

	boolean hasAtLeast(T input, int minimum) {
		return this.available.getInt(input) >= minimum;
	}

	void consume(T input, int count) {
		int i = this.available.addTo(input, -count);
		if (i < count) {
			throw new IllegalStateException("Took " + count + " items, but only had " + i);
		}
	}

	void addInput(T input, int count) {
		this.available.addTo(input, count);
	}

	public boolean match(
			List<? extends RecipeMatcher.RawIngredient<T>> ingredients,
			int quantity,
			RecipeMatcher.@Nullable ItemCallback<T> itemCallback
	) {
		return new RecipeMatcher.Matcher(ingredients).match(quantity, itemCallback);
	}

	public int countCrafts(
			List<? extends RecipeMatcher.RawIngredient<T>> ingredients,
			int max,
			RecipeMatcher.@Nullable ItemCallback<T> itemCallback
	) {
		return new RecipeMatcher.Matcher(ingredients).countCrafts(max, itemCallback);
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.available.clear();
	}

	/**
	 * Add.
	 *
	 * @param input input
	 * @param count count
	 */
	public void add(T input, int count) {
		this.addInput(input, count);
	}

	List<T> createItemRequirementList(Iterable<? extends RecipeMatcher.RawIngredient<T>> ingredients) {
		List<T> list = new ArrayList<>();
		ObjectIterator var3 = Reference2IntMaps.fastIterable(this.available).iterator();

		while (var3.hasNext()) {
			Entry<T> entry = (Entry<T>) var3.next();
			if (entry.getIntValue() > 0 && anyAccept(ingredients, (T) entry.getKey())) {
				list.add((T) entry.getKey());
			}
		}

		return list;
	}

	private static <T> boolean anyAccept(Iterable<? extends RecipeMatcher.RawIngredient<T>> ingredients, T item) {
		for (RecipeMatcher.RawIngredient<T> rawIngredient : ingredients) {
			if (rawIngredient.acceptsItem(item)) {
				return true;
			}
		}

		return false;
	}

	@VisibleForTesting
	public int getMaximumCrafts(List<? extends RecipeMatcher.RawIngredient<T>> ingredients) {
		int i = Integer.MAX_VALUE;
		ObjectIterable<Entry<T>> objectIterable = Reference2IntMaps.fastIterable(this.available);

		label31:
		for (RecipeMatcher.RawIngredient<T> rawIngredient : ingredients) {
			int j = 0;
			ObjectIterator var7 = objectIterable.iterator();

			while (var7.hasNext()) {
				Entry<T> entry = (Entry<T>) var7.next();
				int k = entry.getIntValue();
				if (k > j) {
					if (rawIngredient.acceptsItem((T) entry.getKey())) {
						j = k;
					}

					if (j >= i) {
						continue label31;
					}
				}
			}

			i = j;
			if (j == 0) {
				break;
			}
		}

		return i;
	}

	@FunctionalInterface
	/**
	 * {@code ItemCallback}.
	 */
	public interface ItemCallback<T> {

		void accept(T item);
	}

	/**
	 * {@code Matcher}.
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
			this.totalIngredients = ingredients.size();
			this.requiredItems = RecipeMatcher.this.createItemRequirementList(ingredients);
			this.totalRequiredItems = this.requiredItems.size();
			this.bits = new BitSet(
					this.getVisitedIngredientIndexCount()
							+ this.getVisitedItemIndexCount()
							+ this.getRequirementIndexCount()
							+ this.getItemMatchIndexCount()
							+ this.getMissingIndexCount()
			);
			this.initItemMatch();
		}

		private void initItemMatch() {
			for (int i = 0; i < this.totalIngredients; i++) {
				RecipeMatcher.RawIngredient<T> rawIngredient = (RecipeMatcher.RawIngredient<T>) this.ingredients.get(i);

				for (int j = 0; j < this.totalRequiredItems; j++) {
					if (rawIngredient.acceptsItem(this.requiredItems.get(j))) {
						this.setMatch(j, i);
					}
				}
			}
		}

		/**
		 * Match.
		 *
		 * @param quantity quantity
		 * @param itemCallback item callback
		 *
		 * @return boolean — результат операции
		 */
		public boolean match(int quantity, RecipeMatcher.@Nullable ItemCallback<T> itemCallback) {
			if (quantity <= 0) {
				return true;
			}
			else {
				int i = 0;

				while (true) {
					IntList intList = this.tryFindIngredientItemLookup(quantity);
					if (intList == null) {
						boolean bl = i == this.totalIngredients;
						boolean bl2 = bl && itemCallback != null;
						this.clearVisited();
						this.clearRequirements();

						for (int k = 0; k < this.totalIngredients; k++) {
							for (int l = 0; l < this.totalRequiredItems; l++) {
								if (this.isMissing(l, k)) {
									this.markNotMissing(l, k);
									RecipeMatcher.this.addInput(this.requiredItems.get(l), quantity);
									if (bl2) {
										itemCallback.accept(this.requiredItems.get(l));
									}
									break;
								}
							}
						}

						assert this.bits
								.get(
										this.getMissingIndexOffset(),
										this.getMissingIndexOffset() + this.getMissingIndexCount()
								)
								.isEmpty();

						return bl;
					}

					int j = intList.getInt(0);
					RecipeMatcher.this.consume(this.requiredItems.get(j), quantity);
					int k = intList.size() - 1;
					this.unfulfillRequirement(intList.getInt(k));
					i++;

					for (int lx = 0; lx < intList.size() - 1; lx++) {
						if (isItem(lx)) {
							int m = intList.getInt(lx);
							int n = intList.getInt(lx + 1);
							this.markMissing(m, n);
						}
						else {
							int m = intList.getInt(lx + 1);
							int n = intList.getInt(lx);
							this.markNotMissing(m, n);
						}
					}
				}
			}
		}

		private static boolean isItem(int index) {
			return (index & 1) == 0;
		}

		private @Nullable IntList tryFindIngredientItemLookup(int min) {
			this.clearVisited();

			for (int i = 0; i < this.totalRequiredItems; i++) {
				if (RecipeMatcher.this.hasAtLeast(this.requiredItems.get(i), min)) {
					IntList intList = this.findIngredientItemLookup(i);
					if (intList != null) {
						return intList;
					}
				}
			}

			return null;
		}

		private @Nullable IntList findIngredientItemLookup(int itemIndex) {
			this.ingredientItemLookup.clear();
			this.markItemVisited(itemIndex);
			this.ingredientItemLookup.add(itemIndex);

			while (!this.ingredientItemLookup.isEmpty()) {
				int i = this.ingredientItemLookup.size();
				if (isItem(i - 1)) {
					int j = this.ingredientItemLookup.getInt(i - 1);

					for (int k = 0; k < this.totalIngredients; k++) {
						if (!this.hasVisitedIngredient(k) && this.matches(j, k) && !this.isMissing(j, k)) {
							this.markIngredientVisited(k);
							this.ingredientItemLookup.add(k);
							break;
						}
					}
				}
				else {
					int j = this.ingredientItemLookup.getInt(i - 1);
					if (!this.getRequirement(j)) {
						return this.ingredientItemLookup;
					}

					for (int kx = 0; kx < this.totalRequiredItems; kx++) {
						if (!this.isRequirementUnfulfilled(kx) && this.isMissing(kx, j)) {
							assert this.matches(kx, j);

							this.markItemVisited(kx);
							this.ingredientItemLookup.add(kx);
							break;
						}
					}
				}

				int j = this.ingredientItemLookup.size();
				if (j == i) {
					this.ingredientItemLookup.removeInt(j - 1);
				}
			}

			return null;
		}

		private int getVisitedIngredientIndexOffset() {
			return 0;
		}

		private int getVisitedIngredientIndexCount() {
			return this.totalIngredients;
		}

		private int getVisitedItemIndexOffset() {
			return this.getVisitedIngredientIndexOffset() + this.getVisitedIngredientIndexCount();
		}

		private int getVisitedItemIndexCount() {
			return this.totalRequiredItems;
		}

		private int getRequirementIndexOffset() {
			return this.getVisitedItemIndexOffset() + this.getVisitedItemIndexCount();
		}

		private int getRequirementIndexCount() {
			return this.totalIngredients;
		}

		private int getItemMatchIndexOffset() {
			return this.getRequirementIndexOffset() + this.getRequirementIndexCount();
		}

		private int getItemMatchIndexCount() {
			return this.totalIngredients * this.totalRequiredItems;
		}

		private int getMissingIndexOffset() {
			return this.getItemMatchIndexOffset() + this.getItemMatchIndexCount();
		}

		private int getMissingIndexCount() {
			return this.totalIngredients * this.totalRequiredItems;
		}

		private boolean getRequirement(int itemId) {
			return this.bits.get(this.getRequirementIndex(itemId));
		}

		private void unfulfillRequirement(int itemId) {
			this.bits.set(this.getRequirementIndex(itemId));
		}

		private int getRequirementIndex(int itemId) {
			assert itemId >= 0 && itemId < this.totalIngredients;

			return this.getRequirementIndexOffset() + itemId;
		}

		private void clearRequirements() {
			this.clear(this.getRequirementIndexOffset(), this.getRequirementIndexCount());
		}

		private void setMatch(int itemIndex, int ingredientIndex) {
			this.bits.set(this.getMatchIndex(itemIndex, ingredientIndex));
		}

		private boolean matches(int itemIndex, int ingredientIndex) {
			return this.bits.get(this.getMatchIndex(itemIndex, ingredientIndex));
		}

		private int getMatchIndex(int itemIndex, int ingredientIndex) {
			assert itemIndex >= 0 && itemIndex < this.totalRequiredItems;

			assert ingredientIndex >= 0 && ingredientIndex < this.totalIngredients;

			return this.getItemMatchIndexOffset() + itemIndex * this.totalIngredients + ingredientIndex;
		}

		private boolean isMissing(int itemIndex, int ingredientIndex) {
			return this.bits.get(this.getMissingIndex(itemIndex, ingredientIndex));
		}

		private void markMissing(int itemIndex, int ingredientIndex) {
			int i = this.getMissingIndex(itemIndex, ingredientIndex);

			assert !this.bits.get(i);

			this.bits.set(i);
		}

		private void markNotMissing(int itemIndex, int ingredientIndex) {
			int i = this.getMissingIndex(itemIndex, ingredientIndex);

			assert this.bits.get(i);

			this.bits.clear(i);
		}

		private int getMissingIndex(int itemIndex, int ingredientIndex) {
			assert itemIndex >= 0 && itemIndex < this.totalRequiredItems;

			assert ingredientIndex >= 0 && ingredientIndex < this.totalIngredients;

			return this.getMissingIndexOffset() + itemIndex * this.totalIngredients + ingredientIndex;
		}

		private void markIngredientVisited(int index) {
			this.bits.set(this.getVisitedIngredientIndex(index));
		}

		private boolean hasVisitedIngredient(int index) {
			return this.bits.get(this.getVisitedIngredientIndex(index));
		}

		private int getVisitedIngredientIndex(int index) {
			assert index >= 0 && index < this.totalIngredients;

			return this.getVisitedIngredientIndexOffset() + index;
		}

		private void markItemVisited(int index) {
			this.bits.set(this.getVisitedItemIndex(index));
		}

		private boolean isRequirementUnfulfilled(int index) {
			return this.bits.get(this.getVisitedItemIndex(index));
		}

		private int getVisitedItemIndex(int index) {
			assert index >= 0 && index < this.totalRequiredItems;

			return this.getVisitedItemIndexOffset() + index;
		}

		private void clearVisited() {
			this.clear(this.getVisitedIngredientIndexOffset(), this.getVisitedIngredientIndexCount());
			this.clear(this.getVisitedItemIndexOffset(), this.getVisitedItemIndexCount());
		}

		private void clear(int start, int offset) {
			this.bits.clear(start, start + offset);
		}

		/**
		 * Count crafts.
		 *
		 * @param max max
		 * @param itemCallback item callback
		 *
		 * @return int — результат операции
		 */
		public int countCrafts(int max, RecipeMatcher.@Nullable ItemCallback<T> itemCallback) {
			int i = 0;
			int j = Math.min(max, RecipeMatcher.this.getMaximumCrafts(this.ingredients)) + 1;

			while (true) {
				int k = (i + j) / 2;
				if (this.match(k, null)) {
					if (j - i <= 1) {
						if (k > 0) {
							this.match(k, itemCallback);
						}

						return k;
					}

					i = k;
				}
				else {
					j = k;
				}
			}
		}
	}

	@FunctionalInterface
	/**
	 * {@code RawIngredient}.
	 */
	public interface RawIngredient<T> {

		boolean acceptsItem(T entry);
	}
}
