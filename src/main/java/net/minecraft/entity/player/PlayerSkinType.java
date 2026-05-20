package net.minecraft.entity.player;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import org.jspecify.annotations.Nullable;

public enum PlayerSkinType implements StringIdentifiable {
   SLIM("slim", "slim"),
   WIDE("wide", "default");

   public static final Codec<PlayerSkinType> CODEC = StringIdentifiable.createCodec(PlayerSkinType::values);
   private static final Function<String, PlayerSkinType> BY_MODEL_METADATA = StringIdentifiable.createMapper(
      values(), playerSkinType -> playerSkinType.modelMetadata
   );
   public static final PacketCodec<ByteBuf, PlayerSkinType> PACKET_CODEC = PacketCodecs.BOOLEAN.xmap(slim -> slim ? SLIM : WIDE, model -> model == SLIM);
   private final String name;
   private final String modelMetadata;

   private PlayerSkinType(final String name, final String modelMetadata) {
      this.name = name;
      this.modelMetadata = modelMetadata;
   }

   public static PlayerSkinType byModelMetadata(@Nullable String modelMetadata) {
      return Objects.requireNonNullElse(BY_MODEL_METADATA.apply(modelMetadata), WIDE);
   }

   @Override
   public String asString() {
      return this.name;
   }
}
