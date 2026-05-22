package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

/**
 * Маркерный интерфейс для сущностей, имеющих владельца (другую сущность).
 * Используется для определения принадлежности снарядов, питомцев и т.д.
 */
public interface Ownable {

	@Nullable Entity getOwner();
}
