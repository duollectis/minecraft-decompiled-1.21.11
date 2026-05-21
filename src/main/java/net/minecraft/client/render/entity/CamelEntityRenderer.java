package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer;
import net.minecraft.client.render.entity.model.CamelEntityModel;
import net.minecraft.client.render.entity.model.CamelSaddleEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.CamelEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code CamelEntityRenderer}.
 */
public class CamelEntityRenderer extends AgeableMobEntityRenderer<CamelEntity, CamelEntityRenderState, CamelEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/camel/camel.png");

	public CamelEntityRenderer(EntityRendererFactory.Context context) {
		super(
				context,
				new CamelEntityModel(context.getPart(EntityModelLayers.CAMEL)),
				new CamelEntityModel(context.getPart(EntityModelLayers.CAMEL_BABY)),
				0.7F
		);
		this.addFeature(this.createSaddleFeatureRenderer(context));
	}

	protected SaddleFeatureRenderer<CamelEntityRenderState, CamelEntityModel, CamelSaddleEntityModel> createSaddleFeatureRenderer(
			EntityRendererFactory.Context context
	) {
		return new SaddleFeatureRenderer<>(
				this,
				context.getEquipmentRenderer(),
				EquipmentModel.LayerType.CAMEL_SADDLE,
				state -> state.saddleStack,
				new CamelSaddleEntityModel(context.getPart(EntityModelLayers.CAMEL_SADDLE)),
				new CamelSaddleEntityModel(context.getPart(EntityModelLayers.CAMEL_BABY_SADDLE))
		);
	}

	public Identifier getTexture(CamelEntityRenderState camelEntityRenderState) {
		return TEXTURE;
	}

	public CamelEntityRenderState createRenderState() {
		return new CamelEntityRenderState();
	}

	public void updateRenderState(CamelEntity camelEntity, CamelEntityRenderState camelEntityRenderState, float f) {
		super.updateRenderState(camelEntity, camelEntityRenderState, f);
		camelEntityRenderState.saddleStack = camelEntity.getEquippedStack(EquipmentSlot.SADDLE).copy();
		camelEntityRenderState.hasPassengers = camelEntity.hasPassengers();
		camelEntityRenderState.jumpCooldown = Math.max(camelEntity.getJumpCooldown() - f, 0.0F);
		camelEntityRenderState.sittingTransitionAnimationState.copyFrom(camelEntity.sittingTransitionAnimationState);
		camelEntityRenderState.sittingAnimationState.copyFrom(camelEntity.sittingAnimationState);
		camelEntityRenderState.standingTransitionAnimationState.copyFrom(camelEntity.standingTransitionAnimationState);
		camelEntityRenderState.idlingAnimationState.copyFrom(camelEntity.idlingAnimationState);
		camelEntityRenderState.dashingAnimationState.copyFrom(camelEntity.dashingAnimationState);
	}
}
