package net.minecraft.client.realms;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.DontSerialize;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CheckedGson {
   ExclusionStrategy EXCLUSION_STRATEGY = new ExclusionStrategy() {
      public boolean shouldSkipClass(Class<?> clazz) {
         return false;
      }

      public boolean shouldSkipField(FieldAttributes fieldAttributes) {
         return fieldAttributes.getAnnotation(DontSerialize.class) != null;
      }
   };
   private final Gson GSON = new GsonBuilder()
      .addSerializationExclusionStrategy(this.EXCLUSION_STRATEGY)
      .addDeserializationExclusionStrategy(this.EXCLUSION_STRATEGY)
      .create();

   public String toJson(RealmsSerializable serializable) {
      return this.GSON.toJson(serializable);
   }

   public String toJson(JsonElement json) {
      return this.GSON.toJson(json);
   }

   public <T extends RealmsSerializable> @Nullable T fromJson(String json, Class<T> type) {
      return (T)this.GSON.fromJson(json, type);
   }
}
