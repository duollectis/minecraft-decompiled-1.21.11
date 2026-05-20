package net.minecraft.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Codecs;

public record SingleOptionInputControl(int width, List<SingleOptionInputControl.Entry> entries, Text label, boolean labelVisible) implements InputControl {
   public static final MapCodec<SingleOptionInputControl> CODEC = RecordCodecBuilder.<SingleOptionInputControl>mapCodec(
         instance -> instance.group(
               Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(SingleOptionInputControl::width),
               Codecs.nonEmptyList(SingleOptionInputControl.Entry.CODEC.listOf()).fieldOf("options").forGetter(SingleOptionInputControl::entries),
               TextCodecs.CODEC.fieldOf("label").forGetter(SingleOptionInputControl::label),
               Codec.BOOL.optionalFieldOf("label_visible", true).forGetter(SingleOptionInputControl::labelVisible)
            )
            .apply(instance, SingleOptionInputControl::new)
      )
      .validate(inputControl -> {
         long l = inputControl.entries.stream().filter(SingleOptionInputControl.Entry::initial).count();
         return l > 1L ? DataResult.error(() -> "Multiple initial values") : DataResult.success(inputControl);
      });

   @Override
   public MapCodec<SingleOptionInputControl> getCodec() {
      return CODEC;
   }

   public Optional<SingleOptionInputControl.Entry> getInitialEntry() {
      return this.entries.stream().filter(SingleOptionInputControl.Entry::initial).findFirst();
   }

   public record Entry(String id, Optional<Text> display, boolean initial) {
      public static final Codec<SingleOptionInputControl.Entry> BASE_CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.fieldOf("id").forGetter(SingleOptionInputControl.Entry::id),
               TextCodecs.CODEC.optionalFieldOf("display").forGetter(SingleOptionInputControl.Entry::display),
               Codec.BOOL.optionalFieldOf("initial", false).forGetter(SingleOptionInputControl.Entry::initial)
            )
            .apply(instance, SingleOptionInputControl.Entry::new)
      );
      public static final Codec<SingleOptionInputControl.Entry> CODEC = Codec.withAlternative(
         BASE_CODEC, Codec.STRING, id -> new SingleOptionInputControl.Entry(id, Optional.empty(), false)
      );

      public Text getDisplay() {
         return this.display.orElseGet(() -> Text.literal(this.id));
      }
   }
}
