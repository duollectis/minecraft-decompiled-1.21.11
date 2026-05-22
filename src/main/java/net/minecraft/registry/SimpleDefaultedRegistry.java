package net.minecraft.registry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Реализация {@link DefaultedRegistry} на основе {@link SimpleRegistry}.
 * При отсутствии запрошенного элемента возвращает элемент по умолчанию,
 * зарегистрированный под {@code defaultId}.
 *
 * @param <T> тип элементов реестра
 */
public class SimpleDefaultedRegistry<T> extends SimpleRegistry<T> implements DefaultedRegistry<T> {

	private final Identifier defaultId;
	private RegistryEntry.Reference<T> defaultEntry;

	public SimpleDefaultedRegistry(
			String defaultId,
			RegistryKey<? extends Registry<T>> key,
			Lifecycle lifecycle,
			boolean intrusive
	) {
		super(key, lifecycle, intrusive);
		this.defaultId = Identifier.of(defaultId);
	}

	@Override
	public RegistryEntry.Reference<T> add(RegistryKey<T> key, T value, RegistryEntryInfo info) {
		RegistryEntry.Reference<T> reference = super.add(key, value, info);

		if (defaultId.equals(key.getValue())) {
			defaultEntry = reference;
		}

		return reference;
	}

	@Override
	public int getRawId(@Nullable T value) {
		int rawId = super.getRawId(value);
		return rawId == -1 ? super.getRawId(defaultEntry.value()) : rawId;
	}

	@Override
	public Identifier getId(T value) {
		Identifier identifier = super.getId(value);
		return identifier == null ? defaultId : identifier;
	}

	@Override
	public T get(@Nullable Identifier id) {
		T object = super.get(id);
		return object == null ? defaultEntry.value() : object;
	}

	@Override
	public Optional<T> getOptionalValue(@Nullable Identifier id) {
		return Optional.ofNullable(super.get(id));
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getDefaultEntry() {
		return Optional.ofNullable(defaultEntry);
	}

	@Override
	public T get(int index) {
		T object = super.get(index);
		return object == null ? defaultEntry.value() : object;
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getRandom(Random random) {
		return super.getRandom(random).or(() -> Optional.of(defaultEntry));
	}

	@Override
	public Identifier getDefaultId() {
		return defaultId;
	}
}
