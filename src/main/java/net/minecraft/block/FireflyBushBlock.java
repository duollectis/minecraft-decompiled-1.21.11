package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.attribute.EnvironmentAttributes;

public class FireflyBushBlock extends PlantBlock implements Fertilizable {
   private static final double FIREFLY_CHANCE = 0.7;
   private static final double FIREFLY_HORIZONTAL_RADIUS = 10.0;
   private static final double FIREFLY_VERTICAL_RADIUS = 5.0;
   private static final int LIGHT_LEVEL_THRESHOLD = 13;
   private static final int IDLE_SOUND_CHANCE = 30;
   public static final MapCodec<FireflyBushBlock> CODEC = createCodec(FireflyBushBlock::new);

   public FireflyBushBlock(AbstractBlock.Settings settings) {
      super(settings);
   }

   @Override
   protected MapCodec<? extends FireflyBushBlock> getCodec() {
      return CODEC;
   }

   @Override
   public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
      if (random.nextInt(30) == 0
         && world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.FIREFLY_BUSH_SOUNDS_AUDIO, pos)
         && world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos) <= pos.getY()) {
         world.playSoundAtBlockCenterClient(pos, SoundEvents.BLOCK_FIREFLY_BUSH_IDLE, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
      }

      if (world.getLightLevel(pos) <= 13 && random.nextDouble() <= 0.7) {
         double d = pos.getX() + random.nextDouble() * 10.0 - 5.0;
         double e = pos.getY() + random.nextDouble() * 5.0;
         double f = pos.getZ() + random.nextDouble() * 10.0 - 5.0;
         world.addParticleClient(ParticleTypes.FIREFLY, d, e, f, 0.0, 0.0, 0.0);
      }
   }

   @Override
   public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
      return Fertilizable.canSpread(world, pos, state);
   }

   @Override
   public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
      return true;
   }

   @Override
   public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
      Fertilizable.findPosToSpreadTo(world, pos, state).ifPresent(blockPos -> world.setBlockState(blockPos, this.getDefaultState()));
   }
}
