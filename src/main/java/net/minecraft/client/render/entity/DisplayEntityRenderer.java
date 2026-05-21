package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.render.entity.state.DisplayEntityRenderState;
import net.minecraft.client.render.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.render.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code DisplayEntityRenderer}.
 */
public abstract class DisplayEntityRenderer<T extends DisplayEntity, S, ST extends DisplayEntityRenderState> extends EntityRenderer<T, ST> {

	private final EntityRenderManager renderDispatcher;

	protected DisplayEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.renderDispatcher = context.getRenderDispatcher();
	}

	protected Box getBoundingBox(T displayEntity) {
		return displayEntity.getVisibilityBoundingBox();
	}

	protected boolean canBeCulled(T displayEntity) {
		return displayEntity.shouldRender();
	}

	private static int getBrightnessOverride(DisplayEntity entity) {
		DisplayEntity.RenderState renderState = entity.getRenderState();
		return renderState != null ? renderState.brightnessOverride() : -1;
	}

	protected int getSkyLight(T displayEntity, BlockPos blockPos) {
		int i = getBrightnessOverride(displayEntity);
		return i != -1 ? LightmapTextureManager.getSkyLightCoordinates(i) : super.getSkyLight(displayEntity, blockPos);
	}

	protected int getBlockLight(T displayEntity, BlockPos blockPos) {
		int i = getBrightnessOverride(displayEntity);
		return i != -1 ? LightmapTextureManager.getBlockLightCoordinates(i)
		               : super.getBlockLight(displayEntity, blockPos);
	}

	protected float getShadowRadius(ST displayEntityRenderState) {
		DisplayEntity.RenderState renderState = displayEntityRenderState.displayRenderState;
		return renderState == null ? 0.0F : renderState.shadowRadius().lerp(displayEntityRenderState.lerpProgress);
	}

	protected float getShadowOpacity(ST displayEntityRenderState) {
		DisplayEntity.RenderState renderState = displayEntityRenderState.displayRenderState;
		return renderState == null ? 0.0F : renderState.shadowStrength().lerp(displayEntityRenderState.lerpProgress);
	}

	public void render(
			ST displayEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		DisplayEntity.RenderState renderState = displayEntityRenderState.displayRenderState;
		if (renderState != null && displayEntityRenderState.canRender()) {
			float f = displayEntityRenderState.lerpProgress;
			super.render(displayEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
			matrixStack.push();
			matrixStack.multiply(this.getBillboardRotation(renderState, displayEntityRenderState, new Quaternionf()));
			AffineTransformation affineTransformation = renderState.transformation().interpolate(f);
			matrixStack.multiplyPositionMatrix(affineTransformation.getMatrix());
			this.render(
					displayEntityRenderState,
					matrixStack,
					orderedRenderCommandQueue,
					displayEntityRenderState.light,
					f
			);
			matrixStack.pop();
		}
	}

	private Quaternionf getBillboardRotation(DisplayEntity.RenderState renderState, ST state, Quaternionf rotation) {
		return switch (renderState.billboardConstraints()) {
			case FIXED -> rotation.rotationYXZ(
					(float) (-Math.PI / 180.0) * state.yaw,
					(float) (Math.PI / 180.0) * state.pitch,
					0.0F
			);
			case HORIZONTAL -> rotation.rotationYXZ(
					(float) (-Math.PI / 180.0) * state.yaw,
					(float) (Math.PI / 180.0) * getNegatedPitch(state.cameraPitch),
					0.0F
			);
			case VERTICAL -> rotation.rotationYXZ(
					(float) (-Math.PI / 180.0) * getBackwardsYaw(state.cameraYaw),
					(float) (Math.PI / 180.0) * state.pitch,
					0.0F
			);
			case CENTER -> rotation.rotationYXZ(
					(float) (-Math.PI / 180.0) * getBackwardsYaw(state.cameraYaw),
					(float) (Math.PI / 180.0) * getNegatedPitch(state.cameraPitch),
					0.0F
			);
		};
	}

	private static float getBackwardsYaw(float yaw) {
		return yaw - 180.0F;
	}

	private static float getNegatedPitch(float pitch) {
		return -pitch;
	}

	private static <T extends DisplayEntity> float lerpYaw(T entity, float deltaTicks) {
		return entity.getLerpedYaw(deltaTicks);
	}

	private static <T extends DisplayEntity> float lerpPitch(T entity, float deltaTicks) {
		return entity.getLerpedPitch(deltaTicks);
	}

	protected abstract void render(
			ST state,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			float tickProgress
	);

	public void updateRenderState(T displayEntity, ST displayEntityRenderState, float f) {
		super.updateRenderState(displayEntity, displayEntityRenderState, f);
		displayEntityRenderState.displayRenderState = displayEntity.getRenderState();
		displayEntityRenderState.lerpProgress = displayEntity.getLerpProgress(f);
		displayEntityRenderState.yaw = lerpYaw(displayEntity, f);
		displayEntityRenderState.pitch = lerpPitch(displayEntity, f);
		Camera camera = this.renderDispatcher.camera;
		displayEntityRenderState.cameraPitch = camera.getPitch();
		displayEntityRenderState.cameraYaw = camera.getYaw();
	}

	@Environment(EnvType.CLIENT)
	public static class BlockDisplayEntityRenderer
			extends DisplayEntityRenderer<DisplayEntity.BlockDisplayEntity, DisplayEntity.BlockDisplayEntity.Data, BlockDisplayEntityRenderState> {

		protected BlockDisplayEntityRenderer(EntityRendererFactory.Context context) {
			super(context);
		}

		public BlockDisplayEntityRenderState createRenderState() {
			return new BlockDisplayEntityRenderState();
		}

		public void updateRenderState(
				DisplayEntity.BlockDisplayEntity blockDisplayEntity,
				BlockDisplayEntityRenderState blockDisplayEntityRenderState,
				float f
		) {
			super.updateRenderState(blockDisplayEntity, blockDisplayEntityRenderState, f);
			blockDisplayEntityRenderState.data = blockDisplayEntity.getData();
		}

		public void render(
				BlockDisplayEntityRenderState blockDisplayEntityRenderState,
				MatrixStack matrixStack,
				OrderedRenderCommandQueue orderedRenderCommandQueue,
				int i,
				float f
		) {
			orderedRenderCommandQueue.submitBlock(
					matrixStack,
					blockDisplayEntityRenderState.data.blockState(),
					i,
					OverlayTexture.DEFAULT_UV,
					blockDisplayEntityRenderState.outlineColor
			);
		}
	}

	@Environment(EnvType.CLIENT)
	public static class ItemDisplayEntityRenderer
			extends DisplayEntityRenderer<DisplayEntity.ItemDisplayEntity, DisplayEntity.ItemDisplayEntity.Data, ItemDisplayEntityRenderState> {

		private final ItemModelManager itemModelManager;

		protected ItemDisplayEntityRenderer(EntityRendererFactory.Context context) {
			super(context);
			this.itemModelManager = context.getItemModelManager();
		}

		public ItemDisplayEntityRenderState createRenderState() {
			return new ItemDisplayEntityRenderState();
		}

		public void updateRenderState(
				DisplayEntity.ItemDisplayEntity itemDisplayEntity,
				ItemDisplayEntityRenderState itemDisplayEntityRenderState,
				float f
		) {
			super.updateRenderState(itemDisplayEntity, itemDisplayEntityRenderState, f);
			DisplayEntity.ItemDisplayEntity.Data data = itemDisplayEntity.getData();
			if (data != null) {
				this.itemModelManager
						.updateForNonLivingEntity(
								itemDisplayEntityRenderState.itemRenderState,
								data.itemStack(),
								data.itemTransform(),
								itemDisplayEntity
						);
			}
			else {
				itemDisplayEntityRenderState.itemRenderState.clear();
			}
		}

		public void render(
				ItemDisplayEntityRenderState itemDisplayEntityRenderState,
				MatrixStack matrixStack,
				OrderedRenderCommandQueue orderedRenderCommandQueue,
				int i,
				float f
		) {
			if (!itemDisplayEntityRenderState.itemRenderState.isEmpty()) {
				matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.PI));
				itemDisplayEntityRenderState.itemRenderState
						.render(
								matrixStack,
								orderedRenderCommandQueue,
								i,
								OverlayTexture.DEFAULT_UV,
								itemDisplayEntityRenderState.outlineColor
						);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static class TextDisplayEntityRenderer
			extends DisplayEntityRenderer<DisplayEntity.TextDisplayEntity, DisplayEntity.TextDisplayEntity.Data, TextDisplayEntityRenderState> {

		private final TextRenderer displayTextRenderer;

		protected TextDisplayEntityRenderer(EntityRendererFactory.Context context) {
			super(context);
			this.displayTextRenderer = context.getTextRenderer();
		}

		public TextDisplayEntityRenderState createRenderState() {
			return new TextDisplayEntityRenderState();
		}

		public void updateRenderState(
				DisplayEntity.TextDisplayEntity textDisplayEntity,
				TextDisplayEntityRenderState textDisplayEntityRenderState,
				float f
		) {
			super.updateRenderState(textDisplayEntity, textDisplayEntityRenderState, f);
			textDisplayEntityRenderState.data = textDisplayEntity.getData();
			textDisplayEntityRenderState.textLines = textDisplayEntity.splitLines(this::getLines);
		}

		private DisplayEntity.TextDisplayEntity.TextLines getLines(Text text, int width) {
			List<OrderedText> list = this.displayTextRenderer.wrapLines(text, width);
			List<DisplayEntity.TextDisplayEntity.TextLine> list2 = new ArrayList<>(list.size());
			int i = 0;

			for (OrderedText orderedText : list) {
				int j = this.displayTextRenderer.getWidth(orderedText);
				i = Math.max(i, j);
				list2.add(new DisplayEntity.TextDisplayEntity.TextLine(orderedText, j));
			}

			return new DisplayEntity.TextDisplayEntity.TextLines(list2, i);
		}

		public void render(
				TextDisplayEntityRenderState textDisplayEntityRenderState,
				MatrixStack matrixStack,
				OrderedRenderCommandQueue orderedRenderCommandQueue,
				int i,
				float f
		) {
			DisplayEntity.TextDisplayEntity.Data data = textDisplayEntityRenderState.data;
			byte b = data.flags();
			boolean bl = (b & 2) != 0;
			boolean bl2 = (b & 4) != 0;
			boolean bl3 = (b & 1) != 0;
			DisplayEntity.TextDisplayEntity.TextAlignment
					textAlignment =
					DisplayEntity.TextDisplayEntity.getAlignment(b);
			byte c = (byte) data.textOpacity().lerp(f);
			int j;
			if (bl2) {
				float g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
				j = (int) (g * 255.0F) << 24;
			}
			else {
				j = data.backgroundColor().lerp(f);
			}

			float g = 0.0F;
			Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
			matrix4f.rotate((float) Math.PI, 0.0F, 1.0F, 0.0F);
			matrix4f.scale(-0.025F, -0.025F, -0.025F);
			DisplayEntity.TextDisplayEntity.TextLines textLines = textDisplayEntityRenderState.textLines;
			int k = 1;
			int l = 9 + 1;
			int m = textLines.width();
			int n = textLines.lines().size() * l - 1;
			matrix4f.translate(1.0F - m / 2.0F, -n, 0.0F);
			if (j != 0) {
				orderedRenderCommandQueue.submitCustom(
						matrixStack,
						bl ? RenderLayers.textBackgroundSeeThrough() : RenderLayers.textBackground(),
						(matricesEntry, vertexConsumer) -> {
							vertexConsumer.vertex(matricesEntry, -1.0F, -1.0F, 0.0F).color(j).light(i);
							vertexConsumer.vertex(matricesEntry, -1.0F, (float) n, 0.0F).color(j).light(i);
							vertexConsumer.vertex(matricesEntry, (float) m, (float) n, 0.0F).color(j).light(i);
							vertexConsumer.vertex(matricesEntry, (float) m, -1.0F, 0.0F).color(j).light(i);
						}
				);
			}

			RenderCommandQueue renderCommandQueue = orderedRenderCommandQueue.getBatchingQueue(j != 0 ? 1 : 0);

			for (DisplayEntity.TextDisplayEntity.TextLine textLine : textLines.lines()) {
				float h = switch (textAlignment) {
					case LEFT -> 0.0F;
					case RIGHT -> m - textLine.width();
					case CENTER -> m / 2.0F - textLine.width() / 2.0F;
				};
				renderCommandQueue.submitText(
						matrixStack,
						h,
						g,
						textLine.contents(),
						bl3,
						bl ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.POLYGON_OFFSET,
						i,
						c << 24 | 16777215,
						0,
						0
				);
				g += l;
			}
		}
	}
}
