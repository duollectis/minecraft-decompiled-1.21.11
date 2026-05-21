package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.TurtleEntityModel;
import net.minecraft.client.render.entity.state.TurtleEntityRenderState;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code TurtleEntityRenderer}.
 */
public class TurtleEntityRenderer extends AgeableMobEntityRenderer<TurtleEntity, TurtleEntityRenderState, TurtleEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/turtle/big_sea_turtle.png");

	public TurtleEntityRenderer(EntityRendererFactory.Context context) {
		super(
				context,
				new TurtleEntityModel(context.getPart(EntityModelLayers.TURTLE)),
				new TurtleEntityModel(context.getPart(EntityModelLayers.TURTLE_BABY)),
				0.7F
		);
	}

	protected float getShadowRadius(TurtleEntityRenderState turtleEntityRenderState) {
		float f = super.getShadowRadius(turtleEntityRenderState);
		return turtleEntityRenderState.baby ? f * 0.83F : f;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return TurtleEntityRenderState — результат операции
	 */
	public TurtleEntityRenderState createRenderState() {
		return new TurtleEntityRenderState();
	}

	/**
	 * Обновляет render state.
	 *
	 * @param turtleEntity turtle entity
	 * @param turtleEntityRenderState turtle entity render state
	 * @param f f
	 */
	public void updateRenderState(TurtleEntity turtleEntity, TurtleEntityRenderState turtleEntityRenderState, float f) {
		super.updateRenderState(turtleEntity, turtleEntityRenderState, f);
		turtleEntityRenderState.onLand = !turtleEntity.isTouchingWater() && turtleEntity.isOnGround();
		turtleEntityRenderState.diggingSand = turtleEntity.isDiggingSand();
		turtleEntityRenderState.hasEgg = !turtleEntity.isBaby() && turtleEntity.hasEgg();
	}

	public Identifier getTexture(TurtleEntityRenderState turtleEntityRenderState) {
		return TEXTURE;
	}
}
