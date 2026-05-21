package net.minecraft.client.render.entity;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.BabyModelPair;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.CowEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.CowVariant;
import net.minecraft.util.Identifier;

import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code CowEntityRenderer}.
 */
public class CowEntityRenderer extends MobEntityRenderer<CowEntity, CowEntityRenderState, CowEntityModel> {

	private final Map<CowVariant.Model, BabyModelPair<CowEntityModel>> babyModelPairMap;

	public CowEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new CowEntityModel(context.getPart(EntityModelLayers.COW)), 0.7F);
		this.babyModelPairMap = createBabyModelPairMap(context);
	}

	private static Map<CowVariant.Model, BabyModelPair<CowEntityModel>> createBabyModelPairMap(EntityRendererFactory.Context context) {
		return Maps.newEnumMap(
				Map.of(
						CowVariant.Model.NORMAL,
						new BabyModelPair<>(
								new CowEntityModel(context.getPart(EntityModelLayers.COW)),
								new CowEntityModel(context.getPart(EntityModelLayers.COW_BABY))
						),
						CowVariant.Model.WARM,
						new BabyModelPair<>(
								new CowEntityModel(context.getPart(EntityModelLayers.WARM_COW)),
								new CowEntityModel(context.getPart(EntityModelLayers.WARM_COW_BABY))
						),
						CowVariant.Model.COLD,
						new BabyModelPair<>(
								new CowEntityModel(context.getPart(EntityModelLayers.COLD_COW)),
								new CowEntityModel(context.getPart(EntityModelLayers.COLD_COW_BABY))
						)
				)
		);
	}

	public Identifier getTexture(CowEntityRenderState cowEntityRenderState) {
		return cowEntityRenderState.variant == null ? MissingSprite.getMissingSpriteId() : cowEntityRenderState.variant
		                                                                                   .modelAndTexture()
		                                                                                   .asset()
		                                                                                   .texturePath();
	}

	public CowEntityRenderState createRenderState() {
		return new CowEntityRenderState();
	}

	public void updateRenderState(CowEntity cowEntity, CowEntityRenderState cowEntityRenderState, float f) {
		super.updateRenderState(cowEntity, cowEntityRenderState, f);
		cowEntityRenderState.variant = cowEntity.getVariant().value();
	}

	public void render(
			CowEntityRenderState cowEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (cowEntityRenderState.variant != null) {
			this.model =
					this.babyModelPairMap
							.get(cowEntityRenderState.variant.modelAndTexture().model())
							.get(cowEntityRenderState.baby);
			super.render(cowEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
		}
	}
}
