package net.minecraft.component;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@code ComponentChanges}.
 */
public final class ComponentChanges {

	public static final ComponentChanges EMPTY = new ComponentChanges(Reference2ObjectMaps.emptyMap());
	public static final Codec<ComponentChanges>
			CODEC =
			Codec.dispatchedMap(ComponentChanges.Type.CODEC, ComponentChanges.Type::getValueCodec).xmap(
					changes -> {
						if (changes.isEmpty()) {
							return EMPTY;
						}
						else {
							Reference2ObjectMap<ComponentType<?>, Optional<?>>
									reference2ObjectMap =
									new Reference2ObjectArrayMap<>(changes.size());

							for (Entry<ComponentChanges.Type, ?> entry : changes.entrySet()) {
								ComponentChanges.Type type = entry.getKey();
								if (type.removed()) {
									reference2ObjectMap.put(type.type(), Optional.empty());
								}
								else {
									reference2ObjectMap.put(type.type(), Optional.of(entry.getValue()));
								}
							}

							return new ComponentChanges(reference2ObjectMap);
						}
					}, changes -> {
						@SuppressWarnings("unchecked")
						Reference2ObjectMap<ComponentChanges.Type, Object>
								reference2ObjectMap =
								new Reference2ObjectArrayMap<>(changes.changedComponents.size());
						ObjectIterator var2 = Reference2ObjectMaps.fastIterable(changes.changedComponents).iterator();

						while (var2.hasNext()) {
							Entry<ComponentType<?>, Optional<?>>
									entry =
									(Entry<ComponentType<?>, Optional<?>>) var2.next();
							ComponentType<?> componentType = entry.getKey();
							if (!componentType.shouldSkipSerialization()) {
								Optional<?> optional = entry.getValue();
								if (optional.isPresent()) {
									reference2ObjectMap.put(
											new ComponentChanges.Type(componentType, false),
											optional.get()
									);
								}
								else {
									reference2ObjectMap.put(
											new ComponentChanges.Type(componentType, true),
											Unit.INSTANCE
									);
								}
							}
						}

						return (java.util.Map) reference2ObjectMap;
					}
			);
	public static final PacketCodec<RegistryByteBuf, ComponentChanges>
			PACKET_CODEC =
			createPacketCodec(new ComponentChanges.PacketCodecFunction() {
				@Override
				public <T> PacketCodec<RegistryByteBuf, T> apply(ComponentType<T> componentType) {
					return componentType.getPacketCodec().cast();
				}
			});
	public static final PacketCodec<RegistryByteBuf, ComponentChanges>
			LENGTH_PREPENDED_PACKET_CODEC =
			createPacketCodec(
					new ComponentChanges.PacketCodecFunction() {
						@Override
						public <T> PacketCodec<RegistryByteBuf, T> apply(ComponentType<T> componentType) {
							PacketCodec<RegistryByteBuf, T> packetCodec = componentType.getPacketCodec().cast();
							return packetCodec.collect(PacketCodecs.lengthPrependedRegistry(Integer.MAX_VALUE));
						}
					}
			);
	private static final String REMOVE_PREFIX = "!";
	final Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents;

	private static PacketCodec<RegistryByteBuf, ComponentChanges> createPacketCodec(ComponentChanges.PacketCodecFunction packetCodecFunction) {
		return new PacketCodec<RegistryByteBuf, ComponentChanges>() {
			/**
			 * Decode.
			 *
			 * @param registryByteBuf registry byte buf
			 *
			 * @return ComponentChanges — результат операции
			 */
			public ComponentChanges decode(RegistryByteBuf registryByteBuf) {
				int i = registryByteBuf.readVarInt();
				int j = registryByteBuf.readVarInt();
				if (i == 0 && j == 0) {
					return ComponentChanges.EMPTY;
				}
				else {
					int k = i + j;
					Reference2ObjectMap<ComponentType<?>, Optional<?>>
							reference2ObjectMap =
							new Reference2ObjectArrayMap(Math.min(k, 65536));

					for (int l = 0; l < i; l++) {
						ComponentType<?> componentType = ComponentType.PACKET_CODEC.decode(registryByteBuf);
						Object object = packetCodecFunction.apply(componentType).decode(registryByteBuf);
						reference2ObjectMap.put(componentType, Optional.of(object));
					}

					for (int l = 0; l < j; l++) {
						ComponentType<?> componentType = ComponentType.PACKET_CODEC.decode(registryByteBuf);
						reference2ObjectMap.put(componentType, Optional.empty());
					}

					return new ComponentChanges(reference2ObjectMap);
				}
			}

			/**
			 * Encode.
			 *
			 * @param registryByteBuf registry byte buf
			 * @param componentChanges component changes
			 */
			public void encode(RegistryByteBuf registryByteBuf, ComponentChanges componentChanges) {
				if (componentChanges.isEmpty()) {
					registryByteBuf.writeVarInt(0);
					registryByteBuf.writeVarInt(0);
				}
				else {
					int i = 0;
					int j = 0;
					ObjectIterator
							var5 =
							Reference2ObjectMaps.fastIterable(componentChanges.changedComponents).iterator();

					while (var5.hasNext()) {
						it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>
								entry =
								(it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>) var5.next();
						if (((Optional) entry.getValue()).isPresent()) {
							i++;
						}
						else {
							j++;
						}
					}

					registryByteBuf.writeVarInt(i);
					registryByteBuf.writeVarInt(j);
					var5 = Reference2ObjectMaps.fastIterable(componentChanges.changedComponents).iterator();

					while (var5.hasNext()) {
						it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>
								entry =
								(it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>) var5.next();
						Optional<?> optional = (Optional<?>) entry.getValue();
						if (optional.isPresent()) {
							ComponentType<?> componentType = (ComponentType<?>) entry.getKey();
							ComponentType.PACKET_CODEC.encode(registryByteBuf, componentType);
							this.encode(registryByteBuf, componentType, optional.get());
						}
					}

					var5 = Reference2ObjectMaps.fastIterable(componentChanges.changedComponents).iterator();

					while (var5.hasNext()) {
						it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>
								entry =
								(it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>>) var5.next();
						if (((Optional) entry.getValue()).isEmpty()) {
							ComponentType<?> componentType2 = (ComponentType<?>) entry.getKey();
							ComponentType.PACKET_CODEC.encode(registryByteBuf, componentType2);
						}
					}
				}
			}

			private <T> void encode(RegistryByteBuf buf, ComponentType<T> type, Object value) {
				packetCodecFunction.apply(type).encode(buf, (T) value);
			}
		};
	}

	ComponentChanges(Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents) {
		this.changedComponents = changedComponents;
	}

	public static ComponentChanges.Builder builder() {
		return new ComponentChanges.Builder();
	}

	/**
	 * Get.
	 *
	 * @param type type
	 *
	 * @return @Nullable Optional — 
	 */
	public <T> @Nullable Optional<? extends T> get(ComponentType<? extends T> type) {
		return (Optional<? extends T>) this.changedComponents.get(type);
	}

	/**
	 * Entry set.
	 *
	 * @return Set, Optional>> — результат операции
	 */
	public Set<Entry<ComponentType<?>, Optional<?>>> entrySet() {
		return this.changedComponents.entrySet();
	}

	/**
	 * Size.
	 *
	 * @return int — результат операции
	 */
	public int size() {
		return this.changedComponents.size();
	}

	/**
	 * With removed if.
	 *
	 * @param removedTypePredicate removed type predicate
	 *
	 * @return ComponentChanges — результат операции
	 */
	public ComponentChanges withRemovedIf(Predicate<ComponentType<?>> removedTypePredicate) {
		if (this.isEmpty()) {
			return EMPTY;
		}
		else {
			Reference2ObjectMap<ComponentType<?>, Optional<?>>
					reference2ObjectMap =
					new Reference2ObjectArrayMap(this.changedComponents);
			reference2ObjectMap.keySet().removeIf(removedTypePredicate);
			return reference2ObjectMap.isEmpty() ? EMPTY : new ComponentChanges(reference2ObjectMap);
		}
	}

	public boolean isEmpty() {
		return this.changedComponents.isEmpty();
	}

	public ComponentChanges.AddedRemovedPair toAddedRemovedPair() {
		if (this.isEmpty()) {
			return ComponentChanges.AddedRemovedPair.EMPTY;
		}
		else {
			ComponentMap.Builder builder = ComponentMap.builder();
			Set<ComponentType<?>> set = Sets.newIdentityHashSet();
			this.changedComponents.forEach((type, value) -> {
				if (value.isPresent()) {
					builder.put(type, value.get());
				}
				else {
					set.add(type);
				}
			});
			return new ComponentChanges.AddedRemovedPair(builder.build(), set);
		}
	}

	@Override
	public boolean equals(Object o) {
		return this == o ? true : o instanceof ComponentChanges componentChanges && this.changedComponents.equals(
				componentChanges.changedComponents);
	}

	@Override
	public int hashCode() {
		return this.changedComponents.hashCode();
	}

	@Override
	public String toString() {
		return toString(this.changedComponents);
	}

	static String toString(Reference2ObjectMap<ComponentType<?>, Optional<?>> changes) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append('{');
		boolean bl = true;
		ObjectIterator var3 = Reference2ObjectMaps.fastIterable(changes).iterator();

		while (var3.hasNext()) {
			Entry<ComponentType<?>, Optional<?>> entry = (Entry<ComponentType<?>, Optional<?>>) var3.next();
			if (bl) {
				bl = false;
			}
			else {
				stringBuilder.append(", ");
			}

			Optional<?> optional = entry.getValue();
			if (optional.isPresent()) {
				stringBuilder.append(entry.getKey());
				stringBuilder.append("=>");
				stringBuilder.append(optional.get());
			}
			else {
				stringBuilder.append("!");
				stringBuilder.append(entry.getKey());
			}
		}

		stringBuilder.append('}');
		return stringBuilder.toString();
	}

	/**
	 * {@code AddedRemovedPair}.
	 */
	public record AddedRemovedPair(ComponentMap added, Set<ComponentType<?>> removed) {

		public static final ComponentChanges.AddedRemovedPair
				EMPTY =
				new ComponentChanges.AddedRemovedPair(ComponentMap.EMPTY, Set.of());
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final Reference2ObjectMap<ComponentType<?>, Optional<?>> changes = new Reference2ObjectArrayMap();

		Builder() {
		}

		public <T> ComponentChanges.Builder add(ComponentType<T> type, T value) {
			this.changes.put(type, Optional.of(value));
			return this;
		}

		public <T> ComponentChanges.Builder remove(ComponentType<T> type) {
			this.changes.put(type, Optional.empty());
			return this;
		}

		public <T> ComponentChanges.Builder add(Component<T> component) {
			return this.add(component.type(), component.value());
		}

		/**
		 * Build.
		 *
		 * @return ComponentChanges — результат операции
		 */
		public ComponentChanges build() {
			return this.changes.isEmpty() ? ComponentChanges.EMPTY : new ComponentChanges(this.changes);
		}
	}

	@FunctionalInterface
	/**
	 * {@code PacketCodecFunction}.
	 */
	interface PacketCodecFunction {

		<T> PacketCodec<? super RegistryByteBuf, T> apply(ComponentType<T> type);
	}

	/**
	 * {@code Type}.
	 */
	record Type(ComponentType<?> type, boolean removed) {

		public static final Codec<ComponentChanges.Type> CODEC = Codec.STRING
				.flatXmap(
						id -> {
							boolean bl = id.startsWith("!");
							if (bl) {
								id = id.substring("!".length());
							}

							Identifier identifier = Identifier.tryParse(id);
							ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(identifier);
							if (componentType == null) {
								return DataResult.error(() -> "No component with type: '" + identifier + "'");
							}
							else {
								return componentType.shouldSkipSerialization()
								       ? DataResult.error(() -> "'" + identifier + "' is not a persistent component")
								       : DataResult.success(new ComponentChanges.Type(componentType, bl));
							}
						},
						type -> {
							ComponentType<?> componentType = type.type();
							Identifier identifier = Registries.DATA_COMPONENT_TYPE.getId(componentType);
							return identifier == null
							       ? DataResult.error(() -> "Unregistered component: " + componentType)
							       : DataResult.success(type.removed() ? "!" + identifier : identifier.toString());
						}
				);

		public Codec<?> getValueCodec() {
			return this.removed ? Codec.EMPTY.codec() : this.type.getCodecOrThrow();
		}
	}
}
