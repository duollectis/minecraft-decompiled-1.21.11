package net.minecraft.component;

import it.unimi.dsi.fastutil.objects.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
	 * Реализация {@link ComponentMap}, объединяющая базовый набор компонентов с картой изменений.
	 * Поддерживает паттерн copy-on-write: при первой записи карта изменений копируется,
	 * что позволяет безопасно разделять её между несколькими экземплярами (например, при {@link #copy()}).
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
		 * Создаёт {@link MergedComponentMap} из базовых компонентов и набора изменений.
		 * Если все изменения отличаются от базовых значений, карта изменений используется напрямую
		 * (без копирования) для экономии памяти.
		 *
		 * @param baseComponents базовые компоненты
		 * @param changes        набор изменений для применения
		 * @return новый экземпляр с применёнными изменениями
		 */
	public static MergedComponentMap create(ComponentMap baseComponents, ComponentChanges changes) {
		if (shouldReuseChangesMap(baseComponents, changes.changedComponents)) {
			return new MergedComponentMap(baseComponents, changes.changedComponents, true);
		}

		MergedComponentMap result = new MergedComponentMap(baseComponents);
		result.applyChanges(changes);
		return result;
	}

	/**
		 * Проверяет, можно ли переиспользовать карту изменений без копирования.
		 * Переиспользование невозможно, если хотя бы одно изменение дублирует базовое значение
		 * (такое изменение было бы избыточным и должно быть отфильтровано).
		 */
	private static boolean shouldReuseChangesMap(
			ComponentMap baseComponents,
			Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents
	) {
		for (Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(changedComponents)) {
			Object baseValue = baseComponents.get(entry.getKey());
			Optional<?> change = entry.getValue();

			if (change.isPresent() && change.get().equals(baseValue)) {
				return false;
			}

			if (change.isEmpty() && baseValue == null) {
				return false;
			}
		}

		return true;
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		Optional<? extends T> change = (Optional<? extends T>) changedComponents.get(type);
		return change != null ? change.orElse(null) : baseComponents.get(type);
	}

	public boolean hasChanged(ComponentType<?> type) {
		return changedComponents.containsKey(type);
	}

	/**
		 * Устанавливает значение компонента. Если новое значение совпадает с базовым,
		 * запись об изменении удаляется (нет смысла хранить избыточную дельту).
		 *
		 * @param type  тип компонента
		 * @param value новое значение или {@code null} для удаления
		 * @return предыдущее значение компонента
		 */
	public <T> @Nullable T set(ComponentType<T> type, @Nullable T value) {
		onWrite();
		T baseValue = baseComponents.get(type);
		Optional<T> previous;

		if (Objects.equals(value, baseValue)) {
			previous = (Optional<T>) changedComponents.remove(type);
		} else {
			previous = (Optional<T>) changedComponents.put(type, Optional.ofNullable(value));
		}

		return previous != null ? previous.orElse(baseValue) : baseValue;
	}

	public <T> @Nullable T set(Component<T> component) {
		return set(component.type(), component.value());
	}

	/**
		 * Удаляет компонент. Если компонент присутствует в базовых данных,
		 * добавляет запись об удалении; иначе просто убирает запись об изменении.
		 *
		 * @param type тип компонента для удаления
		 * @return предыдущее значение или {@code null}
		 */
	public <T> @Nullable T remove(ComponentType<? extends T> type) {
		onWrite();
		T baseValue = baseComponents.get(type);
		Optional<? extends T> previous;

		if (baseValue != null) {
			previous = (Optional<? extends T>) changedComponents.put(type, Optional.empty());
		} else {
			previous = (Optional<? extends T>) changedComponents.remove(type);
		}

		return previous != null ? previous.orElse(null) : baseValue;
	}

	public void applyChanges(ComponentChanges changes) {
		onWrite();

		for (Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(changes.changedComponents)) {
			applyChange(entry.getKey(), entry.getValue());
		}
	}

	private void applyChange(ComponentType<?> type, Optional<?> change) {
		Object baseValue = baseComponents.get(type);

		if (change.isPresent()) {
			if (change.get().equals(baseValue)) {
				changedComponents.remove(type);
			} else {
				changedComponents.put(type, change);
			}
		} else if (baseValue != null) {
			changedComponents.put(type, Optional.empty());
		} else {
			changedComponents.remove(type);
		}
	}

	public void setChanges(ComponentChanges changes) {
		onWrite();
		changedComponents.clear();
		changedComponents.putAll(changes.changedComponents);
	}

	public void clearChanges() {
		onWrite();
		changedComponents.clear();
	}

	public void setAll(ComponentMap components) {
		for (Component<?> component : components) {
			component.apply(this);
		}
	}

	private void onWrite() {
		if (copyOnWrite) {
			changedComponents = new Reference2ObjectArrayMap<>(changedComponents);
			copyOnWrite = false;
		}
	}

	@Override
	public Set<ComponentType<?>> getTypes() {
		if (changedComponents.isEmpty()) {
			return baseComponents.getTypes();
		}

		Set<ComponentType<?>> types = new ReferenceArraySet<>(baseComponents.getTypes());

		for (Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(changedComponents)) {
			if (entry.getValue().isPresent()) {
				types.add(entry.getKey());
			} else {
				types.remove(entry.getKey());
			}
		}

		return types;
	}

	@Override
	public Iterator<Component<?>> iterator() {
		if (changedComponents.isEmpty()) {
			return baseComponents.iterator();
		}

		List<Component<?>> components = new ArrayList<>(changedComponents.size() + baseComponents.size());

		for (Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(changedComponents)) {
			Optional<?> value = entry.getValue();
			if (value.isPresent()) {
				components.add(Component.of((ComponentType) entry.getKey(), value.get()));
			}
		}

		for (Component<?> component : baseComponents) {
			if (!changedComponents.containsKey(component.type())) {
				components.add(component);
			}
		}

		return components.iterator();
	}

	@Override
	public int size() {
		int count = baseComponents.size();

		for (Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(changedComponents)) {
			boolean isPresent = entry.getValue().isPresent();
			boolean wasPresent = baseComponents.contains(entry.getKey());

			if (isPresent != wasPresent) {
				count += isPresent ? 1 : -1;
			}
		}

		return count;
	}

	public ComponentChanges getChanges() {
		if (changedComponents.isEmpty()) {
			return ComponentChanges.EMPTY;
		}

		copyOnWrite = true;
		return new ComponentChanges(changedComponents);
	}

	public MergedComponentMap copy() {
		copyOnWrite = true;
		return new MergedComponentMap(baseComponents, changedComponents, true);
	}

	public ComponentMap immutableCopy() {
		return changedComponents.isEmpty() ? baseComponents : copy();
	}

	@Override
	public boolean equals(Object o) {
		return this == o
			? true
			: o instanceof MergedComponentMap other
				&& baseComponents.equals(other.baseComponents)
				&& changedComponents.equals(other.changedComponents);
	}

	@Override
	public int hashCode() {
		return baseComponents.hashCode() + changedComponents.hashCode() * 31;
	}

	@Override
	public String toString() {
		return "{" + stream().map(Component::toString).collect(Collectors.joining(", ")) + "}";
	}
}
