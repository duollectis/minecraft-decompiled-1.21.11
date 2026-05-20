package net.minecraft.entity;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PlayerLikeEntity extends LivingEntity {
   public static final Arm MAIN_ARM = Arm.RIGHT;
   public static final int MODEL_CUSTOMIZATION = 0;
   public static final float EYE_HEIGHT = 1.62F;
   public static final Vec3d VEHICLE_ATTACHMENT = new Vec3d(0.0, 0.6, 0.0);
   private static final float CROUCH_BOX_HEIGHT = 1.5F;
   private static final float SWIMMING_BOX_WIDTH = 0.6F;
   public static final float SWIMMING_BOX_HEIGHT = 0.6F;
   protected static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.changing(0.6F, 1.8F)
      .withEyeHeight(1.62F)
      .withAttachments(EntityAttachments.builder().add(EntityAttachmentType.VEHICLE, VEHICLE_ATTACHMENT));
   @SuppressWarnings("unchecked")
   protected static final Map<EntityPose, EntityDimensions> POSE_DIMENSIONS = (Map<EntityPose, EntityDimensions>) (Map<?, ?>) ImmutableMap.builder()
      .put(EntityPose.STANDING, STANDING_DIMENSIONS)
      .put(EntityPose.SLEEPING, SLEEPING_DIMENSIONS)
      .put(EntityPose.GLIDING, EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
      .put(EntityPose.SWIMMING, EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
      .put(EntityPose.SPIN_ATTACK, EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F))
      .put(
         EntityPose.CROUCHING,
         EntityDimensions.changing(0.6F, 1.5F)
            .withEyeHeight(1.27F)
            .withAttachments(EntityAttachments.builder().add(EntityAttachmentType.VEHICLE, VEHICLE_ATTACHMENT))
      )
      .put(EntityPose.DYING, EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(1.62F))
      .build();
   protected static final TrackedData<Arm> MAIN_ARM_ID = DataTracker.registerData(PlayerLikeEntity.class, TrackedDataHandlerRegistry.ARM);
   protected static final TrackedData<Byte> PLAYER_MODE_CUSTOMIZATION_ID = DataTracker.registerData(PlayerLikeEntity.class, TrackedDataHandlerRegistry.BYTE);

   protected PlayerLikeEntity(EntityType<? extends LivingEntity> entityType, World world) {
      super(entityType, world);
   }

   @Override
   protected void initDataTracker(DataTracker.Builder builder) {
      super.initDataTracker(builder);
      builder.add(MAIN_ARM_ID, MAIN_ARM);
      builder.add(PLAYER_MODE_CUSTOMIZATION_ID, (byte)0);
   }

   @Override
   public Arm getMainArm() {
      return this.dataTracker.get(MAIN_ARM_ID);
   }

   public void setMainArm(Arm arm) {
      this.dataTracker.set(MAIN_ARM_ID, arm);
   }

   public boolean isModelPartVisible(PlayerModelPart part) {
      return (this.getDataTracker().get(PLAYER_MODE_CUSTOMIZATION_ID) & part.getBitFlag()) == part.getBitFlag();
   }

   @Override
   public EntityDimensions getBaseDimensions(EntityPose pose) {
      return POSE_DIMENSIONS.getOrDefault(pose, STANDING_DIMENSIONS);
   }
}
