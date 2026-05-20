package net.minecraft.resource.metadata;

import com.mojang.serialization.Codec;
import java.util.Optional;

public record ResourceMetadataSerializer<T>(String name, Codec<T> codec) {
   public ResourceMetadataSerializer.Value<T> value(T value) {
      return new ResourceMetadataSerializer.Value<>(this, value);
   }

   public record Value<T>(ResourceMetadataSerializer<T> type, T value) {
      @SuppressWarnings("unchecked")
      public <U> Optional<U> getValueIfMatching(ResourceMetadataSerializer<U> o) {
         return o == this.type ? Optional.of((U) this.value) : Optional.empty();
      }
   }
}
