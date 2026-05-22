package net.minecraft.client.render.block.entity;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Unit;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code SignBlockEntityRenderer}.
 */
public class SignBlockEntityRenderer extends AbstractSignBlockEntityRenderer {

	public static final float SCALE = 0.6666667F;
	private static final Vec3d TEXT_OFFSET = new Vec3d(0.0, 0.33333334F, 0.046666667F);
	private final Map<WoodType, SignBlockEntityRenderer.SignModelPair> typeToModelPair;

	public SignBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
		super(ctx);
		this.typeToModelPair = WoodType.stream()
		                               .collect(
				                               ImmutableMap.toImmutableMap(
						                               signType -> signType,
						                               signType -> new SignBlockEntityRenderer.SignModelPair(
								                               createSignModel(
										                               ctx.loadedEntityModels(),
										                               signType,
										                               true
								                               ),
								                               createSignModel(
										                               ctx.loadedEntityModels(),
										                               signType,
										                               false
								                               )
						                               )
				                               )
		                               );
	}

	@Override
	protected Model.SinglePartModel getModel(BlockState state, WoodType woodType) {
		SignBlockEntityRenderer.SignModelPair signModelPair = this.typeToModelPair.get(woodType);
		return state.getBlock() instanceof SignBlock ? signModelPair.standing() : signModelPair.wall();
	}

	@Override
	protected SpriteIdentifier getTextureId(WoodType woodType) {
		return TexturedRenderLayers.getSignTextureId(woodType);
	}

	@Override
	protected float getSignScale() {
		return SCALE;
	}

	@Override
	protected float getTextScale() {
		return SCALE;
	}

	private static void setAngles(MatrixStack matrices, float blockRotationDegrees) {
		matrices.translate(0.5F, 0.5F, 0.5F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(blockRotationDegrees));
	}

	@Override
	protected void applyTransforms(MatrixStack matrices, float blockRotationDegrees, BlockState state) {
		setAngles(matrices, blockRotationDegrees);
		if (!(state.getBlock() instanceof SignBlock)) {
			matrices.translate(0.0F, -0.3125F, -0.4375F);
		}
	}

	@Override
	protected Vec3d getTextOffset() {
		return TEXT_OFFSET;
	}

	public static void renderAsItem(
			SpriteHolder spriteHolder,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			Model.SinglePartModel model,
			SpriteIdentifier texture
	) {
		matrices.push();
		setTransformsForItem(matrices);
		queue.submitModel(
				model,
				Unit.INSTANCE,
				matrices,
				texture.getRenderLayer(model::getLayer),
				light,
				overlay,
				-1,
				spriteHolder.getSprite(texture),
				0,
				null
		);
		matrices.pop();
	}

	public static void setTransformsForItem(MatrixStack matrices) {
		setAngles(matrices, 0.0F);
		matrices.scale(SCALE, -SCALE, -SCALE);
	}

	public static Model.SinglePartModel createSignModel(LoadedEntityModels models, WoodType type, boolean standing) {
		EntityModelLayer
				entityModelLayer =
				standing ? EntityModelLayers.createStandingSign(type) : EntityModelLayers.createWallSign(type);
		return new Model.SinglePartModel(models.getModelPart(entityModelLayer), RenderLayers::entityCutoutNoCull);
	}

	public static TexturedModelData getTexturedModelData(boolean standing) {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"sign",
				ModelPartBuilder.create().uv(0, 0).cuboid(-12.0F, -14.0F, -1.0F, 24.0F, 12.0F, 2.0F),
				ModelTransform.NONE
		);
		if (standing) {
			modelPartData.addChild(
					"stick",
					ModelPartBuilder.create().uv(0, 14).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 14.0F, 2.0F),
					ModelTransform.NONE
			);
		}

		return TexturedModelData.of(modelData, 64, 32);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SignModelPair}.
	 */
	record SignModelPair(Model.SinglePartModel standing, Model.SinglePartModel wall) {
	}
}
