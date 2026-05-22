package net.minecraft.registry;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.dynamic.ForwardingDynamicOps;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Расширение {@link DynamicOps}, несущее контекст реестров для сериализации.
 * Позволяет кодекам получать доступ к реестрам во время encode/decode операций.
 *
 * @param <T> тип сериализованного представления
 */
public class RegistryOps<T> extends ForwardingDynamicOps<T> {

	private final RegistryOps.RegistryInfoGetter registryInfoGetter;

	/**
	 * Создаёт {@link RegistryOps} с кешированием информации о реестрах из {@link RegistryWrapper.WrapperLookup}.
	 *
	 * @param delegate   базовые ops для сериализации
	 * @param registries источник реестров
	 */
	public static <T> RegistryOps<T> of(DynamicOps<T> delegate, RegistryWrapper.WrapperLookup registries) {
		return of(delegate, new RegistryOps.CachedRegistryInfoGetter(registries));
	}

	public static <T> RegistryOps<T> of(DynamicOps<T> delegate, RegistryOps.RegistryInfoGetter registryInfoGetter) {
		return new RegistryOps<>(delegate, registryInfoGetter);
	}

	/**
	 * Оборачивает {@link Dynamic} в {@link RegistryOps} с указанным контекстом реестров.
	 * Используется для передачи контекста при рекурсивной сериализации.
	 */
	public static <T> Dynamic<T> withRegistry(Dynamic<T> dynamic, RegistryWrapper.WrapperLookup registries) {
		return new Dynamic(registries.getOps(dynamic.getOps()), dynamic.getValue());
	}

	private RegistryOps(DynamicOps<T> delegate, RegistryOps.RegistryInfoGetter registryInfoGetter) {
		super(delegate);
		this.registryInfoGetter = registryInfoGetter;
	}

	/**
	 * Создаёт новый {@link RegistryOps} с другим делегатом, но тем же контекстом реестров.
	 * Используется при смене формата сериализации (например, JSON → NBT).
	 */
	public <U> RegistryOps<U> withDelegate(DynamicOps<U> delegate) {
		return (RegistryOps<U>) (delegate == this.delegate
				? this
				: new RegistryOps<>((DynamicOps<T>) delegate, registryInfoGetter));
	}

	public <E> Optional<RegistryEntryOwner<E>> getOwner(RegistryKey<? extends Registry<? extends E>> registryRef) {
		return registryInfoGetter.getRegistryInfo(registryRef).map(RegistryOps.RegistryInfo::owner);
	}

	public <E> Optional<RegistryEntryLookup<E>> getEntryLookup(RegistryKey<? extends Registry<? extends E>> registryRef) {
		return registryInfoGetter.getRegistryInfo(registryRef).map(RegistryOps.RegistryInfo::entryLookup);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		RegistryOps<?> registryOps = (RegistryOps<?>) o;
		return delegate.equals(registryOps.delegate) && registryInfoGetter.equals(registryOps.registryInfoGetter);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode() * 31 + registryInfoGetter.hashCode();
	}

	/**
	 * Создаёт {@link RecordCodecBuilder}, извлекающий {@link RegistryEntryLookup} из контекста ops.
	 * Используется в кодеках, которым нужен доступ к реестру при декодировании.
	 *
	 * @param registryRef ключ реестра, lookup которого нужно получить
	 */
	public static <E, O> RecordCodecBuilder<O, RegistryEntryLookup<E>> getEntryLookupCodec(
			RegistryKey<? extends Registry<? extends E>> registryRef
	) {
		return Codecs.createContextRetrievalCodec(
				ops -> ops instanceof RegistryOps<?> registryOps
						? registryOps.registryInfoGetter
								.getRegistryInfo(registryRef)
								.map(info -> DataResult.success(info.entryLookup(), info.elementsLifecycle()))
								.orElseGet(() -> DataResult.error(() -> "Unknown registry: " + registryRef))
						: DataResult.error(() -> "Not a registry ops")
		).forGetter(object -> null);
	}

	/**
	 * Создаёт {@link RecordCodecBuilder}, извлекающий конкретную {@link RegistryEntry.Reference} из контекста ops.
	 * Используется для инжекции ссылок на конкретные элементы реестра в кодеки.
	 *
	 * @param key ключ конкретного элемента реестра
	 */
	public static <E, O> RecordCodecBuilder<O, RegistryEntry.Reference<E>> getEntryCodec(RegistryKey<E> key) {
		RegistryKey<? extends Registry<E>> registryKey = RegistryKey.ofRegistry(key.getRegistry());
		return Codecs.<RegistryEntry.Reference<E>>createContextRetrievalCodec(
				ops -> ops instanceof RegistryOps<?> registryOps
						? registryOps.registryInfoGetter
								.getRegistryInfo(registryKey)
								.flatMap(info -> info.entryLookup().getOptional(key))
								.<DataResult<RegistryEntry.Reference<E>>>map(DataResult::success)
								.orElseGet(() -> DataResult.error(() -> "Can't find value: " + key))
						: DataResult.error(() -> "Not a registry ops")
		).forGetter(object -> null);
	}

	/**
	 * Кешированная реализация {@link RegistryInfoGetter}, хранящая результаты запросов
	 * к {@link RegistryWrapper.WrapperLookup} в потокобезопасном кеше.
	 */
	static final class CachedRegistryInfoGetter implements RegistryOps.RegistryInfoGetter {

		private final RegistryWrapper.WrapperLookup registries;
		private final Map<RegistryKey<? extends Registry<?>>, Optional<? extends RegistryOps.RegistryInfo<?>>> cache =
				new ConcurrentHashMap<>();

		public CachedRegistryInfoGetter(RegistryWrapper.WrapperLookup registries) {
			this.registries = registries;
		}

		@Override
		public <E> Optional<RegistryOps.RegistryInfo<E>> getRegistryInfo(
				RegistryKey<? extends Registry<? extends E>> registryRef
		) {
			return (Optional<RegistryOps.RegistryInfo<E>>) cache.computeIfAbsent(registryRef, this::compute);
		}

		private Optional<RegistryOps.RegistryInfo<Object>> compute(RegistryKey<? extends Registry<?>> registryRef) {
			return registries.getOptional(registryRef).map(RegistryOps.RegistryInfo::fromWrapper);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			return o instanceof RegistryOps.CachedRegistryInfoGetter other && registries.equals(other.registries);
		}

		@Override
		public int hashCode() {
			return registries.hashCode();
		}
	}

	/**
	 * Метаданные реестра, используемые в контексте сериализации:
	 * владелец записей, lookup для поиска и lifecycle элементов.
	 */
	public record RegistryInfo<T>(
			RegistryEntryOwner<T> owner,
			RegistryEntryLookup<T> entryLookup,
			Lifecycle elementsLifecycle
	) {

		public static <T> RegistryOps.RegistryInfo<T> fromWrapper(RegistryWrapper.Impl<T> wrapper) {
			return new RegistryOps.RegistryInfo<>(wrapper, wrapper, wrapper.getLifecycle());
		}
	}

	/**
	 * Источник информации о реестрах для {@link RegistryOps}.
	 */
	public interface RegistryInfoGetter {

		<T> Optional<RegistryOps.RegistryInfo<T>> getRegistryInfo(
				RegistryKey<? extends Registry<? extends T>> registryRef
		);
	}
}
