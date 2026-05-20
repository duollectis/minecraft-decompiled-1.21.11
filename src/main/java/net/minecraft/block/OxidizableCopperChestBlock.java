package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class OxidizableCopperChestBlock extends CopperChestBlock implements Oxidizable {
   public static final MapCodec<OxidizableCopperChestBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Oxidizable.OxidationLevel.CODEC.fieldOf("weathering_state").forGetter(CopperChestBlock::getOxidationLevel),
            Registries.SOUND_EVENT.getCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenSound),
            Registries.SOUND_EVENT.getCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseSound),
            createSettingsCodec()
         )
         .apply(instance, OxidizableCopperChestBlock::new)
   );

   @Override
   public MapCodec<OxidizableCopperChestBlock> getCodec() {
      return CODEC;
   }

   public OxidizableCopperChestBlock(Oxidizable.OxidationLevel oxidationLevel, SoundEvent soundEvent, SoundEvent soundEvent2, AbstractBlock.Settings settings) {
      super(oxidationLevel, soundEvent, soundEvent2, settings);
   }

   @Override
   protected boolean hasRandomTicks(BlockState state) {
      return Oxidizable.getIncreasedOxidationBlock(state.getBlock()).isPresent();
   }

   @Override
   protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      if (!state.get(ChestBlock.CHEST_TYPE).equals(ChestType.RIGHT)
         && world.getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity
         && chestBlockEntity.getViewingUsers().isEmpty()) {
         this.tickDegradation(state, world, pos, random);
      }
   }

   public Oxidizable.OxidationLevel getDegradationLevel() {
      return this.getOxidationLevel();
   }

   @Override
   public boolean isWaxed() {
      return false;
   }
}
