package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class OxidizableLightningRodBlock extends LightningRodBlock implements Oxidizable {
   public static final MapCodec<OxidizableLightningRodBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Oxidizable.OxidationLevel.CODEC.fieldOf("weathering_state").forGetter(OxidizableLightningRodBlock::getDegradationLevel), createSettingsCodec()
         )
         .apply(instance, OxidizableLightningRodBlock::new)
   );
   private final Oxidizable.OxidationLevel oxidationLevel;

   @Override
   public MapCodec<OxidizableLightningRodBlock> getCodec() {
      return CODEC;
   }

   public OxidizableLightningRodBlock(Oxidizable.OxidationLevel oxidationLevel, AbstractBlock.Settings settings) {
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
