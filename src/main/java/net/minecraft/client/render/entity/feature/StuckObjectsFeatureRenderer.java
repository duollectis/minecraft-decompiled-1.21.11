package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code StuckObjectsFeatureRenderer}.
 */
public abstract class StuckObjectsFeatureRenderer<M extends PlayerEntityModel, S> extends FeatureRenderer<PlayerEntityRenderState, M> {

	private final Model<S> model;
	private final S stuckObjectState;
	private final Identifier texture;
	private final StuckObjectsFeatureRenderer.RenderPosition renderPosition;

	public StuckObjectsFeatureRenderer(
			LivingEntityRenderer<?, PlayerEntityRenderState, M> entityRenderer,
			Model<S> model,
			S stuckObjectState,
			Identifier texture,
			StuckObjectsFeatureRenderer.RenderPosition renderPosition
	) {
		super(entityRenderer);
		this.model = model;
		this.stuckObjectState = stuckObjectState;
		this.texture = texture;
		this.renderPosition = renderPosition;
	}

	protected abstract int getObjectCount(PlayerEntityRenderState playerRenderState);

	private void renderObject(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			float f,
			float directionX,
			float directionY,
			int color
	) {
		float g = MathHelper.sqrt(f * f + directionY * directionY);
		float h = (float) (Math.atan2(f, directionY) * 180.0F / (float) Math.PI);
		float i = (float) (Math.atan2(directionX, g) * 180.0F / (float) Math.PI);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(h - 90.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i));
		queue.submitModel(
				this.model,
				this.stuckObjectState,
				matrices,
				this.model.getLayer(this.texture),
				light,
				OverlayTexture.DEFAULT_UV,
				color,
				null
		);
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			PlayerEntityRenderState playerEntityRenderState,
			float f,
			float g
	) {
		int j = this.getObjectCount(playerEntityRenderState);
		if (j > 0) {
			Random random = Random.create(playerEntityRenderState.id);

			for (int k = 0; k < j; k++) {
				matrixStack.push();
				ModelPart modelPart = this.getContextModel().getRandomPart(random);
				ModelPart.Cuboid cuboid = modelPart.getRandomCuboid(random);
				modelPart.applyTransform(matrixStack);
				float h = random.nextFloat();
				float l = random.nextFloat();
				float m = random.nextFloat();
				if (this.renderPosition == StuckObjectsFeatureRenderer.RenderPosition.ON_SURFACE) {
					int n = random.nextInt(3);
					switch (n) {
						case 0:
							h = snapToSurfaceBoundary(h);
							break;
						case 1:
							l = snapToSurfaceBoundary(l);
							break;
						default:
							m = snapToSurfaceBoundary(m);
					}
				}

				matrixStack.translate(
						MathHelper.lerp(h, cuboid.minX, cuboid.maxX) / 16.0F,
						MathHelper.lerp(l, cuboid.minY, cuboid.maxY) / 16.0F,
						MathHelper.lerp(m, cuboid.minZ, cuboid.maxZ) / 16.0F
				);
				this.renderObject(
						matrixStack,
						orderedRenderCommandQueue,
						i,
						-(h * 2.0F - 1.0F),
						-(l * 2.0F - 1.0F),
						-(m * 2.0F - 1.0F),
						playerEntityRenderState.outlineColor
				);
				matrixStack.pop();
			}
		}
	}

	private static float snapToSurfaceBoundary(float f) {
		return f > 0.5F ? 1.0F : 0.5F;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code RenderPosition}.
	 */
	public static enum RenderPosition {
		IN_CUBE,
		ON_SURFACE;
	}
}
