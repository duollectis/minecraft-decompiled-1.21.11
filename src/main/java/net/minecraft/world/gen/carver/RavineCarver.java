package net.minecraft.world.gen.carver;

import com.mojang.serialization.Codec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.AquiferSampler;

import java.util.function.Function;

/**
 * Карвер оврагов (каньонов). Генерирует длинные узкие разломы в рельефе,
 * используя случайное блуждание с переменным вертикальным масштабом.
 */
public class RavineCarver extends Carver<RavineCarverConfig> {

	public RavineCarver(Codec<RavineCarverConfig> codec) {
		super(codec);
	}

	@Override
	public boolean shouldCarve(RavineCarverConfig config, Random random) {
		return random.nextFloat() <= config.probability;
	}

	@Override
	public boolean carve(
		CarverContext context,
		RavineCarverConfig config,
		Chunk chunk,
		Function<BlockPos, RegistryEntry<Biome>> posToBiome,
		Random random,
		AquiferSampler aquiferSampler,
		ChunkPos chunkPos,
		CarvingMask mask
	) {
		int maxRange = (getBranchFactor() * 2 - 1) * 16;
		double originX = chunkPos.getOffsetX(random.nextInt(16));
		int originY = config.y.get(random, context);
		double originZ = chunkPos.getOffsetZ(random.nextInt(16));
		float yaw = random.nextFloat() * (float) (Math.PI * 2);
		float verticalRotation = config.verticalRotation.get(random);
		double yScale = config.yScale.get(random);
		float width = config.shape.thickness.get(random);
		int branchCount = (int) (maxRange * config.shape.distanceFactor.get(random));

		carveRavine(
			context, config, chunk, posToBiome,
			random.nextLong(), aquiferSampler,
			originX, originY, originZ,
			width, yaw, verticalRotation,
			0, branchCount, yScale, mask
		);

		return true;
	}

	private void carveRavine(
		CarverContext context,
		RavineCarverConfig config,
		Chunk chunk,
		Function<BlockPos, RegistryEntry<Biome>> posToBiome,
		long seed,
		AquiferSampler aquiferSampler,
		double x,
		double y,
		double z,
		float width,
		float yaw,
		float pitch,
		int branchStartIndex,
		int branchCount,
		double yawPitchRatio,
		CarvingMask mask
	) {
		Random random = Random.create(seed);
		float[] stretchFactors = createHorizontalStretchFactors(context, config, random);
		float pitchDelta = 0.0F;
		float yawDelta = 0.0F;

		for (int step = branchStartIndex; step < branchCount; step++) {
			double hRadius = 1.5 + MathHelper.sin(step * (float) Math.PI / branchCount) * width;
			double vRadius = hRadius * yawPitchRatio;
			hRadius *= config.shape.horizontalRadiusFactor.get(random);
			vRadius = getVerticalScale(config, random, vRadius, branchCount, step);

			float cosPitch = MathHelper.cos(pitch);
			float sinPitch = MathHelper.sin(pitch);

			x += MathHelper.cos(yaw) * cosPitch;
			y += sinPitch;
			z += MathHelper.sin(yaw) * cosPitch;

			pitch *= 0.7F;
			pitch += pitchDelta * 0.05F;
			yaw += yawDelta * 0.05F;
			pitchDelta *= 0.8F;
			yawDelta *= 0.5F;
			pitchDelta += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
			yawDelta += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

			if (random.nextInt(4) == 0) {
				continue;
			}

			if (!canCarveBranch(chunk.getPos(), x, z, step, branchCount, width)) {
				return;
			}

			carveRegion(
				context, config, chunk, posToBiome, aquiferSampler,
				x, y, z, hRadius, vRadius, mask,
				(ctx, relX, relY, relZ, blockY) ->
					isPositionExcluded(ctx, stretchFactors, relX, relY, relZ, blockY)
			);
		}
	}

	private float[] createHorizontalStretchFactors(
		CarverContext context,
		RavineCarverConfig config,
		Random random
	) {
		int height = context.getHeight();
		float[] factors = new float[height];
		float current = 1.0F;

		for (int i = 0; i < height; i++) {
			if (i == 0 || random.nextInt(config.shape.widthSmoothness) == 0) {
				current = 1.0F + random.nextFloat() * random.nextFloat();
			}

			factors[i] = current * current;
		}

		return factors;
	}

	private double getVerticalScale(
		RavineCarverConfig config,
		Random random,
		double pitch,
		float branchCount,
		float branchIndex
	) {
		float centerFactor = 1.0F - MathHelper.abs(0.5F - branchIndex / branchCount) * 2.0F;
		float scale = config.shape.verticalRadiusDefaultFactor
			+ config.shape.verticalRadiusCenterFactor * centerFactor;
		return scale * pitch * MathHelper.nextBetween(random, 0.75F, 1.0F);
	}

	private boolean isPositionExcluded(
		CarverContext context,
		float[] stretchFactors,
		double scaledRelativeX,
		double scaledRelativeY,
		double scaledRelativeZ,
		int y
	) {
		int relativeY = y - context.getMinY();
		return (scaledRelativeX * scaledRelativeX + scaledRelativeZ * scaledRelativeZ)
			* stretchFactors[relativeY - 1]
			+ scaledRelativeY * scaledRelativeY / 6.0
			>= 1.0;
	}
}
