package net.minecraft.item.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

public record MapFrameMarker(BlockPos pos, int rotation, int entityId) {
   public static final Codec<MapFrameMarker> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(MapFrameMarker::pos),
            Codec.INT.fieldOf("rotation").forGetter(MapFrameMarker::rotation),
            Codec.INT.fieldOf("entity_id").forGetter(MapFrameMarker::entityId)
         )
         .apply(instance, MapFrameMarker::new)
   );

   public String getKey() {
      return getKey(this.pos);
   }

   public static String getKey(BlockPos pos) {
      return "frame-" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
   }
}
