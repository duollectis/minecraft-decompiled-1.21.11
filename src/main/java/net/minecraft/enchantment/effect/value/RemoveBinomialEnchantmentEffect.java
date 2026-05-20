package net.minecraft.enchantment.effect.value;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentValueEffect;
import net.minecraft.util.math.random.Random;

public record RemoveBinomialEnchantmentEffect(EnchantmentLevelBasedValue chance) implements EnchantmentValueEffect {
   public static final MapCodec<RemoveBinomialEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(EnchantmentLevelBasedValue.CODEC.fieldOf("chance").forGetter(RemoveBinomialEnchantmentEffect::chance))
         .apply(instance, RemoveBinomialEnchantmentEffect::new)
   );

   @Override
   public float apply(int level, Random random, float inputValue) {
      float f = this.chance.getValue(level);
      int i = 0;
      if (!(inputValue <= 128.0F) && !(inputValue * f < 20.0F) && !(inputValue * (1.0F - f) < 20.0F)) {
         double d = Math.floor(inputValue * f);
         double e = Math.sqrt(inputValue * f * (1.0F - f));
         i = (int)Math.round(d + random.nextGaussian() * e);
         i = Math.clamp((long)i, 0, (int)inputValue);
      } else {
         for (int j = 0; j < inputValue; j++) {
            if (random.nextFloat() < f) {
               i++;
            }
         }
      }

      return inputValue - i;
   }

   @Override
   public MapCodec<RemoveBinomialEnchantmentEffect> getCodec() {
      return CODEC;
   }
}
