package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface BakedSimpleModel extends SimpleModel {
   boolean DEFAULT_AMBIENT_OCCLUSION = true;
   UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.BLOCK;

   UnbakedModel getModel();

   @Nullable BakedSimpleModel getParent();

   static ModelTextures getTextures(BakedSimpleModel model) {
      BakedSimpleModel bakedSimpleModel = model;

      ModelTextures.Builder builder;
      for (builder = new ModelTextures.Builder(); bakedSimpleModel != null; bakedSimpleModel = bakedSimpleModel.getParent()) {
         builder.addLast(bakedSimpleModel.getModel().textures());
      }

      return builder.build(model);
   }

   default ModelTextures getTextures() {
      return getTextures(this);
   }

   static boolean getAmbientOcclusion(BakedSimpleModel model) {
      while (model != null) {
         Boolean boolean_ = model.getModel().ambientOcclusion();
         if (boolean_ != null) {
            return boolean_;
         }

         model = model.getParent();
      }

      return true;
   }

   default boolean getAmbientOcclusion() {
      return getAmbientOcclusion(this);
   }

   static UnbakedModel.GuiLight getGuiLight(BakedSimpleModel model) {
      while (model != null) {
         UnbakedModel.GuiLight guiLight = model.getModel().guiLight();
         if (guiLight != null) {
            return guiLight;
         }

         model = model.getParent();
      }

      return DEFAULT_GUI_LIGHT;
   }

   default UnbakedModel.GuiLight getGuiLight() {
      return getGuiLight(this);
   }

   static Geometry getGeometry(BakedSimpleModel model) {
      while (model != null) {
         Geometry geometry = model.getModel().geometry();
         if (geometry != null) {
            return geometry;
         }

         model = model.getParent();
      }

      return Geometry.EMPTY;
   }

   default Geometry getGeometry() {
      return getGeometry(this);
   }

   default BakedGeometry bakeGeometry(ModelTextures textures, Baker baker, ModelBakeSettings settings) {
      return this.getGeometry().bake(textures, baker, settings, this);
   }

   static Sprite getParticleTexture(ModelTextures textures, Baker baker, SimpleModel model) {
      return baker.getSpriteGetter().get(textures, "particle", model);
   }

   default Sprite getParticleTexture(ModelTextures textures, Baker baker) {
      return getParticleTexture(textures, baker, this);
   }

   static Transformation extractTransformation(BakedSimpleModel model, ItemDisplayContext mode) {
      while (model != null) {
         ModelTransformation modelTransformation = model.getModel().transformations();
         if (modelTransformation != null) {
            Transformation transformation = modelTransformation.getTransformation(mode);
            if (transformation != Transformation.IDENTITY) {
               return transformation;
            }
         }

         model = model.getParent();
      }

      return Transformation.IDENTITY;
   }

   static ModelTransformation copyTransformations(BakedSimpleModel model) {
      Transformation transformation = extractTransformation(model, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
      Transformation transformation2 = extractTransformation(model, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
      Transformation transformation3 = extractTransformation(model, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
      Transformation transformation4 = extractTransformation(model, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
      Transformation transformation5 = extractTransformation(model, ItemDisplayContext.HEAD);
      Transformation transformation6 = extractTransformation(model, ItemDisplayContext.GUI);
      Transformation transformation7 = extractTransformation(model, ItemDisplayContext.GROUND);
      Transformation transformation8 = extractTransformation(model, ItemDisplayContext.FIXED);
      Transformation transformation9 = extractTransformation(model, ItemDisplayContext.ON_SHELF);
      return new ModelTransformation(
         transformation, transformation2, transformation3, transformation4, transformation5, transformation6, transformation7, transformation8, transformation9
      );
   }

   default ModelTransformation getTransformations() {
      return copyTransformations(this);
   }
}
