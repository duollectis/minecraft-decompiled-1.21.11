package net.minecraft.client.render.entity;

import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.feature.CopperGolemHeadBlockFeatureRenderer;
import net.minecraft.client.render.entity.feature.EmissiveFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.CopperGolemEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.CopperGolemEntityRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.passive.CopperGolemEntity;
import net.minecraft.entity.passive.CopperGolemOxidationLevels;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class CopperGolemEntityRenderer extends MobEntityRenderer<CopperGolemEntity, CopperGolemEntityRenderState, CopperGolemEntityModel> {
   public CopperGolemEntityRenderer(EntityRendererFactory.Context context) {
      super(context, new CopperGolemEntityModel(context.getPart(EntityModelLayers.COPPER_GOLEM)), 0.5F);
      this.addFeature(
         new EmissiveFeatureRenderer<>(
            this,
            getEyeTextureGetter(),
            (state, tickProgress) -> 1.0F,
            new CopperGolemEntityModel(context.getPart(EntityModelLayers.COPPER_GOLEM)),
            RenderLayers::eyes,
            false
         )
      );
      this.addFeature(new HeldItemFeatureRenderer<>(this));
      this.addFeature(new CopperGolemHeadBlockFeatureRenderer<>(this, state -> state.headBlockItemStack, this.model::transformMatricesForBlock));
      this.addFeature(new HeadFeatureRenderer<>(this, context.getEntityModels(), context.getPlayerSkinCache()));
   }

   public Identifier getTexture(CopperGolemEntityRenderState copperGolemEntityRenderState) {
      return CopperGolemOxidationLevels.get(copperGolemEntityRenderState.oxidationLevel).texture();
   }

   private static Function<CopperGolemEntityRenderState, Identifier> getEyeTextureGetter() {
      return state -> CopperGolemOxidationLevels.get(state.oxidationLevel).eyeTexture();
   }

   public CopperGolemEntityRenderState createRenderState() {
      return new CopperGolemEntityRenderState();
   }

   public void updateRenderState(CopperGolemEntity copperGolemEntity, CopperGolemEntityRenderState copperGolemEntityRenderState, float f) {
      super.updateRenderState(copperGolemEntity, copperGolemEntityRenderState, f);
      ArmedEntityRenderState.updateRenderState(copperGolemEntity, copperGolemEntityRenderState, this.itemModelResolver, f);
      copperGolemEntityRenderState.oxidationLevel = copperGolemEntity.getOxidationLevel();
      copperGolemEntityRenderState.copperGolemState = copperGolemEntity.getState();
      copperGolemEntityRenderState.spinHeadAnimationState.copyFrom(copperGolemEntity.getSpinHeadAnimationState());
      copperGolemEntityRenderState.gettingItemAnimationState.copyFrom(copperGolemEntity.getGettingItemAnimationState());
      copperGolemEntityRenderState.gettingNoItemAnimationState.copyFrom(copperGolemEntity.getGettingNoItemAnimationState());
      copperGolemEntityRenderState.droppingItemAnimationState.copyFrom(copperGolemEntity.getDroppingItemAnimationState());
      copperGolemEntityRenderState.droppingNoItemAnimationState.copyFrom(copperGolemEntity.getDroppingNoItemAnimationState());
      copperGolemEntityRenderState.headBlockItemStack = Optional.of(copperGolemEntity.getEquippedStack(CopperGolemEntity.POPPY_SLOT)).flatMap(stack -> {
         if (stack.getItem() instanceof BlockItem blockItem) {
            BlockStateComponent blockStateComponent = stack.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT);
            return Optional.of(blockStateComponent.applyToState(blockItem.getBlock().getDefaultState()));
         } else {
            return Optional.empty();
         }
      });
   }
}
