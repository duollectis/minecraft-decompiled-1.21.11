package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code CopperGolemHeadBlockFeatureRenderer}.
 */
public class CopperGolemHeadBlockFeatureRenderer<S extends EntityRenderState, M extends EntityModel<S>> extends FeatureRenderer<S, M> {

	private final Function<S, Optional<BlockState>> state;
	private final Consumer<MatrixStack> matrixTransformer;

	public CopperGolemHeadBlockFeatureRenderer(
			FeatureRendererContext<S, M> context,
			Function<S, Optional<BlockState>> state,
			Consumer<MatrixStack> matrixTransformer
	) {
		super(context);
		this.state = state;
		this.matrixTransformer = matrixTransformer;
	}

	@Override
	public void render(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			S state,
			float limbAngle,
			float limbDistance
	) {
		Optional<BlockState> optional = this.state.apply(state);
		if (!optional.isEmpty()) {
			BlockState blockState = optional.get();
			Block block = blockState.getBlock();
			boolean bl = block instanceof CopperGolemStatueBlock;
			matrices.push();
			this.matrixTransformer.accept(matrices);
			if (!bl) {
				matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
			}

			if (bl || block instanceof AbstractSkullBlock || block instanceof AbstractBannerBlock
					|| block instanceof AbstractChestBlock) {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
			}

			if (block instanceof FlowerbedBlock) {
				matrices.translate(-0.25, -1.5, -0.25);
			}
			else if (!bl) {
				matrices.translate(-0.5, -1.5, -0.5);
			}
			else {
				matrices.translate(-0.5, 0.0, -0.5);
			}

			queue.submitBlock(matrices, blockState, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
			matrices.pop();
		}
	}
}
