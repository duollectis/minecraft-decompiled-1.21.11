package net.minecraft.world.gen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.AquiferSampler;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.function.Function;

/**
 * Карвер пещер Нижнего мира. Отличается от обычного карвера:
 * более широкими туннелями (ratio 5.0), меньшим количеством пещер (10),
 * а также тем, что ниже y=31 заполняет пространство лавой вместо воздуха.
 */
public class NetherCaveCarver extends CaveCarver {

	public NetherCaveCarver(Codec<CaveCarverConfig> codec) {
		super(codec);
		carvableFluids = ImmutableSet.of(Fluids.LAVA, Fluids.WATER);
	}

	@Override
	protected int getMaxCaveCount() {
		return 10;
	}

	@Override
	protected float getTunnelSystemWidth(Random random) {
		return (random.nextFloat() * 2.0F + random.nextFloat()) * 2.0F;
	}

	@Override
	protected double getTunnelSystemHeightWidthRatio() {
		return 5.0;
	}

	@Override
	protected boolean carveAtPoint(
		CarverContext context,
		CaveCarverConfig config,
		Chunk chunk,
		Function<BlockPos, RegistryEntry<Biome>> posToBiome,
		CarvingMask mask,
		BlockPos.Mutable pos,
		BlockPos.Mutable tmp,
		AquiferSampler aquiferSampler,
		MutableBoolean replacedGrassy
	) {
		if (!canAlwaysCarveBlock(config, chunk.getBlockState(pos))) {
			return false;
		}

		// Ниже y=31 в Нижнем мире пространство заполняется лавой
		BlockState fill = pos.getY() <= context.getMinY() + 31
			? LAVA.getBlockState()
			: CAVE_AIR;

		chunk.setBlockState(pos, fill);
		return true;
	}
}
