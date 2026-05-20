package net.minecraft.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;

public enum Arm implements StringIdentifiable {
   LEFT(0, "left", "options.mainHand.left"),
   RIGHT(1, "right", "options.mainHand.right");

   public static final Codec<Arm> CODEC = StringIdentifiable.createCodec(Arm::values);
   private static final IntFunction<Arm> BY_ID = ValueLists.createIndexToValueFunction(
      (Arm arm) -> arm.id, values(), ValueLists.OutOfBoundsHandling.ZERO
   );
   public static final PacketCodec<ByteBuf, Arm> PACKET_CODEC = PacketCodecs.indexed(BY_ID, arm -> arm.id);
   private final int id;
   private final String name;
   private final Text text;

   private Arm(final int id, final String name, final String translationKey) {
      this.id = id;
      this.name = name;
      this.text = Text.translatable(translationKey);
   }

   public Arm getOpposite() {
      return switch (this) {
         case LEFT -> RIGHT;
         case RIGHT -> LEFT;
      };
   }

   public Text getText() {
      return this.text;
   }

   @Override
   public String asString() {
      return this.name;
   }
}
