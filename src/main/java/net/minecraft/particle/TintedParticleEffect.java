package net.minecraft.particle;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;

/**
 * Эффект частицы с произвольным ARGB-цветом.
 * Используется для частиц с прозрачностью, например вспышки или листьев.
 */
public class TintedParticleEffect implements ParticleEffect {

	/** Нормализующий делитель для перевода байтового канала цвета [0..255] в float [0..1]. */
	private static final float COLOR_CHANNEL_MAX = 255.0F;

	private final ParticleType<TintedParticleEffect> type;
	private final int color;

	public static MapCodec<TintedParticleEffect> createCodec(ParticleType<TintedParticleEffect> type) {
		return Codecs.ARGB
				.xmap(color -> new TintedParticleEffect(type, color), effect -> effect.color)
				.fieldOf("color");
	}

	public static PacketCodec<? super ByteBuf, TintedParticleEffect> createPacketCodec(
			ParticleType<TintedParticleEffect> type
	) {
		return PacketCodecs.INTEGER.xmap(
				color -> new TintedParticleEffect(type, color),
				effect -> effect.color
		);
	}

	private TintedParticleEffect(ParticleType<TintedParticleEffect> type, int color) {
		this.type = type;
		this.color = color;
	}

	@Override
	public ParticleType<TintedParticleEffect> getType() {
		return type;
	}

	public float getRed() {
		return ColorHelper.getRed(color) / COLOR_CHANNEL_MAX;
	}

	public float getGreen() {
		return ColorHelper.getGreen(color) / COLOR_CHANNEL_MAX;
	}

	public float getBlue() {
		return ColorHelper.getBlue(color) / COLOR_CHANNEL_MAX;
	}

	public float getAlpha() {
		return ColorHelper.getAlpha(color) / COLOR_CHANNEL_MAX;
	}

	public static TintedParticleEffect create(ParticleType<TintedParticleEffect> type, int color) {
		return new TintedParticleEffect(type, color);
	}

	public static TintedParticleEffect create(ParticleType<TintedParticleEffect> type, float r, float g, float b) {
		return create(type, ColorHelper.fromFloats(1.0F, r, g, b));
	}
}
