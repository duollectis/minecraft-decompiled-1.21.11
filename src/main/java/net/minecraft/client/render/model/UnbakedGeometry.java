package net.minecraft.client.render.model;

import java.util.List;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public record UnbakedGeometry(List<ModelElement> elements) implements Geometry {
   @Override
   public BakedGeometry bake(ModelTextures modelTextures, Baker baker, ModelBakeSettings modelBakeSettings, SimpleModel simpleModel) {
      return bakeGeometry(this.elements, modelTextures, baker, modelBakeSettings, simpleModel);
   }

   public static BakedGeometry bakeGeometry(
      List<ModelElement> elements, ModelTextures modelTextures, Baker baker, ModelBakeSettings settings, SimpleModel simpleModel
   ) {
      BakedGeometry.Builder builder = new BakedGeometry.Builder();

      for (ModelElement modelElement : elements) {
         boolean bl = true;
         boolean bl2 = true;
         boolean bl3 = true;
         Vector3fc vector3fc = modelElement.from();
         Vector3fc vector3fc2 = modelElement.to();
         if (vector3fc.x() == vector3fc2.x()) {
            bl2 = false;
            bl3 = false;
         }

         if (vector3fc.y() == vector3fc2.y()) {
            bl = false;
            bl3 = false;
         }

         if (vector3fc.z() == vector3fc2.z()) {
            bl = false;
            bl2 = false;
         }

         if (bl || bl2 || bl3) {
            for (Entry<Direction, ModelElementFace> entry : modelElement.faces().entrySet()) {
               Direction direction = entry.getKey();
               ModelElementFace modelElementFace = entry.getValue();

               boolean bl4 = switch (direction.getAxis()) {
                  case X -> bl;
                  case Y -> bl2;
                  case Z -> bl3;
               };
               if (bl4) {
                  Sprite sprite = baker.getSpriteGetter().get(modelTextures, modelElementFace.textureId(), simpleModel);
                  BakedQuad bakedQuad = BakedQuadFactory.bake(
                     baker.method_76674(),
                     vector3fc,
                     vector3fc2,
                     modelElementFace,
                     sprite,
                     direction,
                     settings,
                     modelElement.rotation(),
                     modelElement.shade(),
                     modelElement.lightEmission()
                  );
                  if (modelElementFace.cullFace() == null) {
                     builder.add(bakedQuad);
                  } else {
                     builder.add(Direction.transform(settings.getRotation().getMatrix(), modelElementFace.cullFace()), bakedQuad);
                  }
               }
            }
         }
      }

      return builder.build();
   }
}
