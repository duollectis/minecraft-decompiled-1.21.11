package net.minecraft.test;

import java.util.Collection;
import net.minecraft.registry.entry.RegistryEntry;

public record GameTestBatch(int index, Collection<GameTestState> states, RegistryEntry<TestEnvironmentDefinition> environment) {
   public GameTestBatch(int index, Collection<GameTestState> states, RegistryEntry<TestEnvironmentDefinition> environment) {
      if (states.isEmpty()) {
         throw new IllegalArgumentException("A GameTestBatch must include at least one GameTestInfo!");
      } else {
         this.index = index;
         this.states = states;
         this.environment = environment;
      }
   }
}
