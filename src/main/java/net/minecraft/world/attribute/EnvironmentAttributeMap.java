package net.minecraft.world.attribute;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code EnvironmentAttributeMap}.
 */
public final class EnvironmentAttributeMap {

	public static final EnvironmentAttributeMap EMPTY = new EnvironmentAttributeMap(Map.of());
	@SuppressWarnings("unchecked")
	public static final Codec<EnvironmentAttributeMap> CODEC = Codec.lazyInitialized(
			() -> Codec
					.dispatchedMap(
							EnvironmentAttributes.CODEC,
							Util.memoize(EnvironmentAttributeMap.Entry::createCodec)
					)
					.xmap(
							EnvironmentAttributeMap::fromRawMap,
							map -> (Map) map.entries
					)
	);

	@SuppressWarnings("unchecked")
	private static EnvironmentAttributeMap fromRawMap(Map<?, ?> raw) {
		return new EnvironmentAttributeMap((Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>>) raw);
	}

	public static final Codec<EnvironmentAttributeMap> NETWORK_CODEC = CODEC.xmap(
			EnvironmentAttributeMap::retainSyncedAttributes, EnvironmentAttributeMap::retainSyncedAttributes
	);
	public static final Codec<EnvironmentAttributeMap> POSITIONAL_CODEC = CODEC.validate(map -> {
		List<EnvironmentAttribute<?>>
				list =
				map.keySet().stream().filter(attribute -> !attribute.isPositional()).toList();
		return !list.isEmpty() ? DataResult.error(() -> "The following attributes cannot be positional: " + list)
		                       : DataResult.success(map);
	});
	final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries;

	private static EnvironmentAttributeMap retainSyncedAttributes(EnvironmentAttributeMap map) {
		return new EnvironmentAttributeMap(Map.copyOf(Maps.filterKeys(map.entries, EnvironmentAttribute::isSynced)));
	}

	EnvironmentAttributeMap(Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries) {
		this.entries = entries;
	}

	public static EnvironmentAttributeMap.Builder builder() {
		return new EnvironmentAttributeMap.Builder();
	}

	public <Value> EnvironmentAttributeMap.@Nullable Entry<Value, ?> getEntry(EnvironmentAttribute<Value> key) {
		return (EnvironmentAttributeMap.Entry<Value, ?>) this.entries.get(key);
	}

	/**
	 * Apply.
	 *
	 * @param key key
	 * @param value value
	 *
	 * @return Value — результат операции
	 */
	public <Value> Value apply(EnvironmentAttribute<Value> key, Value value) {
		EnvironmentAttributeMap.Entry<Value, ?> entry = this.getEntry(key);
		return entry != null ? entry.apply(value) : value;
	}

	/**
	 * Contains key.
	 *
	 * @param key key
	 *
	 * @return boolean — результат операции
	 */
	public boolean containsKey(EnvironmentAttribute<?> key) {
		return this.entries.containsKey(key);
	}

	/**
	 * Key set.
	 *
	 * @return Set> — результат операции
	 */
	public Set<EnvironmentAttribute<?>> keySet() {
		return this.entries.keySet();
	}

	@Override
	public boolean equals(Object o) {
		return o == this ? true : o instanceof EnvironmentAttributeMap environmentAttributeMap && this.entries.equals(
				environmentAttributeMap.entries);
	}

	@Override
	public int hashCode() {
		return this.entries.hashCode();
	}

	@Override
	public String toString() {
		return this.entries.toString();
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries = new HashMap<>();

		Builder() {
		}

		public EnvironmentAttributeMap.Builder addAll(EnvironmentAttributeMap map) {
			this.entries.putAll(map.entries);
			return this;
		}

		public <Value, Parameter> EnvironmentAttributeMap.Builder with(
				EnvironmentAttribute<Value> key,
				EnvironmentAttributeModifier<Value, Parameter> modifier,
				Parameter param
		) {
			key.getType().validate(modifier);
			this.entries.put(key, new EnvironmentAttributeMap.Entry<>(param, modifier));
			return this;
		}

		public <Value> EnvironmentAttributeMap.Builder with(EnvironmentAttribute<Value> key, Value value) {
			return this.with(key, EnvironmentAttributeModifier.override(), value);
		}

		/**
		 * Build.
		 *
		 * @return EnvironmentAttributeMap — результат операции
		 */
		public EnvironmentAttributeMap build() {
			return this.entries.isEmpty() ? EnvironmentAttributeMap.EMPTY
			                              : new EnvironmentAttributeMap(Map.copyOf(this.entries));
		}
	}

	/**
	 * {@code Entry}.
	 */
	public record Entry<Value, Argument>(Argument argument, EnvironmentAttributeModifier<Value, Argument> modifier) {

		private static <Value> Codec<EnvironmentAttributeMap.Entry<Value, ?>> createCodec(EnvironmentAttribute<Value> attribute) {
			Codec<EnvironmentAttributeMap.Entry<Value, ?>> codec = attribute.getType()
			                                                                .modifierCodec()
			                                                                .dispatch(
					                                                                "modifier",
					                                                                EnvironmentAttributeMap.Entry::modifier,
					                                                                Util.memoize(modifier -> createModifierDependentCodec(
							                                                                attribute,
							                                                                modifier
					                                                                ))
			                                                                );
			return Codec.either(attribute.getCodec(), codec)
			            .xmap(
					            either -> (EnvironmentAttributeMap.Entry<Value, ?>) either.map(
							            value -> new EnvironmentAttributeMap.Entry<>(
									            value,
									            EnvironmentAttributeModifier.override()
							            ),
							            entry -> entry
					            ),
					            entry -> entry.modifier == EnvironmentAttributeModifier.override()
					                     ? Either.<Value, EnvironmentAttributeMap.Entry<Value, ?>>left((Value) entry.argument())
					                     : Either.right(entry)
			            );
		}

		private static <Value, Argument> MapCodec<EnvironmentAttributeMap.Entry<Value, Argument>> createModifierDependentCodec(
				EnvironmentAttribute<Value> attribute, EnvironmentAttributeModifier<Value, Argument> modifier
		) {
			return RecordCodecBuilder.mapCodec(
					instance -> instance
							.group(modifier
									.argumentCodec(attribute)
									.fieldOf("argument")
									.forGetter(EnvironmentAttributeMap.Entry::argument))
							.apply(instance, argument -> new EnvironmentAttributeMap.Entry<>(argument, modifier))
			);
		}

		/**
		 * Apply.
		 *
		 * @param value value
		 *
		 * @return Value — результат операции
		 */
		public Value apply(Value value) {
			return this.modifier.apply(value, this.argument);
		}
	}
}
