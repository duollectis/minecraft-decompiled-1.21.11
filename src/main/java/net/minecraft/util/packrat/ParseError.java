package net.minecraft.util.packrat;

/**
 * {@code ParseError}.
 */
public record ParseError<S>(int cursor, Suggestable<S> suggestions, Object reason) {
}
