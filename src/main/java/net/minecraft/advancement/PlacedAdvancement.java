package net.minecraft.advancement;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * {@code PlacedAdvancement}.
 */
public class PlacedAdvancement {

	private final AdvancementEntry advancementEntry;
	private final @Nullable PlacedAdvancement parent;
	private final Set<PlacedAdvancement> children = new ReferenceOpenHashSet();

	@VisibleForTesting
	public PlacedAdvancement(AdvancementEntry advancementEntry, @Nullable PlacedAdvancement parent) {
		this.advancementEntry = advancementEntry;
		this.parent = parent;
	}

	public Advancement getAdvancement() {
		return this.advancementEntry.value();
	}

	public AdvancementEntry getAdvancementEntry() {
		return this.advancementEntry;
	}

	public @Nullable PlacedAdvancement getParent() {
		return this.parent;
	}

	public PlacedAdvancement getRoot() {
		return findRoot(this);
	}

	public static PlacedAdvancement findRoot(PlacedAdvancement advancement) {
		PlacedAdvancement placedAdvancement = advancement;

		while (true) {
			PlacedAdvancement placedAdvancement2 = placedAdvancement.getParent();
			if (placedAdvancement2 == null) {
				return placedAdvancement;
			}

			placedAdvancement = placedAdvancement2;
		}
	}

	public Iterable<PlacedAdvancement> getChildren() {
		return this.children;
	}

	@VisibleForTesting
	public void addChild(PlacedAdvancement advancement) {
		this.children.add(advancement);
	}

	@Override
	public boolean equals(Object o) {
		return this == o ? true : o instanceof PlacedAdvancement placedAdvancement && this.advancementEntry.equals(
				placedAdvancement.advancementEntry);
	}

	@Override
	public int hashCode() {
		return this.advancementEntry.hashCode();
	}

	@Override
	public String toString() {
		return this.advancementEntry.id().toString();
	}
}
