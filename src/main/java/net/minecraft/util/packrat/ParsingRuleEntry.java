package net.minecraft.util.packrat;

/**
 * {@code ParsingRuleEntry}.
 */
public interface ParsingRuleEntry<S, T> {

	Symbol<T> getSymbol();

	ParsingRule<S, T> getRule();
}
