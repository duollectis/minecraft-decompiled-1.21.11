package net.minecraft.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Attackable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Targeter;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class InteractionEntity extends Entity implements Attackable, Targeter {
   private static final TrackedData<Float> WIDTH = DataTracker.registerData(InteractionEntity.class, TrackedDataHandlerRegistry.FLOAT);
   private static final TrackedData<Float> HEIGHT = DataTracker.registerData(InteractionEntity.class, TrackedDataHandlerRegistry.FLOAT);
   private static final TrackedData<Boolean> RESPONSE = DataTracker.registerData(InteractionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
   private static final String WIDTH_KEY = "width";
   private static final String HEIGHT_KEY = "height";
   private static final String ATTACK_KEY = "attack";
   private static final String INTERACTION_KEY = "interaction";
   private static final String RESPONSE_KEY = "response";
   private static final float DEFAULT_WIDTH = 1.0F;
   private static final float DEFAULT_HEIGHT = 1.0F;
   private static final boolean DEFAULT_RESPONSE = false;
   private InteractionEntity.@Nullable Interaction attack;
   private InteractionEntity.@Nullable Interaction interaction;

   public InteractionEntity(EntityType<?> entityType, World world) {
      super(entityType, world);
      this.noClip = true;
   }

   @Override
   protected void initDataTracker(DataTracker.Builder builder) {
      builder.add(WIDTH, 1.0F);
      builder.add(HEIGHT, 1.0F);
      builder.add(RESPONSE, false);
   }

   @Override
   protected void readCustomData(ReadView view) {
      this.setInteractionWidth(view.getFloat("width", 1.0F));
      this.setInteractionHeight(view.getFloat("height", 1.0F));
      this.attack = view.<InteractionEntity.Interaction>read("attack", InteractionEntity.Interaction.CODEC).orElse(null);
      this.interaction = view.<InteractionEntity.Interaction>read("interaction", InteractionEntity.Interaction.CODEC).orElse(null);
      this.setResponse(view.getBoolean("response", false));
      this.setBoundingBox(this.calculateBoundingBox());
   }

   @Override
   protected void writeCustomData(WriteView view) {
      view.putFloat("width", this.getInteractionWidth());
      view.putFloat("height", this.getInteractionHeight());
      view.putNullable("attack", InteractionEntity.Interaction.CODEC, this.attack);
      view.putNullable("interaction", InteractionEntity.Interaction.CODEC, this.interaction);
      view.putBoolean("response", this.shouldRespond());
   }

   @Override
   public void onTrackedDataSet(TrackedData<?> data) {
      super.onTrackedDataSet(data);
      if (HEIGHT.equals(data) || WIDTH.equals(data)) {
         this.calculateDimensions();
      }
   }

   @Override
   public boolean canBeHitByProjectile() {
      return false;
   }

   @Override
   public boolean canHit() {
      return true;
   }

   @Override
   public PistonBehavior getPistonBehavior() {
      return PistonBehavior.IGNORE;
   }

   @Override
   public boolean canAvoidTraps() {
      return true;
   }

   @Override
   public boolean handleAttack(Entity attacker) {
      if (attacker instanceof PlayerEntity playerEntity) {
         this.attack = new InteractionEntity.Interaction(playerEntity.getUuid(), this.getEntityWorld().getTime());
         if (playerEntity instanceof ServerPlayerEntity serverPlayerEntity) {
            Criteria.PLAYER_HURT_ENTITY.trigger(serverPlayerEntity, this, playerEntity.getDamageSources().generic(), 1.0F, 1.0F, false);
         }

         return !this.shouldRespond();
      } else {
         return false;
      }
   }

   @Override
   public final boolean damage(ServerWorld world, DamageSource source, float amount) {
      return false;
   }

   @Override
   public ActionResult interact(PlayerEntity player, Hand hand) {
      if (this.getEntityWorld().isClient()) {
         return this.shouldRespond() ? ActionResult.SUCCESS : ActionResult.CONSUME;
      } else {
         this.interaction = new InteractionEntity.Interaction(player.getUuid(), this.getEntityWorld().getTime());
         return ActionResult.CONSUME;
      }
   }

   @Override
   public void tick() {
   }

   @Override
   public @Nullable LivingEntity getLastAttacker() {
      return this.attack != null ? this.getEntityWorld().getPlayerByUuid(this.attack.player()) : null;
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.interaction != null ? this.getEntityWorld().getPlayerByUuid(this.interaction.player()) : null;
   }

   public final void setInteractionWidth(float width) {
      this.dataTracker.set(WIDTH, width);
   }

   public final float getInteractionWidth() {
      return this.dataTracker.get(WIDTH);
   }

   public final void setInteractionHeight(float height) {
      this.dataTracker.set(HEIGHT, height);
   }

   public final float getInteractionHeight() {
      return this.dataTracker.get(HEIGHT);
   }

   public final void setResponse(boolean response) {
      this.dataTracker.set(RESPONSE, response);
   }

   public final boolean shouldRespond() {
      return this.dataTracker.get(RESPONSE);
   }

   private EntityDimensions getDimensions() {
      return EntityDimensions.changing(this.getInteractionWidth(), this.getInteractionHeight());
   }

   @Override
   public EntityDimensions getDimensions(EntityPose pose) {
      return this.getDimensions();
   }

   @Override
   protected Box calculateDefaultBoundingBox(Vec3d pos) {
      return this.getDimensions().getBoxAt(pos);
   }

   record Interaction(UUID player, long timestamp) {
      public static final Codec<InteractionEntity.Interaction> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Uuids.INT_STREAM_CODEC.fieldOf("player").forGetter(InteractionEntity.Interaction::player),
               Codec.LONG.fieldOf("timestamp").forGetter(InteractionEntity.Interaction::timestamp)
            )
            .apply(instance, InteractionEntity.Interaction::new)
      );
   }
}
