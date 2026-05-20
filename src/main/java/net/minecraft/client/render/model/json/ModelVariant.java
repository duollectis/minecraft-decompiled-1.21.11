package net.minecraft.client.render.model.json;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.GeometryBakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AxisRotation;

@Environment(EnvType.CLIENT)
public record ModelVariant(Identifier modelId, ModelVariant.ModelState modelState) implements BlockModelPart.Unbaked {
   public static final MapCodec<ModelVariant> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Identifier.CODEC.fieldOf("model").forGetter(ModelVariant::modelId), ModelVariant.ModelState.CODEC.forGetter(ModelVariant::modelState)
         )
         .apply(instance, ModelVariant::new)
   );
   public static final Codec<ModelVariant> CODEC = MAP_CODEC.codec();

   public ModelVariant(Identifier model) {
      this(model, ModelVariant.ModelState.DEFAULT);
   }

   public ModelVariant withRotationX(AxisRotation amount) {
      return this.setState(this.modelState.setRotationX(amount));
   }

   public ModelVariant withRotationY(AxisRotation amount) {
      return this.setState(this.modelState.setRotationY(amount));
   }

   public ModelVariant method_76657(AxisRotation axisRotation) {
      return this.setState(this.modelState.method_76658(axisRotation));
   }

   public ModelVariant withUVLock(boolean uvLock) {
      return this.setState(this.modelState.setUVLock(uvLock));
   }

   public ModelVariant withModel(Identifier modelId) {
      return new ModelVariant(modelId, this.modelState);
   }

   public ModelVariant setState(ModelVariant.ModelState modelState) {
      return new ModelVariant(this.modelId, modelState);
   }

   public ModelVariant with(ModelVariantOperator variantOperator) {
      return variantOperator.apply(this);
   }

   @Override
   public BlockModelPart bake(Baker baker) {
      return GeometryBakedModel.create(baker, this.modelId, this.modelState.asModelBakeSettings());
   }

   @Override
   public void resolve(ResolvableModel.Resolver resolver) {
      resolver.markDependency(this.modelId);
   }

   @Environment(EnvType.CLIENT)
   public record ModelState(AxisRotation x, AxisRotation y, AxisRotation z, boolean uvLock) {
      public static final MapCodec<ModelVariant.ModelState> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               AxisRotation.CODEC.optionalFieldOf("x", AxisRotation.R0).forGetter(ModelVariant.ModelState::x),
               AxisRotation.CODEC.optionalFieldOf("y", AxisRotation.R0).forGetter(ModelVariant.ModelState::y),
               AxisRotation.CODEC.optionalFieldOf("z", AxisRotation.R0).forGetter(ModelVariant.ModelState::z),
               Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(ModelVariant.ModelState::uvLock)
            )
            .apply(instance, ModelVariant.ModelState::new)
      );
      public static final ModelVariant.ModelState DEFAULT = new ModelVariant.ModelState(AxisRotation.R0, AxisRotation.R0, AxisRotation.R0, false);

      public ModelBakeSettings asModelBakeSettings() {
         net.minecraft.client.render.model.ModelRotation modelRotation = net.minecraft.client.render.model.ModelRotation.fromDirectionTransformation(
            AxisRotation.method_76600(this.x, this.y, this.z)
         );
         return (ModelBakeSettings)(this.uvLock ? modelRotation.getUVModel() : modelRotation);
      }

      public ModelVariant.ModelState setRotationX(AxisRotation amount) {
         return new ModelVariant.ModelState(amount, this.y, this.z, this.uvLock);
      }

      public ModelVariant.ModelState setRotationY(AxisRotation amount) {
         return new ModelVariant.ModelState(this.x, amount, this.z, this.uvLock);
      }

      public ModelVariant.ModelState method_76658(AxisRotation axisRotation) {
         return new ModelVariant.ModelState(this.x, this.y, axisRotation, this.uvLock);
      }

      public ModelVariant.ModelState setUVLock(boolean uvLock) {
         return new ModelVariant.ModelState(this.x, this.y, this.z, uvLock);
      }
   }
}
