package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Описывает фоновую частицу окружения с вероятностью её появления за тик.
 * Используется в атрибутах биома для задания визуальных эффектов частиц.
 */
public record AmbientParticle(ParticleEffect particle, float probability) {

	public static final Codec<AmbientParticle> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			ParticleTypes.TYPE_CODEC.fieldOf("particle").forGetter(AmbientParticle::particle),
			Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(AmbientParticle::probability)
		).apply(instance, AmbientParticle::new)
	);

	/**
	 * Определяет, нужно ли спавнить частицу в данном тике на основе вероятности.
	 *
	 * @param random источник случайных чисел
	 * @return {@code true}, если частицу следует добавить
	 */
	public boolean shouldAddParticle(Random random) {
		return random.nextFloat() <= probability;
	}

	/**
	 * Создаёт одноэлементный список с данной частицей и вероятностью.
	 * Удобный фабричный метод для регистрации атрибутов биома.
	 *
	 * @param particle тип частицы
	 * @param probability вероятность появления за тик [0.0; 1.0]
	 * @return список из одного {@link AmbientParticle}
	 */
	public static List<AmbientParticle> of(ParticleEffect particle, float probability) {
		return List.of(new AmbientParticle(particle, probability));
	}
}
