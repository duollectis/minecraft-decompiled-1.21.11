package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.feature.EmissiveFeatureRenderer;
import net.minecraft.client.render.entity.model.CreakingEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.CreakingEntityRenderState;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code CreakingEntityRenderer}.
 */
public class CreakingEntityRenderer<T extends CreakingEntity> extends MobEntityRenderer<T, CreakingEntityRenderState, CreakingEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/creaking/creaking.png");
	private static final Identifier EYES_TEXTURE = Identifier.ofVanilla("textures/entity/creaking/creaking_eyes.png");

	public CreakingEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new CreakingEntityModel(context.getPart(EntityModelLayers.CREAKING)), 0.6F);
		this.addFeature(
				new EmissiveFeatureRenderer<>(
						this,
						creakingEntityRenderState -> EYES_TEXTURE,
						(state, tickProgress) -> state.glowingEyes ? 1.0F : 0.0F,
						new CreakingEntityModel(context.getPart(EntityModelLayers.CREAKING_EYES)),
						RenderLayers::eyes,
						true
				)
		);
	}

	public Identifier getTexture(CreakingEntityRenderState creakingEntityRenderState) {
		return TEXTURE;
	}

	public CreakingEntityRenderState createRenderState() {
		return new CreakingEntityRenderState();
	}

	public void updateRenderState(T creakingEntity, CreakingEntityRenderState creakingEntityRenderState, float f) {
		super.updateRenderState(creakingEntity, creakingEntityRenderState, f);
		creakingEntityRenderState.attackAnimationState.copyFrom(creakingEntity.attackAnimationState);
		creakingEntityRenderState.invulnerableAnimationState.copyFrom(creakingEntity.invulnerableAnimationState);
		creakingEntityRenderState.crumblingAnimationState.copyFrom(creakingEntity.crumblingAnimationState);
		if (creakingEntity.isCrumbling()) {
			creakingEntityRenderState.deathTime = 0.0F;
			creakingEntityRenderState.hurt = false;
			creakingEntityRenderState.glowingEyes = creakingEntity.hasGlowingEyesWhileCrumbling();
		}
		else {
			creakingEntityRenderState.glowingEyes = creakingEntity.isActive();
		}

		creakingEntityRenderState.unrooted = creakingEntity.isUnrooted();
	}
}
