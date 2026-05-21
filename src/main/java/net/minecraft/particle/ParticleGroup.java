package net.minecraft.particle;

/**
 * {@code ParticleGroup}.
 */
public record ParticleGroup(int maxCount) {

	public static final ParticleGroup SPORE_BLOSSOM_AIR = new ParticleGroup(1000);
}
