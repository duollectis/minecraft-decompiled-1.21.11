package net.minecraft.text;

@FunctionalInterface
public interface CharacterVisitor {
   boolean accept(int index, Style style, int codePoint);
}
