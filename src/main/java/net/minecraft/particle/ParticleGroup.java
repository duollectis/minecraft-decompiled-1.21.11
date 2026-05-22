package net.minecraft.particle;

/**
 * Группа частиц с ограничением на максимальное количество одновременно активных экземпляров.
 * Используется для предотвращения перегрузки клиента при массовом спавне частиц одного типа.
 */
public record ParticleGroup(int maxCount) {

	public static final ParticleGroup SPORE_BLOSSOM_AIR = new ParticleGroup(1000);
}
