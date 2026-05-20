package net.minecraft.client.render.model.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
public record ModelTransformation(
   Transformation thirdPersonLeftHand,
   Transformation thirdPersonRightHand,
   Transformation firstPersonLeftHand,
   Transformation firstPersonRightHand,
   Transformation head,
   Transformation gui,
   Transformation ground,
   Transformation fixed,
   Transformation fixedFromBottom
) {
   public static final ModelTransformation NONE = new ModelTransformation(
      Transformation.IDENTITY,
      Transformation.IDENTITY,
      Transformation.IDENTITY,
      Transformation.IDENTITY,
      Transformation.IDENTITY,
      Transformation.IDENTITY,
      Transformation.IDENTITY,
      Transformation.IDENTITY,
      Transformation.IDENTITY
   );

   public Transformation getTransformation(ItemDisplayContext renderMode) {
      return switch (renderMode) {
         case THIRD_PERSON_LEFT_HAND -> this.thirdPersonLeftHand;
         case THIRD_PERSON_RIGHT_HAND -> this.thirdPersonRightHand;
         case FIRST_PERSON_LEFT_HAND -> this.firstPersonLeftHand;
         case FIRST_PERSON_RIGHT_HAND -> this.firstPersonRightHand;
         case HEAD -> this.head;
         case GUI -> this.gui;
         case GROUND -> this.ground;
         case FIXED -> this.fixed;
         case ON_SHELF -> this.fixedFromBottom;
         default -> Transformation.IDENTITY;
      };
   }

   @Environment(EnvType.CLIENT)
   protected static class Deserializer implements JsonDeserializer<ModelTransformation> {
      public ModelTransformation deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
         JsonObject jsonObject = jsonElement.getAsJsonObject();
         Transformation transformation = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
         Transformation transformation2 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
         if (transformation2 == Transformation.IDENTITY) {
            transformation2 = transformation;
         }

         Transformation transformation3 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
         Transformation transformation4 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
         if (transformation4 == Transformation.IDENTITY) {
            transformation4 = transformation3;
         }

         Transformation transformation5 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ItemDisplayContext.HEAD);
         Transformation transformation6 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ItemDisplayContext.GUI);
         Transformation transformation7 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ItemDisplayContext.GROUND);
         Transformation transformation8 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ItemDisplayContext.FIXED);
         Transformation transformation9 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ItemDisplayContext.ON_SHELF);
         return new ModelTransformation(
            transformation2,
            transformation,
            transformation4,
            transformation3,
            transformation5,
            transformation6,
            transformation7,
            transformation8,
            transformation9
         );
      }

      private Transformation parseModelTransformation(JsonDeserializationContext ctx, JsonObject json, ItemDisplayContext displayContext) {
         String string = displayContext.asString();
         return json.has(string) ? (Transformation)ctx.deserialize(json.get(string), Transformation.class) : Transformation.IDENTITY;
      }
   }
}
