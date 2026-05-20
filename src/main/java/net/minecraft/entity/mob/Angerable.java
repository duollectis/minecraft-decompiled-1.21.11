package net.minecraft.entity.mob;

import java.util.Optional;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

public interface Angerable {
   String ANGER_END_TIME_KEY = "anger_end_time";
   String ANGRY_AT_KEY = "angry_at";
   long NO_ANGER_END_TIME = -1L;

   long getAngerEndTime();

   default void setAngerDuration(long durationInTicks) {
      this.setAngerEndTime(this.getEntityWorld().getTime() + durationInTicks);
   }

   void setAngerEndTime(long angerEndTime);

   @Nullable LazyEntityReference<LivingEntity> getAngryAt();

   void setAngryAt(@Nullable LazyEntityReference<LivingEntity> angryAt);

   void chooseRandomAngerTime();

   World getEntityWorld();

   default void writeAngerToData(WriteView view) {
      view.putLong("anger_end_time", this.getAngerEndTime());
      view.putNullable("angry_at", LazyEntityReference.createCodec(), this.getAngryAt());
   }

   default void readAngerFromData(World world, ReadView view) {
      Optional<Long> optional = view.getOptionalLong("anger_end_time");
      if (optional.isPresent()) {
         this.setAngerEndTime(optional.get());
      } else {
         Optional<Integer> optional2 = view.getOptionalInt("AngerTime");
         if (optional2.isPresent()) {
            this.setAngerDuration(optional2.get().intValue());
         } else {
            this.setAngerEndTime(-1L);
         }
      }

      if (world instanceof ServerWorld) {
         this.setAngryAt(LazyEntityReference.fromData(view, "angry_at"));
         this.setTarget(LazyEntityReference.getLivingEntity(this.getAngryAt(), world));
      }
   }

   default void tickAngerLogic(ServerWorld world, boolean angerPersistent) {
      LivingEntity livingEntity = this.getTarget();
      LazyEntityReference<LivingEntity> lazyEntityReference = this.getAngryAt();
      if (livingEntity != null
         && livingEntity.isDead()
         && lazyEntityReference != null
         && lazyEntityReference.uuidEquals(livingEntity)
         && livingEntity instanceof MobEntity) {
         this.stopAnger();
      } else {
         if (livingEntity != null) {
            if (lazyEntityReference == null || !lazyEntityReference.uuidEquals(livingEntity)) {
               this.setAngryAt(LazyEntityReference.of(livingEntity));
            }

            this.chooseRandomAngerTime();
         }

         if (lazyEntityReference != null && !this.hasAngerTime() && (livingEntity == null || !canAngerAt(livingEntity) || !angerPersistent)) {
            this.stopAnger();
         }
      }
   }

   private static boolean canAngerAt(LivingEntity target) {
      return target instanceof PlayerEntity playerEntity && !playerEntity.isCreative() && !playerEntity.isSpectator();
   }

   default boolean shouldAngerAt(LivingEntity target, ServerWorld world) {
      if (!this.canTarget(target)) {
         return false;
      } else if (canAngerAt(target) && this.isUniversallyAngry(world)) {
         return true;
      } else {
         LazyEntityReference<LivingEntity> lazyEntityReference = this.getAngryAt();
         return lazyEntityReference != null && lazyEntityReference.uuidEquals(target);
      }
   }

   default boolean isUniversallyAngry(ServerWorld world) {
      return world.getGameRules().getValue(GameRules.UNIVERSAL_ANGER) && this.hasAngerTime() && this.getAngryAt() == null;
   }

   default boolean hasAngerTime() {
      long l = this.getAngerEndTime();
      if (l > 0L) {
         long m = l - this.getEntityWorld().getTime();
         return m > 0L;
      } else {
         return false;
      }
   }

   default void forgive(ServerWorld world, PlayerEntity player) {
      if (world.getGameRules().getValue(GameRules.FORGIVE_DEAD_PLAYERS)) {
         LazyEntityReference<LivingEntity> lazyEntityReference = this.getAngryAt();
         if (lazyEntityReference != null && lazyEntityReference.uuidEquals(player)) {
            this.stopAnger();
         }
      }
   }

   default void universallyAnger() {
      this.stopAnger();
      this.chooseRandomAngerTime();
   }

   default void stopAnger() {
      this.setAttacker(null);
      this.setAngryAt(null);
      this.setTarget(null);
      this.setAngerEndTime(-1L);
   }

   @Nullable LivingEntity getAttacker();

   void setAttacker(@Nullable LivingEntity attacker);

   void setTarget(@Nullable LivingEntity target);

   boolean canTarget(LivingEntity target);

   @Nullable LivingEntity getTarget();
}
