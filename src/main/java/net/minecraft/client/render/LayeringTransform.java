package net.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LayeringTransform {
   private final String name;
   private final @Nullable Consumer<Matrix4fStack> transform;
   public static final LayeringTransform NO_LAYERING = new LayeringTransform("no_layering", null);
   public static final LayeringTransform VIEW_OFFSET_Z_LAYERING = new LayeringTransform(
      "view_offset_z_layering", matrices -> RenderSystem.getProjectionType().apply(matrices, 1.0F)
   );
   public static final LayeringTransform VIEW_OFFSET_Z_LAYERING_FORWARD = new LayeringTransform(
      "view_offset_z_layering_forward", matrices -> RenderSystem.getProjectionType().apply(matrices, -1.0F)
   );

   public LayeringTransform(String name, @Nullable Consumer<Matrix4fStack> transform) {
      this.name = name;
      this.transform = transform;
   }

   @Override
   public String toString() {
      return "LayeringTransform[" + this.name + "]";
   }

   public @Nullable Consumer<Matrix4fStack> getTransform() {
      return this.transform;
   }
}
