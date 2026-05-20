package net.minecraft.particle;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;

public enum ParticlesMode {
   ALL(0, "options.particles.all"),
   DECREASED(1, "options.particles.decreased"),
   MINIMAL(2, "options.particles.minimal");

   private static final IntFunction<ParticlesMode> BY_ID = ValueLists.createIndexToValueFunction(
      (ParticlesMode particlesMode) -> particlesMode.id, values(), ValueLists.OutOfBoundsHandling.WRAP
   );
   public static final Codec<ParticlesMode> CODEC = Codec.INT.xmap(BY_ID::apply, mode -> mode.id);
   private final int id;
   private final Text text;

   private ParticlesMode(final int id, final String translationKey) {
      this.id = id;
      this.text = Text.translatable(translationKey);
   }

   public Text getText() {
      return this.text;
   }
}
