package net.minecraft.world.entity;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Контракт для объектов, поддерживающих поиск уникально идентифицируемых элементов по UUID.
 *
 * @param <T> тип возвращаемого идентифицируемого объекта
 */
public interface EntityQueriable<T extends UniquelyIdentifiable> {

	@Nullable T lookup(UUID uuid);
}
