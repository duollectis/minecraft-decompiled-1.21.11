package net.minecraft.particle;

public record ParticleGroup(int maxCount) {
   public static final ParticleGroup SPORE_BLOSSOM_AIR = new ParticleGroup(1000);
}
