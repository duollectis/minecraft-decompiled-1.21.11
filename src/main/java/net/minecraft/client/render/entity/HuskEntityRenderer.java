package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class HuskEntityRenderer extends ZombieEntityRenderer {
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/zombie/husk.png");

   public HuskEntityRenderer(EntityRendererFactory.Context context) {
      super(context, EntityModelLayers.HUSK, EntityModelLayers.HUSK_BABY, EntityModelLayers.HUSK_EQUIPMENT, EntityModelLayers.HUSK_BABY_EQUIPMENT);
   }

   @Override
   public Identifier getTexture(ZombieEntityRenderState zombieEntityRenderState) {
      return TEXTURE;
   }
}
