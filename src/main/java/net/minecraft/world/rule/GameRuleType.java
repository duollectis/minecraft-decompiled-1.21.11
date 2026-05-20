package net.minecraft.world.rule;

import net.minecraft.util.StringIdentifiable;

public enum GameRuleType implements StringIdentifiable {
   INT("integer"),
   BOOL("boolean");

   private final String name;

   private GameRuleType(final String name) {
      this.name = name;
   }

   @Override
   public String asString() {
      return this.name;
   }
}
