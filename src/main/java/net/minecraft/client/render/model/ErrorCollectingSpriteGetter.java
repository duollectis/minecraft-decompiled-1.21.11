package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.sprite.FabricErrorCollectingSpriteGetter;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;

@Environment(EnvType.CLIENT)
public interface ErrorCollectingSpriteGetter extends FabricErrorCollectingSpriteGetter {
   Sprite get(SpriteIdentifier id, SimpleModel model);

   Sprite getMissing(String name, SimpleModel model);

   default Sprite get(ModelTextures texture, String name, SimpleModel model) {
      SpriteIdentifier spriteIdentifier = texture.get(name);
      return spriteIdentifier != null ? this.get(spriteIdentifier, model) : this.getMissing(name, model);
   }
}
