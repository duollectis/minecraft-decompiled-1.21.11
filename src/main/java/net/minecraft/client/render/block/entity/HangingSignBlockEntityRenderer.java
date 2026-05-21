package net.minecraft.client.render.block.entity;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Unit;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code HangingSignBlockEntityRenderer}.
 */
public class HangingSignBlockEntityRenderer extends AbstractSignBlockEntityRenderer {

	private static final String PLANK = "plank";
	private static final String V_CHAINS = "vChains";
	private static final String NORMAL_CHAINS = "normalChains";
	private static final String CHAIN_L1 = "chainL1";
	private static final String CHAIN_L2 = "chainL2";
	private static final String CHAIN_R1 = "chainR1";
	private static final String CHAIN_R2 = "chainR2";
	private static final String BOARD = "board";
	public static final float MODEL_SCALE = 1.0F;
	private static final float TEXT_SCALE = 0.9F;
	private static final Vec3d TEXT_OFFSET = new Vec3d(0.0, -0.32F, 0.073F);
	private final Map<HangingSignBlockEntityRenderer.Variant, Model.SinglePartModel> models;

	public HangingSignBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
		Stream<HangingSignBlockEntityRenderer.Variant> stream = WoodType.stream()
		                                                                .flatMap(
				                                                                woodType -> Arrays
						                                                                .stream(HangingSignBlockEntityRenderer.AttachmentType.values())
						                                                                .map(attachmentType -> new HangingSignBlockEntityRenderer.Variant(
								                                                                woodType,
								                                                                attachmentType
						                                                                ))
		                                                                );
		this.models = stream.collect(
				ImmutableMap.toImmutableMap(
						variant -> variant,
						variant -> createModel(context.loadedEntityModels(), variant.woodType, variant.attachmentType)
				)
		);
	}

	public static Model.SinglePartModel createModel(
			LoadedEntityModels models,
			WoodType woodType,
			HangingSignBlockEntityRenderer.AttachmentType attachmentType
	) {
		return new Model.SinglePartModel(
				models.getModelPart(EntityModelLayers.createHangingSign(
						woodType,
						attachmentType
				)), RenderLayers::entityCutoutNoCull
		);
	}

	@Override
	protected float getSignScale() {
		return 1.0F;
	}

	@Override
	protected float getTextScale() {
		return 0.9F;
	}

	public static void setAngles(MatrixStack matrices, float blockRotationDegrees) {
		matrices.translate(0.5, 0.9375, 0.5);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(blockRotationDegrees));
		matrices.translate(0.0F, -0.3125F, 0.0F);
	}

	@Override
	protected void applyTransforms(MatrixStack matrices, float blockRotationDegrees, BlockState state) {
		setAngles(matrices, blockRotationDegrees);
	}

	@Override
	protected Model.SinglePartModel getModel(BlockState state, WoodType woodType) {
		HangingSignBlockEntityRenderer.AttachmentType
				attachmentType =
				HangingSignBlockEntityRenderer.AttachmentType.from(state);
		return this.models.get(new HangingSignBlockEntityRenderer.Variant(woodType, attachmentType));
	}

	@Override
	protected SpriteIdentifier getTextureId(WoodType woodType) {
		return TexturedRenderLayers.getHangingSignTextureId(woodType);
	}

	@Override
	protected Vec3d getTextOffset() {
		return TEXT_OFFSET;
	}

	public static void renderAsItem(
			SpriteHolder spriteHolder,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			int j,
			Model.SinglePartModel singlePartModel,
			SpriteIdentifier spriteIdentifier
	) {
		matrixStack.push();
		setAngles(matrixStack, 0.0F);
		matrixStack.scale(1.0F, -1.0F, -1.0F);
		orderedRenderCommandQueue.submitModel(
				singlePartModel,
				Unit.INSTANCE,
				matrixStack,
				spriteIdentifier.getRenderLayer(singlePartModel::getLayer),
				i,
				j,
				-1,
				spriteHolder.getSprite(spriteIdentifier),
				OverlayTexture.DEFAULT_UV,
				null
		);
		matrixStack.pop();
	}

	public static TexturedModelData getTexturedModelData(HangingSignBlockEntityRenderer.AttachmentType attachmentType) {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"board",
				ModelPartBuilder.create().uv(0, 12).cuboid(-7.0F, 0.0F, -1.0F, 14.0F, 10.0F, 2.0F),
				ModelTransform.NONE
		);
		if (attachmentType == HangingSignBlockEntityRenderer.AttachmentType.WALL) {
			modelPartData.addChild(
					"plank",
					ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -6.0F, -2.0F, 16.0F, 2.0F, 4.0F),
					ModelTransform.NONE
			);
		}

		if (attachmentType == HangingSignBlockEntityRenderer.AttachmentType.WALL
				|| attachmentType == HangingSignBlockEntityRenderer.AttachmentType.CEILING) {
			ModelPartData
					modelPartData2 =
					modelPartData.addChild("normalChains", ModelPartBuilder.create(), ModelTransform.NONE);
			modelPartData2.addChild(
					"chainL1",
					ModelPartBuilder.create().uv(0, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
					ModelTransform.of(-5.0F, -6.0F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
			);
			modelPartData2.addChild(
					"chainL2",
					ModelPartBuilder.create().uv(6, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
					ModelTransform.of(-5.0F, -6.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
			);
			modelPartData2.addChild(
					"chainR1",
					ModelPartBuilder.create().uv(0, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
					ModelTransform.of(5.0F, -6.0F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
			);
			modelPartData2.addChild(
					"chainR2",
					ModelPartBuilder.create().uv(6, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
					ModelTransform.of(5.0F, -6.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
			);
		}

		if (attachmentType == HangingSignBlockEntityRenderer.AttachmentType.CEILING_MIDDLE) {
			modelPartData.addChild(
					"vChains",
					ModelPartBuilder.create().uv(14, 6).cuboid(-6.0F, -6.0F, 0.0F, 12.0F, 6.0F, 0.0F),
					ModelTransform.NONE
			);
		}

		return TexturedModelData.of(modelData, 64, 32);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code AttachmentType}.
	 */
	public static enum AttachmentType implements StringIdentifiable {
		WALL("wall"),
		CEILING("ceiling"),
		CEILING_MIDDLE("ceiling_middle");

		private final String id;

		private AttachmentType(final String id) {
			this.id = id;
		}

		public static HangingSignBlockEntityRenderer.AttachmentType from(BlockState state) {
			if (state.getBlock() instanceof HangingSignBlock) {
				return state.get(Properties.ATTACHED) ? CEILING_MIDDLE : CEILING;
			}
			else {
				return WALL;
			}
		}

		@Override
		public String asString() {
			return this.id;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Variant}.
	 */
	public record Variant(WoodType woodType, HangingSignBlockEntityRenderer.AttachmentType attachmentType) {
	}
}
