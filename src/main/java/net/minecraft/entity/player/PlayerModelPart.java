package net.minecraft.entity.player;

import com.mojang.serialization.Codec;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

public enum PlayerModelPart implements StringIdentifiable {
   CAPE(0, "cape"),
   JACKET(1, "jacket"),
   LEFT_SLEEVE(2, "left_sleeve"),
   RIGHT_SLEEVE(3, "right_sleeve"),
   LEFT_PANTS_LEG(4, "left_pants_leg"),
   RIGHT_PANTS_LEG(5, "right_pants_leg"),
   HAT(6, "hat");

   public static final Codec<PlayerModelPart> CODEC = StringIdentifiable.createCodec(PlayerModelPart::values);
   private final int id;
   private final int bitFlag;
   private final String name;
   private final Text optionName;

   private PlayerModelPart(final int id, final String name) {
      this.id = id;
      this.bitFlag = 1 << id;
      this.name = name;
      this.optionName = Text.translatable("options.modelPart." + name);
   }

   public int getBitFlag() {
      return this.bitFlag;
   }

   public int getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public Text getOptionName() {
      return this.optionName;
   }

   @Override
   public String asString() {
      return this.name;
   }
}
