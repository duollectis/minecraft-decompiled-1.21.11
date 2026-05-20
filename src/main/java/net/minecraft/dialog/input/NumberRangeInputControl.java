package net.minecraft.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;

public record NumberRangeInputControl(int width, Text label, String labelFormat, NumberRangeInputControl.RangeInfo rangeInfo) implements InputControl {
   public static final MapCodec<NumberRangeInputControl> CODEC = RecordCodecBuilder.<NumberRangeInputControl>mapCodec(
      instance -> instance.group(
            Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(NumberRangeInputControl::width),
            TextCodecs.CODEC.fieldOf("label").forGetter(NumberRangeInputControl::label),
            Codec.STRING.optionalFieldOf("label_format", "options.generic_value").forGetter(NumberRangeInputControl::labelFormat),
            NumberRangeInputControl.RangeInfo.CODEC.forGetter(NumberRangeInputControl::rangeInfo)
         )
         .apply(instance, NumberRangeInputControl::new)
   );

   @Override
   public MapCodec<NumberRangeInputControl> getCodec() {
      return CODEC;
   }

   public Text getFormattedLabel(String value) {
      return Text.translatable(this.labelFormat, this.label, value);
   }

   public record RangeInfo(float start, float end, Optional<Float> initial, Optional<Float> step) {
      public static final MapCodec<NumberRangeInputControl.RangeInfo> CODEC = RecordCodecBuilder.<NumberRangeInputControl.RangeInfo>mapCodec(
            instance -> instance.group(
                  Codec.FLOAT.fieldOf("start").forGetter(NumberRangeInputControl.RangeInfo::start),
                  Codec.FLOAT.fieldOf("end").forGetter(NumberRangeInputControl.RangeInfo::end),
                  Codec.FLOAT.optionalFieldOf("initial").forGetter(NumberRangeInputControl.RangeInfo::initial),
                  Codecs.POSITIVE_FLOAT.optionalFieldOf("step").forGetter(NumberRangeInputControl.RangeInfo::step)
               )
               .apply(instance, NumberRangeInputControl.RangeInfo::new)
         )
         .validate(rangeInfo -> {
            if (rangeInfo.initial.isPresent()) {
               double d = rangeInfo.initial.get().floatValue();
               double e = Math.min(rangeInfo.start, rangeInfo.end);
               double f = Math.max(rangeInfo.start, rangeInfo.end);
               if (d < e || d > f) {
                  return DataResult.error(() -> "Initial value " + d + " is outside of range [" + e + ", " + f + "]");
               }
            }

            return DataResult.success(rangeInfo);
         });

      public float sliderProgressToValue(float sliderProgress) {
         float f = MathHelper.lerp(sliderProgress, this.start, this.end);
         if (this.step.isEmpty()) {
            return f;
         } else {
            float g = this.step.get();
            float h = this.getInitialValue();
            float i = f - h;
            int j = Math.round(i / g);
            float k = h + j * g;
            if (!this.isValueOutOfRange(k)) {
               return k;
            } else {
               int l = j - MathHelper.sign(j);
               return h + l * g;
            }
         }
      }

      private boolean isValueOutOfRange(float value) {
         float f = this.valueToSliderProgress(value);
         return f < 0.0 || f > 1.0;
      }

      private float getInitialValue() {
         return this.initial.isPresent() ? this.initial.get() : (this.start + this.end) / 2.0F;
      }

      public float getInitialSliderProgress() {
         float f = this.getInitialValue();
         return this.valueToSliderProgress(f);
      }

      private float valueToSliderProgress(float value) {
         return this.start == this.end ? 0.5F : MathHelper.getLerpProgress(value, this.start, this.end);
      }
   }
}
