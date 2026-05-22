package net.minecraft.advancement;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Хранит все загруженные достижения в виде дерева {@link PlacedAdvancement}.
 * Уведомляет {@link Listener} о добавлении, удалении и очистке достижений.
 */
public class AdvancementManager {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final Map<Identifier, PlacedAdvancement> advancements = new Object2ObjectOpenHashMap<>();
	private final Set<PlacedAdvancement> roots = new ObjectLinkedOpenHashSet<>();
	private final Set<PlacedAdvancement> dependents = new ObjectLinkedOpenHashSet<>();
	private @Nullable Listener listener;

	private void remove(PlacedAdvancement advancement) {
		for (PlacedAdvancement child : advancement.getChildren()) {
			remove(child);
		}

		LOGGER.info("Forgot about advancement {}", advancement.getAdvancementEntry());
		advancements.remove(advancement.getAdvancementEntry().id());

		if (advancement.getParent() == null) {
			roots.remove(advancement);
			if (listener != null) {
				listener.onRootRemoved(advancement);
			}
		} else {
			dependents.remove(advancement);
			if (listener != null) {
				listener.onDependentRemoved(advancement);
			}
		}
	}

	public void removeAll(Set<Identifier> ids) {
		for (Identifier id : ids) {
			PlacedAdvancement placed = advancements.get(id);
			if (placed == null) {
				LOGGER.warn("Told to remove advancement {} but I don't know what that is", id);
			} else {
				remove(placed);
			}
		}
	}

	/**
	 * Добавляет коллекцию достижений, разрешая зависимости родитель→ребёнок.
	 * Достижения без ещё не загруженного родителя откладываются до следующей итерации.
	 */
	public void addAll(Collection<AdvancementEntry> entries) {
		ArrayList<AdvancementEntry> pending = new ArrayList<>(entries);

		while (!pending.isEmpty()) {
			if (!pending.removeIf(this::tryAdd)) {
				LOGGER.error("Couldn't load advancements: {}", pending);
				break;
			}
		}

		LOGGER.info("Loaded {} advancements", advancements.size());
	}

	private boolean tryAdd(AdvancementEntry entry) {
		Optional<Identifier> parentId = entry.value().parent();
		PlacedAdvancement parent = parentId.map(advancements::get).orElse(null);

		if (parent == null && parentId.isPresent()) {
			return false;
		}

		PlacedAdvancement placed = new PlacedAdvancement(entry, parent);
		if (parent != null) {
			parent.addChild(placed);
		}

		advancements.put(entry.id(), placed);

		if (parent == null) {
			roots.add(placed);
			if (listener != null) {
				listener.onRootAdded(placed);
			}
		} else {
			dependents.add(placed);
			if (listener != null) {
				listener.onDependentAdded(placed);
			}
		}

		return true;
	}

	public void clear() {
		advancements.clear();
		roots.clear();
		dependents.clear();
		if (listener != null) {
			listener.onClear();
		}
	}

	public Iterable<PlacedAdvancement> getRoots() {
		return roots;
	}

	public Collection<PlacedAdvancement> getAdvancements() {
		return advancements.values();
	}

	public @Nullable PlacedAdvancement get(Identifier id) {
		return advancements.get(id);
	}

	public @Nullable PlacedAdvancement get(AdvancementEntry entry) {
		return advancements.get(entry.id());
	}

	/**
	 * Устанавливает слушателя и немедленно уведомляет его о всех уже загруженных достижениях.
	 */
	public void setListener(@Nullable Listener listener) {
		this.listener = listener;
		if (listener == null) {
			return;
		}
		for (PlacedAdvancement root : roots) {
			listener.onRootAdded(root);
		}
		for (PlacedAdvancement dependent : dependents) {
			listener.onDependentAdded(dependent);
		}
	}

	public interface Listener {

		void onRootAdded(PlacedAdvancement root);

		void onRootRemoved(PlacedAdvancement root);

		void onDependentAdded(PlacedAdvancement dependent);

		void onDependentRemoved(PlacedAdvancement dependent);

		void onClear();
	}
}
