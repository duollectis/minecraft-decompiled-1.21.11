package net.minecraft.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;

/**
 * {@code EffectParticleEffect}.
 */
public class EffectParticleEffect implements ParticleEffect {

	private final ParticleType<EffectParticleEffect> type;
	private final int color;
	private final float power;

	public static MapCodec<EffectParticleEffect> createCodec(ParticleType<EffectParticleEffect> type) {
		return RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Codecs.RGB.optionalFieldOf("color", -1).forGetter(effect -> effect.color),
						                    Codec.FLOAT.optionalFieldOf("power", 1.0F).forGetter(effect -> effect.power)
				                    )
				                    .apply(instance, (color, power) -> new EffectParticleEffect(type, color, power))
		);
	}

	public static PacketCodec<? super ByteBuf, EffectParticleEffect> createPacketCodec(ParticleType<EffectParticleEffect> type) {
		return PacketCodec.tuple(
				PacketCodecs.INTEGER,
				effect -> effect.color,
				PacketCodecs.FLOAT,
				effect -> effect.power,
				(color, power) -> new EffectParticleEffect(type, color, power)
		);
	}

	private EffectParticleEffect(ParticleType<EffectParticleEffect> type, int color, float power) {
		this.type = type;
		this.color = color;
		this.power = power;
	}

	@Override
	public ParticleType<EffectParticleEffect> getType() {
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

	public float getPower() {
		return this.power;
	}

	public static EffectParticleEffect of(ParticleType<EffectParticleEffect> type, int color, float power) {
		return new EffectParticleEffect(type, color, power);
	}

	public static EffectParticleEffect of(
			ParticleType<EffectParticleEffect> type,
			float r,
			float g,
			float b,
			float power
	) {
		return of(type, ColorHelper.fromFloats(1.0F, r, g, b), power);
	}
}
