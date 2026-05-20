package net.minecraft.client.render.model;

import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisRotation;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class MissingModel {
   private static final String TEXTURE_ID = "missingno";
   public static final Identifier ID = Identifier.ofVanilla("builtin/missing");

   public static UnbakedModel create() {
      ModelElementFace.UV uV = new ModelElementFace.UV(0.0F, 0.0F, 16.0F, 16.0F);
      Map<Direction, ModelElementFace> map = Util.mapEnum(Direction.class, direction -> new ModelElementFace(direction, -1, "missingno", uV, AxisRotation.R0));
      ModelElement modelElement = new ModelElement(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), map);
      return new JsonUnbakedModel(
         new UnbakedGeometry(List.of(modelElement)),
         null,
         null,
         ModelTransformation.NONE,
         new ModelTextures.Textures.Builder()
            .addTextureReference("particle", "missingno")
            .addSprite("missingno", new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, MissingSprite.getMissingSpriteId()))
            .build(),
         null
      );
   }
}
