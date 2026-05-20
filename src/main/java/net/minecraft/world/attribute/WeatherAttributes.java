package net.minecraft.world.attribute;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;
import net.minecraft.world.attribute.timeline.Timelines;

public class WeatherAttributes {
   public static final EnvironmentAttributeMap RAIN_EFFECTS = EnvironmentAttributeMap.builder()
      .with(EnvironmentAttributes.SKY_COLOR_VISUAL, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGrayArg(0.6F, 0.75F))
      .with(EnvironmentAttributes.FOG_COLOR_VISUAL, ColorModifier.MULTIPLY_RGB, ColorHelper.fromFloats(1.0F, 0.5F, 0.5F, 0.6F))
      .with(EnvironmentAttributes.CLOUD_COLOR_VISUAL, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGrayArg(0.24F, 0.5F))
      .with(EnvironmentAttributes.SKY_LIGHT_LEVEL_GAMEPLAY, FloatModifier.ALPHA_BLEND, new BlendArgument(4.0F, 0.3125F))
      .with(EnvironmentAttributes.SKY_LIGHT_COLOR_VISUAL, ColorModifier.ALPHA_BLEND, ColorHelper.withAlpha(0.3125F, Timelines.NIGHT_SKY_LIGHT_COLOR))
      .with(EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL, FloatModifier.ALPHA_BLEND, new BlendArgument(0.24F, 0.3125F))
      .with(EnvironmentAttributes.STAR_BRIGHTNESS_VISUAL, 0.0F)
      .with(EnvironmentAttributes.SUNRISE_SUNSET_COLOR_VISUAL, ColorModifier.MULTIPLY_ARGB, ColorHelper.fromFloats(1.0F, 0.5F, 0.5F, 0.6F))
      .with(EnvironmentAttributes.BEES_STAY_IN_HIVE_GAMEPLAY, true)
      .build();
   public static final EnvironmentAttributeMap THUNDER_EFFECTS = EnvironmentAttributeMap.builder()
      .with(EnvironmentAttributes.SKY_COLOR_VISUAL, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGrayArg(0.24F, 0.94F))
      .with(EnvironmentAttributes.FOG_COLOR_VISUAL, ColorModifier.MULTIPLY_RGB, ColorHelper.fromFloats(1.0F, 0.25F, 0.25F, 0.3F))
      .with(EnvironmentAttributes.CLOUD_COLOR_VISUAL, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGrayArg(0.095F, 0.94F))
      .with(EnvironmentAttributes.SKY_LIGHT_LEVEL_GAMEPLAY, FloatModifier.ALPHA_BLEND, new BlendArgument(4.0F, 0.52734375F))
      .with(EnvironmentAttributes.SKY_LIGHT_COLOR_VISUAL, ColorModifier.ALPHA_BLEND, ColorHelper.withAlpha(0.52734375F, Timelines.NIGHT_SKY_LIGHT_COLOR))
      .with(EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL, FloatModifier.ALPHA_BLEND, new BlendArgument(0.24F, 0.52734375F))
      .with(EnvironmentAttributes.STAR_BRIGHTNESS_VISUAL, 0.0F)
      .with(EnvironmentAttributes.SUNRISE_SUNSET_COLOR_VISUAL, ColorModifier.MULTIPLY_ARGB, ColorHelper.fromFloats(1.0F, 0.25F, 0.25F, 0.3F))
      .with(EnvironmentAttributes.BEES_STAY_IN_HIVE_GAMEPLAY, true)
      .build();
   private static final Set<EnvironmentAttribute<?>> ATTRIBUTES = Sets.union(RAIN_EFFECTS.keySet(), THUNDER_EFFECTS.keySet());

   public static void addWeatherAttributes(WorldEnvironmentAttributeAccess.Builder builder, WeatherAttributes.WeatherAccess weather) {
      for (EnvironmentAttribute<?> environmentAttribute : ATTRIBUTES) {
         addWeatherAttribute(builder, weather, environmentAttribute);
      }
   }

   private static <Value> void addWeatherAttribute(
      WorldEnvironmentAttributeAccess.Builder builder, WeatherAttributes.WeatherAccess weather, EnvironmentAttribute<Value> attribute
   ) {
      EnvironmentAttributeMap.Entry<Value, ?> entry = RAIN_EFFECTS.getEntry(attribute);
      EnvironmentAttributeMap.Entry<Value, ?> entry2 = THUNDER_EFFECTS.getEntry(attribute);
      builder.timeBased(attribute, (value, time) -> {
         float f = weather.getThunderGradient();
         float g = weather.getRainGradient() - f;
         if (entry != null && g > 0.0F) {
            Value object = entry.apply(value);
            value = attribute.getType().stateChangeLerp().apply(g, value, object);
         }

         if (entry2 != null && f > 0.0F) {
            Value object = entry2.apply(value);
            value = attribute.getType().stateChangeLerp().apply(f, value, object);
         }

         return value;
      });
   }

   public interface WeatherAccess {
      static WeatherAttributes.WeatherAccess ofWorld(World world) {
         return new WeatherAttributes.WeatherAccess() {
            @Override
            public float getRainGradient() {
               return world.getRainGradient(1.0F);
            }

            @Override
            public float getThunderGradient() {
               return world.getThunderGradient(1.0F);
            }
         };
      }

      float getRainGradient();

      float getThunderGradient();
   }
}
