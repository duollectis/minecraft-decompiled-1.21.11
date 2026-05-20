package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.SkeletonEntityRenderState;
import net.minecraft.entity.mob.ParchedEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ParchedEntityRenderer extends AbstractSkeletonEntityRenderer<ParchedEntity, SkeletonEntityRenderState> {
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/skeleton/parched.png");

   public ParchedEntityRenderer(EntityRendererFactory.Context context) {
      super(context, EntityModelLayers.PARCHED, EntityModelLayers.PARCHED_EQUIPMENT);
   }

   public Identifier getTexture(SkeletonEntityRenderState skeletonEntityRenderState) {
      return TEXTURE;
   }

   public SkeletonEntityRenderState createRenderState() {
      return new SkeletonEntityRenderState();
   }
}
