package net.minecraft.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;

public enum SwingAnimationType implements StringIdentifiable {
   NONE(0, "none"),
   WHACK(1, "whack"),
   STAB(2, "stab");

   private static final IntFunction<SwingAnimationType> BY_PACKET_ID = ValueLists.createIndexToValueFunction(
      SwingAnimationType::getPacketId, values(), ValueLists.OutOfBoundsHandling.ZERO
   );
   public static final Codec<SwingAnimationType> CODEC = StringIdentifiable.createCodec(SwingAnimationType::values);
   public static final PacketCodec<ByteBuf, SwingAnimationType> PACKET_CODEC = PacketCodecs.indexed(BY_PACKET_ID, SwingAnimationType::getPacketId);
   private final int packetId;
   private final String name;

   private SwingAnimationType(final int packetId, final String name) {
      this.packetId = packetId;
      this.name = name;
   }

   public int getPacketId() {
      return this.packetId;
   }

   @Override
   public String asString() {
      return this.name;
   }
}
