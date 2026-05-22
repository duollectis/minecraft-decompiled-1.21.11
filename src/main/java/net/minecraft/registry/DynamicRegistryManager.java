package net.minecraft.registry;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Менеджер динамических реестров — объединяет несколько реестров в единый
 * контекст поиска. Реализует {@link RegistryWrapper.WrapperLookup}, предоставляя
 * доступ только для чтения ко всем зарегистрированным реестрам.
 *
 * <p>Динамические реестры загружаются из датапаков и могут меняться между
 * сессиями, в отличие от статических реестров {@link Registries}.</p>
 */
public interface DynamicRegistryManager extends RegistryWrapper.WrapperLookup {

	Logger LOGGER = LogUtils.getLogger();

	/** Пустой иммутабельный менеджер без каких-либо реестров. */
	DynamicRegistryManager.Immutable EMPTY = new DynamicRegistryManager.ImmutableImpl(Map.of()).toImmutable();

	@Override
	<E> Optional<Registry<E>> getOptional(RegistryKey<? extends Registry<? extends E>> registryRef);

	default <E> Registry<E> getOrThrow(RegistryKey<? extends Registry<? extends E>> key) {
		return getOptional(key).orElseThrow(() -> new IllegalStateException("Missing registry: " + key));
	}

	Stream<DynamicRegistryManager.Entry<?>> streamAllRegistries();

	@Override
	default Stream<RegistryKey<? extends Registry<?>>> streamAllRegistryKeys() {
		return streamAllRegistries().map(entry -> entry.key);
	}

	/**
	 * Создаёт иммутабельный менеджер из реестра реестров (meta-registry).
	 * Используется для оборачивания статических реестров {@link Registries#REGISTRIES}.
	 *
	 * @param registries реестр, содержащий другие реестры
	 * @return иммутабельный менеджер, делегирующий поиск в переданный реестр
	 */
	static DynamicRegistryManager.Immutable of(Registry<? extends Registry<?>> registries) {
		return new DynamicRegistryManager.Immutable() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> Optional<Registry<T>> getOptional(RegistryKey<? extends Registry<? extends T>> registryRef) {
				Registry<Registry<T>> registry = (Registry<Registry<T>>) registries;
				return registry.getOptionalValue((RegistryKey<Registry<T>>) registryRef);
			}

			@Override
			public Stream<DynamicRegistryManager.Entry<?>> streamAllRegistries() {
				return registries.getEntrySet().stream().map(DynamicRegistryManager.Entry::of);
			}

			@Override
			public DynamicRegistryManager.Immutable toImmutable() {
				return this;
			}
		};
	}

	/**
	 * Создаёт иммутабельный снимок текущего состояния менеджера.
	 * Все реестры замораживаются через {@link Registry#freeze()}.
	 *
	 * @return иммутабельная копия данного менеджера
	 */
	default DynamicRegistryManager.Immutable toImmutable() {
		class Immutablized extends DynamicRegistryManager.ImmutableImpl implements DynamicRegistryManager.Immutable {

			protected Immutablized(Stream<DynamicRegistryManager.Entry<?>> entryStream) {
				super(entryStream);
			}
		}

		return new Immutablized(streamAllRegistries().map(DynamicRegistryManager.Entry::freeze));
	}

	/**
	 * Типизированная пара «ключ реестра — реестр».
	 * Используется при итерации по всем реестрам менеджера.
	 *
	 * @param <T> тип элементов реестра
	 */
	record Entry<T>(RegistryKey<? extends Registry<T>> key, Registry<T> value) {

		@SuppressWarnings("unchecked")
		private static <T, R extends Registry<? extends T>> DynamicRegistryManager.Entry<T> of(
				Map.Entry<? extends RegistryKey<? extends Registry<?>>, R> entry
		) {
			return of((RegistryKey<? extends Registry<?>>) entry.getKey(), entry.getValue());
		}

		@SuppressWarnings("unchecked")
		private static <T> DynamicRegistryManager.Entry<T> of(
				RegistryKey<? extends Registry<?>> key,
				Registry<?> value
		) {
			return new DynamicRegistryManager.Entry<>((RegistryKey<? extends Registry<T>>) key, (Registry<T>) value);
		}

		private DynamicRegistryManager.Entry<T> freeze() {
			return new DynamicRegistryManager.Entry<>(key, value.freeze());
		}
	}

	/** Маркерный интерфейс для иммутабельных менеджеров реестров. */
	interface Immutable extends DynamicRegistryManager {
	}

	/**
	 * Базовая реализация иммутабельного менеджера реестров на основе {@link Map}.
	 * Поддерживает три способа конструирования: из списка реестров, из готовой карты
	 * и из потока {@link Entry}.
	 */
	class ImmutableImpl implements DynamicRegistryManager {

		private final Map<? extends RegistryKey<? extends Registry<?>>, ? extends Registry<?>> registries;

		public ImmutableImpl(List<? extends Registry<?>> registries) {
			this.registries = registries.stream()
					.collect(Collectors.toUnmodifiableMap(Registry::getKey, registry -> registry));
		}

		public ImmutableImpl(Map<? extends RegistryKey<? extends Registry<?>>, ? extends Registry<?>> registries) {
			this.registries = Map.copyOf(registries);
		}

		public ImmutableImpl(Stream<DynamicRegistryManager.Entry<?>> entryStream) {
			this.registries = entryStream.collect(ImmutableMap.toImmutableMap(
					DynamicRegistryManager.Entry::key,
					DynamicRegistryManager.Entry::value
			));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E> Optional<Registry<E>> getOptional(RegistryKey<? extends Registry<? extends E>> registryRef) {
			return Optional.ofNullable(registries.get(registryRef)).map(registry -> (Registry<E>) registry);
		}

		@Override
		public Stream<DynamicRegistryManager.Entry<?>> streamAllRegistries() {
			return registries.entrySet().stream().map(DynamicRegistryManager.Entry::of);
		}
	}
}
