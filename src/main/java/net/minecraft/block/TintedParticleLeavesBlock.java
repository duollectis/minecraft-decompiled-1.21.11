package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleUtil;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * {@code TintedParticleLeavesBlock}.
 */
public class TintedParticleLeavesBlock extends LeavesBlock {

	public static final MapCodec<TintedParticleLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Codecs.rangedInclusiveFloat(0.0F, 1.0F)
					                          .fieldOf("leaf_particle_chance")
					                          .forGetter(tintedParticleLeavesBlock -> tintedParticleLeavesBlock.leafParticleChance),
					                    createSettingsCodec()
			                    )
			                    .apply(instance, TintedParticleLeavesBlock::new)
	);

	public TintedParticleLeavesBlock(float f, AbstractBlock.Settings settings) {
		super(f, settings);
	}

	@Override
	protected void spawnLeafParticle(World world, BlockPos pos, Random random) {
		TintedParticleEffect
				tintedParticleEffect =
				TintedParticleEffect.create(ParticleTypes.TINTED_LEAVES, world.getBlockColor(pos));
		ParticleUtil.spawnParticle(world, pos, random, tintedParticleEffect);
	}

	@Override
	public MapCodec<? extends TintedParticleLeavesBlock> getCodec() {
		return CODEC;
	}
}
