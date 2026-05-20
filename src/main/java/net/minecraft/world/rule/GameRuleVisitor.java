package net.minecraft.world.rule;

public interface GameRuleVisitor {
   default <T> void visit(GameRule<T> rule) {
   }

   default void visitBoolean(GameRule<Boolean> rule) {
   }

   default void visitInt(GameRule<Integer> rule) {
   }
}
