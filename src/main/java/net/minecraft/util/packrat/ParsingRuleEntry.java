package net.minecraft.util.packrat;

/**
 * Связка символа и соответствующего ему правила разбора.
 * Используется как ключ мемоизации в {@link ParsingStateImpl}.
 */
public interface ParsingRuleEntry<S, T> {

	Symbol<T> getSymbol();

	ParsingRule<S, T> getRule();
}
