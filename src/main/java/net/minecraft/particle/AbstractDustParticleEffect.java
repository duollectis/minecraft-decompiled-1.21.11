package net.minecraft.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.math.MathHelper;

/**
 * Базовый класс для эффектов пылевых частиц с масштабом.
 * Масштаб автоматически зажимается в диапазон [{@link #MIN_SCALE}, {@link #MAX_SCALE}].
 */
public abstract class AbstractDustParticleEffect implements ParticleEffect {

	public static final float MIN_SCALE = 0.01F;
	public static final float MAX_SCALE = 4.0F;

	protected static final Codec<Float> SCALE_CODEC = Codec.FLOAT
			.validate(scale -> scale >= MIN_SCALE && scale <= MAX_SCALE
					? DataResult.success(scale)
					: DataResult.error(() -> "Value must be within range [" + MIN_SCALE + ";" + MAX_SCALE + "]: " + scale)
			);

	private final float scale;

	public AbstractDustParticleEffect(float scale) {
		this.scale = MathHelper.clamp(scale, MIN_SCALE, MAX_SCALE);
	}

	public float getScale() {
		return scale;
	}
}
