package net.minecraft.component;

import it.unimi.dsi.fastutil.objects.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * {@code MergedComponentMap}.
 */
public final class MergedComponentMap implements ComponentMap {

	private final ComponentMap baseComponents;
	private Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents;
	private boolean copyOnWrite;

	public MergedComponentMap(ComponentMap baseComponents) {
		this(baseComponents, Reference2ObjectMaps.emptyMap(), true);
	}

	private MergedComponentMap(
			ComponentMap baseComponents,
			Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents,
			boolean copyOnWrite
	) {
		this.baseComponents = baseComponents;
		this.changedComponents = changedComponents;
		this.copyOnWrite = copyOnWrite;
	}

	/**
	 * Create.
	 *
	 * @param baseComponents base components
	 * @param changes changes
	 *
	 * @return MergedComponentMap — результат операции
	 */
	public static MergedComponentMap create(ComponentMap baseComponents, ComponentChanges changes) {
		if (shouldReuseChangesMap(baseComponents, changes.changedComponents)) {
			return new MergedComponentMap(baseComponents, changes.changedComponents, true);
		}
		else {
			MergedComponentMap mergedComponentMap = new MergedComponentMap(baseComponents);
			mergedComponentMap.applyChanges(changes);
			return mergedComponentMap;
		}
	}

	private static boolean shouldReuseChangesMap(
			ComponentMap baseComponents,
			Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents
	) {
		ObjectIterator var2 = Reference2ObjectMaps.fastIterable(changedComponents).iterator();

		while (var2.hasNext()) {
			Entry<ComponentType<?>, Optional<?>> entry = (Entry<ComponentType<?>, Optional<?>>) var2.next();
			Object object = baseComponents.get(entry.getKey());
			Optional<?> optional = entry.getValue();
			if (optional.isPresent() && optional.get().equals(object)) {
				return false;
			}

			if (optional.isEmpty() && object == null) {
				return false;
			}
		}

		return true;
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		Optional<? extends T> optional = (Optional<? extends T>) this.changedComponents.get(type);
		return (T) (optional != null ? optional.orElse(null) : this.baseComponents.get(type));
	}

	public boolean hasChanged(ComponentType<?> type) {
		return this.changedComponents.containsKey(type);
	}

	/**
	 * Set.
	 *
	 * @param type type
	 * @param value value
	 *
	 * @return @Nullable T — результат операции
	 */
	public <T> @Nullable T set(ComponentType<T> type, @Nullable T value) {
		this.onWrite();
		T object = this.baseComponents.get(type);
		Optional<T> optional;
		if (Objects.equals(value, object)) {
			optional = (Optional<T>) this.changedComponents.remove(type);
		}
		else {
			optional = (Optional<T>) this.changedComponents.put(type, Optional.ofNullable(value));
		}

		return optional != null ? optional.orElse(object) : object;
	}

	/**
	 * Set.
	 *
	 * @param component component
	 *
	 * @return @Nullable T — результат операции
	 */
	public <T> @Nullable T set(Component<T> component) {
		return this.set(component.type(), component.value());
	}

	/**
	 * Remove.
	 *
	 * @param type type
	 *
	 * @return @Nullable T — результат операции
	 */
	public <T> @Nullable T remove(ComponentType<? extends T> type) {
		this.onWrite();
		T object = this.baseComponents.get(type);
		Optional<? extends T> optional;
		if (object != null) {
			optional = (Optional<? extends T>) this.changedComponents.put(type, Optional.empty());
		}
		else {
			optional = (Optional<? extends T>) this.changedComponents.remove(type);
		}

		return (T) (optional != null ? optional.orElse(null) : object);
	}

	/**
	 * Применяет changes.
	 *
	 * @param changes changes
	 */
	public void applyChanges(ComponentChanges changes) {
		this.onWrite();
		ObjectIterator var2 = Reference2ObjectMaps.fastIterable(changes.changedComponents).iterator();

		while (var2.hasNext()) {
			Entry<ComponentType<?>, Optional<?>> entry = (Entry<ComponentType<?>, Optional<?>>) var2.next();
			this.applyChange(entry.getKey(), entry.getValue());
		}
	}

	private void applyChange(ComponentType<?> type, Optional<?> optional) {
		Object object = this.baseComponents.get(type);
		if (optional.isPresent()) {
			if (optional.get().equals(object)) {
				this.changedComponents.remove(type);
			}
			else {
				this.changedComponents.put(type, optional);
			}
		}
		else if (object != null) {
			this.changedComponents.put(type, Optional.empty());
		}
		else {
			this.changedComponents.remove(type);
		}
	}

	public void setChanges(ComponentChanges changes) {
		this.onWrite();
		this.changedComponents.clear();
		this.changedComponents.putAll(changes.changedComponents);
	}

	/**
	 * Очищает changes.
	 */
	public void clearChanges() {
		this.onWrite();
		this.changedComponents.clear();
	}

	public void setAll(ComponentMap components) {
		for (Component<?> component : components) {
			component.apply(this);
		}
	}

	private void onWrite() {
		if (this.copyOnWrite) {
			this.changedComponents = new Reference2ObjectArrayMap(this.changedComponents);
			this.copyOnWrite = false;
		}
	}

	@Override
	public Set<ComponentType<?>> getTypes() {
		if (this.changedComponents.isEmpty()) {
			return this.baseComponents.getTypes();
		}
		else {
			Set<ComponentType<?>> set = new ReferenceArraySet(this.baseComponents.getTypes());
			ObjectIterator var2 = Reference2ObjectMaps.fastIterable(this.changedComponents).iterator();

			while (var2.hasNext()) {
				it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>
						entry =
						(it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>) var2.next();
				Optional<?> optional = (Optional<?>) entry.getValue();
				if (optional.isPresent()) {
					set.add((ComponentType<?>) entry.getKey());
				}
				else {
					set.remove(entry.getKey());
				}
			}

			return set;
		}
	}

	@Override
	public Iterator<Component<?>> iterator() {
		if (this.changedComponents.isEmpty()) {
			return this.baseComponents.iterator();
		}
		else {
			List<Component<?>> list = new ArrayList<>(this.changedComponents.size() + this.baseComponents.size());
			ObjectIterator var2 = Reference2ObjectMaps.fastIterable(this.changedComponents).iterator();

			while (var2.hasNext()) {
				it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>
						entry =
						(it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>) var2.next();
				if (((Optional) entry.getValue()).isPresent()) {
					list.add(Component.of((ComponentType) entry.getKey(), ((Optional) entry.getValue()).get()));
				}
			}

			for (Component<?> component : this.baseComponents) {
				if (!this.changedComponents.containsKey(component.type())) {
					list.add(component);
				}
			}

			return list.iterator();
		}
	}

	@Override
	public int size() {
		int i = this.baseComponents.size();
		ObjectIterator var2 = Reference2ObjectMaps.fastIterable(this.changedComponents).iterator();

		while (var2.hasNext()) {
			it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>
					entry =
					(it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>) var2.next();
			boolean bl = ((Optional) entry.getValue()).isPresent();
			boolean bl2 = this.baseComponents.contains((ComponentType<?>) entry.getKey());
			if (bl != bl2) {
				i += bl ? 1 : -1;
			}
		}

		return i;
	}

	public ComponentChanges getChanges() {
		if (this.changedComponents.isEmpty()) {
			return ComponentChanges.EMPTY;
		}
		else {
			this.copyOnWrite = true;
			return new ComponentChanges(this.changedComponents);
		}
	}

	/**
	 * Copy.
	 *
	 * @return MergedComponentMap — результат операции
	 */
	public MergedComponentMap copy() {
		this.copyOnWrite = true;
		return new MergedComponentMap(this.baseComponents, this.changedComponents, true);
	}

	/**
	 * Immutable copy.
	 *
	 * @return ComponentMap — результат операции
	 */
	public ComponentMap immutableCopy() {
		return (ComponentMap) (this.changedComponents.isEmpty() ? this.baseComponents : this.copy());
	}

	@Override
	public boolean equals(Object o) {
		return this == o
		       ? true
		       : o instanceof MergedComponentMap mergedComponentMap
		         && this.baseComponents.equals(mergedComponentMap.baseComponents)
		         && this.changedComponents.equals(mergedComponentMap.changedComponents);
	}

	@Override
	public int hashCode() {
		return this.baseComponents.hashCode() + this.changedComponents.hashCode() * 31;
	}

	@Override
	public String toString() {
		return "{" + this.stream().map(Component::toString).collect(Collectors.joining(", ")) + "}";
	}
}
