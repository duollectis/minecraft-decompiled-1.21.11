package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.feature.EmissiveFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.WardenEntityModel;
import net.minecraft.client.render.entity.state.WardenEntityRenderState;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code WardenEntityRenderer}.
 */
public class WardenEntityRenderer extends MobEntityRenderer<WardenEntity, WardenEntityRenderState, WardenEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/warden/warden.png");
	private static final Identifier
			BIOLUMINESCENT_LAYER_TEXTURE =
			Identifier.ofVanilla("textures/entity/warden/warden_bioluminescent_layer.png");
	private static final Identifier HEART_TEXTURE = Identifier.ofVanilla("textures/entity/warden/warden_heart.png");
	private static final Identifier
			PULSATING_SPOTS_1_TEXTURE =
			Identifier.ofVanilla("textures/entity/warden/warden_pulsating_spots_1.png");
	private static final Identifier
			PULSATING_SPOTS_2_TEXTURE =
			Identifier.ofVanilla("textures/entity/warden/warden_pulsating_spots_2.png");

	public WardenEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new WardenEntityModel(context.getPart(EntityModelLayers.WARDEN)), 0.9F);
		WardenEntityModel
				wardenEntityModel =
				new WardenEntityModel(context.getPart(EntityModelLayers.WARDEN_BIOLUMINESCENT));
		WardenEntityModel
				wardenEntityModel2 =
				new WardenEntityModel(context.getPart(EntityModelLayers.WARDEN_PULSATING_SPOTS));
		WardenEntityModel
				wardenEntityModel3 =
				new WardenEntityModel(context.getPart(EntityModelLayers.WARDEN_TENDRILS));
		WardenEntityModel wardenEntityModel4 = new WardenEntityModel(context.getPart(EntityModelLayers.WARDEN_HEART));
		this.addFeature(
				new EmissiveFeatureRenderer<>(
						this,
						wardenEntityRenderState -> BIOLUMINESCENT_LAYER_TEXTURE,
						(state, tickProgress) -> 1.0F,
						wardenEntityModel,
						RenderLayers::entityTranslucentEmissive,
						false
				)
		);
		this.addFeature(
				new EmissiveFeatureRenderer<>(
						this,
						wardenEntityRenderState -> PULSATING_SPOTS_1_TEXTURE,
						(state, tickProgress) -> Math.max(0.0F, MathHelper.cos(tickProgress * 0.045F) * 0.25F),
						wardenEntityModel2,
						RenderLayers::entityTranslucentEmissive,
						false
				)
		);
		this.addFeature(
				new EmissiveFeatureRenderer<>(
						this,
						wardenEntityRenderState -> PULSATING_SPOTS_2_TEXTURE,
						(state, tickProgress) -> Math.max(
								0.0F,
								MathHelper.cos(tickProgress * 0.045F + (float) Math.PI) * 0.25F
						),
						wardenEntityModel2,
						RenderLayers::entityTranslucentEmissive,
						false
				)
		);
		this.addFeature(
				new EmissiveFeatureRenderer<>(
						this,
						wardenEntityRenderState -> TEXTURE,
						(state, tickProgress) -> state.tendrilAlpha,
						wardenEntityModel3,
						RenderLayers::entityTranslucentEmissive,
						false
				)
		);
		this.addFeature(
				new EmissiveFeatureRenderer<>(
						this,
						wardenEntityRenderState -> HEART_TEXTURE,
						(state, tickProgress) -> state.heartAlpha,
						wardenEntityModel4,
						RenderLayers::entityTranslucentEmissive,
						false
				)
		);
	}

	public Identifier getTexture(WardenEntityRenderState wardenEntityRenderState) {
		return TEXTURE;
	}

	public WardenEntityRenderState createRenderState() {
		return new WardenEntityRenderState();
	}

	public void updateRenderState(WardenEntity wardenEntity, WardenEntityRenderState wardenEntityRenderState, float f) {
		super.updateRenderState(wardenEntity, wardenEntityRenderState, f);
		wardenEntityRenderState.tendrilAlpha = wardenEntity.getTendrilAlpha(f);
		wardenEntityRenderState.heartAlpha = wardenEntity.getHeartAlpha(f);
		wardenEntityRenderState.roaringAnimationState.copyFrom(wardenEntity.roaringAnimationState);
		wardenEntityRenderState.sniffingAnimationState.copyFrom(wardenEntity.sniffingAnimationState);
		wardenEntityRenderState.emergingAnimationState.copyFrom(wardenEntity.emergingAnimationState);
		wardenEntityRenderState.diggingAnimationState.copyFrom(wardenEntity.diggingAnimationState);
		wardenEntityRenderState.attackingAnimationState.copyFrom(wardenEntity.attackingAnimationState);
		wardenEntityRenderState.chargingSonicBoomAnimationState.copyFrom(wardenEntity.chargingSonicBoomAnimationState);
	}
}
