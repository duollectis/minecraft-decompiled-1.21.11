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
 * {@code ContextSwappableRegistryLookup}.
 */
public class ContextSwappableRegistryLookup implements RegistryEntryLookup.RegistryLookup {

	final RegistryWrapper.WrapperLookup delegate;
	final ContextSwappableRegistryLookup.EntryLookupImpl
			entryLookupImpl =
			new ContextSwappableRegistryLookup.EntryLookupImpl();
	final Map<RegistryKey<Object>, RegistryEntry.Reference<Object>> entries = new HashMap<>();
	final Map<TagKey<Object>, RegistryEntryList.Named<Object>> tags = new HashMap<>();

	public ContextSwappableRegistryLookup(RegistryWrapper.WrapperLookup delegate) {
		this.delegate = delegate;
	}

	@Override
	public <T> Optional<? extends RegistryEntryLookup<T>> getOptional(RegistryKey<? extends Registry<? extends T>> registryRef) {
		return Optional.of(this.entryLookupImpl.asEntryLookup());
	}

	/**
	 * Создаёт registry ops.
	 *
	 * @param delegateOps delegate ops
	 *
	 * @return RegistryOps — результат операции
	 */
	public <V> RegistryOps<V> createRegistryOps(DynamicOps<V> delegateOps) {
		return RegistryOps.of(
				delegateOps,
				new RegistryOps.RegistryInfoGetter() {
					@Override
					public <T> Optional<RegistryOps.RegistryInfo<T>> getRegistryInfo(RegistryKey<? extends Registry<? extends T>> registryRef) {
						return ContextSwappableRegistryLookup.this.delegate
								.getOptional(registryRef)
								.map(RegistryOps.RegistryInfo::fromWrapper)
								.or(
										() -> Optional.of(
												new RegistryOps.RegistryInfo<>(
														ContextSwappableRegistryLookup.this.entryLookupImpl.asEntryOwner(),
														ContextSwappableRegistryLookup.this.entryLookupImpl.asEntryLookup(),
														Lifecycle.experimental()
												)
										)
								);
					}
				}
		);
	}

	/**
	 * Создаёт context swapper.
	 *
	 * @return ContextSwapper — результат операции
	 */
	public ContextSwapper createContextSwapper() {
		return new ContextSwapper() {
			@Override
			public <T> DataResult<T> swapContext(Codec<T> codec, T value, RegistryWrapper.WrapperLookup registries) {
				return codec.encodeStart(ContextSwappableRegistryLookup.this.createRegistryOps(JavaOps.INSTANCE), value)
				            .flatMap(encodedValue -> codec.parse(registries.getOps(JavaOps.INSTANCE), encodedValue));
			}
		};
	}

	public boolean hasEntries() {
		return !this.entries.isEmpty() || !this.tags.isEmpty();
	}

	/**
	 * {@code EntryLookupImpl}.
	 */
	class EntryLookupImpl implements RegistryEntryLookup<Object>, RegistryEntryOwner<Object> {

		@Override
		public Optional<RegistryEntry.Reference<Object>> getOptional(RegistryKey<Object> key) {
			return Optional.of(this.getOrComputeEntry(key));
		}

		@Override
		public RegistryEntry.Reference<Object> getOrThrow(RegistryKey<Object> key) {
			return this.getOrComputeEntry(key);
		}

		private RegistryEntry.Reference<Object> getOrComputeEntry(RegistryKey<Object> key) {
			return ContextSwappableRegistryLookup.this.entries.computeIfAbsent(
					key,
					key2 -> RegistryEntry.Reference.standAlone(this, (RegistryKey<Object>) key2)
			);
		}

		@Override
		public Optional<RegistryEntryList.Named<Object>> getOptional(TagKey<Object> tag) {
			return Optional.of(this.getOrComputeTag(tag));
		}

		@Override
		public RegistryEntryList.Named<Object> getOrThrow(TagKey<Object> tag) {
			return this.getOrComputeTag(tag);
		}

		private RegistryEntryList.Named<Object> getOrComputeTag(TagKey<Object> tag) {
			return ContextSwappableRegistryLookup.this.tags.computeIfAbsent(
					tag,
					tagKey -> RegistryEntryList.of(this, (TagKey<Object>) tagKey)
			);
		}

		@SuppressWarnings("unchecked")
		/**
		 * As entry lookup.
		 *
		 * @return RegistryEntryLookup — результат операции
		 */
		public <T> RegistryEntryLookup<T> asEntryLookup() {
			return (RegistryEntryLookup<T>) this;
		}

		@SuppressWarnings("unchecked")
		/**
		 * As entry owner.
		 *
		 * @return RegistryEntryOwner — результат операции
		 */
		public <T> RegistryEntryOwner<T> asEntryOwner() {
			return (RegistryEntryOwner<T>) this;
		}
	}
}
