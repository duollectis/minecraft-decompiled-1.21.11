package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.state.SignBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.OrderedText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Unit;
import net.minecraft.util.math.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code AbstractSignBlockEntityRenderer}.
 */
public abstract class AbstractSignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity, SignBlockEntityRenderState> {

	private static final int GLOWING_BLACK_TEXT_COLOR = -988212;
	private static final int MAX_COLORED_TEXT_OUTLINE_RENDER_DISTANCE = MathHelper.square(16);
	private final TextRenderer textRenderer;
	private final SpriteHolder spriteHolder;

	public AbstractSignBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		this.textRenderer = context.textRenderer();
		this.spriteHolder = context.spriteHolder();
	}

	protected abstract Model.SinglePartModel getModel(BlockState state, WoodType woodType);

	protected abstract SpriteIdentifier getTextureId(WoodType woodType);

	protected abstract float getSignScale();

	protected abstract float getTextScale();

	protected abstract Vec3d getTextOffset();

	/**
	 * Применяет transforms.
	 *
	 * @param matrices matrices
	 * @param blockRotationDegrees block rotation degrees
	 * @param state state
	 */
	protected abstract void applyTransforms(MatrixStack matrices, float blockRotationDegrees, BlockState state);

	public void render(
			SignBlockEntityRenderState signBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		BlockState blockState = signBlockEntityRenderState.blockState;
		AbstractSignBlock abstractSignBlock = (AbstractSignBlock) blockState.getBlock();
		Model.SinglePartModel singlePartModel = this.getModel(blockState, abstractSignBlock.getWoodType());
		this.render(
				signBlockEntityRenderState,
				matrixStack,
				blockState,
				abstractSignBlock,
				abstractSignBlock.getWoodType(),
				singlePartModel,
				signBlockEntityRenderState.crumblingOverlay,
				orderedRenderCommandQueue
		);
	}

	private void render(
			SignBlockEntityRenderState renderState,
			MatrixStack matrices,
			BlockState blockState,
			AbstractSignBlock block,
			WoodType woodType,
			Model.SinglePartModel model,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay,
			OrderedRenderCommandQueue queue
	) {
		matrices.push();
		this.applyTransforms(matrices, -block.getRotationDegrees(blockState), blockState);
		this.renderSign(matrices, renderState.lightmapCoordinates, woodType, model, crumblingOverlay, queue);
		this.renderText(renderState, matrices, queue, true);
		this.renderText(renderState, matrices, queue, false);
		matrices.pop();
	}

	protected void renderSign(
			MatrixStack matrices,
			int lightmapCoords,
			WoodType woodType,
			Model.SinglePartModel model,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay,
			OrderedRenderCommandQueue queue
	) {
		matrices.push();
		float f = this.getSignScale();
		matrices.scale(f, -f, -f);
		SpriteIdentifier spriteIdentifier = this.getTextureId(woodType);
		RenderLayer renderLayer = spriteIdentifier.getRenderLayer(model::getLayer);
		queue.submitModel(
				model,
				Unit.INSTANCE,
				matrices,
				renderLayer,
				lightmapCoords,
				OverlayTexture.DEFAULT_UV,
				-1,
				this.spriteHolder.getSprite(spriteIdentifier),
				0,
				crumblingOverlay
		);
		matrices.pop();
	}

	private void renderText(
			SignBlockEntityRenderState renderState,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			boolean front
	) {
		SignText signText = front ? renderState.frontText : renderState.backText;
		if (signText != null) {
			matrices.push();
			this.applyTextTransforms(matrices, front, this.getTextOffset());
			int i = getTextColor(signText);
			int j = 4 * renderState.textLineHeight / 2;
			OrderedText[] orderedTexts = signText.getOrderedMessages(
					renderState.filterText, textx -> {
						List<OrderedText> list = this.textRenderer.wrapLines(textx, renderState.maxTextWidth);
						return list.isEmpty() ? OrderedText.EMPTY : list.get(0);
					}
			);
			int k;
			boolean bl;
			int l;
			if (signText.isGlowing()) {
				k = signText.getColor().getSignColor();
				bl = k == DyeColor.BLACK.getSignColor() || renderState.renderTextOutline;
				l = 15728880;
			}
			else {
				k = i;
				bl = false;
				l = renderState.lightmapCoordinates;
			}

			for (int m = 0; m < 4; m++) {
				OrderedText orderedText = orderedTexts[m];
				float f = -this.textRenderer.getWidth(orderedText) / 2;
				queue.submitText(
						matrices,
						f,
						m * renderState.textLineHeight - j,
						orderedText,
						false,
						TextRenderer.TextLayerType.POLYGON_OFFSET,
						l,
						k,
						0,
						bl ? i : 0
				);
			}

			matrices.pop();
		}
	}

	private void applyTextTransforms(MatrixStack matrices, boolean front, Vec3d textOffset) {
		if (!front) {
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		}

		float f = 0.015625F * this.getTextScale();
		matrices.translate(textOffset);
		matrices.scale(f, -f, f);
	}

	private static boolean shouldRenderTextOutline(BlockPos pos) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
		if (clientPlayerEntity != null && minecraftClient.options.getPerspective().isFirstPerson()
				&& clientPlayerEntity.isUsingSpyglass()) {
			return true;
		}
		else {
			Entity entity = minecraftClient.getCameraEntity();
			return entity != null
					&& entity.squaredDistanceTo(Vec3d.ofCenter(pos)) < MAX_COLORED_TEXT_OUTLINE_RENDER_DISTANCE;
		}
	}

	public static int getTextColor(SignText text) {
		int i = text.getColor().getSignColor();
		return i == DyeColor.BLACK.getSignColor() && text.isGlowing() ? GLOWING_BLACK_TEXT_COLOR : ColorHelper.scaleRgb(i, 0.4F);
	}

	/**
	 * Создаёт render state.
	 *
	 * @return SignBlockEntityRenderState — результат операции
	 */
	public SignBlockEntityRenderState createRenderState() {
		return new SignBlockEntityRenderState();
	}

	public void updateRenderState(
			SignBlockEntity signBlockEntity,
			SignBlockEntityRenderState signBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				signBlockEntity,
				signBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		signBlockEntityRenderState.maxTextWidth = signBlockEntity.getMaxTextWidth();
		signBlockEntityRenderState.textLineHeight = signBlockEntity.getTextLineHeight();
		signBlockEntityRenderState.frontText = signBlockEntity.getFrontText();
		signBlockEntityRenderState.backText = signBlockEntity.getBackText();
		signBlockEntityRenderState.filterText = MinecraftClient.getInstance().shouldFilterText();
		signBlockEntityRenderState.renderTextOutline = shouldRenderTextOutline(signBlockEntity.getPos());
	}
}
