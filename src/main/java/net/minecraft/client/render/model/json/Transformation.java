package net.minecraft.client.render.model.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public record Transformation(Vector3fc rotation, Vector3fc translation, Vector3fc scale) {
   public static final Transformation IDENTITY = new Transformation(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));

   public void apply(boolean leftHanded, MatrixStack.Entry entry) {
      if (this == IDENTITY) {
         entry.translate(-0.5F, -0.5F, -0.5F);
      } else {
         float f;
         float g;
         float h;
         if (leftHanded) {
            f = -this.translation.x();
            g = -this.rotation.y();
            h = -this.rotation.z();
         } else {
            f = this.translation.x();
            g = this.rotation.y();
            h = this.rotation.z();
         }

         entry.translate(f, this.translation.y(), this.translation.z());
         entry.rotate(
            new Quaternionf().rotationXYZ(this.rotation.x() * (float) (Math.PI / 180.0), g * (float) (Math.PI / 180.0), h * (float) (Math.PI / 180.0))
         );
         entry.scale(this.scale.x(), this.scale.y(), this.scale.z());
         entry.translate(-0.5F, -0.5F, -0.5F);
      }
   }

   @Environment(EnvType.CLIENT)
   protected static class Deserializer implements JsonDeserializer<Transformation> {
      private static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
      private static final Vector3f DEFAULT_TRANSLATION = new Vector3f(0.0F, 0.0F, 0.0F);
      private static final Vector3f DEFAULT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);
      public static final float MAX_TRANSLATION = 5.0F;
      public static final float MAX_SCALE = 4.0F;

      public Transformation deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
         JsonObject jsonObject = jsonElement.getAsJsonObject();
         Vector3f vector3f = this.parseVector3f(jsonObject, "rotation", DEFAULT_ROTATION);
         Vector3f vector3f2 = this.parseVector3f(jsonObject, "translation", DEFAULT_TRANSLATION);
         vector3f2.mul(0.0625F);
         vector3f2.set(MathHelper.clamp(vector3f2.x, -5.0F, 5.0F), MathHelper.clamp(vector3f2.y, -5.0F, 5.0F), MathHelper.clamp(vector3f2.z, -5.0F, 5.0F));
         Vector3f vector3f3 = this.parseVector3f(jsonObject, "scale", DEFAULT_SCALE);
         vector3f3.set(MathHelper.clamp(vector3f3.x, -4.0F, 4.0F), MathHelper.clamp(vector3f3.y, -4.0F, 4.0F), MathHelper.clamp(vector3f3.z, -4.0F, 4.0F));
         return new Transformation(vector3f, vector3f2, vector3f3);
      }

      private Vector3f parseVector3f(JsonObject json, String key, Vector3f fallback) {
         if (!json.has(key)) {
            return fallback;
         } else {
            JsonArray jsonArray = JsonHelper.getArray(json, key);
            if (jsonArray.size() != 3) {
               throw new JsonParseException("Expected 3 " + key + " values, found: " + jsonArray.size());
            } else {
               float[] fs = new float[3];

               for (int i = 0; i < fs.length; i++) {
                  fs[i] = JsonHelper.asFloat(jsonArray.get(i), key + "[" + i + "]");
               }

               return new Vector3f(fs[0], fs[1], fs[2]);
            }
         }
      }
   }
}
