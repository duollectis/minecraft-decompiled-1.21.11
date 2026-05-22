package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

/**
 * Функциональный интерфейс для обработки сущностей при их загрузке в мир.
 * Может трансформировать сущность или вернуть {@code null} для её отмены.
 */
@FunctionalInterface
public interface LoadedEntityProcessor {

	LoadedEntityProcessor NOOP = entity -> entity;

	@Nullable Entity process(Entity entity);
}
