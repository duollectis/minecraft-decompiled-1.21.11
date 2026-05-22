package net.minecraft.world.entity;

import java.util.UUID;

/**
 * Контракт для объектов, имеющих уникальный идентификатор UUID и поддерживающих удаление.
 */
public interface UniquelyIdentifiable {

	UUID getUuid();

	boolean isRemoved();
}
