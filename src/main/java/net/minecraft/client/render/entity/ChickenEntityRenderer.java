package net.minecraft.client.render.entity;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.BabyModelPair;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.ChickenEntityModel;
import net.minecraft.client.render.entity.model.ColdChickenEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.ChickenEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.ChickenVariant;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code ChickenEntityRenderer}.
 */
public class ChickenEntityRenderer extends MobEntityRenderer<ChickenEntity, ChickenEntityRenderState, ChickenEntityModel> {

	private final Map<ChickenVariant.Model, BabyModelPair<ChickenEntityModel>> babyModelPairMap;

	public ChickenEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new ChickenEntityModel(context.getPart(EntityModelLayers.CHICKEN)), 0.3F);
		this.babyModelPairMap = createBabyModelPairMap(context);
	}

	private static Map<ChickenVariant.Model, BabyModelPair<ChickenEntityModel>> createBabyModelPairMap(
			EntityRendererFactory.Context context
	) {
		return Maps.newEnumMap(
				Map.of(
						ChickenVariant.Model.NORMAL,
						new BabyModelPair<>(
								new ChickenEntityModel(context.getPart(EntityModelLayers.CHICKEN)),
								new ChickenEntityModel(context.getPart(EntityModelLayers.CHICKEN_BABY))
						),
						ChickenVariant.Model.COLD,
						new BabyModelPair<>(
								new ColdChickenEntityModel(context.getPart(EntityModelLayers.COLD_CHICKEN)),
								new ColdChickenEntityModel(context.getPart(EntityModelLayers.COLD_CHICKEN_BABY))
						)
				)
		);
	}

	public void render(
			ChickenEntityRenderState chickenEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (chickenEntityRenderState.variant != null) {
			this.model =
					this.babyModelPairMap
							.get(chickenEntityRenderState.variant.modelAndTexture().model())
							.get(chickenEntityRenderState.baby);
			super.render(chickenEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
		}
	}

	public Identifier getTexture(ChickenEntityRenderState chickenEntityRenderState) {
		return chickenEntityRenderState.variant == null
		       ? MissingSprite.getMissingSpriteId()
		       : chickenEntityRenderState.variant.modelAndTexture().asset().texturePath();
	}

	public ChickenEntityRenderState createRenderState() {
		return new ChickenEntityRenderState();
	}

	public void updateRenderState(
			ChickenEntity chickenEntity,
			ChickenEntityRenderState chickenEntityRenderState,
			float f
	) {
		super.updateRenderState(chickenEntity, chickenEntityRenderState, f);
		chickenEntityRenderState.flapProgress =
				MathHelper.lerp(f, chickenEntity.lastFlapProgress, chickenEntity.flapProgress);
		chickenEntityRenderState.maxWingDeviation =
				MathHelper.lerp(f, chickenEntity.lastMaxWingDeviation, chickenEntity.maxWingDeviation);
		chickenEntityRenderState.variant = chickenEntity.getVariant().value();
	}
}
