package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public abstract class MobEntityRenderer<T extends MobEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
   extends LivingEntityRenderer<T, S, M> {
   public MobEntityRenderer(EntityRendererFactory.Context context, M entityModel, float f) {
      super(context, entityModel, f);
   }

   protected boolean hasLabel(T mobEntity, double d) {
      return super.hasLabel(mobEntity, d) && (mobEntity.shouldRenderName() || mobEntity.hasCustomName() && mobEntity == this.dispatcher.targetedEntity);
   }

   @Override
   protected float getShadowRadius(S livingEntityRenderState) {
      return super.getShadowRadius(livingEntityRenderState) * livingEntityRenderState.ageScale;
   }

   protected static boolean nameEquals(Entity entity, String name) {
      Text text = entity.getCustomName();
      return text != null && name.equals(text.getString());
   }
}
