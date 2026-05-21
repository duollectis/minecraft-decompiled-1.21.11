package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.PaintingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
/**
 * {@code PaintingEntityRenderer}.
 */
public class PaintingEntityRenderer extends EntityRenderer<PaintingEntity, PaintingEntityRenderState> {

	private static final Identifier BACK_TEXTURE_ID = Identifier.ofVanilla("back");
	private final SpriteAtlasTexture paintingAtlas;

	public PaintingEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.paintingAtlas = context.getSpriteAtlasTexture(Atlases.PAINTINGS);
	}

	public void render(
			PaintingEntityRenderState paintingEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		PaintingVariant paintingVariant = paintingEntityRenderState.variant;
		if (paintingVariant != null) {
			matrixStack.push();
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
					180 - paintingEntityRenderState.facing.getHorizontalQuarterTurns() * 90));
			Sprite sprite = this.paintingAtlas.getSprite(paintingVariant.assetId());
			Sprite sprite2 = this.paintingAtlas.getSprite(BACK_TEXTURE_ID);
			this.renderPainting(
					matrixStack,
					orderedRenderCommandQueue,
					RenderLayers.entitySolidZOffsetForward(sprite2.getAtlasId()),
					paintingEntityRenderState.lightmapCoordinates,
					paintingVariant.width(),
					paintingVariant.height(),
					sprite,
					sprite2
			);
			matrixStack.pop();
			super.render(paintingEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
		}
	}

	public PaintingEntityRenderState createRenderState() {
		return new PaintingEntityRenderState();
	}

	public void updateRenderState(
			PaintingEntity paintingEntity,
			PaintingEntityRenderState paintingEntityRenderState,
			float f
	) {
		super.updateRenderState(paintingEntity, paintingEntityRenderState, f);
		Direction direction = paintingEntity.getHorizontalFacing();
		PaintingVariant paintingVariant = paintingEntity.getVariant().value();
		paintingEntityRenderState.facing = direction;
		paintingEntityRenderState.variant = paintingVariant;
		int i = paintingVariant.width();
		int j = paintingVariant.height();
		if (paintingEntityRenderState.lightmapCoordinates.length != i * j) {
			paintingEntityRenderState.lightmapCoordinates = new int[i * j];
		}

		float g = -i / 2.0F;
		float h = -j / 2.0F;
		World world = paintingEntity.getEntityWorld();

		for (int k = 0; k < j; k++) {
			for (int l = 0; l < i; l++) {
				float m = l + g + 0.5F;
				float n = k + h + 0.5F;
				int o = paintingEntity.getBlockX();
				int p = MathHelper.floor(paintingEntity.getY() + n);
				int q = paintingEntity.getBlockZ();
				switch (direction) {
					case NORTH:
						o = MathHelper.floor(paintingEntity.getX() + m);
						break;
					case WEST:
						q = MathHelper.floor(paintingEntity.getZ() - m);
						break;
					case SOUTH:
						o = MathHelper.floor(paintingEntity.getX() - m);
						break;
					case EAST:
						q = MathHelper.floor(paintingEntity.getZ() + m);
				}

				paintingEntityRenderState.lightmapCoordinates[l + k * i] =
						WorldRenderer.getLightmapCoordinates(world, new BlockPos(o, p, q));
			}
		}
	}

	private void renderPainting(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			RenderLayer renderLayer,
			int[] is,
			int i,
			int j,
			Sprite sprite,
			Sprite sprite2
	) {
		orderedRenderCommandQueue.submitCustom(
				matrixStack, renderLayer, (entry, vertexConsumer) -> {
					float f = -i / 2.0F;
					float g = -j / 2.0F;
					float h = 0.03125F;
					float k = sprite2.getMinU();
					float l = sprite2.getMaxU();
					float m = sprite2.getMinV();
					float n = sprite2.getMaxV();
					float o = sprite2.getMinU();
					float p = sprite2.getMaxU();
					float q = sprite2.getMinV();
					float r = sprite2.getFrameV(0.0625F);
					float s = sprite2.getMinU();
					float t = sprite2.getFrameU(0.0625F);
					float u = sprite2.getMinV();
					float v = sprite2.getMaxV();
					double d = 1.0 / i;
					double e = 1.0 / j;

					for (int w = 0; w < i; w++) {
						for (int x = 0; x < j; x++) {
							float y = f + (w + 1);
							float z = f + w;
							float aa = g + (x + 1);
							float ab = g + x;
							int ac = is[w + x * i];
							float ad = sprite.getFrameU((float) (d * (i - w)));
							float ae = sprite.getFrameU((float) (d * (i - (w + 1))));
							float af = sprite.getFrameV((float) (e * (j - x)));
							float ag = sprite.getFrameV((float) (e * (j - (x + 1))));
							this.vertex(entry, vertexConsumer, y, ab, ae, af, -0.03125F, 0, 0, -1, ac);
							this.vertex(entry, vertexConsumer, z, ab, ad, af, -0.03125F, 0, 0, -1, ac);
							this.vertex(entry, vertexConsumer, z, aa, ad, ag, -0.03125F, 0, 0, -1, ac);
							this.vertex(entry, vertexConsumer, y, aa, ae, ag, -0.03125F, 0, 0, -1, ac);
							this.vertex(entry, vertexConsumer, y, aa, l, m, 0.03125F, 0, 0, 1, ac);
							this.vertex(entry, vertexConsumer, z, aa, k, m, 0.03125F, 0, 0, 1, ac);
							this.vertex(entry, vertexConsumer, z, ab, k, n, 0.03125F, 0, 0, 1, ac);
							this.vertex(entry, vertexConsumer, y, ab, l, n, 0.03125F, 0, 0, 1, ac);
							this.vertex(entry, vertexConsumer, y, aa, o, q, -0.03125F, 0, 1, 0, ac);
							this.vertex(entry, vertexConsumer, z, aa, p, q, -0.03125F, 0, 1, 0, ac);
							this.vertex(entry, vertexConsumer, z, aa, p, r, 0.03125F, 0, 1, 0, ac);
							this.vertex(entry, vertexConsumer, y, aa, o, r, 0.03125F, 0, 1, 0, ac);
							this.vertex(entry, vertexConsumer, y, ab, o, q, 0.03125F, 0, -1, 0, ac);
							this.vertex(entry, vertexConsumer, z, ab, p, q, 0.03125F, 0, -1, 0, ac);
							this.vertex(entry, vertexConsumer, z, ab, p, r, -0.03125F, 0, -1, 0, ac);
							this.vertex(entry, vertexConsumer, y, ab, o, r, -0.03125F, 0, -1, 0, ac);
							this.vertex(entry, vertexConsumer, y, aa, t, u, 0.03125F, -1, 0, 0, ac);
							this.vertex(entry, vertexConsumer, y, ab, t, v, 0.03125F, -1, 0, 0, ac);
							this.vertex(entry, vertexConsumer, y, ab, s, v, -0.03125F, -1, 0, 0, ac);
							this.vertex(entry, vertexConsumer, y, aa, s, u, -0.03125F, -1, 0, 0, ac);
							this.vertex(entry, vertexConsumer, z, aa, t, u, -0.03125F, 1, 0, 0, ac);
							this.vertex(entry, vertexConsumer, z, ab, t, v, -0.03125F, 1, 0, 0, ac);
							this.vertex(entry, vertexConsumer, z, ab, s, v, 0.03125F, 1, 0, 0, ac);
							this.vertex(entry, vertexConsumer, z, aa, s, u, 0.03125F, 1, 0, 0, ac);
						}
					}
				}
		);
	}

	private void vertex(
			MatrixStack.Entry matrix,
			VertexConsumer vertexConsumer,
			float x,
			float y,
			float u,
			float v,
			float z,
			int normalX,
			int normalY,
			int normalZ,
			int light
	) {
		vertexConsumer
				.vertex(matrix, x, y, z)
				.color(-1)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(matrix, normalX, normalY, normalZ);
	}
}
