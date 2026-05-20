package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public class WeepingVinesPlantBlock extends AbstractPlantBlock {
   public static final MapCodec<WeepingVinesPlantBlock> CODEC = createCodec(WeepingVinesPlantBlock::new);
   private static final VoxelShape SHAPE = Block.createColumnShape(14.0, 0.0, 16.0);

   @Override
   public MapCodec<WeepingVinesPlantBlock> getCodec() {
      return CODEC;
   }

   public WeepingVinesPlantBlock(AbstractBlock.Settings settings) {
      super(settings, Direction.DOWN, SHAPE, false);
   }

   @Override
   protected AbstractPlantStemBlock getStem() {
      return (AbstractPlantStemBlock)Blocks.WEEPING_VINES;
   }
}
