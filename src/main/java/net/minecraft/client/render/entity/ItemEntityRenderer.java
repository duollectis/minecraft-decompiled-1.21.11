package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code ItemEntityRenderer}.
 */
public class ItemEntityRenderer extends EntityRenderer<ItemEntity, ItemEntityRenderState> {

	private static final float ITEM_PIXEL = 0.0625F;
	private static final float ITEM_Y_OFFSET = 0.15F;
	private static final float ITEM_SCALE = 0.0625F;
	private final ItemModelManager itemModelManager;
	private final Random random = Random.create();

	public ItemEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.itemModelManager = context.getItemModelManager();
		this.shadowRadius = 0.15F;
		this.shadowOpacity = 0.75F;
	}

	public ItemEntityRenderState createRenderState() {
		return new ItemEntityRenderState();
	}

	public void updateRenderState(ItemEntity itemEntity, ItemEntityRenderState itemEntityRenderState, float f) {
		super.updateRenderState(itemEntity, itemEntityRenderState, f);
		itemEntityRenderState.uniqueOffset = itemEntity.uniqueOffset;
		itemEntityRenderState.update(itemEntity, itemEntity.getStack(), this.itemModelManager);
	}

	public void render(
			ItemEntityRenderState itemEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (!itemEntityRenderState.itemRenderState.isEmpty()) {
			matrixStack.push();
			Box box = itemEntityRenderState.itemRenderState.getModelBoundingBox();
			float f = -((float) box.minY) + 0.0625F;
			float
					g =
					MathHelper.sin(itemEntityRenderState.age / 10.0F + itemEntityRenderState.uniqueOffset) * 0.1F
							+ 0.1F;
			matrixStack.translate(0.0F, g + f, 0.0F);
			float h = ItemEntity.getRotation(itemEntityRenderState.age, itemEntityRenderState.uniqueOffset);
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(h));
			render(
					matrixStack,
					orderedRenderCommandQueue,
					itemEntityRenderState.light,
					itemEntityRenderState,
					this.random,
					box
			);
			matrixStack.pop();
			super.render(itemEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
		}
	}

	public static void render(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			ItemStackEntityRenderState state,
			Random random
	) {
		render(matrices, queue, light, state, random, state.itemRenderState.getModelBoundingBox());
	}

	public static void render(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			ItemStackEntityRenderState state,
			Random random,
			Box boundingBox
	) {
		int i = state.renderedAmount;
		if (i != 0) {
			random.setSeed(state.seed);
			ItemRenderState itemRenderState = state.itemRenderState;
			float f = (float) boundingBox.getLengthZ();
			if (f > 0.0625F) {
				itemRenderState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);

				for (int j = 1; j < i; j++) {
					matrices.push();
					float g = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
					float h = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
					float k = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
					matrices.translate(g, h, k);
					itemRenderState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
					matrices.pop();
				}
			}
			else {
				float l = f * 1.5F;
				matrices.translate(0.0F, 0.0F, -(l * (i - 1) / 2.0F));
				itemRenderState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
				matrices.translate(0.0F, 0.0F, l);

				for (int m = 1; m < i; m++) {
					matrices.push();
					float h = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
					float k = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
					matrices.translate(h, k, 0.0F);
					itemRenderState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
					matrices.pop();
					matrices.translate(0.0F, 0.0F, l);
				}
			}
		}
	}

	public static void renderStack(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			ItemStackEntityRenderState state,
			Random random
	) {
		Box box = state.itemRenderState.getModelBoundingBox();
		int i = state.renderedAmount;
		if (i != 0) {
			random.setSeed(state.seed);
			ItemRenderState itemRenderState = state.itemRenderState;
			float f = (float) box.getLengthZ();
			if (f > 0.0625F) {
				itemRenderState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);

				for (int j = 1; j < i; j++) {
					matrices.push();
					float g = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
					float h = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
					float k = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
					matrices.translate(g, h, k);
					itemRenderState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
					matrices.pop();
				}
			}
			else {
				float l = f * 1.5F;
				matrices.translate(0.0F, 0.0F, -(l * (i - 1) / 2.0F));
				itemRenderState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
				matrices.translate(0.0F, 0.0F, l);

				for (int m = 1; m < i; m++) {
					matrices.push();
					float h = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
					float k = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
					matrices.translate(h, k, 0.0F);
					itemRenderState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
					matrices.pop();
					matrices.translate(0.0F, 0.0F, l);
				}
			}
		}
	}
}
