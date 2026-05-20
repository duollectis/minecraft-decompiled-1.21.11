package net.minecraft.entity.ai.brain;

import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.kinds.Const.Mu;
import com.mojang.datafixers.util.Unit;
import org.jspecify.annotations.Nullable;

public interface MemoryQuery<F extends K1, Val> {
   MemoryModuleType<Val> memory();

   MemoryModuleState getState();

   @Nullable MemoryQueryResult<F, Val> toQueryResult(Brain<?> brain, java.util.Optional<Val> value);

   public record Absent<V>(MemoryModuleType<V> memory) implements MemoryQuery<Mu<Unit>, V> {
      @Override
      public MemoryModuleState getState() {
         return MemoryModuleState.VALUE_ABSENT;
      }

      @Override
      public MemoryQueryResult<Mu<Unit>, V> toQueryResult(Brain<?> brain, java.util.Optional<V> value) {
         return value.isPresent() ? null : new MemoryQueryResult<>(brain, this.memory, Const.create(Unit.INSTANCE));
      }
   }

   public record Optional<V>(MemoryModuleType<V> memory) implements MemoryQuery<com.mojang.datafixers.kinds.OptionalBox.Mu, V> {
      @Override
      public MemoryModuleState getState() {
         return MemoryModuleState.REGISTERED;
      }

      @Override
      public MemoryQueryResult<com.mojang.datafixers.kinds.OptionalBox.Mu, V> toQueryResult(Brain<?> brain, java.util.Optional<V> value) {
         return new MemoryQueryResult<>(brain, this.memory, OptionalBox.create(value));
      }
   }

   public record MemoryValue<V>(MemoryModuleType<V> memory) implements MemoryQuery<com.mojang.datafixers.kinds.IdF.Mu, V> {
      @Override
      public MemoryModuleState getState() {
         return MemoryModuleState.VALUE_PRESENT;
      }

      @Override
      public MemoryQueryResult<com.mojang.datafixers.kinds.IdF.Mu, V> toQueryResult(Brain<?> brain, java.util.Optional<V> value) {
         return value.isEmpty() ? null : new MemoryQueryResult<>(brain, this.memory, IdF.create(value.get()));
      }
   }
}
