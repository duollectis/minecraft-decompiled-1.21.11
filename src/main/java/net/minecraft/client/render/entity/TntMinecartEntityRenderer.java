package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.TntMinecartEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code TntMinecartEntityRenderer}.
 */
public class TntMinecartEntityRenderer extends AbstractMinecartEntityRenderer<TntMinecartEntity, TntMinecartEntityRenderState> {

	public TntMinecartEntityRenderer(EntityRendererFactory.Context context) {
		super(context, EntityModelLayers.TNT_MINECART);
	}

	protected void renderBlock(
			TntMinecartEntityRenderState tntMinecartEntityRenderState,
			BlockState blockState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i
	) {
		float f = tntMinecartEntityRenderState.fuseTicks;
		if (f > -1.0F && f < 10.0F) {
			float g = 1.0F - f / 10.0F;
			g = MathHelper.clamp(g, 0.0F, 1.0F);
			g *= g;
			g *= g;
			float h = 1.0F + g * 0.3F;
			matrixStack.scale(h, h, h);
		}

		renderFlashingBlock(
				blockState,
				matrixStack,
				orderedRenderCommandQueue,
				i,
				f > -1.0F && (int) f / 5 % 2 == 0,
				tntMinecartEntityRenderState.outlineColor
		);
	}

	public static void renderFlashingBlock(
			BlockState state,
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			boolean bl,
			int j
	) {
		int k;
		if (bl) {
			k = OverlayTexture.packUv(OverlayTexture.getU(1.0F), 10);
		}
		else {
			k = OverlayTexture.DEFAULT_UV;
		}

		orderedRenderCommandQueue.submitBlock(matrices, state, i, k, j);
	}

	public TntMinecartEntityRenderState createRenderState() {
		return new TntMinecartEntityRenderState();
	}

	public void updateRenderState(
			TntMinecartEntity tntMinecartEntity,
			TntMinecartEntityRenderState tntMinecartEntityRenderState,
			float f
	) {
		super.updateRenderState(tntMinecartEntity, tntMinecartEntityRenderState, f);
		tntMinecartEntityRenderState.fuseTicks =
				tntMinecartEntity.getFuseTicks() > -1 ? tntMinecartEntity.getFuseTicks() - f + 1.0F : -1.0F;
	}
}
