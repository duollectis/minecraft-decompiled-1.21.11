package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;

public record DebugStickStateComponent(Map<RegistryEntry<Block>, Property<?>> properties) {
   public static final DebugStickStateComponent DEFAULT = new DebugStickStateComponent(Map.of());
   @SuppressWarnings("unchecked")
   public static final Codec<DebugStickStateComponent> CODEC = ((Codec<Map<?, ?>>) (Codec<?>) Codec.dispatchedMap(
         Registries.BLOCK.getEntryCodec(),
         block -> Codec.STRING
            .comapFlatMap(
               property -> {
                  Property<?> property2 = ((Block)block.value()).getStateManager().getProperty(property);
                  return property2 != null
                     ? DataResult.success(property2)
                     : DataResult.error(() -> "No property on " + block.getIdAsString() + " with name: " + property);
               },
               Property::getName
            )
      ))
      .xmap(
         map -> new DebugStickStateComponent(castMap(map)),
         component -> (Map<?, ?>) component.properties()
      );

   public DebugStickStateComponent with(RegistryEntry<Block> block, Property<?> property) {
      return new DebugStickStateComponent(Util.mapWith(this.properties, block, property));
   }

   @SuppressWarnings("unchecked")
   private static Map<RegistryEntry<Block>, Property<?>> castMap(Map<?, ?> map) {
      return (Map<RegistryEntry<Block>, Property<?>>) map;
   }
}
