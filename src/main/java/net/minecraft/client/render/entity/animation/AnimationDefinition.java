package net.minecraft.client.render.entity.animation;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;

@Environment(EnvType.CLIENT)
public record AnimationDefinition(float lengthInSeconds, boolean looping, Map<String, List<Transformation>> boneAnimations) {
   public Animation createAnimation(ModelPart root) {
      return Animation.of(root, this);
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final float lengthInSeconds;
      private final Map<String, List<Transformation>> transformations = Maps.newHashMap();
      private boolean looping;

      public static AnimationDefinition.Builder create(float lengthInSeconds) {
         return new AnimationDefinition.Builder(lengthInSeconds);
      }

      private Builder(float lengthInSeconds) {
         this.lengthInSeconds = lengthInSeconds;
      }

      public AnimationDefinition.Builder looping() {
         this.looping = true;
         return this;
      }

      public AnimationDefinition.Builder addBoneAnimation(String name, Transformation transformation) {
         this.transformations.computeIfAbsent(name, namex -> new ArrayList<>()).add(transformation);
         return this;
      }

      public AnimationDefinition build() {
         return new AnimationDefinition(this.lengthInSeconds, this.looping, this.transformations);
      }
   }
}
