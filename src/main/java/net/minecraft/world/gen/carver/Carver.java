package net.minecraft.world.gen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

/**
 * Базовый абстрактный класс для всех карверов (пещеры, овраги и т.д.).
 * Карвер вырезает пространство в чанке, формируя подземные полости.
 *
 * @param <C> тип конфигурации карвера
 */
public abstract class Carver<C extends CarverConfig> {

	public static final Carver<CaveCarverConfig> CAVE = register("cave", new CaveCarver(CaveCarverConfig.CAVE_CODEC));
	public static final Carver<CaveCarverConfig> NETHER_CAVE = register(
		"nether_cave",
		new NetherCaveCarver(CaveCarverConfig.CAVE_CODEC)
	);
	public static final Carver<RavineCarverConfig> RAVINE = register(
		"canyon",
		new RavineCarver(RavineCarverConfig.RAVINE_CODEC)
	);

	protected static final BlockState AIR = Blocks.AIR.getDefaultState();
	protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.getDefaultState();
	protected static final FluidState WATER = Fluids.WATER.getDefaultState();
	protected static final FluidState LAVA = Fluids.LAVA.getDefaultState();

	protected Set<Fluid> carvableFluids = ImmutableSet.of(Fluids.WATER);

	private final MapCodec<ConfiguredCarver<C>> codec;

	public Carver(Codec<C> configCodec) {
		this.codec = configCodec.fieldOf("config").xmap(this::configure, ConfiguredCarver::config);
	}

	public ConfiguredCarver<C> configure(C config) {
		return new ConfiguredCarver<>(this, config);
	}

	public MapCodec<ConfiguredCarver<C>> getCodec() {
		return codec;
	}

	public int getBranchFactor() {
		return 4;
	}

	/**
	 * Вырезает регион пещеры/оврага в заданном чанке в пределах эллипсоида,
	 * определённого центром (x, y, z) и полуосями (width, height).
	 *
	 * @return {@code true} если хотя бы один блок был изменён
	 */
	protected boolean carveRegion(
		CarverContext context,
		C config,
		Chunk chunk,
		Function<BlockPos, RegistryEntry<Biome>> posToBiome,
		AquiferSampler aquiferSampler,
		double x,
		double y,
		double z,
		double width,
		double height,
		CarvingMask mask,
		Carver.SkipPredicate skipPredicate
	) {
		ChunkPos chunkPos = chunk.getPos();
		double centerX = chunkPos.getCenterX();
		double centerZ = chunkPos.getCenterZ();
		double maxDist = 16.0 + width * 2.0;

		if (Math.abs(x - centerX) > maxDist || Math.abs(z - centerZ) > maxDist) {
			return false;
		}

		int startX = chunkPos.getStartX();
		int startZ = chunkPos.getStartZ();
		int minLocalX = Math.max(MathHelper.floor(x - width) - startX - 1, 0);
		int maxLocalX = Math.min(MathHelper.floor(x + width) - startX, 15);
		int minY = Math.max(MathHelper.floor(y - height) - 1, context.getMinY() + 1);
		int retrogrenOffset = chunk.hasBelowZeroRetrogen() ? 0 : 7;
		int maxY = Math.min(
			MathHelper.floor(y + height) + 1,
			context.getMinY() + context.getHeight() - 1 - retrogrenOffset
		);
		int minLocalZ = Math.max(MathHelper.floor(z - width) - startZ - 1, 0);
		int maxLocalZ = Math.min(MathHelper.floor(z + width) - startZ, 15);

		boolean carved = false;
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockPos.Mutable below = new BlockPos.Mutable();

		for (int localX = minLocalX; localX <= maxLocalX; localX++) {
			int worldX = chunkPos.getOffsetX(localX);
			double relX = (worldX + 0.5 - x) / width;

			for (int localZ = minLocalZ; localZ <= maxLocalZ; localZ++) {
				int worldZ = chunkPos.getOffsetZ(localZ);
				double relZ = (worldZ + 0.5 - z) / width;

				if (relX * relX + relZ * relZ >= 1.0) {
					continue;
				}

				MutableBoolean replacedGrassy = new MutableBoolean(false);

				for (int blockY = maxY; blockY > minY; blockY--) {
					double relY = (blockY - 0.5 - y) / height;

					if (skipPredicate.shouldSkip(context, relX, relY, relZ, blockY)) {
						continue;
					}

					if (mask.get(localX, blockY, localZ) && !isDebug(config)) {
						continue;
					}

					mask.set(localX, blockY, localZ);
					mutable.set(worldX, blockY, worldZ);
					carved |= carveAtPoint(
						context,
						config,
						chunk,
						posToBiome,
						mask,
						mutable,
						below,
						aquiferSampler,
						replacedGrassy
					);
				}
			}
		}

		return carved;
	}

	protected boolean carveAtPoint(
		CarverContext context,
		C config,
		Chunk chunk,
		Function<BlockPos, RegistryEntry<Biome>> posToBiome,
		CarvingMask mask,
		BlockPos.Mutable pos,
		BlockPos.Mutable tmp,
		AquiferSampler aquiferSampler,
		MutableBoolean replacedGrassy
	) {
		BlockState existing = chunk.getBlockState(pos);

		if (existing.isOf(Blocks.GRASS_BLOCK) || existing.isOf(Blocks.MYCELIUM)) {
			replacedGrassy.setTrue();
		}

		if (!canAlwaysCarveBlock(config, existing) && !isDebug(config)) {
			return false;
		}

		BlockState replacement = getState(context, config, pos, aquiferSampler);

		if (replacement == null) {
			return false;
		}

		chunk.setBlockState(pos, replacement);

		if (aquiferSampler.needsFluidTick() && !replacement.getFluidState().isEmpty()) {
			chunk.markBlockForPostProcessing(pos);
		}

		if (replacedGrassy.isTrue()) {
			tmp.set(pos, Direction.DOWN);

			if (chunk.getBlockState(tmp).isOf(Blocks.DIRT)) {
				context
					.applyMaterialRule(posToBiome, chunk, tmp, !replacement.getFluidState().isEmpty())
					.ifPresent(state -> {
						chunk.setBlockState(tmp, state);

						if (!state.getFluidState().isEmpty()) {
							chunk.markBlockForPostProcessing(tmp);
						}
					});
			}
		}

		return true;
	}

	private @Nullable BlockState getState(CarverContext context, C config, BlockPos pos, AquiferSampler sampler) {
		if (pos.getY() <= config.lavaLevel.getY(context)) {
			return LAVA.getBlockState();
		}

		BlockState aquiferState = sampler.apply(
			new DensityFunction.UnblendedNoisePos(pos.getX(), pos.getY(), pos.getZ()),
			0.0
		);

		if (aquiferState == null) {
			return isDebug(config) ? config.debugConfig.getBarrierState() : null;
		}

		return isDebug(config) ? getDebugState(config, aquiferState) : aquiferState;
	}

	private static BlockState getDebugState(CarverConfig config, BlockState state) {
		if (state.isOf(Blocks.AIR)) {
			return config.debugConfig.getAirState();
		}

		if (state.isOf(Blocks.WATER)) {
			BlockState waterDebug = config.debugConfig.getWaterState();
			return waterDebug.contains(Properties.WATERLOGGED)
				? waterDebug.with(Properties.WATERLOGGED, true)
				: waterDebug;
		}

		return state.isOf(Blocks.LAVA) ? config.debugConfig.getLavaState() : state;
	}

	public abstract boolean carve(
		CarverContext context,
		C config,
		Chunk chunk,
		Function<BlockPos, RegistryEntry<Biome>> posToBiome,
		Random random,
		AquiferSampler aquiferSampler,
		ChunkPos pos,
		CarvingMask mask
	);

	public abstract boolean shouldCarve(C config, Random random);

	protected boolean canAlwaysCarveBlock(C config, BlockState state) {
		return state.isIn(config.replaceable);
	}

	protected static boolean canCarveBranch(
		ChunkPos pos,
		double x,
		double z,
		int branchIndex,
		int branchCount,
		float baseWidth
	) {
		double centerX = pos.getCenterX();
		double centerZ = pos.getCenterZ();
		double dx = x - centerX;
		double dz = z - centerZ;
		double remaining = branchCount - branchIndex;
		double maxReach = baseWidth + 2.0F + 16.0F;
		return dx * dx + dz * dz - remaining * remaining <= maxReach * maxReach;
	}

	private static boolean isDebug(CarverConfig config) {
		return SharedConstants.CARVERS || config.debugConfig.isDebugMode();
	}

	private static <C extends CarverConfig, F extends Carver<C>> F register(String name, F carver) {
		return Registry.register(Registries.CARVER, name, carver);
	}

	/**
	 * Предикат, определяющий, нужно ли пропустить вырезание в данной точке.
	 */
	public interface SkipPredicate {

		boolean shouldSkip(
			CarverContext context,
			double scaledRelativeX,
			double scaledRelativeY,
			double scaledRelativeZ,
			int y
		);
	}
}
