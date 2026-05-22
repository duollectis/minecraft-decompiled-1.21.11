package net.minecraft.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;
import org.joml.Vector3f;

/**
 * Эффект пылевой частицы с плавным переходом цвета от {@code fromColor} к {@code toColor}.
 * Оба цвета хранятся как упакованные RGB-int.
 */
public class DustColorTransitionParticleEffect extends AbstractDustParticleEffect {

	/** Синий цвет скалка (0x39A0E0). */
	public static final int SCULK_BLUE = 0x39A0E0;

	public static final DustColorTransitionParticleEffect DEFAULT =
			new DustColorTransitionParticleEffect(SCULK_BLUE, DustParticleEffect.RED, 1.0F);

	public static final MapCodec<DustColorTransitionParticleEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codecs.RGB.fieldOf("from_color").forGetter(particle -> particle.fromColor),
					Codecs.RGB.fieldOf("to_color").forGetter(particle -> particle.toColor),
					SCALE_CODEC.fieldOf("scale").forGetter(AbstractDustParticleEffect::getScale)
			).apply(instance, DustColorTransitionParticleEffect::new)
	);

	public static final PacketCodec<RegistryByteBuf, DustColorTransitionParticleEffect> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER, particle -> particle.fromColor,
			PacketCodecs.INTEGER, particle -> particle.toColor,
			PacketCodecs.FLOAT, AbstractDustParticleEffect::getScale,
			DustColorTransitionParticleEffect::new
	);

	private final int fromColor;
	private final int toColor;

	public DustColorTransitionParticleEffect(int fromColor, int toColor, float scale) {
		super(scale);
		this.fromColor = fromColor;
		this.toColor = toColor;
	}

	@Override
	public ParticleType<DustColorTransitionParticleEffect> getType() {
		return ParticleTypes.DUST_COLOR_TRANSITION;
	}

	public Vector3f getFromColor() {
		return ColorHelper.toRgbVector(fromColor);
	}

	public Vector3f getToColor() {
		return ColorHelper.toRgbVector(toColor);
	}
}
