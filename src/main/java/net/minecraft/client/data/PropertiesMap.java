package net.minecraft.client.data;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public record PropertiesMap(List<Property.Value<?>> values) {
   public static final PropertiesMap EMPTY = new PropertiesMap(List.of());
   private static final Comparator<Property.Value<?>> COMPARATOR = Comparator.comparing(value -> value.property().getName());

   public PropertiesMap withValue(Property.Value<?> value) {
      return new PropertiesMap(Util.withAppended(this.values, value));
   }

   public PropertiesMap copyOf(PropertiesMap propertiesMap) {
      return new PropertiesMap(
         ImmutableList.<Property.Value<?>>builder()
            .addAll(this.values)
            .addAll(propertiesMap.values)
            .build()
      );
   }

   public static PropertiesMap withValues(Property.Value<?>... values) {
      return new PropertiesMap(List.of(values));
   }

   public String asString() {
      return this.values.stream().sorted(COMPARATOR).map(Property.Value::toString).collect(Collectors.joining(","));
   }

   @Override
   public String toString() {
      return this.asString();
   }
}
