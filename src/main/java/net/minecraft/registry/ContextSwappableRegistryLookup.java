package net.minecraft.registry;

import com.mojang.serialization.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.registry.tag.TagKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Реализация {@link RegistryEntryLookup.RegistryLookup}, которая создаёт
 * «заглушки» для записей и тегов реестра во время датагена.
 *
 * <p>Используется в контексте, где реальные реестры ещё не загружены.
 * Все запрошенные записи и теги кешируются и могут быть впоследствии
 * перенесены в реальный контекст через {@link ContextSwapper}.</p>
 */
public class ContextSwappableRegistryLookup implements RegistryEntryLookup.RegistryLookup {

	final RegistryWrapper.WrapperLookup delegate;
	final EntryLookupImpl entryLookupImpl = new EntryLookupImpl();
	final Map<RegistryKey<Object>, RegistryEntry.Reference<Object>> entries = new HashMap<>();
	final Map<TagKey<Object>, RegistryEntryList.Named<Object>> tags = new HashMap<>();

	public ContextSwappableRegistryLookup(RegistryWrapper.WrapperLookup delegate) {
		this.delegate = delegate;
	}

	@Override
	public <T> Optional<? extends RegistryEntryLookup<T>> getOptional(
			RegistryKey<? extends Registry<? extends T>> registryRef
	) {
		return Optional.of(entryLookupImpl.asEntryLookup());
	}

	/**
	 * Создаёт {@link RegistryOps} с кастомным {@link RegistryOps.RegistryInfoGetter},
	 * который сначала ищет реестр в делегате, а при отсутствии возвращает
	 * экспериментальный контекст на основе {@link EntryLookupImpl}.
	 *
	 * @param delegateOps базовые DynamicOps для сериализации
	 * @param <V>         тип сериализованного представления
	 * @return RegistryOps с подменённым контекстом реестров
	 */
	public <V> RegistryOps<V> createRegistryOps(DynamicOps<V> delegateOps) {
		return RegistryOps.of(
				delegateOps,
				new RegistryOps.RegistryInfoGetter() {
					@Override
					public <T> Optional<RegistryOps.RegistryInfo<T>> getRegistryInfo(
							RegistryKey<? extends Registry<? extends T>> registryRef
					) {
						return delegate
								.getOptional(registryRef)
								.map(RegistryOps.RegistryInfo::fromWrapper)
								.or(() -> Optional.of(new RegistryOps.RegistryInfo<>(
										entryLookupImpl.asEntryOwner(),
										entryLookupImpl.asEntryLookup(),
										Lifecycle.experimental()
								)));
					}
				}
		);
	}

	/**
	 * Создаёт {@link ContextSwapper}, который перекодирует значения из текущего
	 * контекста (с заглушками) в реальный контекст реестров.
	 *
	 * @return swapper для переноса данных в реальный контекст
	 */
	public ContextSwapper createContextSwapper() {
		return new ContextSwapper() {
			@Override
			public <T> DataResult<T> swapContext(
					Codec<T> codec,
					T value,
					RegistryWrapper.WrapperLookup registries
			) {
				return codec
						.encodeStart(createRegistryOps(JavaOps.INSTANCE), value)
						.flatMap(encodedValue -> codec.parse(registries.getOps(JavaOps.INSTANCE), encodedValue));
			}
		};
	}

	/**
	 * Проверяет, были ли созданы какие-либо записи или теги через этот lookup.
	 *
	 * @return {@code true} если есть хотя бы одна запись или тег
	 */
	public boolean hasEntries() {
		return !entries.isEmpty() || !tags.isEmpty();
	}

	/**
	 * Внутренняя реализация lookup, создающая stand-alone записи и теги-заглушки.
	 * Все созданные объекты кешируются в полях внешнего класса.
	 */
	class EntryLookupImpl implements RegistryEntryLookup<Object>, RegistryEntryOwner<Object> {

		@Override
		public Optional<RegistryEntry.Reference<Object>> getOptional(RegistryKey<Object> key) {
			return Optional.of(getOrComputeEntry(key));
		}

		@Override
		public RegistryEntry.Reference<Object> getOrThrow(RegistryKey<Object> key) {
			return getOrComputeEntry(key);
		}

		private RegistryEntry.Reference<Object> getOrComputeEntry(RegistryKey<Object> key) {
			return entries.computeIfAbsent(
					key,
					entryKey -> RegistryEntry.Reference.standAlone(this, (RegistryKey<Object>) entryKey)
			);
		}

		@Override
		public Optional<RegistryEntryList.Named<Object>> getOptional(TagKey<Object> tag) {
			return Optional.of(getOrComputeTag(tag));
		}

		@Override
		public RegistryEntryList.Named<Object> getOrThrow(TagKey<Object> tag) {
			return getOrComputeTag(tag);
		}

		private RegistryEntryList.Named<Object> getOrComputeTag(TagKey<Object> tag) {
			return tags.computeIfAbsent(
					tag,
					tagKey -> RegistryEntryList.of(this, (TagKey<Object>) tagKey)
			);
		}

		@SuppressWarnings("unchecked")
		public <T> RegistryEntryLookup<T> asEntryLookup() {
			return (RegistryEntryLookup<T>) this;
		}

		@SuppressWarnings("unchecked")
		public <T> RegistryEntryOwner<T> asEntryOwner() {
			return (RegistryEntryOwner<T>) this;
		}
	}
}
