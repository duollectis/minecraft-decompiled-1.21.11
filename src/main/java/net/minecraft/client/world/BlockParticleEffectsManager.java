package net.minecraft.client.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.BlockParticleEffect;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockParticleEffectsManager}.
 */
public class BlockParticleEffectsManager {

	private static final int MAX_PARTICLES = 512;
	private final List<BlockParticleEffectsManager.Entry> pool = new ArrayList<>();

	public void scheduleBlockParticles(
			Vec3d center,
			float radius,
			int blockCount,
			Pool<BlockParticleEffect> particles
	) {
		if (!particles.isEmpty()) {
			this.pool.add(new BlockParticleEffectsManager.Entry(center, radius, blockCount, particles));
		}
	}

	/**
	 * Tick.
	 *
	 * @param world world
	 */
	public void tick(ClientWorld world) {
		if (MinecraftClient.getInstance().options.getParticles().getValue() != ParticlesMode.ALL) {
			this.pool.clear();
		}
		else {
			int i = Weighting.getWeightSum(this.pool, BlockParticleEffectsManager.Entry::blockCount);
			int j = Math.min(i, 512);

			for (int k = 0; k < j; k++) {
				Weighting.getRandom(world.getRandom(), this.pool, i, BlockParticleEffectsManager.Entry::blockCount)
				         .ifPresent(entry -> this.addEffect(world, entry));
			}

			this.pool.clear();
		}
	}

	private void addEffect(ClientWorld world, BlockParticleEffectsManager.Entry entry) {
		Random random = world.getRandom();
		Vec3d vec3d = entry.center();
		Vec3d
				vec3d2 =
				new Vec3d(
						random.nextFloat() * 2.0F - 1.0F,
						random.nextFloat() * 2.0F - 1.0F,
						random.nextFloat() * 2.0F - 1.0F
				).normalize();
		float f = (float) Math.cbrt(random.nextFloat()) * entry.radius();
		Vec3d vec3d3 = vec3d2.multiply(f);
		Vec3d vec3d4 = vec3d.add(vec3d3);
		if (world.getBlockState(BlockPos.ofFloored(vec3d4)).isAir()) {
			float g = 0.5F / (f / entry.radius() + 0.1F) * random.nextFloat() * random.nextFloat() + 0.3F;
			BlockParticleEffect blockParticleEffect = entry.blockParticles.get(random);
			Vec3d vec3d5 = vec3d.add(vec3d3.multiply(blockParticleEffect.scaling()));
			Vec3d vec3d6 = vec3d2.multiply(g * blockParticleEffect.speed());
			world.addParticleClient(
					blockParticleEffect.particle(),
					vec3d5.getX(),
					vec3d5.getY(),
					vec3d5.getZ(),
					vec3d6.getX(),
					vec3d6.getY(),
					vec3d6.getZ()
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	record Entry(Vec3d center, float radius, int blockCount, Pool<BlockParticleEffect> blockParticles) {
	}
}
