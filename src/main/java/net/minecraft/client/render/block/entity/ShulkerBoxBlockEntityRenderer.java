package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.state.ShulkerBoxBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code ShulkerBoxBlockEntityRenderer}.
 */
public class ShulkerBoxBlockEntityRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity, ShulkerBoxBlockEntityRenderState> {

	private final SpriteHolder materials;
	private final ShulkerBoxBlockEntityRenderer.ShulkerBoxBlockModel model;

	public ShulkerBoxBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
		this(ctx.loadedEntityModels(), ctx.spriteHolder());
	}

	public ShulkerBoxBlockEntityRenderer(SpecialModelRenderer.BakeContext context) {
		this(context.entityModelSet(), context.spriteHolder());
	}

	public ShulkerBoxBlockEntityRenderer(LoadedEntityModels models, SpriteHolder materials) {
		this.materials = materials;
		this.model =
				new ShulkerBoxBlockEntityRenderer.ShulkerBoxBlockModel(models.getModelPart(EntityModelLayers.SHULKER_BOX));
	}

	public ShulkerBoxBlockEntityRenderState createRenderState() {
		return new ShulkerBoxBlockEntityRenderState();
	}

	public void updateRenderState(
			ShulkerBoxBlockEntity shulkerBoxBlockEntity,
			ShulkerBoxBlockEntityRenderState shulkerBoxBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				shulkerBoxBlockEntity,
				shulkerBoxBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		shulkerBoxBlockEntityRenderState.facing =
				shulkerBoxBlockEntity.getCachedState().get(ShulkerBoxBlock.FACING, Direction.UP);
		shulkerBoxBlockEntityRenderState.dyeColor = shulkerBoxBlockEntity.getColor();
		shulkerBoxBlockEntityRenderState.animationProgress = shulkerBoxBlockEntity.getAnimationProgress(f);
	}

	public void render(
			ShulkerBoxBlockEntityRenderState shulkerBoxBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		DyeColor dyeColor = shulkerBoxBlockEntityRenderState.dyeColor;
		SpriteIdentifier spriteIdentifier;
		if (dyeColor == null) {
			spriteIdentifier = TexturedRenderLayers.SHULKER_TEXTURE_ID;
		}
		else {
			spriteIdentifier = TexturedRenderLayers.getShulkerBoxTextureId(dyeColor);
		}

		this.render(
				matrixStack,
				orderedRenderCommandQueue,
				shulkerBoxBlockEntityRenderState.lightmapCoordinates,
				OverlayTexture.DEFAULT_UV,
				shulkerBoxBlockEntityRenderState.facing,
				shulkerBoxBlockEntityRenderState.animationProgress,
				shulkerBoxBlockEntityRenderState.crumblingOverlay,
				spriteIdentifier,
				0
		);
	}

	public void render(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			Direction facing,
			float openness,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay,
			SpriteIdentifier spriteId,
			int i
	) {
		matrices.push();
		this.setTransforms(matrices, facing, openness);
		queue.submitModel(
				this.model,
				openness,
				matrices,
				spriteId.getRenderLayer(this.model::getLayer),
				light,
				overlay,
				-1,
				this.materials.getSprite(spriteId),
				i,
				crumblingOverlay
		);
		matrices.pop();
	}

	private void setTransforms(MatrixStack matrices, Direction facing, float openness) {
		matrices.translate(0.5F, 0.5F, 0.5F);
		float f = 0.9995F;
		matrices.scale(0.9995F, 0.9995F, 0.9995F);
		matrices.multiply(facing.getRotationQuaternion());
		matrices.scale(1.0F, -1.0F, -1.0F);
		matrices.translate(0.0F, -1.0F, 0.0F);
		this.model.setAngles(openness);
	}

	public void collectVertices(Direction facing, float openness, Consumer<Vector3fc> consumer) {
		MatrixStack matrixStack = new MatrixStack();
		this.setTransforms(matrixStack, facing, openness);
		this.model.getRootPart().collectVertices(matrixStack, consumer);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ShulkerBoxBlockModel}.
	 */
	static class ShulkerBoxBlockModel extends Model<Float> {

		private final ModelPart lid;

		public ShulkerBoxBlockModel(ModelPart root) {
			super(root, RenderLayers::entityCutoutNoCull);
			this.lid = root.getChild("lid");
		}

		public void setAngles(Float float_) {
			super.setAngles(float_);
			this.lid.setOrigin(0.0F, 24.0F - float_ * 0.5F * 16.0F, 0.0F);
			this.lid.yaw = 270.0F * float_ * (float) (Math.PI / 180.0);
		}
	}
}
