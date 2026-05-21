package net.minecraft.client.render.entity;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.BabyModelPair;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer;
import net.minecraft.client.render.entity.model.ColdPigEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PigEntityModel;
import net.minecraft.client.render.entity.state.PigEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PigVariant;
import net.minecraft.util.Identifier;

import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code PigEntityRenderer}.
 */
public class PigEntityRenderer extends MobEntityRenderer<PigEntity, PigEntityRenderState, PigEntityModel> {

	private final Map<PigVariant.Model, BabyModelPair<PigEntityModel>> modelPairs;

	public PigEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new PigEntityModel(context.getPart(EntityModelLayers.PIG)), 0.7F);
		this.modelPairs = createModelPairs(context);
		this.addFeature(
				new SaddleFeatureRenderer<>(
						this,
						context.getEquipmentRenderer(),
						EquipmentModel.LayerType.PIG_SADDLE,
						state -> state.saddleStack,
						new PigEntityModel(context.getPart(EntityModelLayers.PIG_SADDLE)),
						new PigEntityModel(context.getPart(EntityModelLayers.PIG_BABY_SADDLE))
				)
		);
	}

	private static Map<PigVariant.Model, BabyModelPair<PigEntityModel>> createModelPairs(EntityRendererFactory.Context context) {
		return Maps.newEnumMap(
				Map.of(
						PigVariant.Model.NORMAL,
						new BabyModelPair<>(
								new PigEntityModel(context.getPart(EntityModelLayers.PIG)),
								new PigEntityModel(context.getPart(EntityModelLayers.PIG_BABY))
						),
						PigVariant.Model.COLD,
						new BabyModelPair<>(
								new ColdPigEntityModel(context.getPart(EntityModelLayers.COLD_PIG)),
								new ColdPigEntityModel(context.getPart(EntityModelLayers.COLD_PIG_BABY))
						)
				)
		);
	}

	public void render(
			PigEntityRenderState pigEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (pigEntityRenderState.variant != null) {
			this.model =
					this.modelPairs
							.get(pigEntityRenderState.variant.modelAndTexture().model())
							.get(pigEntityRenderState.baby);
			super.render(pigEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
		}
	}

	public Identifier getTexture(PigEntityRenderState pigEntityRenderState) {
		return pigEntityRenderState.variant == null ? MissingSprite.getMissingSpriteId() : pigEntityRenderState.variant
		                                                                                   .modelAndTexture()
		                                                                                   .asset()
		                                                                                   .texturePath();
	}

	/**
	 * Создаёт render state.
	 *
	 * @return PigEntityRenderState — результат операции
	 */
	public PigEntityRenderState createRenderState() {
		return new PigEntityRenderState();
	}

	/**
	 * Обновляет render state.
	 *
	 * @param pigEntity pig entity
	 * @param pigEntityRenderState pig entity render state
	 * @param f f
	 */
	public void updateRenderState(PigEntity pigEntity, PigEntityRenderState pigEntityRenderState, float f) {
		super.updateRenderState(pigEntity, pigEntityRenderState, f);
		pigEntityRenderState.saddleStack = pigEntity.getEquippedStack(EquipmentSlot.SADDLE).copy();
		pigEntityRenderState.variant = pigEntity.getVariant().value();
	}
}
