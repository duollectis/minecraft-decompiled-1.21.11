package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class OxidizableChainBlock extends ChainBlock implements Oxidizable {
   public static final MapCodec<OxidizableChainBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Oxidizable.OxidationLevel.CODEC.fieldOf("weathering_state").forGetter(OxidizableChainBlock::getDegradationLevel), createSettingsCodec()
         )
         .apply(instance, OxidizableChainBlock::new)
   );
   private final Oxidizable.OxidationLevel oxidationLevel;

   @Override
   public MapCodec<OxidizableChainBlock> getCodec() {
      return CODEC;
   }

   public OxidizableChainBlock(Oxidizable.OxidationLevel oxidationLevel, AbstractBlock.Settings settings) {
      super(settings);
      this.oxidationLevel = oxidationLevel;
   }

   @Override
   protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      this.tickDegradation(state, world, pos, random);
   }

   @Override
   protected boolean hasRandomTicks(BlockState state) {
      return Oxidizable.getIncreasedOxidationBlock(state.getBlock()).isPresent();
   }

   public Oxidizable.OxidationLevel getDegradationLevel() {
      return this.oxidationLevel;
   }
}
