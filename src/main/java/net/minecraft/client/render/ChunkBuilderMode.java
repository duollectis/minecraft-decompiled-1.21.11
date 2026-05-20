package net.minecraft.client.render;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;

@Environment(EnvType.CLIENT)
public enum ChunkBuilderMode {
   NONE(0, "options.prioritizeChunkUpdates.none"),
   PLAYER_AFFECTED(1, "options.prioritizeChunkUpdates.byPlayer"),
   NEARBY(2, "options.prioritizeChunkUpdates.nearby");

   private static final IntFunction<ChunkBuilderMode> BY_ID = ValueLists.createIndexToValueFunction(
      (ChunkBuilderMode chunkBuilderMode) -> chunkBuilderMode.id, values(), ValueLists.OutOfBoundsHandling.WRAP
   );
   public static final Codec<ChunkBuilderMode> CODEC = Codec.INT.xmap(BY_ID::apply, mode -> mode.id);
   private final int id;
   private final Text text;

   private ChunkBuilderMode(final int id, final String name) {
      this.id = id;
      this.text = Text.translatable(name);
   }

   public Text getText() {
      return this.text;
   }
}
