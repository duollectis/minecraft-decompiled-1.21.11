package net.minecraft.predicate.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.component.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {@code ComponentMapPredicate}.
 */
public final class ComponentMapPredicate implements Predicate<ComponentsAccess> {

	public static final Codec<ComponentMapPredicate> CODEC = ComponentType.TYPE_TO_VALUE_MAP_CODEC
			.xmap(
					map -> new ComponentMapPredicate(map
							.entrySet()
							.stream()
							.<Component<?>>map(entry -> Component.of(entry.getKey(), entry.getValue()))
							.collect(Collectors.toList())),
					predicate -> predicate.components
							.stream()
							.filter(component -> component.type().shouldSkipSerialization() == false)
							.collect(Collectors.toMap(Component::type, Component::value))
			);
	public static final PacketCodec<RegistryByteBuf, ComponentMapPredicate> PACKET_CODEC = Component.PACKET_CODEC
			.collect(PacketCodecs.toList())
			.xmap(ComponentMapPredicate::new, predicate -> predicate.components);
	public static final ComponentMapPredicate EMPTY = new ComponentMapPredicate(List.of());
	private final List<Component<?>> components;

	ComponentMapPredicate(List<Component<?>> components) {
		this.components = components;
	}

	public static ComponentMapPredicate.Builder builder() {
		return new ComponentMapPredicate.Builder();
	}

	public static <T> ComponentMapPredicate of(ComponentType<T> type, T value) {
		return new ComponentMapPredicate(List.of(new Component<>(type, value)));
	}

	public static ComponentMapPredicate of(ComponentMap components) {
		return new ComponentMapPredicate(ImmutableList.copyOf(components));
	}

	public static ComponentMapPredicate ofFiltered(ComponentMap components, ComponentType<?>... types) {
		ComponentMapPredicate.Builder builder = new ComponentMapPredicate.Builder();

		for (ComponentType<?> componentType : types) {
			Component<?> component = components.getTyped(componentType);
			if (component != null) {
				builder.add(component);
			}
		}

		return builder.build();
	}

	public boolean isEmpty() {
		return this.components.isEmpty();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ComponentMapPredicate componentMapPredicate
				&& this.components.equals(componentMapPredicate.components);
	}

	@Override
	public int hashCode() {
		return this.components.hashCode();
	}

	@Override
	public String toString() {
		return this.components.toString();
	}

	public boolean test(ComponentsAccess componentsAccess) {
		for (Component<?> component : this.components) {
			Object object = componentsAccess.get(component.type());
			if (!Objects.equals(component.value(), object)) {
				return false;
			}
		}

		return true;
	}

	public boolean isPredicateEmpty() {
		return this.components.isEmpty();
	}

	public ComponentChanges toChanges() {
		ComponentChanges.Builder builder = ComponentChanges.builder();

		for (Component<?> component : this.components) {
			builder.add(component);
		}

		return builder.build();
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final List<Component<?>> components = new ArrayList<>();

		Builder() {
		}

		public <T> ComponentMapPredicate.Builder add(Component<T> component) {
			return this.add(component.type(), component.value());
		}

		public <T> ComponentMapPredicate.Builder add(ComponentType<? super T> type, T value) {
			for (Component<?> component : this.components) {
				if (component.type() == type) {
					throw new IllegalArgumentException("Predicate already has component of type: '" + type + "'");
				}
			}

			this.components.add(new Component<>(type, value));
			return this;
		}

		public ComponentMapPredicate build() {
			return new ComponentMapPredicate(List.copyOf(this.components));
		}
	}
}
