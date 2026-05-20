package net.minecraft.client.input;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record MouseInput(@MouseInput.ButtonCode int button, @AbstractInput.Modifier int modifiers) implements AbstractInput {
   @MouseInput.ButtonCode
   @Override
   public int getKeycode() {
      return this.button;
   }

   @Retention(RetentionPolicy.CLASS)
   @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE_USE})
   @Environment(EnvType.CLIENT)
   public @interface ButtonCode {
   }

   @Retention(RetentionPolicy.CLASS)
   @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE_USE})
   @Environment(EnvType.CLIENT)
   public @interface MouseAction {
   }
}
