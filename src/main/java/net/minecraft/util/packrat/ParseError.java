package net.minecraft.util.packrat;

/**
 * Запись об ошибке разбора: позиция курсора, источник подсказок и причина ошибки.
 */
public record ParseError<S>(int cursor, Suggestable<S> suggestions, Object reason) {
}
