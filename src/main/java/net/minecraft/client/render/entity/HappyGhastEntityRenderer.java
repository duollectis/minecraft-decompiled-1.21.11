package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.HappyGhastRopesFeatureRenderer;
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.HappyGhastEntityModel;
import net.minecraft.client.render.entity.model.HappyGhastHarnessEntityModel;
import net.minecraft.client.render.entity.state.HappyGhastEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
/**
 * {@code HappyGhastEntityRenderer}.
 */
public class HappyGhastEntityRenderer extends AgeableMobEntityRenderer<HappyGhastEntity, HappyGhastEntityRenderState, HappyGhastEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/ghast/happy_ghast.png");
	private static final Identifier BABY_TEXTURE = Identifier.ofVanilla("textures/entity/ghast/happy_ghast_baby.png");
	private static final Identifier ROPES_TEXTURE = Identifier.ofVanilla("textures/entity/ghast/happy_ghast_ropes.png");

	public HappyGhastEntityRenderer(EntityRendererFactory.Context context) {
		super(
				context,
				new HappyGhastEntityModel(context.getPart(EntityModelLayers.HAPPY_GHAST)),
				new HappyGhastEntityModel(context.getPart(EntityModelLayers.HAPPY_GHAST_BABY)),
				2.0F
		);
		this.addFeature(
				new SaddleFeatureRenderer<>(
						this,
						context.getEquipmentRenderer(),
						EquipmentModel.LayerType.HAPPY_GHAST_BODY,
						state -> state.harnessStack,
						new HappyGhastHarnessEntityModel(context.getPart(EntityModelLayers.HAPPY_GHAST_HARNESS)),
						new HappyGhastHarnessEntityModel(context.getPart(EntityModelLayers.HAPPY_GHAST_BABY_HARNESS))
				)
		);
		this.addFeature(new HappyGhastRopesFeatureRenderer<>(this, context.getEntityModels(), ROPES_TEXTURE));
	}

	public Identifier getTexture(HappyGhastEntityRenderState happyGhastEntityRenderState) {
		return happyGhastEntityRenderState.baby ? BABY_TEXTURE : TEXTURE;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return HappyGhastEntityRenderState — результат операции
	 */
	public HappyGhastEntityRenderState createRenderState() {
		return new HappyGhastEntityRenderState();
	}

	protected Box getBoundingBox(HappyGhastEntity happyGhastEntity) {
		Box box = super.getBoundingBox(happyGhastEntity);
		float f = happyGhastEntity.getHeight();
		return box.withMinY(box.minY - f / 2.0F);
	}

	public void updateRenderState(
			HappyGhastEntity happyGhastEntity,
			HappyGhastEntityRenderState happyGhastEntityRenderState,
			float f
	) {
		super.updateRenderState(happyGhastEntity, happyGhastEntityRenderState, f);
		happyGhastEntityRenderState.harnessStack = happyGhastEntity.getEquippedStack(EquipmentSlot.BODY).copy();
		happyGhastEntityRenderState.hasPassengers = happyGhastEntity.hasPassengers();
		happyGhastEntityRenderState.hasRopes = happyGhastEntity.hasRopes();
	}
}
