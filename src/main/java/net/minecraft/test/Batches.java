package net.minecraft.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;

public class Batches {
   private static final int BATCH_SIZE = 50;
   public static final Batches.Decorator DEFAULT_DECORATOR = (instance, world) -> Stream.of(
      new GameTestState(instance, BlockRotation.NONE, world, TestAttemptConfig.once())
   );

   public static List<GameTestBatch> batch(Collection<RegistryEntry.Reference<TestInstance>> instances, Batches.Decorator decorator, ServerWorld world) {
      Map<RegistryEntry<TestEnvironmentDefinition>, List<GameTestState>> map = instances.stream()
         .flatMap(instance -> decorator.decorate((RegistryEntry.Reference<TestInstance>)instance, world))
         .collect(Collectors.groupingBy(state -> state.getInstance().getEnvironment()));
      return map.entrySet().stream().flatMap(entry -> {
         RegistryEntry<TestEnvironmentDefinition> registryEntry = entry.getKey();
         List<GameTestState> list = entry.getValue();
         return Streams.mapWithIndex(Lists.partition(list, 50).stream(), (states, index) -> create(states, registryEntry, (int)index));
      }).toList();
   }

   public static TestRunContext.Batcher defaultBatcher() {
      return batcher(50);
   }

   public static TestRunContext.Batcher batcher(int batchSize) {
      return states -> {
         Map<RegistryEntry<TestEnvironmentDefinition>, List<GameTestState>> map = states.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(state -> state.getInstance().getEnvironment()));
         return map.entrySet().stream().flatMap(entry -> {
            RegistryEntry<TestEnvironmentDefinition> registryEntry = entry.getKey();
            List<GameTestState> list = entry.getValue();
            return Streams.mapWithIndex(Lists.partition(list, batchSize).stream(), (statesx, index) -> create(List.copyOf(statesx), registryEntry, (int)index));
         }).toList();
      };
   }

   public static GameTestBatch create(Collection<GameTestState> states, RegistryEntry<TestEnvironmentDefinition> environment, int index) {
      return new GameTestBatch(index, states, environment);
   }

   @FunctionalInterface
   public interface Decorator {
      Stream<GameTestState> decorate(RegistryEntry.Reference<TestInstance> instance, ServerWorld world);
   }
}
