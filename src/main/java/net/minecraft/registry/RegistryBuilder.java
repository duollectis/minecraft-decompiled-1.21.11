package net.minecraft.registry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Строитель реестров для генерации данных (datagen).
 *
 * <p>Позволяет декларативно описать набор реестров с их содержимым через
 * {@link BootstrapFunction}, а затем получить {@link RegistryWrapper.WrapperLookup}
 * для использования в кодеках и сериализации. Поддерживает два режима:
 * <ul>
 *   <li>{@link #createWrapperLookup(DynamicRegistryManager)} — создаёт обёртку
 *       только из зарегистрированных в этом билдере реестров;</li>
 *   <li>{@link #createWrapperLookup(DynamicRegistryManager, RegistryWrapper.WrapperLookup, RegistryCloner.CloneableRegistries)}
 *       — создаёт полную обёртку с патчами поверх базовых реестров.</li>
 * </ul>
 */
public class RegistryBuilder {

	private final List<RegistryInfo<?>> registries = new ArrayList<>();

	static <T> RegistryEntryLookup<T> toLookup(RegistryWrapper.Impl<T> wrapper) {
		return new EntryListCreatingLookup<T>(wrapper) {
			@Override
			public Optional<RegistryEntry.Reference<T>> getOptional(RegistryKey<T> key) {
				return wrapper.getOptional(key);
			}
		};
	}

	static <T> RegistryWrapper.Impl<T> createWrapper(
			RegistryKey<? extends Registry<? extends T>> registryRef,
			Lifecycle lifecycle,
			RegistryEntryOwner<T> owner,
			Map<RegistryKey<T>, RegistryEntry.Reference<T>> entries
	) {
		return new UntaggedLookup<T>(owner) {
			@Override
			public RegistryKey<? extends Registry<? extends T>> getKey() {
				return registryRef;
			}

			@Override
			public Lifecycle getLifecycle() {
				return lifecycle;
			}

			@Override
			public Optional<RegistryEntry.Reference<T>> getOptional(RegistryKey<T> key) {
				return Optional.ofNullable(entries.get(key));
			}

			@Override
			public Stream<RegistryEntry.Reference<T>> streamEntries() {
				return entries.values().stream();
			}
		};
	}

	public <T> RegistryBuilder addRegistry(
			RegistryKey<? extends Registry<T>> registryRef,
			Lifecycle lifecycle,
			BootstrapFunction<T> bootstrapFunction
	) {
		registries.add(new RegistryInfo<>(registryRef, lifecycle, bootstrapFunction));
		return this;
	}

	public <T> RegistryBuilder addRegistry(
			RegistryKey<? extends Registry<T>> registryRef,
			BootstrapFunction<T> bootstrapFunction
	) {
		return addRegistry(registryRef, Lifecycle.stable(), bootstrapFunction);
	}

	private Registries createBootstrappedRegistries(DynamicRegistryManager registryManager) {
		Registries bootstrapped = Registries.of(
				registryManager,
				registries.stream().map(RegistryInfo::key)
		);
		registries.forEach(registry -> registry.runBootstrap(bootstrapped));
		return bootstrapped;
	}

	/**
	 * Создаёт {@link RegistryWrapper.WrapperLookup} из зарегистрированных реестров и базового менеджера.
	 *
	 * <p>Реестры из {@code registryManager} добавляются как есть (без патчей),
	 * а реестры из этого билдера оборачиваются в {@link UntaggedDelegatingLookup}
	 * с {@link AnyOwner} в качестве владельца записей.</p>
	 */
	private static RegistryWrapper.WrapperLookup createWrapperLookup(
			AnyOwner entryOwner,
			DynamicRegistryManager registryManager,
			Stream<RegistryWrapper.Impl<?>> wrappers
	) {
		record WrapperInfoPair<T>(RegistryWrapper.Impl<T> lookup, RegistryOps.RegistryInfo<T> opsInfo) {

			public static <T> WrapperInfoPair<T> of(RegistryWrapper.Impl<T> wrapper) {
				return new WrapperInfoPair<>(
						new UntaggedDelegatingLookup<>(wrapper, wrapper),
						RegistryOps.RegistryInfo.fromWrapper(wrapper)
				);
			}

			public static <T> WrapperInfoPair<T> of(AnyOwner owner, RegistryWrapper.Impl<T> wrapper) {
				return new WrapperInfoPair<>(
						new UntaggedDelegatingLookup<>(owner.downcast(), wrapper),
						new RegistryOps.RegistryInfo<>(owner.downcast(), wrapper, wrapper.getLifecycle())
				);
			}
		}

		final Map<RegistryKey<? extends Registry<?>>, WrapperInfoPair<?>> pairMap = new HashMap<>();
		registryManager.streamAllRegistries()
				.forEach(registry -> pairMap.put(registry.key(), WrapperInfoPair.of(registry.value())));
		wrappers.forEach(wrapper -> pairMap.put(wrapper.getKey(), WrapperInfoPair.of(entryOwner, wrapper)));

		return new RegistryWrapper.WrapperLookup() {
			@Override
			public Stream<RegistryKey<? extends Registry<?>>> streamAllRegistryKeys() {
				return pairMap.keySet().stream();
			}

			<T> Optional<WrapperInfoPair<T>> get(RegistryKey<? extends Registry<? extends T>> registryRef) {
				return Optional.ofNullable((WrapperInfoPair<T>) pairMap.get(registryRef));
			}

			@Override
			public <T> Optional<RegistryWrapper.Impl<T>> getOptional(
					RegistryKey<? extends Registry<? extends T>> registryRef
			) {
				return get(registryRef).map(WrapperInfoPair::lookup);
			}

			@Override
			public <V> RegistryOps<V> getOps(DynamicOps<V> delegate) {
				return RegistryOps.of(
						delegate,
						new RegistryOps.RegistryInfoGetter() {
							@Override
							public <T> Optional<RegistryOps.RegistryInfo<T>> getRegistryInfo(
									RegistryKey<? extends Registry<? extends T>> registryRef
							) {
								return get(registryRef).map(WrapperInfoPair::opsInfo);
							}
						}
				);
			}
		};
	}

	public RegistryWrapper.WrapperLookup createWrapperLookup(DynamicRegistryManager registryManager) {
		Registries bootstrapped = createBootstrappedRegistries(registryManager);
		Stream<RegistryWrapper.Impl<?>> stream = registries.stream()
				.map(info -> info.init(bootstrapped).toWrapper(bootstrapped.owner));
		RegistryWrapper.WrapperLookup wrapperLookup = createWrapperLookup(bootstrapped.owner, registryManager, stream);
		bootstrapped.checkUnreferencedKeys();
		bootstrapped.checkOrphanedValues();
		bootstrapped.throwErrors();
		return wrapperLookup;
	}

	private RegistryWrapper.WrapperLookup createFullWrapperLookup(
			DynamicRegistryManager registryManager,
			RegistryWrapper.WrapperLookup base,
			RegistryCloner.CloneableRegistries cloneableRegistries,
			Map<RegistryKey<? extends Registry<?>>, InitializedRegistry<?>> initializedRegistries,
			RegistryWrapper.WrapperLookup patches
	) {
		AnyOwner anyOwner = new AnyOwner();
		MutableObject<RegistryWrapper.WrapperLookup> lazyWrapper = new MutableObject<>();
		List<RegistryWrapper.Impl<?>> patchedWrappers = initializedRegistries.keySet()
				.stream()
				.map(registryRef -> applyPatches(
						anyOwner,
						cloneableRegistries,
						(RegistryKey<? extends Registry<? extends Object>>) registryRef,
						patches,
						base,
						lazyWrapper
				))
				.collect(Collectors.toUnmodifiableList());
		RegistryWrapper.WrapperLookup wrapperLookup = createWrapperLookup(anyOwner, registryManager, patchedWrappers.stream());
		lazyWrapper.setValue(wrapperLookup);
		return wrapperLookup;
	}

	/**
	 * Применяет патчи поверх базового реестра, создавая ленивые ссылки на клонированные значения.
	 *
	 * <p>Записи из {@code patches} имеют приоритет над записями из {@code base}.
	 * Клонирование выполняется лениво при первом обращении к значению через
	 * {@link LazyReferenceEntry}, чтобы избежать циклических зависимостей при инициализации.</p>
	 */
	private <T> RegistryWrapper.Impl<T> applyPatches(
			RegistryEntryOwner<T> owner,
			RegistryCloner.CloneableRegistries cloneableRegistries,
			RegistryKey<? extends Registry<? extends T>> registryRef,
			RegistryWrapper.WrapperLookup patches,
			RegistryWrapper.WrapperLookup base,
			MutableObject<RegistryWrapper.WrapperLookup> lazyWrapper
	) {
		RegistryCloner<T> registryCloner = cloneableRegistries.get(registryRef);
		if (registryCloner == null) {
			throw new NullPointerException("No cloner for " + registryRef.getValue());
		}

		Map<RegistryKey<T>, RegistryEntry.Reference<T>> entryMap = new HashMap<>();
		RegistryWrapper.Impl<T> patchImpl = patches.getOrThrow(registryRef);

		patchImpl.streamEntries().forEach(entry -> {
			RegistryKey<T> registryKey = entry.registryKey();
			LazyReferenceEntry<T> lazyEntry = new LazyReferenceEntry<>(owner, registryKey);
			lazyEntry.supplier = () -> registryCloner.clone(
					(T) entry.value(),
					patches,
					(RegistryWrapper.WrapperLookup) lazyWrapper.get()
			);
			entryMap.put(registryKey, lazyEntry);
		});

		RegistryWrapper.Impl<T> baseImpl = base.getOrThrow(registryRef);
		baseImpl.streamEntries().forEach(entry -> {
			RegistryKey<T> registryKey = entry.registryKey();
			entryMap.computeIfAbsent(registryKey, key -> {
				LazyReferenceEntry<T> lazyEntry = new LazyReferenceEntry<>(owner, registryKey);
				lazyEntry.supplier = () -> registryCloner.clone(
						(T) entry.value(),
						base,
						(RegistryWrapper.WrapperLookup) lazyWrapper.get()
				);
				return lazyEntry;
			});
		});

		Lifecycle lifecycle = patchImpl.getLifecycle().add(baseImpl.getLifecycle());
		return createWrapper(registryRef, lifecycle, owner, entryMap);
	}

	/**
	 * Создаёт пару {@link FullPatchesRegistriesPair}: полный реестр с патчами и реестр только с патчами.
	 *
	 * <p>Используется при загрузке датапаков: {@code patches} содержит только новые/изменённые
	 * записи, а {@code full} — объединение базовых реестров и патчей.</p>
	 *
	 * @param baseRegistryManager базовый менеджер реестров (ванильные данные)
	 * @param registries          базовая обёртка для поиска существующих записей
	 * @param cloneableRegistries реестр клонеров для глубокого копирования значений
	 * @return пара (полный реестр, только патчи)
	 */
	public FullPatchesRegistriesPair createWrapperLookup(
			DynamicRegistryManager baseRegistryManager,
			RegistryWrapper.WrapperLookup registries,
			RegistryCloner.CloneableRegistries cloneableRegistries
	) {
		Registries bootstrapped = createBootstrappedRegistries(baseRegistryManager);
		Map<RegistryKey<? extends Registry<?>>, InitializedRegistry<?>> initializedMap = new HashMap<>();
		this.registries.stream()
				.map(info -> info.init(bootstrapped))
				.forEach(registry -> initializedMap.put(registry.key(), registry));

		Set<RegistryKey<? extends Registry<?>>> baseKeys = baseRegistryManager
				.streamAllRegistryKeys()
				.collect(Collectors.toUnmodifiableSet());

		registries.streamAllRegistryKeys()
				.filter(key -> !baseKeys.contains(key))
				.forEach(key -> initializedMap.putIfAbsent(
						(RegistryKey<? extends Registry<?>>) key,
						new InitializedRegistry<>(
								(RegistryKey<? extends Registry<?>>) key,
								Lifecycle.stable(),
								Map.of()
						)
				));

		Stream<RegistryWrapper.Impl<?>> patchStream = initializedMap.values()
				.stream()
				.map(registry -> registry.toWrapper(bootstrapped.owner));
		RegistryWrapper.WrapperLookup patchesLookup = createWrapperLookup(bootstrapped.owner, baseRegistryManager, patchStream);
		bootstrapped.checkOrphanedValues();
		bootstrapped.throwErrors();

		RegistryWrapper.WrapperLookup fullLookup = createFullWrapperLookup(
				baseRegistryManager,
				registries,
				cloneableRegistries,
				initializedMap,
				patchesLookup
		);
		return new FullPatchesRegistriesPair(fullLookup, patchesLookup);
	}

	/**
	 * Универсальный владелец записей реестра, используемый в datagen-контексте.
	 *
	 * <p>Принимает любой тип через небезопасное приведение — это намеренно,
	 * так как в datagen все реестры используют единый {@code AnyOwner}.</p>
	 */
	static class AnyOwner implements RegistryEntryOwner<Object> {

		@SuppressWarnings("unchecked")
		public <T> RegistryEntryOwner<T> downcast() {
			return (RegistryEntryOwner<T>) this;
		}
	}

	/**
	 * Функция начальной загрузки реестра — регистрирует все элементы через {@link Registerable}.
	 */
	@FunctionalInterface
	public interface BootstrapFunction<T> {

		void run(Registerable<T> registerable);
	}

	record EntryAssociatedValue<T>(
			RegisteredValue<T> value,
			Optional<RegistryEntry.Reference<T>> entry
	) {
	}

	/**
	 * Базовый класс для lookup-объектов, которые умеют создавать именованные списки тегов.
	 *
	 * <p>Метод {@link #getOptional(TagKey)} всегда возвращает {@code Optional.of(...)},
	 * создавая пустой именованный список — это нужно для datagen, где теги ещё не загружены.</p>
	 */
	abstract static class EntryListCreatingLookup<T> implements RegistryEntryLookup<T> {

		protected final RegistryEntryOwner<T> entryOwner;

		protected EntryListCreatingLookup(RegistryEntryOwner<T> entryOwner) {
			this.entryOwner = entryOwner;
		}

		@Override
		public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> tag) {
			return Optional.of(RegistryEntryList.of(entryOwner, tag));
		}
	}

	/**
	 * Пара из полного реестра (base + patches) и реестра только с патчами.
	 *
	 * @param full    полный реестр, объединяющий базовые данные и патчи датапака
	 * @param patches реестр только с записями из датапака (без базовых данных)
	 */
	public record FullPatchesRegistriesPair(
			RegistryWrapper.WrapperLookup full,
			RegistryWrapper.WrapperLookup patches
	) {
	}

	/**
	 * Инициализированный реестр с финальным набором записей, готовый к созданию обёртки.
	 */
	record InitializedRegistry<T>(
			RegistryKey<? extends Registry<? extends T>> key,
			Lifecycle lifecycle,
			Map<RegistryKey<T>, EntryAssociatedValue<T>> values
	) {

		public RegistryWrapper.Impl<T> toWrapper(AnyOwner anyOwner) {
			Map<RegistryKey<T>, RegistryEntry.Reference<T>> entryMap = values.entrySet()
					.stream()
					.collect(Collectors.toUnmodifiableMap(
							Entry::getKey,
							entry -> {
								EntryAssociatedValue<T> associated = entry.getValue();
								RegistryEntry.Reference<T> reference = associated.entry()
										.orElseGet(() -> RegistryEntry.Reference.standAlone(
												anyOwner.downcast(),
												entry.getKey()
										));
								reference.setValue(associated.value().value());
								return reference;
							}
					));
			return createWrapper(key, lifecycle, anyOwner.downcast(), entryMap);
		}
	}

	/**
	 * Ленивая ссылка на запись реестра, вычисляющая значение при первом обращении.
	 *
	 * <p>Используется в {@link #applyPatches} для разрыва циклических зависимостей:
	 * значение клонируется только тогда, когда оно реально запрошено.</p>
	 */
	static class LazyReferenceEntry<T> extends RegistryEntry.Reference<T> {

		@Nullable Supplier<T> supplier;

		protected LazyReferenceEntry(RegistryEntryOwner<T> owner, @Nullable RegistryKey<T> key) {
			super(RegistryEntry.Reference.Type.STAND_ALONE, owner, key, null);
		}

		@Override
		public void setValue(T value) {
			super.setValue(value);
			supplier = null;
		}

		@Override
		public T value() {
			if (supplier != null) {
				setValue(supplier.get());
			}

			return super.value();
		}
	}

	record RegisteredValue<T>(T value, Lifecycle lifecycle) {
	}

	/**
	 * Контекст выполнения bootstrap-функций: хранит зарегистрированные значения,
	 * lookup-объекты и список ошибок.
	 *
	 * <p>Создаётся через {@link #of} и передаётся в {@link RegistryInfo#runBootstrap}.
	 * После выполнения всех bootstrap-функций вызываются методы проверки
	 * {@link #checkUnreferencedKeys()}, {@link #checkOrphanedValues()}, {@link #throwErrors()}.</p>
	 */
	record Registries(
			AnyOwner owner,
			StandAloneEntryCreatingLookup lookup,
			Map<Identifier, RegistryEntryLookup<?>> registries,
			Map<RegistryKey<?>, RegisteredValue<?>> registeredValues,
			List<RuntimeException> errors
	) {

		public static Registries of(
				DynamicRegistryManager dynamicRegistryManager,
				Stream<RegistryKey<? extends Registry<?>>> registryRefs
		) {
			AnyOwner anyOwner = new AnyOwner();
			List<RuntimeException> errorList = new ArrayList<>();
			StandAloneEntryCreatingLookup standAloneLookup = new StandAloneEntryCreatingLookup(anyOwner);
			Builder<Identifier, RegistryEntryLookup<?>> builder = ImmutableMap.builder();
			dynamicRegistryManager.streamAllRegistries()
					.forEach(entry -> builder.put(entry.key().getValue(), toLookup(entry.value())));
			registryRefs.forEach(registryRef -> builder.put(registryRef.getValue(), standAloneLookup));
			return new Registries(
					anyOwner,
					standAloneLookup,
					builder.build(),
					new HashMap<>(),
					errorList
			);
		}

		public <T> Registerable<T> createRegisterable() {
			return new Registerable<T>() {
				@Override
				public RegistryEntry.Reference<T> register(RegistryKey<T> key, T value, Lifecycle lifecycle) {
					RegisteredValue<?> existing = Registries.this.registeredValues
							.put(key, new RegisteredValue<>(value, lifecycle));
					if (existing != null) {
						Registries.this.errors.add(new IllegalStateException(
								"Duplicate registration for " + key + ", new=" + value + ", old=" + existing.value
						));
					}

					return Registries.this.lookup.getOrCreate(key);
				}

				@Override
				public <S> RegistryEntryLookup<S> getRegistryLookup(
						RegistryKey<? extends Registry<? extends S>> registryRef
				) {
					return (RegistryEntryLookup<S>) Registries.this.registries.getOrDefault(
							registryRef.getValue(),
							Registries.this.lookup
					);
				}
			};
		}

		public void checkOrphanedValues() {
			registeredValues.forEach((key, value) -> errors.add(
					new IllegalStateException("Orpaned value " + value.value + " for key " + key)
			));
		}

		public void checkUnreferencedKeys() {
			for (RegistryKey<Object> registryKey : lookup.keysToEntries.keySet()) {
				errors.add(new IllegalStateException("Unreferenced key: " + registryKey));
			}
		}

		public void throwErrors() {
			if (errors.isEmpty()) {
				return;
			}

			IllegalStateException aggregate = new IllegalStateException("Errors during registry creation");
			for (RuntimeException error : errors) {
				aggregate.addSuppressed(error);
			}

			throw aggregate;
		}
	}

	/**
	 * Метаданные одного реестра: ключ, жизненный цикл и bootstrap-функция.
	 */
	record RegistryInfo<T>(
			RegistryKey<? extends Registry<T>> key,
			Lifecycle lifecycle,
			BootstrapFunction<T> bootstrap
	) {

		void runBootstrap(Registries registries) {
			bootstrap.run(registries.createRegisterable());
		}

		/**
		 * Инициализирует реестр: извлекает зарегистрированные значения из общего контекста
		 * и связывает их с соответствующими ссылками на записи.
		 */
		public InitializedRegistry<T> init(Registries registries) {
			Map<RegistryKey<T>, EntryAssociatedValue<T>> entryMap = new HashMap<>();
			Iterator<Entry<RegistryKey<?>, RegisteredValue<?>>> iterator =
					registries.registeredValues.entrySet().iterator();

			while (iterator.hasNext()) {
				Entry<RegistryKey<?>, RegisteredValue<?>> entry = iterator.next();
				RegistryKey<?> registryKey = entry.getKey();
				if (!registryKey.isOf(key)) {
					continue;
				}

				RegisteredValue<T> registeredValue = (RegisteredValue<T>) entry.getValue();
				RegistryEntry.Reference<T> reference =
						(RegistryEntry.Reference<T>) registries.lookup.keysToEntries.remove(registryKey);
				entryMap.put(
						(RegistryKey<T>) registryKey,
						new EntryAssociatedValue<>(registeredValue, Optional.ofNullable(reference))
				);
				iterator.remove();
			}

			return new InitializedRegistry<>(key, lifecycle, entryMap);
		}
	}

	/**
	 * Lookup, создающий stand-alone ссылки на записи при первом обращении.
	 *
	 * <p>Используется как fallback в datagen-контексте: если запись ещё не зарегистрирована,
	 * создаётся пустая stand-alone ссылка, которая будет заполнена позже.</p>
	 */
	static class StandAloneEntryCreatingLookup extends EntryListCreatingLookup<Object> {

		final Map<RegistryKey<Object>, RegistryEntry.Reference<Object>> keysToEntries = new HashMap<>();

		public StandAloneEntryCreatingLookup(RegistryEntryOwner<Object> registryEntryOwner) {
			super(registryEntryOwner);
		}

		@Override
		public Optional<RegistryEntry.Reference<Object>> getOptional(RegistryKey<Object> key) {
			return Optional.of(getOrCreate(key));
		}

		@SuppressWarnings("unchecked")
		<T> RegistryEntry.Reference<T> getOrCreate(RegistryKey<T> key) {
			RegistryKey<Object> objectKey = (RegistryKey<Object>) (RegistryKey<?>) key;
			return (RegistryEntry.Reference<T>) keysToEntries
					.computeIfAbsent(objectKey, k -> RegistryEntry.Reference.standAlone(entryOwner, k));
		}
	}

	/**
	 * Делегирующий lookup без поддержки тегов, используемый в datagen-контексте.
	 */
	static class UntaggedDelegatingLookup<T>
			extends UntaggedLookup<T>
			implements RegistryWrapper.Impl.Delegating<T> {

		private final RegistryWrapper.Impl<T> base;

		UntaggedDelegatingLookup(RegistryEntryOwner<T> entryOwner, RegistryWrapper.Impl<T> base) {
			super(entryOwner);
			this.base = base;
		}

		@Override
		public RegistryWrapper.Impl<T> getBase() {
			return base;
		}
	}

	/**
	 * Абстрактный lookup без поддержки тегов — бросает {@link UnsupportedOperationException}
	 * при попытке получить теги, так как в datagen они недоступны.
	 */
	abstract static class UntaggedLookup<T>
			extends EntryListCreatingLookup<T>
			implements RegistryWrapper.Impl<T> {

		protected UntaggedLookup(RegistryEntryOwner<T> registryEntryOwner) {
			super(registryEntryOwner);
		}

		@Override
		public Stream<RegistryEntryList.Named<T>> getTags() {
			throw new UnsupportedOperationException("Tags are not available in datagen");
		}
	}
}
