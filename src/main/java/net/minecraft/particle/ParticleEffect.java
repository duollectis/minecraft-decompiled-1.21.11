package net.minecraft.particle;

/**
 * Базовый интерфейс для всех эффектов частиц.
 * Каждый эффект хранит ссылку на свой тип и может быть сериализован через {@link ParticleType}.
 */
public interface ParticleEffect {

	ParticleType<?> getType();
}
