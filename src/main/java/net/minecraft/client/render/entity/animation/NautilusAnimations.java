package net.minecraft.client.render.entity.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code NautilusAnimations}.
 */
public class NautilusAnimations {

	public static final AnimationDefinition ANIMATION = AnimationDefinition.Builder.create(1.0F)
	                                                                               .looping()
	                                                                               .addBoneAnimation(
			                                                                               "body",
			                                                                               new Transformation(
					                                                                               Transformation.Targets.SCALE,
					                                                                               new Keyframe(
							                                                                               0.0F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.5F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.2F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.75F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               0.9F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.875F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               1.0F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               )
			                                                                               )
	                                                                               )
	                                                                               .addBoneAnimation(
			                                                                               "upper_mouth",
			                                                                               new Transformation(
					                                                                               Transformation.Targets.ROTATE,
					                                                                               new Keyframe(
							                                                                               0.0F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               0.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.5F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               30.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.75F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               0.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.875F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               0.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               1.0F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               0.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               )
			                                                                               )
	                                                                               )
	                                                                               .addBoneAnimation(
			                                                                               "upper_mouth",
			                                                                               new Transformation(
					                                                                               Transformation.Targets.SCALE,
					                                                                               new Keyframe(
							                                                                               0.0F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.5F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.4F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.75F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               0.9F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.875F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               1.0F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               )
			                                                                               )
	                                                                               )
	                                                                               .addBoneAnimation(
			                                                                               "inner_mouth",
			                                                                               new Transformation(
					                                                                               Transformation.Targets.SCALE,
					                                                                               new Keyframe(
							                                                                               0.0F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.5F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               0.8F,
									                                                                               0.8F,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.75F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               0.9F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.875F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               1.0F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               )
			                                                                               )
	                                                                               )
	                                                                               .addBoneAnimation(
			                                                                               "lower_mouth",
			                                                                               new Transformation(
					                                                                               Transformation.Targets.ROTATE,
					                                                                               new Keyframe(
							                                                                               0.0F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               0.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.5F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               -30.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.75F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               0.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.875F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               0.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               1.0F,
							                                                                               AnimationHelper.createRotationalVector(
									                                                                               0.0F,
									                                                                               0.0F,
									                                                                               0.0F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               )
			                                                                               )
	                                                                               )
	                                                                               .addBoneAnimation(
			                                                                               "lower_mouth",
			                                                                               new Transformation(
					                                                                               Transformation.Targets.SCALE,
					                                                                               new Keyframe(
							                                                                               0.0F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.5F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.4F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.75F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               0.9F
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               0.875F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               ),
					                                                                               new Keyframe(
							                                                                               1.0F,
							                                                                               AnimationHelper.createScalingVector(
									                                                                               1.0,
									                                                                               1.0,
									                                                                               1.0
							                                                                               ),
							                                                                               Transformation.Interpolations.LINEAR
					                                                                               )
			                                                                               )
	                                                                               )
	                                                                               .build();
}
