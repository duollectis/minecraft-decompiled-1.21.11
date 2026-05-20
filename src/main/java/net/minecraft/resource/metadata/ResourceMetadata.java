package net.minecraft.resource.metadata;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.resource.InputSupplier;
import net.minecraft.util.JsonHelper;

public interface ResourceMetadata {
   ResourceMetadata NONE = new ResourceMetadata() {
      @Override
      public <T> Optional<T> decode(ResourceMetadataSerializer<T> serializer) {
         return Optional.empty();
      }
   };
   InputSupplier<ResourceMetadata> NONE_SUPPLIER = () -> NONE;

   static ResourceMetadata create(InputStream stream) throws IOException {
      ResourceMetadata var3;
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
         final JsonObject jsonObject = JsonHelper.deserialize(bufferedReader);
         var3 = new ResourceMetadata() {
            @Override
            public <T> Optional<T> decode(ResourceMetadataSerializer<T> serializer) {
               String string = serializer.name();
               if (jsonObject.has(string)) {
                  T object = (T)serializer.codec().parse(JsonOps.INSTANCE, jsonObject.get(string)).getOrThrow(JsonParseException::new);
                  return Optional.of(object);
               } else {
                  return Optional.empty();
               }
            }
         };
      }

      return var3;
   }

   <T> Optional<T> decode(ResourceMetadataSerializer<T> serializer);

   default <T> Optional<ResourceMetadataSerializer.Value<T>> decodeAsValue(ResourceMetadataSerializer<T> additionalMetadata) {
      return this.decode(additionalMetadata).map(additionalMetadata::value);
   }

   default List<ResourceMetadataSerializer.Value<?>> decode(Collection<ResourceMetadataSerializer<?>> serializers) {
      return serializers.stream().map(this::decodeAsValue).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
   }
}
