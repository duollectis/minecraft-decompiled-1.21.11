package net.minecraft.particle;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;

public class TintedParticleEffect implements ParticleEffect {
   private final ParticleType<TintedParticleEffect> type;
   private final int color;

   public static MapCodec<TintedParticleEffect> createCodec(ParticleType<TintedParticleEffect> type) {
      return Codecs.ARGB.xmap(color -> new TintedParticleEffect(type, color), effect -> effect.color).fieldOf("color");
   }

   public static PacketCodec<? super ByteBuf, TintedParticleEffect> createPacketCodec(ParticleType<TintedParticleEffect> type) {
      return PacketCodecs.INTEGER.xmap(color -> new TintedParticleEffect(type, color), particleEffect -> particleEffect.color);
   }

   private TintedParticleEffect(ParticleType<TintedParticleEffect> type, int color) {
      this.type = type;
      this.color = color;
   }

   @Override
   public ParticleType<TintedParticleEffect> getType() {
      return this.type;
   }

   public float getRed() {
      return ColorHelper.getRed(this.color) / 255.0F;
   }

   public float getGreen() {
      return ColorHelper.getGreen(this.color) / 255.0F;
   }

   public float getBlue() {
      return ColorHelper.getBlue(this.color) / 255.0F;
   }

   public float getAlpha() {
      return ColorHelper.getAlpha(this.color) / 255.0F;
   }

   public static TintedParticleEffect create(ParticleType<TintedParticleEffect> type, int color) {
      return new TintedParticleEffect(type, color);
   }

   public static TintedParticleEffect create(ParticleType<TintedParticleEffect> type, float r, float g, float b) {
      return create(type, ColorHelper.fromFloats(1.0F, r, g, b));
   }
}
