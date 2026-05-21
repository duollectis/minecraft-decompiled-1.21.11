package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.LightningEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
/**
 * {@code LightningEntityRenderer}.
 */
public class LightningEntityRenderer extends EntityRenderer<LightningEntity, LightningEntityRenderState> {

	public LightningEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	public void render(
			LightningEntityRenderState lightningEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		float[] fs = new float[8];
		float[] gs = new float[8];
		float f = 0.0F;
		float g = 0.0F;
		Random random = Random.create(lightningEntityRenderState.seed);

		for (int i = 7; i >= 0; i--) {
			fs[i] = f;
			gs[i] = g;
			f += random.nextInt(11) - 5;
			g += random.nextInt(11) - 5;
		}

		float h = f;
		float j = g;
		orderedRenderCommandQueue.submitCustom(
				matrixStack, RenderLayers.lightning(), (entry, vertexConsumer) -> {
					Matrix4f matrix4f = entry.getPositionMatrix();

					for (int i = 0; i < 4; i++) {
						Random randomx = Random.create(lightningEntityRenderState.seed);

						for (int jx = 0; jx < 3; jx++) {
							int k = 7;
							int l = 0;
							if (jx > 0) {
								k = 7 - jx;
							}

							if (jx > 0) {
								l = k - 2;
							}

							float hx = fs[k] - h;
							float m = gs[k] - j;

							for (int n = k; n >= l; n--) {
								float o = hx;
								float p = m;
								if (jx == 0) {
									hx += randomx.nextInt(11) - 5;
									m += randomx.nextInt(11) - 5;
								}
								else {
									hx += randomx.nextInt(31) - 15;
									m += randomx.nextInt(31) - 15;
								}

								float q = 0.5F;
								float r = 0.45F;
								float s = 0.45F;
								float t = 0.5F;
								float u = 0.1F + i * 0.2F;
								if (jx == 0) {
									u *= n * 0.1F + 1.0F;
								}

								float v = 0.1F + i * 0.2F;
								if (jx == 0) {
									v *= (n - 1.0F) * 0.1F + 1.0F;
								}

								drawBranch(
										matrix4f,
										vertexConsumer,
										hx,
										m,
										n,
										o,
										p,
										0.45F,
										0.45F,
										0.5F,
										u,
										v,
										false,
										false,
										true,
										false
								);
								drawBranch(
										matrix4f,
										vertexConsumer,
										hx,
										m,
										n,
										o,
										p,
										0.45F,
										0.45F,
										0.5F,
										u,
										v,
										true,
										false,
										true,
										true
								);
								drawBranch(
										matrix4f,
										vertexConsumer,
										hx,
										m,
										n,
										o,
										p,
										0.45F,
										0.45F,
										0.5F,
										u,
										v,
										true,
										true,
										false,
										true
								);
								drawBranch(
										matrix4f,
										vertexConsumer,
										hx,
										m,
										n,
										o,
										p,
										0.45F,
										0.45F,
										0.5F,
										u,
										v,
										false,
										true,
										false,
										false
								);
							}
						}
					}
				}
		);
	}

	private static void drawBranch(
			Matrix4f matrix,
			VertexConsumer buffer,
			float x1,
			float z1,
			int y,
			float x2,
			float z2,
			float red,
			float green,
			float blue,
			float offset2,
			float offset1,
			boolean shiftEast1,
			boolean shiftSouth1,
			boolean shiftEast2,
			boolean shiftSouth2
	) {
		buffer
				.vertex(
						matrix,
						x1 + (shiftEast1 ? offset1 : -offset1),
						(float) (y * 16),
						z1 + (shiftSouth1 ? offset1 : -offset1)
				)
				.color(red, green, blue, 0.3F);
		buffer
				.vertex(
						matrix,
						x2 + (shiftEast1 ? offset2 : -offset2),
						(float) ((y + 1) * 16),
						z2 + (shiftSouth1 ? offset2 : -offset2)
				)
				.color(red, green, blue, 0.3F);
		buffer
				.vertex(
						matrix,
						x2 + (shiftEast2 ? offset2 : -offset2),
						(float) ((y + 1) * 16),
						z2 + (shiftSouth2 ? offset2 : -offset2)
				)
				.color(red, green, blue, 0.3F);
		buffer
				.vertex(
						matrix,
						x1 + (shiftEast2 ? offset1 : -offset1),
						(float) (y * 16),
						z1 + (shiftSouth2 ? offset1 : -offset1)
				)
				.color(red, green, blue, 0.3F);
	}

	public LightningEntityRenderState createRenderState() {
		return new LightningEntityRenderState();
	}

	public void updateRenderState(
			LightningEntity lightningEntity,
			LightningEntityRenderState lightningEntityRenderState,
			float f
	) {
		super.updateRenderState(lightningEntity, lightningEntityRenderState, f);
		lightningEntityRenderState.seed = lightningEntity.seed;
	}

	protected boolean canBeCulled(LightningEntity lightningEntity) {
		return false;
	}
}
