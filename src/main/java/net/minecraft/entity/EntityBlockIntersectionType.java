package net.minecraft.entity;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;

public enum EntityBlockIntersectionType {
   IN_BLOCK(0, 1610678016),
   IN_FLUID(1, 1610612991),
   IN_AIR(2, 1613968179);

   private static final IntFunction<EntityBlockIntersectionType> BY_ID = ValueLists.<EntityBlockIntersectionType>createIndexToValueFunction(
      type -> type.id, values(), ValueLists.OutOfBoundsHandling.ZERO
   );
   public static final PacketCodec<ByteBuf, EntityBlockIntersectionType> PACKET_CODEC = PacketCodecs.indexed(BY_ID, type -> type.id);
   private final int id;
   private final int color;

   private EntityBlockIntersectionType(final int id, final int color) {
      this.id = id;
      this.color = color;
   }

   public int getColor() {
      return this.color;
   }
}
