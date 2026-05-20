package net.minecraft.util.packrat;

public record ParseError<S>(int cursor, Suggestable<S> suggestions, Object reason) {
}
