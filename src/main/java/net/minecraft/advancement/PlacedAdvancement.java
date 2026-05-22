package net.minecraft.advancement;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Узел дерева достижений, хранящий ссылку на родителя и дочерние узлы.
 * Используется {@link AdvancementManager} для построения иерархии достижений.
 */
public class PlacedAdvancement {

	private final AdvancementEntry advancementEntry;
	private final @Nullable PlacedAdvancement parent;
	private final Set<PlacedAdvancement> children = new ReferenceOpenHashSet<>();

	@VisibleForTesting
	public PlacedAdvancement(AdvancementEntry advancementEntry, @Nullable PlacedAdvancement parent) {
		this.advancementEntry = advancementEntry;
		this.parent = parent;
	}

	public Advancement getAdvancement() {
		return advancementEntry.value();
	}

	public AdvancementEntry getAdvancementEntry() {
		return advancementEntry;
	}

	public @Nullable PlacedAdvancement getParent() {
		return parent;
	}

	public PlacedAdvancement getRoot() {
		return findRoot(this);
	}

	/**
	 * Поднимается по цепочке родителей до корневого узла дерева достижений.
	 *
	 * @param advancement начальный узел для поиска
	 * @return корневой узел дерева
	 */
	public static PlacedAdvancement findRoot(PlacedAdvancement advancement) {
		PlacedAdvancement current = advancement;

		while (true) {
			PlacedAdvancement parentNode = current.getParent();
			if (parentNode == null) {
				return current;
			}

			current = parentNode;
		}
	}

	public Iterable<PlacedAdvancement> getChildren() {
		return children;
	}

	@VisibleForTesting
	public void addChild(PlacedAdvancement advancement) {
		children.add(advancement);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof PlacedAdvancement other && advancementEntry.equals(other.advancementEntry);
	}

	@Override
	public int hashCode() {
		return advancementEntry.hashCode();
	}

	@Override
	public String toString() {
		return advancementEntry.id().toString();
	}
}
