package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record ParticleTextureSheet(String name) {
   public static final ParticleTextureSheet SINGLE_QUADS = new ParticleTextureSheet("SINGLE_QUADS");
   public static final ParticleTextureSheet ITEM_PICKUP = new ParticleTextureSheet("ITEM_PICKUP");
   public static final ParticleTextureSheet ELDER_GUARDIANS = new ParticleTextureSheet("ELDER_GUARDIANS");
   public static final ParticleTextureSheet NO_RENDER = new ParticleTextureSheet("NO_RENDER");
}
