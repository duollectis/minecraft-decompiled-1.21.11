package net.minecraft.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.SpriteIdentifier;

@Environment(EnvType.CLIENT)
public interface SpriteHolder {
   Sprite getSprite(SpriteIdentifier id);
}
