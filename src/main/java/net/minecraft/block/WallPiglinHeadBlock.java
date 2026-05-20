package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class WallPiglinHeadBlock extends WallSkullBlock {
   public static final MapCodec<WallPiglinHeadBlock> CODEC = createCodec(WallPiglinHeadBlock::new);
   private static final Map<Direction, VoxelShape> SHAPES = VoxelShapes.createHorizontalFacingShapeMap(Block.createCuboidZShape(10.0, 8.0, 8.0, 16.0));

   @Override
   public MapCodec<WallPiglinHeadBlock> getCodec() {
      return CODEC;
   }

   public WallPiglinHeadBlock(AbstractBlock.Settings settings) {
      super(SkullBlock.Type.PIGLIN, settings);
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPES.get(state.get(FACING));
   }
}
