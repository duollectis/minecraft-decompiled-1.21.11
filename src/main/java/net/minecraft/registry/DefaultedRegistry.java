package net.minecraft.registry;

import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Расширение {@link Registry}, гарантирующее ненулевые возвращаемые значения
 * за счёт использования элемента по умолчанию при отсутствии запрошенного ключа.
 *
 * @param <T> тип элементов реестра
 */
public interface DefaultedRegistry<T> extends Registry<T> {

	@Override
	@NonNull Identifier getId(T value);

	@Override
	@NonNull T get(@Nullable Identifier id);

	@Override
	@NonNull T get(int index);

	Identifier getDefaultId();
}
