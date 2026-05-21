package net.minecraft.client.render.command;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code OrderedRenderCommandQueueImpl}.
 */
public class OrderedRenderCommandQueueImpl implements OrderedRenderCommandQueue {

	private final Int2ObjectAVLTreeMap<BatchingRenderCommandQueue> batchingQueues = new Int2ObjectAVLTreeMap();

	public BatchingRenderCommandQueue getBatchingQueue(int i) {
		return (BatchingRenderCommandQueue) this.batchingQueues.computeIfAbsent(
				i,
				orderx -> new BatchingRenderCommandQueue(this)
		);
	}

	@Override
	public void submitShadowPieces(
			MatrixStack matrices,
			float shadowRadius,
			List<EntityRenderState.ShadowPiece> shadowPieces
	) {
		this.getBatchingQueue(0).submitShadowPieces(matrices, shadowRadius, shadowPieces);
	}

	@Override
	public void submitLabel(
			MatrixStack matrices,
			@Nullable Vec3d nameLabelPos,
			int y,
			Text label,
			boolean notSneaking,
			int light,
			double squaredDistanceToCamera,
			CameraRenderState cameraState
	) {
		this
				.getBatchingQueue(0)
				.submitLabel(
						matrices,
						nameLabelPos,
						y,
						label,
						notSneaking,
						light,
						squaredDistanceToCamera,
						cameraState
				);
	}

	@Override
	public void submitText(
			MatrixStack matrices,
			float x,
			float y,
			OrderedText text,
			boolean dropShadow,
			TextRenderer.TextLayerType layerType,
			int light,
			int color,
			int backgroundColor,
			int outlineColor
	) {
		this
				.getBatchingQueue(0)
				.submitText(matrices, x, y, text, dropShadow, layerType, light, color, backgroundColor, outlineColor);
	}

	@Override
	public void submitFire(MatrixStack matrices, EntityRenderState renderState, Quaternionf rotation) {
		this.getBatchingQueue(0).submitFire(matrices, renderState, rotation);
	}

	@Override
	public void submitLeash(MatrixStack matrices, EntityRenderState.LeashData leashData) {
		this.getBatchingQueue(0).submitLeash(matrices, leashData);
	}

	@Override
	public <S> void submitModel(
			Model<? super S> model,
			S state,
			MatrixStack matrices,
			RenderLayer renderLayer,
			int light,
			int overlay,
			int tintedColor,
			@Nullable Sprite sprite,
			int outlineColor,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay
	) {
		this
				.getBatchingQueue(0)
				.submitModel(
						model,
						state,
						matrices,
						renderLayer,
						light,
						overlay,
						tintedColor,
						sprite,
						outlineColor,
						crumblingOverlay
				);
	}

	@Override
	public void submitModelPart(
			ModelPart part,
			MatrixStack matrices,
			RenderLayer renderLayer,
			int light,
			int overlay,
			@Nullable Sprite sprite,
			boolean sheeted,
			boolean hasGlint,
			int tintedColor,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay,
			int i
	) {
		this
				.getBatchingQueue(0)
				.submitModelPart(
						part,
						matrices,
						renderLayer,
						light,
						overlay,
						sprite,
						sheeted,
						hasGlint,
						tintedColor,
						crumblingOverlay,
						i
				);
	}

	@Override
	public void submitBlock(MatrixStack matrices, BlockState state, int light, int overlay, int outlineColor) {
		this.getBatchingQueue(0).submitBlock(matrices, state, light, overlay, outlineColor);
	}

	@Override
	public void submitMovingBlock(MatrixStack matrices, MovingBlockRenderState state) {
		this.getBatchingQueue(0).submitMovingBlock(matrices, state);
	}

	@Override
	public void submitBlockStateModel(
			MatrixStack matrices,
			RenderLayer renderLayer,
			BlockStateModel model,
			float r,
			float g,
			float b,
			int light,
			int overlay,
			int outlineColor
	) {
		this
				.getBatchingQueue(0)
				.submitBlockStateModel(matrices, renderLayer, model, r, g, b, light, overlay, outlineColor);
	}

	@Override
	public void submitItem(
			MatrixStack matrices,
			ItemDisplayContext displayContext,
			int light,
			int overlay,
			int outlineColors,
			int[] tintLayers,
			List<BakedQuad> quads,
			RenderLayer renderLayer,
			ItemRenderState.Glint glintType
	) {
		this
				.getBatchingQueue(0)
				.submitItem(
						matrices,
						displayContext,
						light,
						overlay,
						outlineColors,
						tintLayers,
						quads,
						renderLayer,
						glintType
				);
	}

	@Override
	public void submitCustom(
			MatrixStack matrices,
			RenderLayer renderLayer,
			OrderedRenderCommandQueue.Custom customRenderer
	) {
		this.getBatchingQueue(0).submitCustom(matrices, renderLayer, customRenderer);
	}

	@Override
	public void submitCustom(OrderedRenderCommandQueue.LayeredCustom customRenderer) {
		this.getBatchingQueue(0).submitCustom(customRenderer);
	}

	public void clear() {
		this.batchingQueues.values().forEach(BatchingRenderCommandQueue::clear);
	}

	public void onNextFrame() {
		this.batchingQueues.values().removeIf(queue -> !queue.hasCommands());
		this.batchingQueues.values().forEach(BatchingRenderCommandQueue::onNextFrame);
	}

	public Int2ObjectAVLTreeMap<BatchingRenderCommandQueue> getBatchingQueues() {
		return this.batchingQueues;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BlendedModelCommand}.
	 */
	public record BlendedModelCommand<S>(
			OrderedRenderCommandQueueImpl.ModelCommand<S> model,
			RenderLayer renderType,
			Vector3f position
	) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BlockCommand}.
	 */
	public record BlockCommand(
			MatrixStack.Entry matricesEntry,
			BlockState state,
			int lightCoords,
			int overlayCoords,
			int outlineColor
	) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BlockStateModelCommand}.
	 */
	public record BlockStateModelCommand(
			MatrixStack.Entry matricesEntry,
			RenderLayer renderLayer,
			BlockStateModel model,
			float r,
			float g,
			float b,
			int lightCoords,
			int overlayCoords,
			int outlineColor
	) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CustomCommand}.
	 */
	public record CustomCommand(MatrixStack.Entry matricesEntry, OrderedRenderCommandQueue.Custom customRenderer) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FireCommand}.
	 */
	public record FireCommand(MatrixStack.Entry matricesEntry, EntityRenderState renderState, Quaternionf rotation) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ItemCommand}.
	 */
	public record ItemCommand(
			MatrixStack.Entry positionMatrix,
			ItemDisplayContext displayContext,
			int lightCoords,
			int overlayCoords,
			int outlineColor,
			int[] tintLayers,
			List<BakedQuad> quads,
			RenderLayer renderLayer,
			ItemRenderState.Glint glintType
	) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LabelCommand}.
	 */
	public record LabelCommand(
			Matrix4f matricesEntry,
			float x,
			float y,
			Text text,
			int lightCoords,
			int color,
			int backgroundColor,
			double distanceToCameraSq
	) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LeashCommand}.
	 */
	public record LeashCommand(Matrix4f matricesEntry, EntityRenderState.LeashData leashState) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ModelCommand}.
	 */
	public record ModelCommand<S>(
			MatrixStack.Entry matricesEntry,
			Model<? super S> model,
			S state,
			int lightCoords,
			int overlayCoords,
			int tintedColor,
			@Nullable Sprite sprite,
			int outlineColor,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay
	) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ModelPartCommand}.
	 */
	public record ModelPartCommand(
			MatrixStack.Entry matricesEntry,
			ModelPart modelPart,
			int lightCoords,
			int overlayCoords,
			@Nullable Sprite sprite,
			boolean sheeted,
			boolean hasGlint,
			int tintedColor,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay,
			int outlineColor
	) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code MovingBlockCommand}.
	 */
	public record MovingBlockCommand(Matrix4f matricesEntry, MovingBlockRenderState movingBlockRenderState) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ShadowPiecesCommand}.
	 */
	public record ShadowPiecesCommand(
			Matrix4f matricesEntry,
			float radius,
			List<EntityRenderState.ShadowPiece> pieces
	) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TextCommand}.
	 */
	public record TextCommand(
			Matrix4f matricesEntry,
			float x,
			float y,
			OrderedText text,
			boolean dropShadow,
			TextRenderer.TextLayerType layerType,
			int lightCoords,
			int color,
			int backgroundColor,
			int outlineColor
	) {
	}
}
