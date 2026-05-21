package net.minecraft.client.render.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
/**
 * {@code ShadowPiecesCommandRenderer}.
 */
public class ShadowPiecesCommandRenderer {

	private static final RenderLayer
			renderLayer =
			RenderLayers.entityShadow(Identifier.ofVanilla("textures/misc/shadow.png"));

	public void render(BatchingRenderCommandQueue queue, VertexConsumerProvider.Immediate vertexConsumers) {
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);

		for (OrderedRenderCommandQueueImpl.ShadowPiecesCommand shadowPiecesCommand : queue.getShadowPiecesCommands()) {
			for (EntityRenderState.ShadowPiece shadowPiece : shadowPiecesCommand.pieces()) {
				Box box = shadowPiece.shapeBelow().getBoundingBox();
				float f = shadowPiece.relativeX() + (float) box.minX;
				float g = shadowPiece.relativeX() + (float) box.maxX;
				float h = shadowPiece.relativeY() + (float) box.minY;
				float i = shadowPiece.relativeZ() + (float) box.minZ;
				float j = shadowPiece.relativeZ() + (float) box.maxZ;
				float k = shadowPiecesCommand.radius();
				float l = -f / 2.0F / k + 0.5F;
				float m = -g / 2.0F / k + 0.5F;
				float n = -i / 2.0F / k + 0.5F;
				float o = -j / 2.0F / k + 0.5F;
				int p = ColorHelper.getWhite(shadowPiece.alpha());
				vertex(shadowPiecesCommand.matricesEntry(), vertexConsumer, p, f, h, i, l, n);
				vertex(shadowPiecesCommand.matricesEntry(), vertexConsumer, p, f, h, j, l, o);
				vertex(shadowPiecesCommand.matricesEntry(), vertexConsumer, p, g, h, j, m, o);
				vertex(shadowPiecesCommand.matricesEntry(), vertexConsumer, p, g, h, i, m, n);
			}
		}
	}

	private static void vertex(
			Matrix4f matrix,
			VertexConsumer vertexConsumer,
			int color,
			float x,
			float y,
			float z,
			float u,
			float v
	) {
		Vector3f vector3f = matrix.transformPosition(x, y, z, new Vector3f());
		vertexConsumer.vertex(
				vector3f.x(),
				vector3f.y(),
				vector3f.z(),
				color,
				u,
				v,
				OverlayTexture.DEFAULT_UV,
				15728880,
				0.0F,
				1.0F,
				0.0F
		);
	}
}
