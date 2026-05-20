package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class PotatoesBlock extends CropBlock {
   public static final MapCodec<PotatoesBlock> CODEC = createCodec(PotatoesBlock::new);
   private static final VoxelShape[] SHAPES_BY_AGE = Block.createShapeArray(7, age -> Block.createColumnShape(16.0, 0.0, 2 + age));

   @Override
   public MapCodec<PotatoesBlock> getCodec() {
      return CODEC;
   }

   public PotatoesBlock(AbstractBlock.Settings settings) {
      super(settings);
   }

   @Override
   protected ItemConvertible getSeedsItem() {
      return Items.POTATO;
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPES_BY_AGE[this.getAge(state)];
   }
}
