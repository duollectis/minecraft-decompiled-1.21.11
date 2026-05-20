package net.minecraft.world.attribute;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.attribute.timeline.Timeline;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionType;
import org.jspecify.annotations.Nullable;

public class WorldEnvironmentAttributeAccess implements EnvironmentAttributeAccess {
   private final Map<EnvironmentAttribute<?>, WorldEnvironmentAttributeAccess.Entry<?>> entries = new Reference2ObjectOpenHashMap();

   WorldEnvironmentAttributeAccess(Map<EnvironmentAttribute<?>, List<EnvironmentAttributeFunction<?>>> modificationsByAttribute) {
      modificationsByAttribute.forEach(
         (attribute, mods) -> this.entries
            .put(
               (EnvironmentAttribute<?>)attribute, this.computeEntry((EnvironmentAttribute<?>)attribute, (List<? extends EnvironmentAttributeFunction<?>>)mods)
            )
      );
   }

   private <Value> WorldEnvironmentAttributeAccess.Entry<Value> computeEntry(
      EnvironmentAttribute<Value> attribute, List<? extends EnvironmentAttributeFunction<?>> mods
   ) {
      List<EnvironmentAttributeFunction<Value>> list = new ArrayList<>((Collection<? extends EnvironmentAttributeFunction<Value>>)mods);
      Value object = attribute.getDefaultValue();

      while (!list.isEmpty()) {
         if (!(list.getFirst() instanceof EnvironmentAttributeFunction.Constant<Value> constant)) {
            break;
         }

         object = constant.applyConstant(object);
         list.removeFirst();
      }

      boolean bl = list.stream().anyMatch(function -> function instanceof EnvironmentAttributeFunction.Positional);
      return new WorldEnvironmentAttributeAccess.Entry<>(attribute, object, List.copyOf(list), bl);
   }

   public static WorldEnvironmentAttributeAccess.Builder builder() {
      return new WorldEnvironmentAttributeAccess.Builder();
   }

   static void addModifiersFromWorld(WorldEnvironmentAttributeAccess.Builder builder, World world) {
      DynamicRegistryManager dynamicRegistryManager = world.getRegistryManager();
      BiomeAccess biomeAccess = world.getBiomeAccess();
      LongSupplier longSupplier = world::getTimeOfDay;
      addModifiersFromDimension(builder, world.getDimension());
      addModifiersFromBiomes(builder, dynamicRegistryManager.getOrThrow(RegistryKeys.BIOME), biomeAccess);
      world.getDimension().timelines().forEach(attribute -> builder.addFromTimeline((RegistryEntry<Timeline>)attribute, longSupplier));
      if (world.canHaveWeather()) {
         WeatherAttributes.addWeatherAttributes(builder, WeatherAttributes.WeatherAccess.ofWorld(world));
      }
   }

   private static void addModifiersFromDimension(WorldEnvironmentAttributeAccess.Builder builder, DimensionType dimensionType) {
      builder.addFromMap(dimensionType.attributes());
   }

   private static void addModifiersFromBiomes(WorldEnvironmentAttributeAccess.Builder builder, RegistryWrapper<Biome> biome, BiomeAccess biomeAccess) {
      Stream<EnvironmentAttribute<?>> stream = biome.streamEntries()
         .flatMap(biomex -> ((Biome)biomex.value()).getEnvironmentAttributes().keySet().stream())
         .distinct();
      stream.forEach(attribute -> addModifiersFromBiomes(builder, (EnvironmentAttribute<?>)attribute, biomeAccess));
   }

   private static <Value> void addModifiersFromBiomes(
      WorldEnvironmentAttributeAccess.Builder builder, EnvironmentAttribute<Value> attribute, BiomeAccess biomeAccess
   ) {
      builder.positional(attribute, (value, pos, weightedAttributeList) -> {
         if (weightedAttributeList != null && attribute.isInterpolated()) {
            return weightedAttributeList.interpolate(attribute, value);
         } else {
            RegistryEntry<Biome> registryEntry = biomeAccess.getBiomeForNoiseGen(pos.x, pos.y, pos.z);
            return registryEntry.value().getEnvironmentAttributes().apply(attribute, value);
         }
      });
   }

   public void tick() {
      this.entries.values().forEach(WorldEnvironmentAttributeAccess.Entry::tick);
   }

   private <Value> WorldEnvironmentAttributeAccess.@Nullable Entry<Value> getEntry(EnvironmentAttribute<Value> attribute) {
      return (WorldEnvironmentAttributeAccess.Entry<Value>)this.entries.get(attribute);
   }

   @Override
   public <Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute) {
      if (SharedConstants.isDevelopment && attribute.isPositional()) {
         throw new IllegalStateException("Position must always be provided for positional attribute " + attribute);
      } else {
         WorldEnvironmentAttributeAccess.Entry<Value> entry = this.getEntry(attribute);
         return entry == null ? attribute.getDefaultValue() : entry.get();
      }
   }

   @Override
   public <Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute, Vec3d pos, @Nullable WeightedAttributeList pool) {
      WorldEnvironmentAttributeAccess.Entry<Value> entry = this.getEntry(attribute);
      return entry == null ? attribute.getDefaultValue() : entry.getAt(pos, pool);
   }

   @VisibleForTesting
   <Value> Value getDefaultValue(EnvironmentAttribute<Value> attribute) {
      WorldEnvironmentAttributeAccess.Entry<Value> entry = this.getEntry(attribute);
      return entry != null ? entry.defaultValue : attribute.getDefaultValue();
   }

   @VisibleForTesting
   boolean isPositional(EnvironmentAttribute<?> attribute) {
      WorldEnvironmentAttributeAccess.Entry<?> entry = this.getEntry(attribute);
      return entry != null && entry.positional;
   }

   public static class Builder {
      private final Map<EnvironmentAttribute<?>, List<EnvironmentAttributeFunction<?>>> modifications = new HashMap<>();

      Builder() {
      }

      public WorldEnvironmentAttributeAccess.Builder world(World world) {
         WorldEnvironmentAttributeAccess.addModifiersFromWorld(this, world);
         return this;
      }

      public WorldEnvironmentAttributeAccess.Builder addFromMap(EnvironmentAttributeMap attributes) {
         for (EnvironmentAttribute<?> environmentAttribute : attributes.keySet()) {
            this.addFromMap(environmentAttribute, attributes);
         }

         return this;
      }

      private <Value> WorldEnvironmentAttributeAccess.Builder addFromMap(EnvironmentAttribute<Value> attribute, EnvironmentAttributeMap attributeMap) {
         EnvironmentAttributeMap.Entry<Value, ?> entry = attributeMap.getEntry(attribute);
         if (entry == null) {
            throw new IllegalArgumentException("Missing attribute " + attribute);
         } else {
            return this.constant(attribute, entry::apply);
         }
      }

      public <Value> WorldEnvironmentAttributeAccess.Builder constant(EnvironmentAttribute<Value> attribute, EnvironmentAttributeFunction.Constant<Value> mod) {
         return this.addModification(attribute, mod);
      }

      public <Value> WorldEnvironmentAttributeAccess.Builder timeBased(EnvironmentAttribute<Value> attribute, EnvironmentAttributeFunction.TimeBased<Value> mod) {
         return this.addModification(attribute, mod);
      }

      public <Value> WorldEnvironmentAttributeAccess.Builder positional(
         EnvironmentAttribute<Value> attribute, EnvironmentAttributeFunction.Positional<Value> mod
      ) {
         return this.addModification(attribute, mod);
      }

      private <Value> WorldEnvironmentAttributeAccess.Builder addModification(EnvironmentAttribute<Value> attribute, EnvironmentAttributeFunction<Value> mod) {
         this.modifications.computeIfAbsent(attribute, environmentAttribute -> new ArrayList<>()).add(mod);
         return this;
      }

      public WorldEnvironmentAttributeAccess.Builder addFromTimeline(RegistryEntry<Timeline> timeline, LongSupplier timeSupplier) {
         for (EnvironmentAttribute<?> environmentAttribute : timeline.value().getAttributes()) {
            this.addModificationFromTimeline(timeline, environmentAttribute, timeSupplier);
         }

         return this;
      }

      private <Value> void addModificationFromTimeline(RegistryEntry<Timeline> timeline, EnvironmentAttribute<Value> attribute, LongSupplier timeSupplier) {
         this.timeBased(attribute, timeline.value().getModification(attribute, timeSupplier));
      }

      public WorldEnvironmentAttributeAccess build() {
         return new WorldEnvironmentAttributeAccess(this.modifications);
      }
   }

   static class Entry<Value> {
      private final EnvironmentAttribute<Value> attribute;
      final Value defaultValue;
      private final List<EnvironmentAttributeFunction<Value>> modifications;
      final boolean positional;
      private @Nullable Value cachedValue;
      private int age;

      Entry(EnvironmentAttribute<Value> attribute, Value defaultValue, List<EnvironmentAttributeFunction<Value>> modifications, boolean positional) {
         this.attribute = attribute;
         this.defaultValue = defaultValue;
         this.modifications = modifications;
         this.positional = positional;
      }

      public void tick() {
         this.cachedValue = null;
         this.age++;
      }

      public Value get() {
         if (this.cachedValue != null) {
            return this.cachedValue;
         } else {
            Value object = this.compute();
            this.cachedValue = object;
            return object;
         }
      }

      public Value getAt(Vec3d pos, @Nullable WeightedAttributeList weightedAttributeList) {
         return !this.positional ? this.get() : this.computeAt(pos, weightedAttributeList);
      }

      private Value computeAt(Vec3d pos, @Nullable WeightedAttributeList weightedAttributeList) {
         Value object = this.defaultValue;

         for (EnvironmentAttributeFunction<Value> environmentAttributeFunction : this.modifications) {
            object = (Value)(switch (environmentAttributeFunction) {
               case EnvironmentAttributeFunction.Constant<Value> constant -> (Object)constant.applyConstant(object);
               case EnvironmentAttributeFunction.TimeBased<Value> timeBased -> (Object)timeBased.applyTimeBased(object, this.age);
               case EnvironmentAttributeFunction.Positional<Value> positional -> (Object)positional.applyPositional(
                  object, Objects.requireNonNull(pos), weightedAttributeList
               );
               default -> throw new MatchException(null, null);
            });
         }

         return this.attribute.clamp(object);
      }

      private Value compute() {
         Value object = this.defaultValue;

         for (EnvironmentAttributeFunction<Value> environmentAttributeFunction : this.modifications) {
            object = (Value)(switch (environmentAttributeFunction) {
               case EnvironmentAttributeFunction.Constant<Value> constant -> (Object)constant.applyConstant(object);
               case EnvironmentAttributeFunction.TimeBased<Value> timeBased -> (Object)timeBased.applyTimeBased(object, this.age);
               case EnvironmentAttributeFunction.Positional<Value> positional -> (Object)object;
               default -> throw new MatchException(null, null);
            });
         }

         return this.attribute.clamp(object);
      }
   }
}
