package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ElderGuardianParticleModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.ElderGuardianEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code ElderGuardianParticle}.
 */
public class ElderGuardianParticle extends Particle {

	protected final ElderGuardianParticleModel model;
	protected final RenderLayer renderLayer = RenderLayers.entityTranslucent(ElderGuardianEntityRenderer.TEXTURE);

	ElderGuardianParticle(ClientWorld clientWorld, double d, double e, double f) {
		super(clientWorld, d, e, f);
		this.model =
				new ElderGuardianParticleModel(MinecraftClient
						.getInstance()
						.getLoadedEntityModels()
						.getModelPart(EntityModelLayers.ELDER_GUARDIAN));
		this.gravityStrength = 0.0F;
		this.maxAge = 30;
	}

	@Override
	public ParticleTextureSheet textureSheet() {
		return ParticleTextureSheet.ELDER_GUARDIANS;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		public Particle createParticle(
				SimpleParticleType simpleParticleType,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			return new ElderGuardianParticle(clientWorld, d, e, f);
		}
	}
}
