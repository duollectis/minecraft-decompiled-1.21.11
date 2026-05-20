package net.minecraft.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

public enum EntityPose implements StringIdentifiable {
   STANDING(0, "standing"),
   GLIDING(1, "fall_flying"),
   SLEEPING(2, "sleeping"),
   SWIMMING(3, "swimming"),
   SPIN_ATTACK(4, "spin_attack"),
   CROUCHING(5, "crouching"),
   LONG_JUMPING(6, "long_jumping"),
   DYING(7, "dying"),
   CROAKING(8, "croaking"),
   USING_TONGUE(9, "using_tongue"),
   SITTING(10, "sitting"),
   ROARING(11, "roaring"),
   SNIFFING(12, "sniffing"),
   EMERGING(13, "emerging"),
   DIGGING(14, "digging"),
   SLIDING(15, "sliding"),
   SHOOTING(16, "shooting"),
   INHALING(17, "inhaling");

   public static final IntFunction<EntityPose> INDEX_TO_VALUE = ValueLists.createIndexToValueFunction(
      EntityPose::getIndex, values(), ValueLists.OutOfBoundsHandling.ZERO
   );
   public static final Codec<EntityPose> CODEC = StringIdentifiable.createCodec(EntityPose::values);
   public static final PacketCodec<ByteBuf, EntityPose> PACKET_CODEC = PacketCodecs.indexed(INDEX_TO_VALUE, EntityPose::getIndex);
   private final int index;
   private final String name;

   private EntityPose(final int index, final String name) {
      this.index = index;
      this.name = name;
   }

   public int getIndex() {
      return this.index;
   }

   @Override
   public String asString() {
      return this.name;
   }
}
