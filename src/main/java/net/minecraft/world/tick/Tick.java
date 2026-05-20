package net.minecraft.world.tick;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import org.jspecify.annotations.Nullable;

public record Tick<T>(T type, BlockPos pos, int delay, TickPriority priority) {
   public static final Strategy<Tick<?>> HASH_STRATEGY = new Strategy<Tick<?>>() {
      public int hashCode(Tick<?> tick) {
         return 31 * tick.pos().hashCode() + tick.type().hashCode();
      }

      public boolean equals(@Nullable Tick<?> tick, @Nullable Tick<?> tick2) {
         if (tick == tick2) {
            return true;
         } else {
            return tick != null && tick2 != null ? tick.type() == tick2.type() && tick.pos().equals(tick2.pos()) : false;
         }
      }
   };

   public static <T> Codec<Tick<T>> createCodec(Codec<T> typeCodec) {
      MapCodec<BlockPos> mapCodec = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Codec.INT.fieldOf("x").forGetter(Vec3i::getX), Codec.INT.fieldOf("y").forGetter(Vec3i::getY), Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
            )
            .apply(instance, BlockPos::new)
      );
      return RecordCodecBuilder.create(
         instance -> instance.group(
               typeCodec.fieldOf("i").forGetter(Tick::type),
               mapCodec.forGetter(Tick::pos),
               Codec.INT.fieldOf("t").forGetter(Tick::delay),
               TickPriority.CODEC.fieldOf("p").forGetter(Tick::priority)
            )
            .apply(instance, Tick::new)
      );
   }

   public static <T> List<Tick<T>> filter(List<Tick<T>> ticks, ChunkPos chunkPos) {
      long l = chunkPos.toLong();
      return ticks.stream().filter(tick -> ChunkPos.toLong(tick.pos()) == l).toList();
   }

   public OrderedTick<T> createOrderedTick(long time, long subTickOrder) {
      return new OrderedTick<>(this.type, this.pos, time + this.delay, this.priority, subTickOrder);
   }

   public static <T> Tick<T> create(T type, BlockPos pos) {
      return new Tick<>(type, pos, 0, TickPriority.NORMAL);
   }
}
