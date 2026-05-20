package net.minecraft.client.render;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class OutputTarget {
   private final String name;
   private final Supplier<@Nullable Framebuffer> framebuffer;
   public static final OutputTarget MAIN_TARGET = new OutputTarget("main_target", () -> MinecraftClient.getInstance().getFramebuffer());
   public static final OutputTarget OUTLINE_TARGET = new OutputTarget(
      "outline_target", () -> MinecraftClient.getInstance().worldRenderer.getEntityOutlinesFramebuffer()
   );
   public static final OutputTarget WEATHER_TARGET = new OutputTarget(
      "weather_target", () -> MinecraftClient.getInstance().worldRenderer.getWeatherFramebuffer()
   );
   public static final OutputTarget ITEM_ENTITY_TARGET = new OutputTarget(
      "item_entity_target", () -> MinecraftClient.getInstance().worldRenderer.getEntityFramebuffer()
   );

   public OutputTarget(String name, Supplier<@Nullable Framebuffer> framebuffer) {
      this.name = name;
      this.framebuffer = framebuffer;
   }

   public Framebuffer getFramebuffer() {
      Framebuffer framebuffer = this.framebuffer.get();
      return framebuffer != null ? framebuffer : MinecraftClient.getInstance().getFramebuffer();
   }

   @Override
   public String toString() {
      return "OutputTarget[" + this.name + "]";
   }
}
