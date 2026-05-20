package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record CountProperty(boolean normalize) implements NumericProperty {
   public static final MapCodec<CountProperty> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Codec.BOOL.optionalFieldOf("normalize", true).forGetter(CountProperty::normalize)).apply(instance, CountProperty::new)
   );

   @Override
   public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable HeldItemContext context, int seed) {
      float f = stack.getCount();
      float g = stack.getMaxCount();
      return this.normalize ? MathHelper.clamp(f / g, 0.0F, 1.0F) : MathHelper.clamp(f, 0.0F, g);
   }

   @Override
   public MapCodec<CountProperty> getCodec() {
      return CODEC;
   }
}
