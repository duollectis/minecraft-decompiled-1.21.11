package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.BreezeEyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.BreezeWindFeatureRenderer;
import net.minecraft.client.render.entity.model.BreezeEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.BreezeEntityRenderState;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code BreezeEntityRenderer}.
 */
public class BreezeEntityRenderer extends MobEntityRenderer<BreezeEntity, BreezeEntityRenderState, BreezeEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/breeze/breeze.png");

	public BreezeEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new BreezeEntityModel(context.getPart(EntityModelLayers.BREEZE)), 0.5F);
		this.addFeature(new BreezeWindFeatureRenderer(this, context.getEntityModels()));
		this.addFeature(new BreezeEyesFeatureRenderer(this, context.getEntityModels()));
	}

	public Identifier getTexture(BreezeEntityRenderState breezeEntityRenderState) {
		return TEXTURE;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return BreezeEntityRenderState — результат операции
	 */
	public BreezeEntityRenderState createRenderState() {
		return new BreezeEntityRenderState();
	}

	/**
	 * Обновляет render state.
	 *
	 * @param breezeEntity breeze entity
	 * @param breezeEntityRenderState breeze entity render state
	 * @param f f
	 */
	public void updateRenderState(BreezeEntity breezeEntity, BreezeEntityRenderState breezeEntityRenderState, float f) {
		super.updateRenderState(breezeEntity, breezeEntityRenderState, f);
		breezeEntityRenderState.idleAnimationState.copyFrom(breezeEntity.idleAnimationState);
		breezeEntityRenderState.shootingAnimationState.copyFrom(breezeEntity.shootingAnimationState);
		breezeEntityRenderState.slidingAnimationState.copyFrom(breezeEntity.slidingAnimationState);
		breezeEntityRenderState.slidingBackAnimationState.copyFrom(breezeEntity.slidingBackAnimationState);
		breezeEntityRenderState.inhalingAnimationState.copyFrom(breezeEntity.inhalingAnimationState);
		breezeEntityRenderState.longJumpingAnimationState.copyFrom(breezeEntity.longJumpingAnimationState);
	}
}
