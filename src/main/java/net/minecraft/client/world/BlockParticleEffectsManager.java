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

/**
 * Менеджер частиц разрушения блоков.
 *
 * <p>Накапливает запросы на спавн частиц через {@link #scheduleBlockParticles},
 * а в {@link #tick} случайно выбирает до {@link #MAX_PARTICLES} частиц
 * пропорционально весу каждой записи и добавляет их в мир.
 * При настройке частиц не {@code ALL} очередь просто очищается.
 */
@Environment(EnvType.CLIENT)
public class BlockParticleEffectsManager {

	private static final int MAX_PARTICLES = 512;

	private final List<Entry> pool = new ArrayList<>();

	public void scheduleBlockParticles(
		Vec3d center,
		float radius,
		int blockCount,
		Pool<BlockParticleEffect> particles
	) {
		if (!particles.isEmpty()) {
			pool.add(new Entry(center, radius, blockCount, particles));
		}
	}

	public void tick(ClientWorld world) {
		if (MinecraftClient.getInstance().options.getParticles().getValue() != ParticlesMode.ALL) {
			pool.clear();
			return;
		}

		int totalWeight = Weighting.getWeightSum(pool, Entry::blockCount);
		int spawnCount = Math.min(totalWeight, MAX_PARTICLES);

		for (int i = 0; i < spawnCount; i++) {
			Weighting.getRandom(world.getRandom(), pool, totalWeight, Entry::blockCount)
				.ifPresent(entry -> addEffect(world, entry));
		}

		pool.clear();
	}

	/**
	 * Спавнит одну частицу для записи {@code entry} в случайной точке сферы.
	 * Частица не спавнится, если целевая позиция находится внутри непрозрачного блока.
	 */
	private void addEffect(ClientWorld world, Entry entry) {
		Random random = world.getRandom();
		Vec3d center = entry.center();
		Vec3d direction = new Vec3d(
			random.nextFloat() * 2.0F - 1.0F,
			random.nextFloat() * 2.0F - 1.0F,
			random.nextFloat() * 2.0F - 1.0F
		).normalize();
		float distance = (float) Math.cbrt(random.nextFloat()) * entry.radius();
		Vec3d offset = direction.multiply(distance);
		Vec3d spawnPos = center.add(offset);

		if (world.getBlockState(BlockPos.ofFloored(spawnPos)).isAir()) {
			float speed = 0.5F / (distance / entry.radius() + 0.1F) * random.nextFloat() * random.nextFloat() + 0.3F;
			BlockParticleEffect effect = entry.blockParticles.get(random);
			Vec3d particlePos = center.add(offset.multiply(effect.scaling()));
			Vec3d velocity = direction.multiply(speed * effect.speed());

			world.addParticleClient(
				effect.particle(),
				particlePos.getX(),
				particlePos.getY(),
				particlePos.getZ(),
				velocity.getX(),
				velocity.getY(),
				velocity.getZ()
			);
		}
	}

	@Environment(EnvType.CLIENT)
	record Entry(Vec3d center, float radius, int blockCount, Pool<BlockParticleEffect> blockParticles) {
	}
}
