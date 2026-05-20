package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class AttachedToLogsTreeDecorator extends TreeDecorator {
   public static final MapCodec<AttachedToLogsTreeDecorator> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(treeDecorator -> treeDecorator.probability),
            BlockStateProvider.TYPE_CODEC.fieldOf("block_provider").forGetter(treeDecorator -> treeDecorator.blockProvider),
            Codecs.nonEmptyList(Direction.CODEC.listOf()).fieldOf("directions").forGetter(treeDecorator -> treeDecorator.directions)
         )
         .apply(instance, AttachedToLogsTreeDecorator::new)
   );
   private final float probability;
   private final BlockStateProvider blockProvider;
   private final List<Direction> directions;

   public AttachedToLogsTreeDecorator(float probability, BlockStateProvider blockProvider, List<Direction> directions) {
      this.probability = probability;
      this.blockProvider = blockProvider;
      this.directions = directions;
   }

   @Override
   public void generate(TreeDecorator.Generator generator) {
      Random random = generator.getRandom();

      for (BlockPos blockPos : Util.copyShuffled(generator.getLogPositions(), random)) {
         Direction direction = Util.getRandom(this.directions, random);
         BlockPos blockPos2 = blockPos.offset(direction);
         if (random.nextFloat() <= this.probability && generator.isAir(blockPos2)) {
            generator.replace(blockPos2, this.blockProvider.get(random, blockPos2));
         }
      }
   }

   @Override
   protected TreeDecoratorType<?> getType() {
      return TreeDecoratorType.ATTACHED_TO_LOGS;
   }
}
