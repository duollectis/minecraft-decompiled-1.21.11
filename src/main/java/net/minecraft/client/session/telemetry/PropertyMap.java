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

/**
 * Типобезопасное хранилище свойств телеметрии, отображающее
 * {@link TelemetryEventProperty} на соответствующие значения.
 * Используется для сборки данных перед отправкой события телеметрии.
 */
@Environment(EnvType.CLIENT)
public class PropertyMap {

	final Map<TelemetryEventProperty<?>, Object> backingMap;

	PropertyMap(Map<TelemetryEventProperty<?>, Object> backingMap) {
		this.backingMap = backingMap;
	}

	public static PropertyMap.Builder builder() {
		return new PropertyMap.Builder();
	}

	/**
	 * Создаёт {@link MapCodec} для сериализации/десериализации {@link PropertyMap}
	 * по заданному списку свойств. Только свойства из этого списка участвуют
	 * в кодировании и декодировании.
	 *
	 * @param properties список свойств, включаемых в codec
	 * @return {@link MapCodec} для {@link PropertyMap}
	 */
	public static MapCodec<PropertyMap> createCodec(List<TelemetryEventProperty<?>> properties) {
		return new MapCodec<PropertyMap>() {
			public <T> RecordBuilder<T> encode(
				PropertyMap propertyMap,
				DynamicOps<T> dynamicOps,
				RecordBuilder<T> recordBuilder
			) {
				for (TelemetryEventProperty<?> property : properties) {
					recordBuilder = encode(propertyMap, recordBuilder, property);
				}

				return recordBuilder;
			}

			private <T, V> RecordBuilder<T> encode(
				PropertyMap map,
				RecordBuilder<T> builder,
				TelemetryEventProperty<V> property
			) {
				V value = map.get(property);
				return value != null ? builder.add(property.id(), value, property.codec()) : builder;
			}

			public <T> DataResult<PropertyMap> decode(DynamicOps<T> ops, MapLike<T> map) {
				DataResult<PropertyMap.Builder> result = DataResult.success(new PropertyMap.Builder());

				for (TelemetryEventProperty<?> property : properties) {
					result = decode(result, ops, map, property);
				}

				return result.map(PropertyMap.Builder::build);
			}

			private <T, V> DataResult<PropertyMap.Builder> decode(
				DataResult<PropertyMap.Builder> result,
				DynamicOps<T> ops,
				MapLike<T> map,
				TelemetryEventProperty<V> property
			) {
				T raw = (T) map.get(property.id());
				if (raw == null) {
					return result;
				}

				DataResult<V> parsed = property.codec().parse(ops, raw);
				return result.apply2stable((mapBuilder, value) -> mapBuilder.put(property, (V) value), parsed);
			}

			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return properties.stream().map(TelemetryEventProperty::id).map(ops::createString);
			}
		};
	}

	public <T> @Nullable T get(TelemetryEventProperty<T> property) {
		return (T) backingMap.get(property);
	}

	@Override
	public String toString() {
		return backingMap.toString();
	}

	public Set<TelemetryEventProperty<?>> keySet() {
		return backingMap.keySet();
	}

	/**
	 * Строитель для пошаговой сборки {@link PropertyMap}.
	 * Не является потокобезопасным — предназначен для использования
	 * в одном потоке перед финальным вызовом {@link #build()}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final Map<TelemetryEventProperty<?>, Object> backingMap = new Reference2ObjectOpenHashMap<>();

		Builder() {
		}

		public <T> PropertyMap.Builder put(TelemetryEventProperty<T> property, T value) {
			backingMap.put(property, value);
			return this;
		}

		public <T> PropertyMap.Builder putIfNonNull(TelemetryEventProperty<T> property, @Nullable T value) {
			if (value != null) {
				backingMap.put(property, value);
			}

			return this;
		}

		public PropertyMap.Builder putAll(PropertyMap map) {
			backingMap.putAll(map.backingMap);
			return this;
		}

		public PropertyMap build() {
			return new PropertyMap(backingMap);
		}
	}
}
