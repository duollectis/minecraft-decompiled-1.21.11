package net.minecraft.world.gen.carver;

import com.mojang.serialization.Codec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.AquiferSampler;

import java.util.function.Function;

/**
 * Карвер пещер. Генерирует систему туннелей и сферических полостей
 * путём рекурсивного ветвления и случайного блуждания.
 */
public class CaveCarver extends Carver<CaveCarverConfig> {

	public CaveCarver(Codec<CaveCarverConfig> codec) {
		super(codec);
	}

	@Override
	public boolean shouldCarve(CaveCarverConfig config, Random random) {
		return random.nextFloat() <= config.probability;
	}

	@Override
	public boolean carve(
		CarverContext context,
		CaveCarverConfig config,
		Chunk chunk,
		Function<BlockPos, RegistryEntry<Biome>> posToBiome,
		Random random,
		AquiferSampler aquiferSampler,
		ChunkPos chunkPos,
		CarvingMask mask
	) {
		int maxBranchRange = ChunkSectionPos.getBlockCoord(getBranchFactor() * 2 - 1);
		int caveCount = random.nextInt(random.nextInt(random.nextInt(getMaxCaveCount()) + 1) + 1);

		for (int caveIndex = 0; caveIndex < caveCount; caveIndex++) {
			double originX = chunkPos.getOffsetX(random.nextInt(16));
			double originY = config.y.get(random, context);
			double originZ = chunkPos.getOffsetZ(random.nextInt(16));
			double hRadiusMultiplier = config.horizontalRadiusMultiplier.get(random);
			double vRadiusMultiplier = config.verticalRadiusMultiplier.get(random);
			double floorLevel = config.floorLevel.get(random);

			Carver.SkipPredicate skipPredicate = (ctx, relX, relY, relZ, y) ->
				isPositionExcluded(relX, relY, relZ, floorLevel);

			int tunnelCount = 1;

			if (random.nextInt(4) == 0) {
				double yScale = config.yScale.get(random);
				float sphereRadius = 1.0F + random.nextFloat() * 6.0F;
				carveCave(
					context, config, chunk, posToBiome, aquiferSampler,
					originX, originY, originZ, sphereRadius, yScale, mask, skipPredicate
				);
				tunnelCount += random.nextInt(4);
			}

			for (int tunnelIndex = 0; tunnelIndex < tunnelCount; tunnelIndex++) {
				float yaw = random.nextFloat() * (float) (Math.PI * 2);
				float pitch = (random.nextFloat() - 0.5F) / 4.0F;
				float width = getTunnelSystemWidth(random);
				int branchCount = maxBranchRange - random.nextInt(maxBranchRange / 4);

				carveTunnels(
					context, config, chunk, posToBiome,
					random.nextLong(), aquiferSampler,
					originX, originY, originZ,
					hRadiusMultiplier, vRadiusMultiplier,
					width, yaw, pitch,
					0, branchCount,
					getTunnelSystemHeightWidthRatio(),
					mask, skipPredicate
				);
			}
		}

		return true;
	}

	protected int getMaxCaveCount() {
		return 15;
	}

	protected float getTunnelSystemWidth(Random random) {
		float width = random.nextFloat() * 2.0F + random.nextFloat();

		if (random.nextInt(10) == 0) {
			width *= random.nextFloat() * random.nextFloat() * 3.0F + 1.0F;
		}

		return width;
	}

	protected double getTunnelSystemHeightWidthRatio() {
		return 1.0;
	}

	protected void carveCave(
		CarverContext context,
		CaveCarverConfig config,
		Chunk chunk,
		Function<BlockPos, RegistryEntry<Biome>> posToBiome,
		AquiferSampler aquiferSampler,
		double x,
		double y,
		double z,
		float radius,
		double yScale,
		CarvingMask mask,
		Carver.SkipPredicate skipPredicate
	) {
		double hRadius = 1.5 + MathHelper.sin((float) (Math.PI / 2)) * radius;
		double vRadius = hRadius * yScale;
		carveRegion(
			context, config, chunk, posToBiome, aquiferSampler,
			x + 1.0, y, z, hRadius, vRadius, mask, skipPredicate
		);
	}

	/**
	 * Рекурсивно вырезает систему туннелей, случайно блуждая в пространстве.
	 * При достижении точки ветвления создаёт два дочерних туннеля под углом ±90°.
	 */
	protected void carveTunnels(
		CarverContext context,
		CaveCarverConfig config,
		Chunk chunk,
		Function<BlockPos, RegistryEntry<Biome>> posToBiome,
		long seed,
		AquiferSampler aquiferSampler,
		double x,
		double y,
		double z,
		double horizontalScale,
		double verticalScale,
		float width,
		float yaw,
		float pitch,
		int branchStartIndex,
		int branchCount,
		double yawPitchRatio,
		CarvingMask mask,
		Carver.SkipPredicate skipPredicate
	) {
		Random random = Random.create(seed);
		int branchPoint = random.nextInt(branchCount / 2) + branchCount / 4;
		boolean steepPitch = random.nextInt(6) == 0;
		float pitchDelta = 0.0F;
		float yawDelta = 0.0F;

		for (int step = branchStartIndex; step < branchCount; step++) {
			double hRadius = 1.5 + MathHelper.sin((float) Math.PI * step / branchCount) * width;
			double vRadius = hRadius * yawPitchRatio;
			float cosPitch = MathHelper.cos(pitch);

			x += MathHelper.cos(yaw) * cosPitch;
			y += MathHelper.sin(pitch);
			z += MathHelper.sin(yaw) * cosPitch;

			pitch *= steepPitch ? 0.92F : 0.7F;
			pitch += pitchDelta * 0.1F;
			yaw += yawDelta * 0.1F;
			pitchDelta *= 0.9F;
			yawDelta *= 0.75F;
			pitchDelta += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
			yawDelta += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

			if (step == branchPoint && width > 1.0F) {
				carveTunnels(
					context, config, chunk, posToBiome, random.nextLong(), aquiferSampler,
					x, y, z, horizontalScale, verticalScale,
					random.nextFloat() * 0.5F + 0.5F,
					yaw - (float) (Math.PI / 2), pitch / 3.0F,
					step, branchCount, 1.0, mask, skipPredicate
				);
				carveTunnels(
					context, config, chunk, posToBiome, random.nextLong(), aquiferSampler,
					x, y, z, horizontalScale, verticalScale,
					random.nextFloat() * 0.5F + 0.5F,
					yaw + (float) (Math.PI / 2), pitch / 3.0F,
					step, branchCount, 1.0, mask, skipPredicate
				);
				return;
			}

			if (random.nextInt(4) == 0) {
				continue;
			}

			if (!canCarveBranch(chunk.getPos(), x, z, step, branchCount, width)) {
				return;
			}

			carveRegion(
				context, config, chunk, posToBiome, aquiferSampler,
				x, y, z,
				hRadius * horizontalScale, vRadius * verticalScale,
				mask, skipPredicate
			);
		}
	}

	private static boolean isPositionExcluded(
		double scaledRelativeX,
		double scaledRelativeY,
		double scaledRelativeZ,
		double floorY
	) {
		if (scaledRelativeY <= floorY) {
			return true;
		}

		return scaledRelativeX * scaledRelativeX
			+ scaledRelativeY * scaledRelativeY
			+ scaledRelativeZ * scaledRelativeZ >= 1.0;
	}
}
