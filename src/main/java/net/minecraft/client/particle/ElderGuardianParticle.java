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

/**
 * Частица проклятия Старшего Стража — отображает полноразмерную 3D-модель
 * существа, парящую над игроком при наложении эффекта Mining Fatigue III.
 * Использует специальный {@link ParticleTextureSheet#ELDER_GUARDIANS} для рендеринга.
 */
@Environment(EnvType.CLIENT)
public class ElderGuardianParticle extends Particle {

	private static final int MAX_AGE = 30;

	protected final ElderGuardianParticleModel model;
	protected final RenderLayer renderLayer = RenderLayers.entityTranslucent(ElderGuardianEntityRenderer.TEXTURE);

	ElderGuardianParticle(ClientWorld world, double x, double y, double z) {
		super(world, x, y, z);
		this.model = new ElderGuardianParticleModel(
				MinecraftClient.getInstance()
						.getLoadedEntityModels()
						.getModelPart(EntityModelLayers.ELDER_GUARDIAN)
		);
		this.gravityStrength = 0.0F;
		this.maxAge = MAX_AGE;
	}

	@Override
	public ParticleTextureSheet textureSheet() {
		return ParticleTextureSheet.ELDER_GUARDIANS;
	}

	/**
	 * Фабрика для создания частицы проклятия Старшего Стража.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		@Override
		public Particle createParticle(
				SimpleParticleType type,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			return new ElderGuardianParticle(world, x, y, z);
		}
	}
}
