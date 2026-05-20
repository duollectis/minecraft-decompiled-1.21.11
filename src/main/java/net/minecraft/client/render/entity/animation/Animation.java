package net.minecraft.client.render.entity.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.AnimationState;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class Animation {
   private final AnimationDefinition definition;
   private final List<Animation.TransformationEntry> entries;

   private Animation(AnimationDefinition definition, List<Animation.TransformationEntry> entries) {
      this.definition = definition;
      this.entries = entries;
   }

   static Animation of(ModelPart root, AnimationDefinition definition) {
      List<Animation.TransformationEntry> list = new ArrayList<>();
      Function<String, ModelPart> function = root.createPartGetter();

      for (Entry<String, List<Transformation>> entry : definition.boneAnimations().entrySet()) {
         String string = entry.getKey();
         List<Transformation> list2 = entry.getValue();
         ModelPart modelPart = function.apply(string);
         if (modelPart == null) {
            throw new IllegalArgumentException("Cannot animate " + string + ", which does not exist in model");
         }

         for (Transformation transformation : list2) {
            list.add(new Animation.TransformationEntry(modelPart, transformation.target(), transformation.keyframes()));
         }
      }

      return new Animation(definition, List.copyOf(list));
   }

   public void applyStatic() {
      this.apply(0L, 1.0F);
   }

   public void applyWalking(float limbSwingAnimationProgress, float limbSwingAmplitude, float f, float g) {
      long l = (long)(limbSwingAnimationProgress * 50.0F * f);
      float h = Math.min(limbSwingAmplitude * g, 1.0F);
      this.apply(l, h);
   }

   public void apply(AnimationState animationState, float age) {
      this.apply(animationState, age, 1.0F);
   }

   public void apply(AnimationState animationState, float age, float speedMultiplier) {
      animationState.run(state -> this.apply((long)((float)state.getTimeInMilliseconds(age) * speedMultiplier), 1.0F));
   }

   public void apply(long timeInMilliseconds, float scale) {
      float f = this.getRunningSeconds(timeInMilliseconds);
      Vector3f vector3f = new Vector3f();

      for (Animation.TransformationEntry transformationEntry : this.entries) {
         transformationEntry.apply(f, scale, vector3f);
      }
   }

   private float getRunningSeconds(long timeInMilliseconds) {
      float f = (float)timeInMilliseconds / 1000.0F;
      return this.definition.looping() ? f % this.definition.lengthInSeconds() : f;
   }

   @Environment(EnvType.CLIENT)
   record TransformationEntry(ModelPart part, Transformation.Target target, Keyframe[] keyframes) {
      public void apply(float runningSeconds, float scale, Vector3f vec) {
         int i = Math.max(0, MathHelper.binarySearch(0, this.keyframes.length, index -> runningSeconds <= this.keyframes[index].timestamp()) - 1);
         int j = Math.min(this.keyframes.length - 1, i + 1);
         Keyframe keyframe = this.keyframes[i];
         Keyframe keyframe2 = this.keyframes[j];
         float f = runningSeconds - keyframe.timestamp();
         float g;
         if (j != i) {
            g = MathHelper.clamp(f / (keyframe2.timestamp() - keyframe.timestamp()), 0.0F, 1.0F);
         } else {
            g = 0.0F;
         }

         keyframe2.interpolation().apply(vec, g, this.keyframes, i, j, scale);
         this.target.apply(this.part, vec);
      }
   }
}
