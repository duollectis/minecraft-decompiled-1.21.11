package net.minecraft.block.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.OptionalInt;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LargeEntitySpawnHelper;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.GameEventTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

public class SculkShriekerBlockEntity extends BlockEntity implements GameEventListener.Holder<Vibrations.VibrationListener>, Vibrations {
   private static final int field_38750 = 10;
   private static final int WARDEN_SPAWN_TRIES = 20;
   private static final int WARDEN_SPAWN_HORIZONTAL_RANGE = 5;
   private static final int WARDEN_SPAWN_VERTICAL_RANGE = 6;
   private static final int DARKNESS_RANGE = 40;
   private static final int SHRIEK_DELAY = 90;
   private static final Int2ObjectMap<SoundEvent> WARNING_SOUNDS = Util.make(new Int2ObjectOpenHashMap(), warningSounds -> {
      warningSounds.put(1, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSE);
      warningSounds.put(2, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSER);
      warningSounds.put(3, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSEST);
      warningSounds.put(4, SoundEvents.ENTITY_WARDEN_LISTENING_ANGRY);
   });
   private static final int DEFAULT_WARNING_LEVEL = 0;
   private int warningLevel = 0;
   private final Vibrations.Callback vibrationCallback = new SculkShriekerBlockEntity.VibrationCallback();
   private Vibrations.ListenerData vibrationListenerData = new Vibrations.ListenerData();
   private final Vibrations.VibrationListener vibrationListener = new Vibrations.VibrationListener(this);

   public SculkShriekerBlockEntity(BlockPos pos, BlockState state) {
      super(BlockEntityType.SCULK_SHRIEKER, pos, state);
   }

   @Override
   public Vibrations.ListenerData getVibrationListenerData() {
      return this.vibrationListenerData;
   }

   @Override
   public Vibrations.Callback getVibrationCallback() {
      return this.vibrationCallback;
   }

   @Override
   protected void readData(ReadView view) {
      super.readData(view);
      this.warningLevel = view.getInt("warning_level", 0);
      this.vibrationListenerData = view.<Vibrations.ListenerData>read("listener", Vibrations.ListenerData.CODEC).orElseGet(Vibrations.ListenerData::new);
   }

   @Override
   protected void writeData(WriteView view) {
      super.writeData(view);
      view.putInt("warning_level", this.warningLevel);
      view.put("listener", Vibrations.ListenerData.CODEC, this.vibrationListenerData);
   }

   public static @Nullable ServerPlayerEntity findResponsiblePlayerFromEntity(@Nullable Entity entity) {
      if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
         return serverPlayerEntity;
      } else if (entity != null && entity.getControllingPassenger() instanceof ServerPlayerEntity serverPlayerEntity) {
         return serverPlayerEntity;
      } else if (entity instanceof ProjectileEntity projectileEntity && projectileEntity.getOwner() instanceof ServerPlayerEntity serverPlayerEntity2) {
         return serverPlayerEntity2;
      } else {
         return entity instanceof ItemEntity itemEntity && itemEntity.getOwner() instanceof ServerPlayerEntity serverPlayerEntity2 ? serverPlayerEntity2 : null;
      }
   }

   public void shriek(ServerWorld world, @Nullable ServerPlayerEntity player) {
      if (player != null) {
         BlockState blockState = this.getCachedState();
         if (!blockState.get(SculkShriekerBlock.SHRIEKING)) {
            this.warningLevel = 0;
            if (!this.canWarn(world) || this.trySyncWarningLevel(world, player)) {
               this.shriek(world, (Entity)player);
            }
         }
      }
   }

   private boolean trySyncWarningLevel(ServerWorld world, ServerPlayerEntity player) {
      OptionalInt optionalInt = SculkShriekerWarningManager.warnNearbyPlayers(world, this.getPos(), player);
      optionalInt.ifPresent(warningLevel -> this.warningLevel = warningLevel);
      return optionalInt.isPresent();
   }

   private void shriek(ServerWorld world, @Nullable Entity entity) {
      BlockPos blockPos = this.getPos();
      BlockState blockState = this.getCachedState();
      world.setBlockState(blockPos, blockState.with(SculkShriekerBlock.SHRIEKING, true), 2);
      world.scheduleBlockTick(blockPos, blockState.getBlock(), 90);
      world.syncWorldEvent(3007, blockPos, 0);
      world.emitGameEvent(GameEvent.SHRIEK, blockPos, GameEvent.Emitter.of(entity));
   }

   private boolean canWarn(ServerWorld world) {
      return this.getCachedState().get(SculkShriekerBlock.CAN_SUMMON)
         && world.getDifficulty() != Difficulty.PEACEFUL
         && world.getGameRules().getValue(GameRules.SPAWN_WARDENS);
   }

   @Override
   public void onBlockReplaced(BlockPos pos, BlockState oldState) {
      if (oldState.get(SculkShriekerBlock.SHRIEKING) && this.world instanceof ServerWorld serverWorld) {
         this.warn(serverWorld);
      }
   }

   public void warn(ServerWorld world) {
      if (this.canWarn(world) && this.warningLevel > 0) {
         if (!this.trySpawnWarden(world)) {
            this.playWarningSound(world);
         }

         WardenEntity.addDarknessToClosePlayers(world, Vec3d.ofCenter(this.getPos()), null, 40);
      }
   }

   private void playWarningSound(World world) {
      SoundEvent soundEvent = (SoundEvent)WARNING_SOUNDS.get(this.warningLevel);
      if (soundEvent != null) {
         BlockPos blockPos = this.getPos();
         int i = blockPos.getX() + MathHelper.nextBetween(world.random, -10, 10);
         int j = blockPos.getY() + MathHelper.nextBetween(world.random, -10, 10);
         int k = blockPos.getZ() + MathHelper.nextBetween(world.random, -10, 10);
         world.playSound(null, (double)i, (double)j, (double)k, soundEvent, SoundCategory.HOSTILE, 5.0F, 1.0F);
      }
   }

   private boolean trySpawnWarden(ServerWorld world) {
      return this.warningLevel < 4
         ? false
         : LargeEntitySpawnHelper.trySpawnAt(
               EntityType.WARDEN, SpawnReason.TRIGGERED, world, this.getPos(), 20, 5, 6, LargeEntitySpawnHelper.Requirements.WARDEN, false
            )
            .isPresent();
   }

   public Vibrations.VibrationListener getEventListener() {
      return this.vibrationListener;
   }

   class VibrationCallback implements Vibrations.Callback {
      private static final int RANGE = 8;
      private final PositionSource positionSource = new BlockPositionSource(SculkShriekerBlockEntity.this.pos);

      public VibrationCallback() {
      }

      @Override
      public int getRange() {
         return 8;
      }

      @Override
      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      @Override
      public TagKey<GameEvent> getTag() {
         return GameEventTags.SHRIEKER_CAN_LISTEN;
      }

      @Override
      public boolean accepts(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter) {
         return !SculkShriekerBlockEntity.this.getCachedState().get(SculkShriekerBlock.SHRIEKING)
            && SculkShriekerBlockEntity.findResponsiblePlayerFromEntity(emitter.sourceEntity()) != null;
      }

      @Override
      public void accept(
         ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity entity, float distance
      ) {
         SculkShriekerBlockEntity.this.shriek(world, SculkShriekerBlockEntity.findResponsiblePlayerFromEntity(entity != null ? entity : sourceEntity));
      }

      @Override
      public void onListen() {
         SculkShriekerBlockEntity.this.markDirty();
      }

      @Override
      public boolean requiresTickingChunksAround() {
         return true;
      }
   }
}
