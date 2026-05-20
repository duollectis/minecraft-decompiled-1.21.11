package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleUtil;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class UntintedParticleLeavesBlock extends LeavesBlock {
   public static final MapCodec<UntintedParticleLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Codecs.rangedInclusiveFloat(0.0F, 1.0F)
               .fieldOf("leaf_particle_chance")
               .forGetter(untintedParticleLeavesBlock -> untintedParticleLeavesBlock.leafParticleChance),
            ParticleTypes.TYPE_CODEC.fieldOf("leaf_particle").forGetter(untintedParticleLeavesBlock -> untintedParticleLeavesBlock.leafParticleEffect),
            createSettingsCodec()
         )
         .apply(instance, UntintedParticleLeavesBlock::new)
   );
   protected final ParticleEffect leafParticleEffect;

   public UntintedParticleLeavesBlock(float leafParticleChance, ParticleEffect leafParticleEffect, AbstractBlock.Settings settings) {
      super(leafParticleChance, settings);
      this.leafParticleEffect = leafParticleEffect;
   }

   @Override
   protected void spawnLeafParticle(World world, BlockPos pos, Random random) {
      ParticleUtil.spawnParticle(world, pos, random, this.leafParticleEffect);
   }

   @Override
   public MapCodec<UntintedParticleLeavesBlock> getCodec() {
      return CODEC;
   }
}
