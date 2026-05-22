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
 * Эффект частицы с цветом и интенсивностью (мощностью).
 * Используется для частиц зелий и мгновенных эффектов.
 * Цвет хранится как упакованный RGB-int; значение {@code -1} означает «цвет не задан».
 */
public class EffectParticleEffect implements ParticleEffect {

	/** Нормализующий делитель для перевода байтового канала цвета [0..255] в float [0..1]. */
	private static final float COLOR_CHANNEL_MAX = 255.0F;

	/** Значение цвета по умолчанию — «не задан». */
	private static final int NO_COLOR = -1;

	/** Мощность частицы по умолчанию. */
	private static final float DEFAULT_POWER = 1.0F;

	private final ParticleType<EffectParticleEffect> type;
	private final int color;
	private final float power;

	/**
	 * Создаёт фабричный {@link MapCodec} для данного типа частицы.
	 * Поле {@code color} опционально и по умолчанию равно {@value #NO_COLOR}.
	 * Поле {@code power} опционально и по умолчанию равно {@value #DEFAULT_POWER}.
	 */
	public static MapCodec<EffectParticleEffect> createCodec(ParticleType<EffectParticleEffect> type) {
		return RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Codecs.RGB.optionalFieldOf("color", NO_COLOR).forGetter(effect -> effect.color),
						Codec.FLOAT.optionalFieldOf("power", DEFAULT_POWER).forGetter(effect -> effect.power)
				)
				.apply(instance, (color, power) -> new EffectParticleEffect(type, color, power))
		);
	}

	public static PacketCodec<? super ByteBuf, EffectParticleEffect> createPacketCodec(
			ParticleType<EffectParticleEffect> type
	) {
		return PacketCodec.tuple(
				PacketCodecs.INTEGER, effect -> effect.color,
				PacketCodecs.FLOAT, effect -> effect.power,
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

	public float getPower() {
		return power;
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
