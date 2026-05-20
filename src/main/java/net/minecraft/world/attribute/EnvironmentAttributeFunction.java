package net.minecraft.world.attribute;

import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public sealed interface EnvironmentAttributeFunction<Value>
   permits EnvironmentAttributeFunction.Constant,
   EnvironmentAttributeFunction.TimeBased,
   EnvironmentAttributeFunction.Positional {
   @FunctionalInterface
   public non-sealed interface Constant<Value> extends EnvironmentAttributeFunction<Value> {
      Value applyConstant(Value value);
   }

   @FunctionalInterface
   public non-sealed interface Positional<Value> extends EnvironmentAttributeFunction<Value> {
      Value applyPositional(Value value, Vec3d pos, @Nullable WeightedAttributeList weightedAttributeList);
   }

   @FunctionalInterface
   public non-sealed interface TimeBased<Value> extends EnvironmentAttributeFunction<Value> {
      Value applyTimeBased(Value value, int time);
   }
}
