package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code HeadFeatureRenderer}.
 */
public class HeadFeatureRenderer<S extends LivingEntityRenderState, M extends EntityModel<S> & ModelWithHead> extends FeatureRenderer<S, M> {

	private static final float HEAD_SCALE = 0.625F;
	private static final float HEAD_Y_OFFSET = 1.1875F;
	private final HeadFeatureRenderer.HeadTransformation headTransformation;
	private final Function<SkullBlock.SkullType, SkullBlockEntityModel> headModels;
	private final PlayerSkinCache skinCache;

	public HeadFeatureRenderer(
			FeatureRendererContext<S, M> context,
			LoadedEntityModels models,
			PlayerSkinCache skinCache
	) {
		this(context, models, skinCache, HeadFeatureRenderer.HeadTransformation.DEFAULT);
	}

	public HeadFeatureRenderer(
			FeatureRendererContext<S, M> context,
			LoadedEntityModels models,
			PlayerSkinCache skinCache,
			HeadFeatureRenderer.HeadTransformation headTransformation
	) {
		super(context);
		this.headTransformation = headTransformation;
		this.headModels = Util.memoize(type -> SkullBlockEntityRenderer.getModels(models, type));
		this.skinCache = skinCache;
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			S livingEntityRenderState,
			float f,
			float g
	) {
		if (!livingEntityRenderState.headItemRenderState.isEmpty()
				|| livingEntityRenderState.wearingSkullType != null) {
			matrixStack.push();
			matrixStack.scale(
					this.headTransformation.horizontalScale(),
					1.0F,
					this.headTransformation.horizontalScale()
			);
			M entityModel = this.getContextModel();
			entityModel.getRootPart().applyTransform(matrixStack);
			entityModel.applyTransform(matrixStack);
			if (livingEntityRenderState.wearingSkullType != null) {
				matrixStack.translate(0.0F, this.headTransformation.skullYOffset(), 0.0F);
				matrixStack.scale(1.1875F, -1.1875F, -1.1875F);
				matrixStack.translate(-0.5, 0.0, -0.5);
				SkullBlock.SkullType skullType = livingEntityRenderState.wearingSkullType;
				SkullBlockEntityModel skullBlockEntityModel = this.headModels.apply(skullType);
				RenderLayer renderLayer = this.getRenderLayer(livingEntityRenderState, skullType);
				SkullBlockEntityRenderer.render(
						null,
						180.0F,
						livingEntityRenderState.headItemAnimationProgress,
						matrixStack,
						orderedRenderCommandQueue,
						i,
						skullBlockEntityModel,
						renderLayer,
						livingEntityRenderState.outlineColor,
						null
				);
			}
			else {
				translate(matrixStack, this.headTransformation);
				livingEntityRenderState.headItemRenderState
						.render(
								matrixStack,
								orderedRenderCommandQueue,
								i,
								OverlayTexture.DEFAULT_UV,
								livingEntityRenderState.outlineColor
						);
			}

			matrixStack.pop();
		}
	}

	private RenderLayer getRenderLayer(LivingEntityRenderState state, SkullBlock.SkullType skullType) {
		if (skullType == SkullBlock.Type.PLAYER) {
			ProfileComponent profileComponent = state.wearingSkullProfile;
			if (profileComponent != null) {
				return this.skinCache.get(profileComponent).getRenderLayer();
			}
		}

		return SkullBlockEntityRenderer.getCutoutRenderLayer(skullType, null);
	}

	/**
	 * Translate.
	 *
	 * @param matrices matrices
	 * @param transformation transformation
	 */
	public static void translate(MatrixStack matrices, HeadFeatureRenderer.HeadTransformation transformation) {
		matrices.translate(0.0F, -0.25F + transformation.yOffset(), 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		matrices.scale(0.625F, -0.625F, -0.625F);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code HeadTransformation}.
	 */
	public record HeadTransformation(float yOffset, float skullYOffset, float horizontalScale) {

		public static final HeadFeatureRenderer.HeadTransformation
				DEFAULT =
				new HeadFeatureRenderer.HeadTransformation(0.0F, 0.0F, 1.0F);
	}
}
