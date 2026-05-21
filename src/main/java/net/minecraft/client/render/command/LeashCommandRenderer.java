package net.minecraft.client.render.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
/**
 * {@code LeashCommandRenderer}.
 */
public class LeashCommandRenderer {

	private static final int LEASH_SEGMENTS = 24;
	private static final float LEASH_WIDTH = 0.05F;

	public void render(BatchingRenderCommandQueue queue, VertexConsumerProvider.Immediate vertexConsumers) {
		for (OrderedRenderCommandQueueImpl.LeashCommand leashCommand : queue.getLeashCommands()) {
			render(leashCommand.matricesEntry(), vertexConsumers, leashCommand.leashState());
		}
	}

	private static void render(
			Matrix4f matrix,
			VertexConsumerProvider vertexConsumers,
			EntityRenderState.LeashData data
	) {
		float f = (float) (data.endPos.x - data.startPos.x);
		float g = (float) (data.endPos.y - data.startPos.y);
		float h = (float) (data.endPos.z - data.startPos.z);
		float i = MathHelper.inverseSqrt(f * f + h * h) * 0.05F / 2.0F;
		float j = h * i;
		float k = f * i;
		matrix.translate((float) data.offset.x, (float) data.offset.y, (float) data.offset.z);
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.leash());

		for (int l = 0; l <= 24; l++) {
			render(vertexConsumer, matrix, f, g, h, 0.05F, j, k, l, false, data);
		}

		for (int l = 24; l >= 0; l--) {
			render(vertexConsumer, matrix, f, g, h, 0.0F, j, k, l, true, data);
		}
	}

	private static void render(
			VertexConsumer vertexConsumer,
			Matrix4f matrix,
			float offsetX,
			float offsetY,
			float offsetZ,
			float yOffset,
			float sideOffset,
			float perpendicularOffset,
			int segmentIndex,
			boolean backside,
			EntityRenderState.LeashData data
	) {
		float f = segmentIndex / 24.0F;
		int i = (int) MathHelper.lerp(f, (float) data.leashedEntityBlockLight, (float) data.leashHolderBlockLight);
		int j = (int) MathHelper.lerp(f, (float) data.leashedEntitySkyLight, (float) data.leashHolderSkyLight);
		int k = LightmapTextureManager.pack(i, j);
		float g = segmentIndex % 2 == (backside ? 1 : 0) ? 0.7F : 1.0F;
		float h = 0.5F * g;
		float l = 0.4F * g;
		float m = 0.3F * g;
		float n = offsetX * f;
		float o;
		if (data.slack) {
			o = offsetY > 0.0F ? offsetY * f * f : offsetY - offsetY * (1.0F - f) * (1.0F - f);
		}
		else {
			o = offsetY * f;
		}

		float p = offsetZ * f;
		vertexConsumer
				.vertex(matrix, n - sideOffset, o + yOffset, p + perpendicularOffset)
				.color(h, l, m, 1.0F)
				.light(k);
		vertexConsumer
				.vertex(matrix, n + sideOffset, o + 0.05F - yOffset, p - perpendicularOffset)
				.color(h, l, m, 1.0F)
				.light(k);
	}
}
