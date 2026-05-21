package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * {@code AmbientParticle}.
 */
public record AmbientParticle(ParticleEffect particle, float probability) {

	public static final Codec<AmbientParticle> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    ParticleTypes.TYPE_CODEC.fieldOf("particle").forGetter(config -> config.particle),
					                    Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(config -> config.probability)
			                    )
			                    .apply(instance, AmbientParticle::new)
	);

	public boolean shouldAddParticle(Random random) {
		return random.nextFloat() <= this.probability;
	}

	public static List<AmbientParticle> of(ParticleEffect particle, float probability) {
		return List.of(new AmbientParticle(particle, probability));
	}
}
