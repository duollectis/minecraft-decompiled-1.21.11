package net.minecraft.util.packrat;

public interface ParsingRuleEntry<S, T> {
   Symbol<T> getSymbol();

   ParsingRule<S, T> getRule();
}
