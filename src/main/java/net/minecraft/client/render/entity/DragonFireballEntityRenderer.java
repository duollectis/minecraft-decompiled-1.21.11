package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
/**
 * {@code DragonFireballEntityRenderer}.
 */
public class DragonFireballEntityRenderer extends EntityRenderer<DragonFireballEntity, EntityRenderState> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/enderdragon/dragon_fireball.png");
	private static final RenderLayer LAYER = RenderLayers.entityCutoutNoCull(TEXTURE);

	public DragonFireballEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	protected int getBlockLight(DragonFireballEntity dragonFireballEntity, BlockPos blockPos) {
		return 15;
	}

	@Override
	public void render(
			EntityRenderState renderState,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			CameraRenderState cameraState
	) {
		matrices.push();
		matrices.scale(2.0F, 2.0F, 2.0F);
		matrices.multiply(cameraState.orientation);
		queue.submitCustom(
				matrices, LAYER, (entry, vertexConsumer) -> {
					produceVertex(vertexConsumer, entry, renderState.light, 0.0F, 0, 0, 1);
					produceVertex(vertexConsumer, entry, renderState.light, 1.0F, 0, 1, 1);
					produceVertex(vertexConsumer, entry, renderState.light, 1.0F, 1, 1, 0);
					produceVertex(vertexConsumer, entry, renderState.light, 0.0F, 1, 0, 0);
				}
		);
		matrices.pop();
		super.render(renderState, matrices, queue, cameraState);
	}

	private static void produceVertex(
			VertexConsumer vertexConsumer,
			MatrixStack.Entry matrix,
			int light,
			float x,
			int z,
			int textureU,
			int textureV
	) {
		vertexConsumer.vertex(matrix, x - 0.5F, z - 0.25F, 0.0F)
		              .color(-1)
		              .texture(textureU, textureV)
		              .overlay(OverlayTexture.DEFAULT_UV)
		              .light(light)
		              .normal(matrix, 0.0F, 1.0F, 0.0F);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}
