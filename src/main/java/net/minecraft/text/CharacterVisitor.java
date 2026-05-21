package net.minecraft.text;

@FunctionalInterface
/**
 * {@code CharacterVisitor}.
 */
public interface CharacterVisitor {

	boolean accept(int index, Style style, int codePoint);
}
