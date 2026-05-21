package net.minecraft.client.session.telemetry;

import com.mojang.serialization.*;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code PropertyMap}.
 */
public class PropertyMap {

	final Map<TelemetryEventProperty<?>, Object> backingMap;

	PropertyMap(Map<TelemetryEventProperty<?>, Object> backingMap) {
		this.backingMap = backingMap;
	}

	public static PropertyMap.Builder builder() {
		return new PropertyMap.Builder();
	}

	/**
	 * Создаёт codec.
	 *
	 * @param properties properties
	 *
	 * @return MapCodec — результат операции
	 */
	public static MapCodec<PropertyMap> createCodec(List<TelemetryEventProperty<?>> properties) {
		return new MapCodec<PropertyMap>() {
			public <T> RecordBuilder<T> encode(
					PropertyMap propertyMap,
					DynamicOps<T> dynamicOps,
					RecordBuilder<T> recordBuilder
			) {
				RecordBuilder<T> recordBuilder2 = recordBuilder;

				for (TelemetryEventProperty<?> telemetryEventProperty : properties) {
					recordBuilder2 = this.encode(propertyMap, recordBuilder2, telemetryEventProperty);
				}

				return recordBuilder2;
			}

			private <T, V> RecordBuilder<T> encode(
					PropertyMap map,
					RecordBuilder<T> builder,
					TelemetryEventProperty<V> property
			) {
				V object = map.get(property);
				return object != null ? builder.add(property.id(), object, property.codec()) : builder;
			}

			/**
			 * Decode.
			 *
			 * @param ops ops
			 * @param map map
			 *
			 * @return DataResult — результат операции
			 */
			public <T> DataResult<PropertyMap> decode(DynamicOps<T> ops, MapLike<T> map) {
				DataResult<PropertyMap.Builder> dataResult = DataResult.success(new PropertyMap.Builder());

				for (TelemetryEventProperty<?> telemetryEventProperty : properties) {
					dataResult = this.decode(dataResult, ops, map, telemetryEventProperty);
				}

				return dataResult.map(PropertyMap.Builder::build);
			}

			private <T, V> DataResult<PropertyMap.Builder> decode(
					DataResult<PropertyMap.Builder> result,
					DynamicOps<T> ops,
					MapLike<T> map,
					TelemetryEventProperty<V> property
			) {
				T object = (T) map.get(property.id());
				if (object != null) {
					DataResult<V> dataResult = property.codec().parse(ops, object);
					return result.apply2stable((mapBuilder, value) -> mapBuilder.put(property, (V) value), dataResult);
				}
				else {
					return result;
				}
			}

			/**
			 * Keys.
			 *
			 * @param ops ops
			 *
			 * @return Stream — результат операции
			 */
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return properties.stream().map(TelemetryEventProperty::id).map(ops::createString);
			}
		};
	}

	/**
	 * Get.
	 *
	 * @param property property
	 *
	 * @return @Nullable T — 
	 */
	public <T> @Nullable T get(TelemetryEventProperty<T> property) {
		return (T) this.backingMap.get(property);
	}

	@Override
	public String toString() {
		return this.backingMap.toString();
	}

	/**
	 * Key set.
	 *
	 * @return Set> — результат операции
	 */
	public Set<TelemetryEventProperty<?>> keySet() {
		return this.backingMap.keySet();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final Map<TelemetryEventProperty<?>, Object> backingMap = new Reference2ObjectOpenHashMap();

		Builder() {
		}

		public <T> PropertyMap.Builder put(TelemetryEventProperty<T> property, T value) {
			this.backingMap.put(property, value);
			return this;
		}

		public <T> PropertyMap.Builder putIfNonNull(TelemetryEventProperty<T> property, @Nullable T value) {
			if (value != null) {
				this.backingMap.put(property, value);
			}

			return this;
		}

		public PropertyMap.Builder putAll(PropertyMap map) {
			this.backingMap.putAll(map.backingMap);
			return this;
		}

		/**
		 * Build.
		 *
		 * @return PropertyMap — результат операции
		 */
		public PropertyMap build() {
			return new PropertyMap(this.backingMap);
		}
	}
}
