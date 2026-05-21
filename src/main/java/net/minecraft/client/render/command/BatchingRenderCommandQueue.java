package net.minecraft.client.render.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code BatchingRenderCommandQueue}.
 */
public class BatchingRenderCommandQueue implements RenderCommandQueue {

	private final List<OrderedRenderCommandQueueImpl.ShadowPiecesCommand> shadowPiecesCommands = new ArrayList<>();
	private final List<OrderedRenderCommandQueueImpl.FireCommand> fireCommands = new ArrayList<>();
	private final LabelCommandRenderer.Commands labelCommands = new LabelCommandRenderer.Commands();
	private final List<OrderedRenderCommandQueueImpl.TextCommand> textCommands = new ArrayList<>();
	private final List<OrderedRenderCommandQueueImpl.LeashCommand> leashCommands = new ArrayList<>();
	private final List<OrderedRenderCommandQueueImpl.BlockCommand> blockCommands = new ArrayList<>();
	private final List<OrderedRenderCommandQueueImpl.MovingBlockCommand> movingBlockCommands = new ArrayList<>();
	private final List<OrderedRenderCommandQueueImpl.BlockStateModelCommand>
			blockStateModelCommands =
			new ArrayList<>();
	private final List<OrderedRenderCommandQueueImpl.ItemCommand> itemCommands = new ArrayList<>();
	private final List<OrderedRenderCommandQueue.LayeredCustom> layeredCustomCommands = new ArrayList<>();
	private final ModelCommandRenderer.Commands modelCommands = new ModelCommandRenderer.Commands();
	private final ModelPartCommandRenderer.Commands modelPartCommands = new ModelPartCommandRenderer.Commands();
	private final CustomCommandRenderer.Commands customCommands = new CustomCommandRenderer.Commands();
	private final OrderedRenderCommandQueueImpl orderedQueueImpl;
	private boolean hasCommands = false;

	public BatchingRenderCommandQueue(OrderedRenderCommandQueueImpl orderedQueueImpl) {
		this.orderedQueueImpl = orderedQueueImpl;
	}

	@Override
	public void submitShadowPieces(
			MatrixStack matrices,
			float shadowRadius,
			List<EntityRenderState.ShadowPiece> shadowPieces
	) {
		this.hasCommands = true;
		MatrixStack.Entry entry = matrices.peek();
		this.shadowPiecesCommands.add(new OrderedRenderCommandQueueImpl.ShadowPiecesCommand(
				new Matrix4f(entry.getPositionMatrix()),
				shadowRadius,
				shadowPieces
		));
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
		this.hasCommands = true;
		this.labelCommands.add(
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
		this.hasCommands = true;
		this.textCommands
				.add(
						new OrderedRenderCommandQueueImpl.TextCommand(
								new Matrix4f(matrices.peek().getPositionMatrix()),
								x,
								y,
								text,
								dropShadow,
								layerType,
								light,
								color,
								backgroundColor,
								outlineColor
						)
				);
	}

	@Override
	public void submitFire(MatrixStack matrices, EntityRenderState renderState, Quaternionf rotation) {
		this.hasCommands = true;
		this.fireCommands.add(new OrderedRenderCommandQueueImpl.FireCommand(
				matrices.peek().copy(),
				renderState,
				rotation
		));
	}

	@Override
	public void submitLeash(MatrixStack matrices, EntityRenderState.LeashData leashData) {
		this.hasCommands = true;
		this.leashCommands.add(new OrderedRenderCommandQueueImpl.LeashCommand(
				new Matrix4f(matrices
						.peek()
						.getPositionMatrix()), leashData
		));
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
		this.hasCommands = true;
		OrderedRenderCommandQueueImpl.ModelCommand<S> modelCommand = new OrderedRenderCommandQueueImpl.ModelCommand<>(
				matrices.peek().copy(),
				model,
				state,
				light,
				overlay,
				tintedColor,
				sprite,
				outlineColor,
				crumblingOverlay
		);
		this.modelCommands.add(renderLayer, modelCommand);
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
		this.hasCommands = true;
		this.modelPartCommands
				.add(
						renderLayer,
						new OrderedRenderCommandQueueImpl.ModelPartCommand(
								matrices.peek().copy(),
								part,
								light,
								overlay,
								sprite,
								sheeted,
								hasGlint,
								tintedColor,
								crumblingOverlay,
								i
						)
				);
	}

	@Override
	public void submitBlock(MatrixStack matrices, BlockState state, int light, int overlay, int outlineColor) {
		this.hasCommands = true;
		this.blockCommands.add(new OrderedRenderCommandQueueImpl.BlockCommand(
				matrices.peek().copy(),
				state,
				light,
				overlay,
				outlineColor
		));
		MinecraftClient.getInstance()
		               .getBakedModelManager()
		               .getBlockEntityModelsSupplier()
		               .render(
				               state.getBlock(),
				               ItemDisplayContext.NONE,
				               matrices,
				               this.orderedQueueImpl,
				               light,
				               overlay,
				               outlineColor
		               );
	}

	@Override
	public void submitMovingBlock(MatrixStack matrices, MovingBlockRenderState state) {
		this.hasCommands = true;
		this.movingBlockCommands.add(new OrderedRenderCommandQueueImpl.MovingBlockCommand(
				new Matrix4f(matrices
						.peek()
						.getPositionMatrix()), state
		));
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
		this.hasCommands = true;
		this.blockStateModelCommands
				.add(new OrderedRenderCommandQueueImpl.BlockStateModelCommand(
						matrices.peek().copy(),
						renderLayer,
						model,
						r,
						g,
						b,
						light,
						overlay,
						outlineColor
				));
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
		this.hasCommands = true;
		this.itemCommands
				.add(
						new OrderedRenderCommandQueueImpl.ItemCommand(
								matrices.peek().copy(),
								displayContext,
								light,
								overlay,
								outlineColors,
								tintLayers,
								quads,
								renderLayer,
								glintType
						)
				);
	}

	@Override
	public void submitCustom(
			MatrixStack matrices,
			RenderLayer renderLayer,
			OrderedRenderCommandQueue.Custom customRenderer
	) {
		this.hasCommands = true;
		this.customCommands.add(matrices, renderLayer, customRenderer);
	}

	@Override
	public void submitCustom(OrderedRenderCommandQueue.LayeredCustom customRenderer) {
		this.hasCommands = true;
		this.layeredCustomCommands.add(customRenderer);
	}

	public List<OrderedRenderCommandQueueImpl.ShadowPiecesCommand> getShadowPiecesCommands() {
		return this.shadowPiecesCommands;
	}

	public List<OrderedRenderCommandQueueImpl.FireCommand> getFireCommands() {
		return this.fireCommands;
	}

	public LabelCommandRenderer.Commands getLabelCommands() {
		return this.labelCommands;
	}

	public List<OrderedRenderCommandQueueImpl.TextCommand> getTextCommands() {
		return this.textCommands;
	}

	public List<OrderedRenderCommandQueueImpl.LeashCommand> getLeashCommands() {
		return this.leashCommands;
	}

	public List<OrderedRenderCommandQueueImpl.BlockCommand> getBlockCommands() {
		return this.blockCommands;
	}

	public List<OrderedRenderCommandQueueImpl.MovingBlockCommand> getMovingBlockCommands() {
		return this.movingBlockCommands;
	}

	public List<OrderedRenderCommandQueueImpl.BlockStateModelCommand> getBlockStateModelCommands() {
		return this.blockStateModelCommands;
	}

	public ModelPartCommandRenderer.Commands getModelPartCommands() {
		return this.modelPartCommands;
	}

	public List<OrderedRenderCommandQueueImpl.ItemCommand> getItemCommands() {
		return this.itemCommands;
	}

	public List<OrderedRenderCommandQueue.LayeredCustom> getLayeredCustomCommands() {
		return this.layeredCustomCommands;
	}

	public ModelCommandRenderer.Commands getModelCommands() {
		return this.modelCommands;
	}

	public CustomCommandRenderer.Commands getCustomCommands() {
		return this.customCommands;
	}

	public boolean hasCommands() {
		return this.hasCommands;
	}

	public void clear() {
		this.shadowPiecesCommands.clear();
		this.fireCommands.clear();
		this.labelCommands.clear();
		this.textCommands.clear();
		this.leashCommands.clear();
		this.blockCommands.clear();
		this.movingBlockCommands.clear();
		this.blockStateModelCommands.clear();
		this.itemCommands.clear();
		this.layeredCustomCommands.clear();
		this.modelCommands.clear();
		this.customCommands.clear();
		this.modelPartCommands.clear();
	}

	public void onNextFrame() {
		this.modelCommands.nextFrame();
		this.modelPartCommands.nextFrame();
		this.customCommands.nextFrame();
		this.hasCommands = false;
	}
}
