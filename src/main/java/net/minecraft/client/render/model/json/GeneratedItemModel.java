package net.minecraft.client.render.model.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.BakedGeometry;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.Geometry;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelTextures;
import net.minecraft.client.render.model.SimpleModel;
import net.minecraft.client.render.model.UnbakedGeometry;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AxisRotation;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GeneratedItemModel implements UnbakedModel {
   public static final Identifier GENERATED = Identifier.ofVanilla("builtin/generated");
   public static final List<String> LAYERS = List.of("layer0", "layer1", "layer2", "layer3", "layer4");
   private static final float field_32806 = 7.5F;
   private static final float field_32807 = 8.5F;
   private static final ModelTextures.Textures TEXTURES = new ModelTextures.Textures.Builder().addTextureReference("particle", "layer0").build();
   private static final ModelElementFace.UV FACING_SOUTH_UV = new ModelElementFace.UV(0.0F, 0.0F, 16.0F, 16.0F);
   private static final ModelElementFace.UV FACING_NORTH_UV = new ModelElementFace.UV(16.0F, 0.0F, 0.0F, 16.0F);
   private static final float field_64230 = 0.1F;

   @Override
   public ModelTextures.Textures textures() {
      return TEXTURES;
   }

   @Override
   public Geometry geometry() {
      return GeneratedItemModel::bakeGeometry;
   }

   @Override
   public UnbakedModel.@Nullable GuiLight guiLight() {
      return UnbakedModel.GuiLight.ITEM;
   }

   private static BakedGeometry bakeGeometry(ModelTextures textures, Baker baker, ModelBakeSettings settings, SimpleModel model) {
      List<ModelElement> list = new ArrayList<>();

      for (int i = 0; i < LAYERS.size(); i++) {
         String string = LAYERS.get(i);
         SpriteIdentifier spriteIdentifier = textures.get(string);
         if (spriteIdentifier == null) {
            break;
         }

         SpriteContents spriteContents = baker.getSpriteGetter().get(spriteIdentifier, model).getContents();
         list.addAll(addLayerElements(i, string, spriteContents));
      }

      return UnbakedGeometry.bakeGeometry(list, textures, baker, settings, model);
   }

   private static List<ModelElement> addLayerElements(int tintIndex, String name, SpriteContents spriteContents) {
      Map<Direction, ModelElementFace> map = Map.of(
         Direction.SOUTH,
         new ModelElementFace(null, tintIndex, name, FACING_SOUTH_UV, AxisRotation.R0),
         Direction.NORTH,
         new ModelElementFace(null, tintIndex, name, FACING_NORTH_UV, AxisRotation.R0)
      );
      List<ModelElement> list = new ArrayList<>();
      list.add(new ModelElement(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), map));
      list.addAll(addSubComponents(spriteContents, name, tintIndex));
      return list;
   }

   private static List<ModelElement> addSubComponents(SpriteContents spriteContents, String string, int i) {
      float f = 16.0F / spriteContents.getWidth();
      float g = 16.0F / spriteContents.getHeight();
      List<ModelElement> list = new ArrayList<>();

      for (GeneratedItemModel.class_12295 lv : getFrames(spriteContents)) {
         float h = lv.x();
         float j = lv.y();
         GeneratedItemModel.Side side = lv.facing();
         float k = h + 0.1F;
         float l = h + 1.0F - 0.1F;
         float m;
         float n;
         if (side.isVertical()) {
            m = j + 0.1F;
            n = j + 1.0F - 0.1F;
         } else {
            m = j + 1.0F - 0.1F;
            n = j + 0.1F;
         }

         float o = h;
         float p = j;
         float q = h;
         float r = j;
         switch (side) {
            case UP:
               q = h + 1.0F;
               break;
            case DOWN:
               q = h + 1.0F;
               p = j + 1.0F;
               r = j + 1.0F;
               break;
            case LEFT:
               r = j + 1.0F;
               break;
            case RIGHT:
               o = h + 1.0F;
               q = h + 1.0F;
               r = j + 1.0F;
         }

         o *= f;
         q *= f;
         p *= g;
         r *= g;
         p = 16.0F - p;
         r = 16.0F - r;
         Map<Direction, ModelElementFace> map = Map.of(
            side.getDirection(), new ModelElementFace(null, i, string, new ModelElementFace.UV(k * f, m * f, l * g, n * g), AxisRotation.R0)
         );
         switch (side) {
            case UP:
               list.add(new ModelElement(new Vector3f(o, p, 7.5F), new Vector3f(q, p, 8.5F), map));
               break;
            case DOWN:
               list.add(new ModelElement(new Vector3f(o, r, 7.5F), new Vector3f(q, r, 8.5F), map));
               break;
            case LEFT:
               list.add(new ModelElement(new Vector3f(o, p, 7.5F), new Vector3f(o, r, 8.5F), map));
               break;
            case RIGHT:
               list.add(new ModelElement(new Vector3f(q, p, 7.5F), new Vector3f(q, r, 8.5F), map));
         }
      }

      return list;
   }

   private static Collection<GeneratedItemModel.class_12295> getFrames(SpriteContents spriteContents) {
      int i = spriteContents.getWidth();
      int j = spriteContents.getHeight();
      Set<GeneratedItemModel.class_12295> set = new HashSet<>();
      spriteContents.getDistinctFrameCount().forEach(k -> {
         for (int l = 0; l < j; l++) {
            for (int m = 0; m < i; m++) {
               boolean bl = !isPixelTransparent(spriteContents, k, m, l, i, j);
               if (bl) {
                  buildCube(GeneratedItemModel.Side.UP, set, spriteContents, k, m, l, i, j);
                  buildCube(GeneratedItemModel.Side.DOWN, set, spriteContents, k, m, l, i, j);
                  buildCube(GeneratedItemModel.Side.LEFT, set, spriteContents, k, m, l, i, j);
                  buildCube(GeneratedItemModel.Side.RIGHT, set, spriteContents, k, m, l, i, j);
               }
            }
         }
      });
      return set;
   }

   private static void buildCube(
      GeneratedItemModel.Side side, Set<GeneratedItemModel.class_12295> set, SpriteContents spriteContents, int i, int j, int k, int l, int m
   ) {
      if (isPixelTransparent(spriteContents, i, j - side.direction.getOffsetX(), k - side.direction.getOffsetY(), l, m)) {
         set.add(new GeneratedItemModel.class_12295(side, j, k));
      }
   }

   private static boolean isPixelTransparent(SpriteContents spriteContents, int i, int j, int k, int l, int m) {
      return j >= 0 && k >= 0 && j < l && k < m ? spriteContents.isPixelTransparent(i, j, k) : true;
   }

   @Environment(EnvType.CLIENT)
   static enum Side {
      UP(Direction.UP),
      DOWN(Direction.DOWN),
      LEFT(Direction.EAST),
      RIGHT(Direction.WEST);

      final Direction direction;

      private Side(final Direction direction) {
         this.direction = direction;
      }

      public Direction getDirection() {
         return this.direction;
      }

      boolean isVertical() {
         return this == DOWN || this == UP;
      }
   }

   @Environment(EnvType.CLIENT)
   record class_12295(GeneratedItemModel.Side facing, int x, int y) {
   }
}
