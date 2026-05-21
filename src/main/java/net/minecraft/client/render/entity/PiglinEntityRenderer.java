package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.render.entity.model.PiglinEntityModel;
import net.minecraft.client.render.entity.state.PiglinEntityRenderState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code PiglinEntityRenderer}.
 */
public class PiglinEntityRenderer extends BipedEntityRenderer<AbstractPiglinEntity, PiglinEntityRenderState, PiglinEntityModel> {

	private static final Identifier PIGLIN_TEXTURE = Identifier.ofVanilla("textures/entity/piglin/piglin.png");
	private static final Identifier
			PIGLIN_BRUTE_TEXTURE =
			Identifier.ofVanilla("textures/entity/piglin/piglin_brute.png");
	public static final HeadFeatureRenderer.HeadTransformation
			HEAD_TRANSFORMATION =
			new HeadFeatureRenderer.HeadTransformation(0.0F, 0.0F, 1.0019531F);

	public PiglinEntityRenderer(
			EntityRendererFactory.Context ctx,
			EntityModelLayer mainLayer,
			EntityModelLayer babyMainLayer,
			EquipmentModelData<EntityModelLayer> equipmentModelData,
			EquipmentModelData<EntityModelLayer> equipmentModelData2
	) {
		super(
				ctx,
				new PiglinEntityModel(ctx.getPart(mainLayer)),
				new PiglinEntityModel(ctx.getPart(babyMainLayer)),
				0.5F,
				HEAD_TRANSFORMATION
		);
		this.addFeature(
				new ArmorFeatureRenderer<>(
						this,
						EquipmentModelData.mapToEntityModel(
								equipmentModelData,
								ctx.getEntityModels(),
								PiglinEntityModel::new
						),
						EquipmentModelData.mapToEntityModel(
								equipmentModelData2,
								ctx.getEntityModels(),
								PiglinEntityModel::new
						),
						ctx.getEquipmentRenderer()
				)
		);
	}

	public Identifier getTexture(PiglinEntityRenderState piglinEntityRenderState) {
		return piglinEntityRenderState.brute ? PIGLIN_BRUTE_TEXTURE : PIGLIN_TEXTURE;
	}

	public PiglinEntityRenderState createRenderState() {
		return new PiglinEntityRenderState();
	}

	public void updateRenderState(
			AbstractPiglinEntity abstractPiglinEntity,
			PiglinEntityRenderState piglinEntityRenderState,
			float f
	) {
		super.updateRenderState(abstractPiglinEntity, piglinEntityRenderState, f);
		piglinEntityRenderState.brute = abstractPiglinEntity.getType() == EntityType.PIGLIN_BRUTE;
		piglinEntityRenderState.activity = abstractPiglinEntity.getActivity();
		piglinEntityRenderState.piglinCrossbowPullTime =
				CrossbowItem.getPullTime(abstractPiglinEntity.getActiveItem(), abstractPiglinEntity);
		piglinEntityRenderState.shouldZombify = abstractPiglinEntity.shouldZombify();
	}

	protected boolean isShaking(PiglinEntityRenderState piglinEntityRenderState) {
		return super.isShaking(piglinEntityRenderState) || piglinEntityRenderState.shouldZombify;
	}
}
