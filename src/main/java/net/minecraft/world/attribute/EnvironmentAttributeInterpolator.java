package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.WeightedInterpolation;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttributeInterpolator {
   private final Map<EnvironmentAttribute<?>, EnvironmentAttributeInterpolator.Entry<?>> entries = new Reference2ObjectOpenHashMap();
   private final Function<EnvironmentAttribute<?>, EnvironmentAttributeInterpolator.Entry<?>> entryFactory = value -> new EnvironmentAttributeInterpolator.Entry<>(
      value
   );
   @Nullable World world;
   @Nullable Vec3d pos;
   final WeightedAttributeList pool = new WeightedAttributeList();

   public void clear() {
      this.world = null;
      this.pos = null;
      this.pool.clear();
      this.entries.clear();
   }

   public void update(World world, Vec3d pos) {
      this.world = world;
      this.pos = pos;
      this.entries.values().removeIf(EnvironmentAttributeInterpolator.Entry::update);
      this.pool.clear();
      WeightedInterpolation.interpolate(
         pos.multiply(0.25), world.getBiomeAccess()::getBiomeForNoiseGen, (weight, biome) -> this.pool.add(weight, biome.value().getEnvironmentAttributes())
      );
   }

   public <Value> Value get(EnvironmentAttribute<Value> attribute, float tickProgress) {
      EnvironmentAttributeInterpolator.Entry<Value> entry = (EnvironmentAttributeInterpolator.Entry<Value>)this.entries
         .computeIfAbsent(attribute, this.entryFactory);
      return entry.get(attribute, tickProgress);
   }

   class Entry<Value> {
      private Value last;
      private @Nullable Value current;

      public Entry(final EnvironmentAttribute<Value> attribute) {
         Value object = this.compute(attribute);
         this.last = object;
         this.current = object;
      }

      private Value compute(EnvironmentAttribute<Value> attribute) {
         return EnvironmentAttributeInterpolator.this.world != null && EnvironmentAttributeInterpolator.this.pos != null
            ? EnvironmentAttributeInterpolator.this.world
               .getEnvironmentAttributes()
               .getAttributeValue(attribute, EnvironmentAttributeInterpolator.this.pos, EnvironmentAttributeInterpolator.this.pool)
            : attribute.getDefaultValue();
      }

      public boolean update() {
         if (this.current == null) {
            return true;
         } else {
            this.last = this.current;
            this.current = null;
            return false;
         }
      }

      public Value get(EnvironmentAttribute<Value> attribute, float tickProgress) {
         if (this.current == null) {
            this.current = this.compute(attribute);
         }

         return attribute.getType().partialTickLerp().apply(tickProgress, this.last, this.current);
      }
   }
}
