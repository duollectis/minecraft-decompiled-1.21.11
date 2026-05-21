package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.BatEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.BatEntityRenderState;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code BatEntityRenderer}.
 */
public class BatEntityRenderer extends MobEntityRenderer<BatEntity, BatEntityRenderState, BatEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/bat.png");

	public BatEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new BatEntityModel(context.getPart(EntityModelLayers.BAT)), 0.25F);
	}

	public Identifier getTexture(BatEntityRenderState batEntityRenderState) {
		return TEXTURE;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return BatEntityRenderState — результат операции
	 */
	public BatEntityRenderState createRenderState() {
		return new BatEntityRenderState();
	}

	/**
	 * Обновляет render state.
	 *
	 * @param batEntity bat entity
	 * @param batEntityRenderState bat entity render state
	 * @param f f
	 */
	public void updateRenderState(BatEntity batEntity, BatEntityRenderState batEntityRenderState, float f) {
		super.updateRenderState(batEntity, batEntityRenderState, f);
		batEntityRenderState.roosting = batEntity.isRoosting();
		batEntityRenderState.flyingAnimationState.copyFrom(batEntity.flyingAnimationState);
		batEntityRenderState.roostingAnimationState.copyFrom(batEntity.roostingAnimationState);
	}
}
