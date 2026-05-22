package net.minecraft.resource;

/**
 * Позиция ресурс-пака в списке: обязательность, позиция вставки и закреплённость.
 *
 * @param required        обязателен ли пак (не может быть отключён)
 * @param defaultPosition позиция вставки по умолчанию (TOP или BOTTOM)
 * @param fixedPosition   закреплён ли пак на своей позиции
 */
public record ResourcePackPosition(
	boolean required,
	ResourcePackProfile.InsertionPosition defaultPosition,
	boolean fixedPosition
) {}
