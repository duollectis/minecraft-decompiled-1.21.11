package net.minecraft.client.render.entity.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class CopperGolemAnimations {
   public static final AnimationDefinition WALKING_WITHOUT_ITEM = AnimationDefinition.Builder.create(0.8333F)
      .looping()
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(10.0F, 15.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(10.0F, -1.87F, -10.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(10.0F, -15.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.625F, AnimationHelper.createRotationalVector(10.0F, -0.82F, 10.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(10.0F, 15.0F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-10.0F, 1.87F, 10.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.625F, AnimationHelper.createRotationalVector(-10.0F, 0.82F, -10.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(70.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-80.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(70.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(-80.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(70.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-80.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(-60.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(60.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-60.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(60.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-60.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(60.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .build();
   public static final AnimationDefinition SPIN_HEAD = AnimationDefinition.Builder.create(3.5F)
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, -35.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5F, AnimationHelper.createRotationalVector(0.0F, -35.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.625F, AnimationHelper.createRotationalVector(0.0F, 35.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(0.0F, 35.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createRotationalVector(0.0F, 35.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.625F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createRotationalVector(0.0F, 300.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(0.0F, 300.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.75F, AnimationHelper.createRotationalVector(-25.0F, 300.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createRotationalVector(-25.0F, 300.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 360.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.5F, AnimationHelper.createRotationalVector(0.0F, 360.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .build();
   public static final AnimationDefinition WALKING_WITH_ITEM = AnimationDefinition.Builder.create(0.8333F)
      .looping()
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(10.0F, 7.5F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(10.0F, -1.87F, -5.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(10.0F, -7.5F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.625F, AnimationHelper.createRotationalVector(10.0F, -0.82F, 5.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(10.0F, 7.5F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-10.0F, 1.87F, 10.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.625F, AnimationHelper.createRotationalVector(-10.0F, 0.82F, -10.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(-59.78638F, -6.49053F, -3.76613F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(-59.78638F, 6.49053F, 3.76613F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(-0.21129F, -0.0212F, -0.07004F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(-30.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(30.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-30.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(30.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-30.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(30.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
         )
      )
      .build();
   public static final AnimationDefinition DROPPING_ITEM = AnimationDefinition.Builder.create(3.0F)
      .looping()
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(18.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(24.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.9167F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.125F, AnimationHelper.createRotationalVector(12.40525F, -4.4E-4F, 0.00829F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(12.40525F, -4.4E-4F, 0.00829F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(13.92716F, 26.80536F, 6.38918F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(21.43F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7917F, AnimationHelper.createRotationalVector(21.43F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createRotationalVector(12.40725F, 0.0F, 0.00783F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.375F, AnimationHelper.createRotationalVector(-7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4167F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createRotationalVector(15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createRotationalVector(17.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createRotationalVector(22.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createTranslationalVector(0.0F, 0.4F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.01805F, 0.88303F, -0.09783F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7917F, AnimationHelper.createTranslationalVector(-0.01805F, 0.88303F, -0.09783F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.375F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -1.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -1.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-20.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-20.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.6667F, AnimationHelper.createRotationalVector(0.0F, -20.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(0.0F, -20.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(0.0F, 27.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createRotationalVector(0.0F, -2.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createRotationalVector(0.0F, -2.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(9.73588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7083F, AnimationHelper.createRotationalVector(9.73588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7917F, AnimationHelper.createRotationalVector(10.15255F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(17.86088F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.875F, AnimationHelper.createRotationalVector(17.23588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.9167F, AnimationHelper.createRotationalVector(17.23588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(17.23588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createRotationalVector(-0.26F, -1.93F, -3.73F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(-15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createRotationalVector(-15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(0.0F, -0.15451F, 0.47553F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.875F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.9167F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createTranslationalVector(-0.39F, 0.52F, -2.21F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-7.38733F, 1.29876F, 9.91615F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-7.38733F, 1.29876F, 9.91615F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 32.5F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-34.55418F, 11.73507F, 36.8361F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4583F, AnimationHelper.createRotationalVector(-117.82767F, 2.94538F, 0.22703F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5417F, AnimationHelper.createRotationalVector(-97.7902F, 0.73403F, 1.39387F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(-92.79F, 0.73F, 1.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(-92.79F, 0.73F, 1.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-95.83405F, 33.18639F, -0.40081F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createRotationalVector(-95.83F, 33.19F, -0.4F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createRotationalVector(-44.60123F, 10.14454F, 8.66307F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(-4.31506F, 6.54961F, 13.21388F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createRotationalVector(-4.31506F, 6.54961F, 13.21388F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createRotationalVector(-4.31506F, 6.54961F, 13.21388F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2083F, AnimationHelper.createRotationalVector(-113.7629F, 21.38835F, 15.48184F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(-113.76F, 21.39F, 15.48F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(-39.99304F, 7.3511F, 14.05666F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createRotationalVector(68.07913F, -3.61348F, 1.39182F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createRotationalVector(193.16708F, -1.90441F, -0.43495F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createRotationalVector(250.66708F, -1.90441F, -0.43495F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7917F, AnimationHelper.createRotationalVector(264.19006F, -4.41519F, -0.66792F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.8333F, AnimationHelper.createRotationalVector(270.53693F, 16.03493F, 0.47968F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createRotationalVector(319.31668F, 17.97846F, 1.34328F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createRotationalVector(0.0F, -5.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createTranslationalVector(0.25358F, -0.20153F, 2.21248F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createTranslationalVector(0.25F, -0.2F, 2.21F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5417F, AnimationHelper.createTranslationalVector(-0.79739F, -0.10573F, 1.70592F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createTranslationalVector(-0.51052F, -0.38088F, 0.79745F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createTranslationalVector(-0.51052F, -0.38088F, 0.79745F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createTranslationalVector(-0.51052F, -0.38088F, 0.79745F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(-0.51F, -0.38F, 0.8F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(25.0F, 0.0F, -37.5F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createRotationalVector(-21.59341F, -12.60837F, -45.69252F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-120.7755F, -5.21988F, -2.02064F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(-98.27419F, -1.79323F, -1.15048F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-93.27F, -1.79F, -1.15F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5417F, AnimationHelper.createRotationalVector(-93.27419F, -1.79323F, -1.15048F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.7083F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(-95.75F, -2.42F, 5.97F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.25F, AnimationHelper.createRotationalVector(-98.4029F, -17.39503F, 6.85104F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(-101.24523F, -29.87096F, 7.69993F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createRotationalVector(-101.25F, -29.87F, 7.7F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(-88.58526F, -17.10045F, 11.7676F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(-88.59F, -17.1F, 11.77F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(-46.59531F, -16.13694F, -3.85578F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.00677F, -0.76064F, 3.19059F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(0.0512F, -0.76176F, 3.12882F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.375F, AnimationHelper.createRotationalVector(2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5833F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.375F, AnimationHelper.createTranslationalVector(0.0F, 0.0909F, -0.10834F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.09F, -0.11F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createTranslationalVector(0.0F, 0.09F, -0.11F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.375F, AnimationHelper.createRotationalVector(2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5833F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.375F, AnimationHelper.createTranslationalVector(0.0F, 0.0909F, -0.10834F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.09F, -0.11F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createTranslationalVector(0.0F, 0.09F, -0.11F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .build();
   public static final AnimationDefinition DROPPING_NO_ITEM = AnimationDefinition.Builder.create(3.0F)
      .looping()
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(18.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(24.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.9167F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.125F, AnimationHelper.createRotationalVector(12.40525F, -4.4E-4F, 0.00829F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(12.40525F, -4.4E-4F, 0.00829F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(13.92716F, 26.80536F, 6.38918F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(21.43F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7917F, AnimationHelper.createRotationalVector(21.43F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(12.40725F, 0.0F, 0.00783F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createTranslationalVector(0.0F, 0.4F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.01805F, 0.88303F, -0.09783F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7917F, AnimationHelper.createTranslationalVector(-0.01805F, 0.88303F, -0.09783F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-20.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-20.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.6667F, AnimationHelper.createRotationalVector(0.0F, -20.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(0.0F, -20.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(0.0F, 27.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createRotationalVector(0.0F, -2.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createRotationalVector(0.0F, -2.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(9.73588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7083F, AnimationHelper.createRotationalVector(9.73588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7917F, AnimationHelper.createRotationalVector(10.15255F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(17.86088F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.875F, AnimationHelper.createRotationalVector(17.23588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.9167F, AnimationHelper.createRotationalVector(17.23588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(17.23588F, -1.93433F, -3.73384F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createRotationalVector(-0.26F, -1.93F, -3.73F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(1.25F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, -360.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(0.0F, -0.15451F, 0.47553F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.875F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.9167F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createTranslationalVector(-0.39F, 0.52F, -2.21F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.5F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createTranslationalVector(0.0F, -0.01091F, -0.02988F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.0F, -0.01F, -0.03F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createTranslationalVector(0.0F, -0.01F, -0.03F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, -0.01F, -0.03F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-7.38733F, 1.29876F, 9.91615F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-7.38733F, 1.29876F, 9.91615F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 32.5F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-34.55418F, 11.73507F, 36.8361F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4583F, AnimationHelper.createRotationalVector(-117.82767F, 2.94538F, 0.22703F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5417F, AnimationHelper.createRotationalVector(-97.7902F, 0.73403F, 1.39387F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(-92.79F, 0.73F, 1.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(-92.79F, 0.73F, 1.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-95.83405F, 33.18639F, -0.40081F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createRotationalVector(-95.83F, 33.19F, -0.4F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createRotationalVector(-44.60123F, 10.14454F, 8.66307F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(-4.31506F, 6.54961F, 13.21388F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createRotationalVector(-4.31506F, 6.54961F, 13.21388F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(-4.31506F, 6.54961F, 13.21388F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(-6.53898F, 13.96898F, 14.34786F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(3.50393F, -4.70737F, 8.3608F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(3.50393F, -4.70737F, 8.3608F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(3.90089F, -4.3843F, 3.35549F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(3.9F, -4.38F, 3.36F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createRotationalVector(3.9F, -4.38F, 3.36F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(3.90089F, -4.3843F, 3.35549F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createTranslationalVector(0.25358F, -0.20153F, 2.21248F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createTranslationalVector(0.25F, -0.2F, 2.21F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5417F, AnimationHelper.createTranslationalVector(-0.79739F, -0.10573F, 1.70592F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createTranslationalVector(-0.51052F, -0.38088F, 0.79745F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createTranslationalVector(-0.51052F, -0.38088F, 0.79745F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(-0.51052F, -0.38088F, 0.79745F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(-0.46F, -0.34F, 0.72F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(-0.46F, 0.1159F, -0.30086F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(-0.46F, 0.1159F, -0.30086F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createTranslationalVector(-0.46F, 0.1159F, -0.30086F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createTranslationalVector(-0.46F, -0.88F, -0.3F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createTranslationalVector(-0.46F, -0.88F, -0.3F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(-0.46F, 0.1159F, -0.30086F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(25.0F, 0.0F, -37.5F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createRotationalVector(-21.59341F, -12.60837F, -45.69252F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-120.7755F, -5.21988F, -2.02064F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(-98.27419F, -1.79323F, -1.15048F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-93.27F, -1.79F, -1.15F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5417F, AnimationHelper.createRotationalVector(-93.27419F, -1.79323F, -1.15048F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.7083F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(-95.75F, -2.42F, 5.97F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.25F, AnimationHelper.createRotationalVector(-98.4029F, -17.39503F, 6.85104F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(-101.24523F, -29.87096F, 7.69993F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createRotationalVector(-101.25F, -29.87F, 7.7F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -20.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(2.47864F, -0.32621F, -12.50706F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(2.47864F, -0.32621F, -12.50706F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(2.41492F, -0.64686F, -5.01363F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(2.41F, -0.65F, -5.01F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createRotationalVector(2.41F, -0.65F, -5.01F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(2.41492F, -0.64686F, -5.01363F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.00677F, -0.76064F, 3.19059F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createTranslationalVector(-0.00677F, -0.76064F, 3.19059F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(-0.00677F, -0.76064F, 3.19059F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(0.03F, -0.76F, 0.45F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.03F, -0.28229F, -0.07133F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(0.03F, -0.28229F, -0.07133F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createTranslationalVector(0.03F, -0.28229F, -0.07133F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createTranslationalVector(0.03F, -1.28F, -0.07F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createTranslationalVector(0.03F, -1.28F, -0.07F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.03F, -0.28229F, -0.07133F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.SCALE,
            new Keyframe(0.0F, AnimationHelper.createScalingVector(1.0, 1.0, 1.0), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createScalingVector(1.0, 1.0, 1.0), Transformation.Interpolations.LINEAR)
         )
      )
      .build();
   public static final AnimationDefinition GETTING_ITEM = AnimationDefinition.Builder.create(3.0F)
      .looping()
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(18.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(24.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.9167F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.125F, AnimationHelper.createRotationalVector(12.40525F, -4.4E-4F, 0.00829F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(12.40525F, -4.4E-4F, 0.00829F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(13.92716F, 26.80536F, 6.38918F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(21.43F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7917F, AnimationHelper.createRotationalVector(21.43F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createRotationalVector(12.40725F, 0.0F, 0.00783F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.375F, AnimationHelper.createRotationalVector(15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4167F, AnimationHelper.createRotationalVector(17.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(22.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createRotationalVector(24.14867F, -20.70481F, -9.00717F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createRotationalVector(24.14867F, -20.70481F, -9.00717F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.75F, AnimationHelper.createRotationalVector(22.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createTranslationalVector(0.0F, 0.4F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.01805F, 0.88303F, -0.09783F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7917F, AnimationHelper.createTranslationalVector(-0.01805F, 0.88303F, -0.09783F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createTranslationalVector(0.0F, 0.46194F, -0.19134F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.46194F, -0.19134F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-20.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-20.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.6667F, AnimationHelper.createRotationalVector(0.0F, -20.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(0.0F, -20.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(0.0F, 27.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createRotationalVector(0.0F, -2.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(10.16381F, -16.71134F, -6.35306F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(10.16381F, -16.71134F, -6.35306F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.9167F, AnimationHelper.createRotationalVector(10.16381F, -16.71134F, -6.35306F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.9583F, AnimationHelper.createRotationalVector(10.16381F, -16.71134F, -6.35306F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0F, AnimationHelper.createRotationalVector(5.16381F, -16.71134F, -6.35306F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(0.16381F, -16.71134F, -6.35306F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createRotationalVector(0.15732F, -4.21139F, -6.31751F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createRotationalVector(0.07901F, 5.3943F, -3.15187F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(0.0F, 7.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(4.53867F, 7.47675F, 0.59181F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(-2.53852F, 9.99038F, -0.44067F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4167F, AnimationHelper.createRotationalVector(-12.68664F, 9.76061F, -2.18558F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createRotationalVector(-15.19938F, 22.36971F, -3.52259F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createRotationalVector(-3.02173F, 22.37156F, -2.41802F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createRotationalVector(-0.52173F, 22.37156F, -2.41802F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.75F, AnimationHelper.createRotationalVector(-12.40598F, -0.4674F, -1.79838F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.8333F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(0.0F, -0.15451F, 0.47553F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.9167F, AnimationHelper.createTranslationalVector(-0.22438F, 0.82319F, -1.27252F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.9583F, AnimationHelper.createTranslationalVector(-0.52521F, 0.96725F, -0.32978F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0F, AnimationHelper.createTranslationalVector(-0.52521F, 0.96725F, -0.32978F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(-0.5345F, 1.16541F, -0.37206F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createTranslationalVector(-0.5345F, 1.16541F, -0.37206F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.8333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-7.38733F, 1.29876F, 9.91615F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-7.38733F, 1.29876F, 9.91615F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 32.5F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-34.55418F, 11.73507F, 36.8361F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4583F, AnimationHelper.createRotationalVector(-82.47403F, 17.82361F, 2.17224F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5F, AnimationHelper.createRotationalVector(-85.08388F, 14.26971F, 1.99595F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5417F, AnimationHelper.createRotationalVector(-85.16266F, 13.19102F, 2.43976F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(-92.79F, 0.73F, 1.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(-92.79F, 0.73F, 1.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-95.83405F, 33.18639F, -0.40081F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.25F, AnimationHelper.createRotationalVector(-95.83F, 33.19F, -0.4F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(-98.33F, 33.19F, -0.4F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5417F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(-84.12204F, 8.95753F, 14.11779F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2083F, AnimationHelper.createRotationalVector(-84.12204F, 8.95753F, 14.11779F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(-93.6065F, 13.90544F, 15.98524F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(-124.48661F, 66.29146F, -7.28605F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.375F, AnimationHelper.createRotationalVector(-129.4866F, 66.29146F, -7.28605F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4167F, AnimationHelper.createRotationalVector(-108.91607F, 1.79762F, 20.93924F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(-102.18303F, 4.35881F, 17.40962F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(-98.33642F, -0.70114F, 4.09322F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createRotationalVector(-98.39385F, 6.71929F, 3.00137F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createRotationalVector(-98.33981F, 1.77244F, 3.7307F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createRotationalVector(-100.70987F, 3.48829F, 7.1138F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.75F, AnimationHelper.createRotationalVector(-97.95F, 6.92F, 13.88F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7917F, AnimationHelper.createRotationalVector(-87.95F, 6.92F, 13.88F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.8333F, AnimationHelper.createRotationalVector(-97.95F, 6.92F, 13.88F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createRotationalVector(-102.95F, 6.92F, 13.88F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createRotationalVector(-76.475F, 3.46F, 6.94F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(-26.475F, 3.46F, 6.94F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createTranslationalVector(0.25358F, -0.20153F, 2.21248F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.25F, AnimationHelper.createTranslationalVector(0.25F, -0.2F, 2.21F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createTranslationalVector(0.25F, -0.2F, 2.21F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5417F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(-0.51F, -0.38F, 0.8F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(-0.51F, -0.38F, 0.8F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.375F, AnimationHelper.createTranslationalVector(-0.51F, -0.38F, 0.8F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4167F, AnimationHelper.createTranslationalVector(-2.14094F, 0.69619F, 1.23422F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(-0.97932F, 0.38244F, 0.12884F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createTranslationalVector(-1.55232F, 1.79904F, 0.37956F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createTranslationalVector(-1.53125F, 1.64598F, 1.41168F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createTranslationalVector(-1.57256F, 1.05375F, 1.32469F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.75F, AnimationHelper.createTranslationalVector(-1.33F, 0.16F, 1.02F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7917F, AnimationHelper.createTranslationalVector(-1.33F, 0.16F, 1.02F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.8333F, AnimationHelper.createTranslationalVector(-1.33F, 0.16F, 1.02F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createTranslationalVector(-1.33F, 0.16F, 1.02F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createTranslationalVector(-0.5748F, 0.38848F, 1.45646F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(-0.67F, 0.08F, 0.51F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(25.0F, 0.0F, -37.5F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createRotationalVector(-21.59341F, -12.60837F, -45.69252F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-120.7755F, -5.21988F, -2.02064F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(-98.27419F, -1.79323F, -1.15048F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-93.27F, -1.79F, -1.15F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5417F, AnimationHelper.createRotationalVector(-93.27419F, -1.79323F, -1.15048F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.7083F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(-95.75F, -2.42F, 5.97F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.25F, AnimationHelper.createRotationalVector(-98.4029F, -17.39503F, 6.85104F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(-101.24523F, -29.87096F, 7.69993F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createRotationalVector(-101.25F, -29.87F, 7.7F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.9583F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2083F, AnimationHelper.createRotationalVector(-88.58526F, -17.10045F, 11.7676F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(-88.59F, -17.1F, 11.77F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4167F, AnimationHelper.createRotationalVector(-46.59531F, -16.13694F, -3.85578F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(-24.5317F, -19.0214F, -13.70805F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.8333F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.00677F, -0.76064F, 3.19059F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.0512F, -0.76176F, 3.12882F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.8333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7917F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.8333F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.875F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.7917F, AnimationHelper.createTranslationalVector(0.0F, 0.09F, -0.11F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.8333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4167F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.625F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .build();
   public static final AnimationDefinition GETTING_NO_ITEM = AnimationDefinition.Builder.create(3.0F)
      .looping()
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(18.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(24.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.9167F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createRotationalVector(14.72765F, -31.63886F, -7.85085F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.125F, AnimationHelper.createRotationalVector(12.40525F, -4.4E-4F, 0.00829F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(12.40525F, -4.4E-4F, 0.00829F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(13.92716F, 26.80536F, 6.38918F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createRotationalVector(13.93F, 26.81F, 6.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7083F, AnimationHelper.createRotationalVector(12.40725F, 0.00444F, 0.00783F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(12.40725F, 0.00444F, 0.00783F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.125F, AnimationHelper.createRotationalVector(12.40725F, 0.0F, 0.00783F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "body",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.6F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createTranslationalVector(0.0F, 0.4F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.34F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.34F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.6667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-20.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-20.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.6667F, AnimationHelper.createRotationalVector(0.0F, -20.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(0.0F, -20.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(0.0F, 27.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createRotationalVector(0.0F, -2.5F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7083F, AnimationHelper.createRotationalVector(0.57F, -1.25F, 0.07F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.75F, AnimationHelper.createRotationalVector(0.89798F, -18.12465F, -0.16276F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7917F, AnimationHelper.createRotationalVector(1.21328F, -21.15422F, -0.2148F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.875F, AnimationHelper.createRotationalVector(1.21328F, -21.15422F, -0.2148F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0F, AnimationHelper.createRotationalVector(1.21328F, -21.15422F, -0.2148F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(2.56546F, 0.76525F, 0.57246F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(4.53867F, 7.47675F, 0.59181F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(4.53867F, 7.47675F, 0.59181F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, -360.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "head",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(0.0F, -0.15451F, 0.47553F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.4583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.625F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, -0.01F, -0.03F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-7.38733F, 1.29876F, 9.91615F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-7.38733F, 1.29876F, 9.91615F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 32.5F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-34.55418F, 11.73507F, 36.8361F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4583F, AnimationHelper.createRotationalVector(-82.47403F, 17.82361F, 2.17224F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5F, AnimationHelper.createRotationalVector(-85.08388F, 14.26971F, 1.99595F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5417F, AnimationHelper.createRotationalVector(-85.16266F, 13.19102F, 2.43976F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(-92.79F, 0.73F, 1.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createRotationalVector(-92.79F, 0.73F, 1.39F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-95.83405F, 33.18639F, -0.40081F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.25F, AnimationHelper.createRotationalVector(-95.83F, 33.19F, -0.4F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(-98.33F, 33.19F, -0.4F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5417F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(-56.46674F, 3.3853F, 14.45894F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(3.9F, -4.38F, 3.36F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createRotationalVector(3.9F, -4.38F, 3.36F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(3.90089F, -4.3843F, 3.35549F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_arm",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.75F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.8333F, AnimationHelper.createTranslationalVector(0.25358F, -0.20153F, 2.21248F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.25F, AnimationHelper.createTranslationalVector(0.25F, -0.2F, 2.21F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createTranslationalVector(0.25F, -0.2F, 2.21F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5417F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(-0.26323F, -1.46323F, 0.66566F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createTranslationalVector(-0.46F, -0.88F, -0.3F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createTranslationalVector(-0.46F, -0.88F, -0.3F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(-0.46F, 0.1159F, -0.30086F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(25.0F, 0.0F, -37.5F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.25F, AnimationHelper.createRotationalVector(-21.59341F, -12.60837F, -45.69252F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-120.7755F, -5.21988F, -2.02064F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.375F, AnimationHelper.createRotationalVector(-98.27419F, -1.79323F, -1.15048F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-93.27F, -1.79F, -1.15F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5417F, AnimationHelper.createRotationalVector(-93.27419F, -1.79323F, -1.15048F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.5833F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.7083F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.1667F, AnimationHelper.createRotationalVector(-93.55693F, -22.3224F, 3.64383F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createRotationalVector(-95.75F, -2.42F, 5.97F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.25F, AnimationHelper.createRotationalVector(-98.4029F, -17.39503F, 6.85104F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2917F, AnimationHelper.createRotationalVector(-101.24523F, -29.87096F, 7.69993F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createRotationalVector(-101.25F, -29.87F, 7.7F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.8333F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0833F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.1667F, AnimationHelper.createRotationalVector(-88.17772F, -42.09094F, 10.96195F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2083F, AnimationHelper.createRotationalVector(-88.58526F, -17.10045F, 11.7676F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(-88.59F, -17.1F, 11.77F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4167F, AnimationHelper.createRotationalVector(-46.59531F, -16.13694F, -3.85578F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createRotationalVector(-24.5317F, -19.0214F, -13.70805F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createRotationalVector(-24.5317F, -19.0214F, -13.70805F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createRotationalVector(2.41F, -0.65F, -5.01F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createRotationalVector(2.41F, -0.65F, -5.01F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(2.41492F, -0.64686F, -5.01363F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_arm",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.5833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(1.6667F, AnimationHelper.createTranslationalVector(-0.00677F, -0.76064F, 3.19059F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.0512F, -0.76176F, 3.12882F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.4583F, AnimationHelper.createTranslationalVector(0.03F, -0.51F, 2.09F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5F, AnimationHelper.createTranslationalVector(0.03F, -0.51F, 2.09F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.5417F, AnimationHelper.createTranslationalVector(0.03F, -1.28F, -0.07F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9167F, AnimationHelper.createTranslationalVector(0.03F, -1.28F, -0.07F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.03F, -0.28229F, -0.07133F), Transformation.Interpolations.LINEAR),
            new Keyframe(3.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "right_leg",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.ROTATE,
            new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .addBoneAnimation(
         "left_leg",
         new Transformation(
            Transformation.Targets.MOVE_ORIGIN,
            new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(0.2083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.0417F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.3333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
            new Keyframe(2.9583F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
         )
      )
      .build();
}
