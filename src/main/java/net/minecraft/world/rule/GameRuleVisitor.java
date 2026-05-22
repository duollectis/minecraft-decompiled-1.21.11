package net.minecraft.world.rule;

/**
 * Посетитель правил игры — позволяет обходить все зарегистрированные правила
 * с типизированной обработкой булевых и целочисленных значений.
 */
public interface GameRuleVisitor {

	default <T> void visit(GameRule<T> rule) {
	}

	default void visitBoolean(GameRule<Boolean> rule) {
	}

	default void visitInt(GameRule<Integer> rule) {
	}
}
