package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DecoratedPotPatterns;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.Sherds;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.state.DecoratedPotBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code DecoratedPotBlockEntityRenderer}.
 */
public class DecoratedPotBlockEntityRenderer implements BlockEntityRenderer<DecoratedPotBlockEntity, DecoratedPotBlockEntityRenderState> {

	private final SpriteHolder materials;
	private static final String NECK = "neck";
	private static final String FRONT = "front";
	private static final String BACK = "back";
	private static final String LEFT = "left";
	private static final String RIGHT = "right";
	private static final String TOP = "top";
	private static final String BOTTOM = "bottom";
	private final ModelPart neck;
	private final ModelPart front;
	private final ModelPart back;
	private final ModelPart left;
	private final ModelPart right;
	private final ModelPart top;
	private final ModelPart bottom;
	private static final float POT_SCALE = 0.125F;

	public DecoratedPotBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		this(context.loadedEntityModels(), context.spriteHolder());
	}

	public DecoratedPotBlockEntityRenderer(SpecialModelRenderer.BakeContext context) {
		this(context.entityModelSet(), context.spriteHolder());
	}

	public DecoratedPotBlockEntityRenderer(LoadedEntityModels entityModelSet, SpriteHolder materials) {
		this.materials = materials;
		ModelPart modelPart = entityModelSet.getModelPart(EntityModelLayers.DECORATED_POT_BASE);
		this.neck = modelPart.getChild("neck");
		this.top = modelPart.getChild("top");
		this.bottom = modelPart.getChild("bottom");
		ModelPart modelPart2 = entityModelSet.getModelPart(EntityModelLayers.DECORATED_POT_SIDES);
		this.front = modelPart2.getChild("front");
		this.back = modelPart2.getChild("back");
		this.left = modelPart2.getChild("left");
		this.right = modelPart2.getChild("right");
	}

	public static TexturedModelData getTopBottomNeckTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		Dilation dilation = new Dilation(0.2F);
		Dilation dilation2 = new Dilation(-0.1F);
		modelPartData.addChild(
				"neck",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(4.0F, 17.0F, 4.0F, 8.0F, 3.0F, 8.0F, dilation2)
				                .uv(0, 5)
				                .cuboid(5.0F, 20.0F, 5.0F, 6.0F, 1.0F, 6.0F, dilation),
				ModelTransform.of(0.0F, 37.0F, 16.0F, (float) Math.PI, 0.0F, 0.0F)
		);
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(-14, 13).cuboid(0.0F, 0.0F, 0.0F, 14.0F, 0.0F, 14.0F);
		modelPartData.addChild("top", modelPartBuilder, ModelTransform.of(1.0F, 16.0F, 1.0F, 0.0F, 0.0F, 0.0F));
		modelPartData.addChild("bottom", modelPartBuilder, ModelTransform.of(1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F));
		return TexturedModelData.of(modelData, 32, 32);
	}

	public static TexturedModelData getSidesTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder
						.create()
						.uv(1, 0)
						.cuboid(0.0F, 0.0F, 0.0F, 14.0F, 16.0F, 0.0F, EnumSet.of(Direction.NORTH));
		modelPartData.addChild(
				"back",
				modelPartBuilder,
				ModelTransform.of(15.0F, 16.0F, 1.0F, 0.0F, 0.0F, (float) Math.PI)
		);
		modelPartData.addChild(
				"left",
				modelPartBuilder,
				ModelTransform.of(1.0F, 16.0F, 1.0F, 0.0F, (float) (-Math.PI / 2), (float) Math.PI)
		);
		modelPartData.addChild(
				"right",
				modelPartBuilder,
				ModelTransform.of(15.0F, 16.0F, 15.0F, 0.0F, (float) (Math.PI / 2), (float) Math.PI)
		);
		modelPartData.addChild(
				"front",
				modelPartBuilder,
				ModelTransform.of(1.0F, 16.0F, 15.0F, (float) Math.PI, 0.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 16, 16);
	}

	private static SpriteIdentifier getTextureIdFromSherd(Optional<Item> sherd) {
		if (sherd.isPresent()) {
			SpriteIdentifier
					spriteIdentifier =
					TexturedRenderLayers.getDecoratedPotPatternTextureId(DecoratedPotPatterns.fromSherd(sherd.get()));
			if (spriteIdentifier != null) {
				return spriteIdentifier;
			}
		}

		return TexturedRenderLayers.DECORATED_POT_SIDE;
	}

	public DecoratedPotBlockEntityRenderState createRenderState() {
		return new DecoratedPotBlockEntityRenderState();
	}

	public void updateRenderState(
			DecoratedPotBlockEntity decoratedPotBlockEntity,
			DecoratedPotBlockEntityRenderState decoratedPotBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				decoratedPotBlockEntity,
				decoratedPotBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		decoratedPotBlockEntityRenderState.sherds = decoratedPotBlockEntity.getSherds();
		decoratedPotBlockEntityRenderState.facing = decoratedPotBlockEntity.getHorizontalFacing();
		DecoratedPotBlockEntity.WobbleType wobbleType = decoratedPotBlockEntity.lastWobbleType;
		if (wobbleType != null && decoratedPotBlockEntity.getWorld() != null) {
			decoratedPotBlockEntityRenderState.wobbleAnimationProgress = (
					(float) (decoratedPotBlockEntity.getWorld().getTime() - decoratedPotBlockEntity.lastWobbleTime) + f
			)
					/ wobbleType.lengthInTicks;
		}
		else {
			decoratedPotBlockEntityRenderState.wobbleAnimationProgress = 0.0F;
		}
	}

	public void render(
			DecoratedPotBlockEntityRenderState decoratedPotBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.push();
		Direction direction = decoratedPotBlockEntityRenderState.facing;
		matrixStack.translate(0.5, 0.0, 0.5);
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
				180.0F - direction.getPositiveHorizontalDegrees()));
		matrixStack.translate(-0.5, 0.0, -0.5);
		if (decoratedPotBlockEntityRenderState.wobbleAnimationProgress >= 0.0F
				&& decoratedPotBlockEntityRenderState.wobbleAnimationProgress <= 1.0F) {
			if (decoratedPotBlockEntityRenderState.wobbleType == DecoratedPotBlockEntity.WobbleType.POSITIVE) {
				float f = 0.015625F;
				float g = decoratedPotBlockEntityRenderState.wobbleAnimationProgress * (float) (Math.PI * 2);
				float h = -1.5F * (MathHelper.cos(g) + 0.5F) * MathHelper.sin(g / 2.0F);
				matrixStack.multiply(RotationAxis.POSITIVE_X.rotation(h * 0.015625F), 0.5F, 0.0F, 0.5F);
				float i = MathHelper.sin(g);
				matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(i * 0.015625F), 0.5F, 0.0F, 0.5F);
			}
			else {
				float
						f =
						MathHelper.sin(
								-decoratedPotBlockEntityRenderState.wobbleAnimationProgress * 3.0F * (float) Math.PI)
								* 0.125F;
				float g = 1.0F - decoratedPotBlockEntityRenderState.wobbleAnimationProgress;
				matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(f * g), 0.5F, 0.0F, 0.5F);
			}
		}

		this.render(
				matrixStack,
				orderedRenderCommandQueue,
				decoratedPotBlockEntityRenderState.lightmapCoordinates,
				OverlayTexture.DEFAULT_UV,
				decoratedPotBlockEntityRenderState.sherds,
				0
		);
		matrixStack.pop();
	}

	public void render(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			Sherds sherds,
			int i
	) {
		RenderLayer renderLayer = TexturedRenderLayers.DECORATED_POT_BASE.getRenderLayer(RenderLayers::entitySolid);
		Sprite sprite = this.materials.getSprite(TexturedRenderLayers.DECORATED_POT_BASE);
		queue.submitModelPart(this.neck, matrices, renderLayer, light, overlay, sprite, false, false, -1, null, i);
		queue.submitModelPart(this.top, matrices, renderLayer, light, overlay, sprite, false, false, -1, null, i);
		queue.submitModelPart(this.bottom, matrices, renderLayer, light, overlay, sprite, false, false, -1, null, i);
		SpriteIdentifier spriteIdentifier = getTextureIdFromSherd(sherds.front());
		queue.submitModelPart(
				this.front,
				matrices,
				spriteIdentifier.getRenderLayer(RenderLayers::entitySolid),
				light,
				overlay,
				this.materials.getSprite(spriteIdentifier),
				false,
				false,
				-1,
				null,
				i
		);
		SpriteIdentifier spriteIdentifier2 = getTextureIdFromSherd(sherds.back());
		queue.submitModelPart(
				this.back,
				matrices,
				spriteIdentifier2.getRenderLayer(RenderLayers::entitySolid),
				light,
				overlay,
				this.materials.getSprite(spriteIdentifier2),
				false,
				false,
				-1,
				null,
				i
		);
		SpriteIdentifier spriteIdentifier3 = getTextureIdFromSherd(sherds.left());
		queue.submitModelPart(
				this.left,
				matrices,
				spriteIdentifier3.getRenderLayer(RenderLayers::entitySolid),
				light,
				overlay,
				this.materials.getSprite(spriteIdentifier3),
				false,
				false,
				-1,
				null,
				i
		);
		SpriteIdentifier spriteIdentifier4 = getTextureIdFromSherd(sherds.right());
		queue.submitModelPart(
				this.right,
				matrices,
				spriteIdentifier4.getRenderLayer(RenderLayers::entitySolid),
				light,
				overlay,
				this.materials.getSprite(spriteIdentifier4),
				false,
				false,
				-1,
				null,
				i
		);
	}

	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrixStack = new MatrixStack();
		this.neck.collectVertices(matrixStack, consumer);
		this.top.collectVertices(matrixStack, consumer);
		this.bottom.collectVertices(matrixStack, consumer);
	}
}
