package net.minecraft.client.render.model.json;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.state.property.Property;

@Environment(EnvType.CLIENT)
public class MultipartModelConditionBuilder {
   private final Builder<String, SimpleMultipartModelSelector.Terms> values = ImmutableMap.builder();

   private <T extends Comparable<T>> void putTerms(Property<T> property, SimpleMultipartModelSelector.Terms terms) {
      this.values.put(property.getName(), terms);
   }

   public final <T extends Comparable<T>> MultipartModelConditionBuilder put(Property<T> property, T value) {
      this.putTerms(property, new SimpleMultipartModelSelector.Terms(List.of(new SimpleMultipartModelSelector.Term(property.name(value), false))));
      return this;
   }

   @SafeVarargs
   public final <T extends Comparable<T>> MultipartModelConditionBuilder put(Property<T> property, T value, T... values) {
      List<SimpleMultipartModelSelector.Term> list = Stream.concat(Stream.of(value), Stream.of(values))
         .map(property::name)
         .sorted()
         .distinct()
         .map(valuex -> new SimpleMultipartModelSelector.Term(valuex, false))
         .toList();
      this.putTerms(property, new SimpleMultipartModelSelector.Terms(list));
      return this;
   }

   public final <T extends Comparable<T>> MultipartModelConditionBuilder replace(Property<T> property, T value) {
      this.putTerms(property, new SimpleMultipartModelSelector.Terms(List.of(new SimpleMultipartModelSelector.Term(property.name(value), true))));
      return this;
   }

   public MultipartModelCondition build() {
      return new SimpleMultipartModelSelector(this.values.buildOrThrow());
   }
}
